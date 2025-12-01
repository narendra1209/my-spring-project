package com.iisl.restcon.common;

import java.security.SecureRandom;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.googlecode.protobuf.format.JsonFormat;

import com.iisl.beans.common.RequestResponse.AccessTokenResponse;
import com.iisl.constants.CSConstants;
import com.iisl.constants.KeyConstants;

import com.iisl.controller.common.BaseController;
import com.iisl.controller.common.LoginController;
import com.iisl.controller.common.UserMgmtController;

import com.iisl.exception.ConversionException;
import com.iisl.exception.EncryptionException;

import com.iisl.services.common.ReqAttMapperService;
import com.iisl.systemlibrary.Util;
import com.iisl.utilities.common.MasterUtil;

import com.iisl.valueobjects.common.ResponseFormatter;
import com.iisl.valueobjects.common.ResponseVO;

import com.iisl.valueobjects.common.usermgmt.UserDomain;

/**
 * User Management REST Controller
 */
@RestController
@RequestMapping("/UserMgmtRstCtrl")
public class UserMgmtRstCtrl extends BaseController {

    @Autowired
    @Qualifier("loginController")
    private LoginController loginController;

    @Autowired
    @Qualifier("masterControllerMisc")
    private UserMgmtController userMgmtController;

    @Autowired
    private ReqAttMapperService reqAttMapperService;

    /* =========================== LOGIN =========================== */

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public ResponseVO validateUser(@RequestBody UserDomain user) {

        super.logLoginRequest(CSConstants.FUNCTION_LOGIN,
                CacheManager.menuMap.get(CSConstants.USR_ADMIN));

        String username = user.getUserId();
        String password = user.getUserPsswrd();

        HttpSession session = MasterUtil.getSession();
        String captcha = (String) session.getAttribute(CSConstants.CAPTCHA);
        String inputCaptcha = user.getCaptcha();
        String requestToken = user.getLoginToken();

        ResponseVO responseVO = new ResponseVO();

        if (!captcha.equals(inputCaptcha)) {
            ResponseFormatter.getResponseString(responseVO,
                    CSConstants.FAILURE_CODE, "Invalid Captcha");
            return responseVO;
        }

        if (session.getAttribute(CSConstants.ACCESS_TOKEN_NAME) == null) {
            ResponseFormatter.getResponseString(responseVO,
                    CSConstants.WARNING_CODE, "Please refresh your browser.");
            return responseVO;
        }

        try {
            password = Util.decryptStringAES(
                    password,
                    KeyConstants.key1,
                    session.getAttribute(CSConstants.ACCESS_TOKEN_NAME).toString()
            );

            requestToken = Util.decryptStringAES(
                    requestToken,
                    KeyConstants.key1,
                    session.getAttribute(CSConstants.ACCESS_TOKEN_NAME).toString()
            );

        } catch (EncryptionException e) {
            ResponseFormatter.getResponseString(
                    responseVO, CSConstants.FAILURE_CODE, "Invalid session");
            return responseVO;
        }

        responseVO = loginController.genericValidateUser(
                username, password, requestToken);

        super.logLoginResponse(CSConstants.FUNCTION_LOGIN,
                CacheManager.menuMap.get(CSConstants.USR_ADMIN), responseVO);

        return responseVO;
    }

    @RequestMapping(value = "/logout", method = RequestMethod.POST)
    public ResponseVO logoutUser(HttpSession session) {

        super.logLogoutRequest(CSConstants.FUNCTION_LOGIN,
                CacheManager.menuMap.get(CSConstants.USR_ADMIN));

        ResponseVO responseVO = loginController.logoutUser(session);

        super.logLogoutResponse(CSConstants.FUNCTION_LOGIN,
                CacheManager.menuMap.get(CSConstants.USR_ADMIN), responseVO);

        return responseVO;
    }

    @RequestMapping(value = "/checkSession", method = RequestMethod.POST)
    public ResponseVO checkSession() {

        ResponseVO responseVO = new ResponseVO();

        ResponseFormatter.getResponseString(
                responseVO,
                CSConstants.SUCCESS_CODE,
                CSConstants.NO_REASON_CODE,
                MasterUtil.getUserDomain()
        );

        return responseVO;
    }

    /* =========================== USER CRUD =========================== */

    @RequestMapping(value = "/user/addNewUser/", method = RequestMethod.POST)
    public ResponseVO addNewUser(@RequestBody UserDomain user) {

        super.logRequest(CSConstants.FUNCTION_ADD,
                CacheManager.menuMap.get(CSConstants.USER_MSTR_ADD));

        ResponseVO responseVO =
                this.userMgmtController.addNewUser(
                        user, MasterUtil.getUserDomain());

        super.logResponse(CSConstants.FUNCTION_ADD,
                CacheManager.menuMap.get(CSConstants.USER_MSTR_ADD), responseVO);

        return responseVO;
    }

    @RequestMapping(value = "/user/updateUser/", method = RequestMethod.POST)
    public ResponseVO updateUser(@RequestBody UserDomain user) {

        ResponseVO responseVO =
                this.userMgmtController.updateUser(
                        user, MasterUtil.getUserDomain());

        return responseVO;
    }

    @RequestMapping(value = "/user/searchUserId/", method = RequestMethod.POST)
    public ResponseVO getSchUserId() {
        return this.userMgmtController.getSchUserId();
    }

    /* =========================== GROUP & ROLE =========================== */

    @RequestMapping(value = "/user/getUserGroups/", method = RequestMethod.POST)
    public ResponseVO getUserGroups() {
        return this.userMgmtController.getUserGroups(false);
    }

    @RequestMapping(value = "/user/getRoles/", method = RequestMethod.POST)
    public ResponseVO getRoles() {
        return this.userMgmtController.loadAllRoles();
    }

    /* =========================== LOGIN TOKEN + CAPTCHA =========================== */

    @RequestMapping(value = "/user/getLoginToken/", method = RequestMethod.POST)
    public String getLoginToken(HttpSession session) {

        AccessTokenResponse.Builder responseBuilder =
                AccessTokenResponse.newBuilder();

        try {
            String accessToken = Util.getRandomAccessToken();

            responseBuilder.setResponseCode(CSConstants.SUCCESS_CODE);
            responseBuilder.setResponse("SUCCESS");
            responseBuilder.setAccessToken(accessToken);

            String captcha = generateCaptchaTextMethod2(6);
            responseBuilder.setCaptcha(captcha);

            session.setAttribute(CSConstants.CAPTCHA, captcha);
            session.setAttribute(CSConstants.ACCESS_TOKEN_NAME, accessToken);

            return JsonFormat.printToString(
                    responseBuilder.build());

        } catch (Exception e) {

            responseBuilder.setResponseCode(CSConstants.FAILURE_CODE);
            responseBuilder.setResponse("Access token generation failed");

            return JsonFormat.printToString(
                    responseBuilder.build());
        }
    }

    /* =========================== CAPTCHA =========================== */

    private static String generateCaptchaTextMethod2(int captchaLength) {

        String saltChars =
                "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890!@#$%*";

        StringBuilder captchaStrBuffer = new StringBuilder();
        SecureRandom secureRandom = new SecureRandom();

        while (captchaStrBuffer.length() < captchaLength) {
            int index =
                    (int) (secureRandom.nextFloat() * saltChars.length());
            captchaStrBuffer.append(
                    saltChars.substring(index, index + 1));
        }

        return captchaStrBuffer.toString();
    }
}

package com.iisl.controller.common;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.iisl.cache.common.CacheManager;
import com.iisl.constants.CSConstants;
import com.iisl.constants.MsgConstants;
import com.iisl.exception.ServiceException;
import com.iisl.service.common.usermgmt.LoginService;
import com.iisl.utilities.common.MasterUtil;
import com.iisl.utilities.common.ResponseFormatter;
import com.iisl.valueobjects.common.ResponseVO;

/**
 * Controller class for Login Maintenance / Session Handling
 *
 * NOTE:
 *  - Ye controller REST nahi hai, isko doosre controllers (UserMgmtRstCtrl)
 *    login/logout ke liye use karte hain.
 */
@Controller("loginController")
public class LoginController {

    @Autowired
    private LoginService iLoginService;

    /**
     * Generic user validation
     *
     * @param username     user id
     * @param password     password (already decrypted by REST controller)
     * @param requestToken request token (already decrypted)
     * @return ResponseVO
     */
    public ResponseVO genericValidateUser(String username,
                                          String password,
                                          String requestToken) {

        ResponseVO responseVO = new ResponseVO();
        List<HttpSession> sessions = null;

        try {
            CacheManager.LOGIN_LOGGER.info("Login requested by {}", username);

            // 1) Basic input validation
            if (!password.matches(CSConstants.REGX_PASSWORD)
                    || !username.matches(CSConstants.REGX_USERID)) {

                throw new ServiceException(MsgConstants.INVALID_USER_PASS);
            }

            // 2) Existing sessions check
            sessions = CacheManager.userSessionMap.get(username);

            if (sessions != null && !sessions.isEmpty()) {
                HttpSession existing = sessions.get(0);
                if (checkSession(existing)) {
                    ResponseFormatter.getResponseString(
                            responseVO,
                            CSConstants.FAILURE_CODE,
                            "You are already logged in"
                    );
                    return responseVO;
                } else {
                    // Purane dead sessions clean
                    sessions.clear();
                    CacheManager.userSessionMap.remove(username);
                }
            }

            // 3) Backend login (LDAP/DB/etc)
            Object retVal = iLoginService.doGenericLogin(username, password);

            // 4) Fresh session create karo
            HttpSession session = MasterUtil.getSession();
            try {
                session.invalidate();
            } catch (IllegalStateException ignore) {
                // already invalid
            }
            session = MasterUtil.getSession(true);

            session.setAttribute("Request_Token", requestToken);

            // 5) User set + responseVO fill
            iLoginService.setUser(retVal, username, responseVO, session);

            // 6) Keep session list in CacheManager
            sessions = new ArrayList<>();
            sessions.add(session);
            CacheManager.userSessionMap.put(username, sessions);

            CacheManager.LOGIN_LOGGER.info(
                    "Login successful for {} from {}",
                    username, MasterUtil.getUserIp());

        } catch (ServiceException e) {

            CacheManager.LOGIN_LOGGER.error(
                    "{} : ServiceException during login : {}",
                    username, e.getMessage(), e);

            ResponseFormatter.getResponseString(
                    responseVO,
                    CSConstants.FAILURE_CODE,
                    e.getMessage(),
                    CSConstants.USER_REASON_CODE,
                    null
            );

        } catch (Exception e) {

            CacheManager.LOGIN_LOGGER.error(
                    "{} : Error occurred during login !! error details:",
                    username, e);

            ResponseFormatter.getResponseString(
                    responseVO,
                    CSConstants.FAILURE_CODE,
                    CSConstants.MG_DATAACCESS_ERR,
                    CSConstants.USER_REASON_CODE,
                    null
            );
        }

        return responseVO;
    }

    /**
     * Check if session is still valid based on last access time
     */
    public boolean checkSession(HttpSession userSession) {
        boolean isValid = true;
        if (userSession == null) {
            return false;
        }

        long timeout = userSession.getMaxInactiveInterval(); // in seconds
        try {
            long lastAccess = userSession.getLastAccessedTime();
            long curTime = (new Date()).getTime();
            if ((curTime - lastAccess) > timeout * 1000L) {
                isValid = false;
            }
        } catch (Exception e) {
            isValid = false;
        }
        return isValid;
    }

    /**
     * Logout current user: session invalidate + entry remove from userSessionMap
     */
    public ResponseVO logoutUser(HttpSession session) {

        ResponseVO responseVO = new ResponseVO();

        try {
            if (session != null && session.getAttribute("user") != null) {

                String username = MasterUtil.getUserName();

                CacheManager.LOGIN_LOGGER.debug(
                        "Logging out user: {} < {} >",
                        username, MasterUtil.getUserIp());

                // Success response
                ResponseFormatter.getResponseString(
                        responseVO,
                        CSConstants.SUCCESS_CODE,
                        CSConstants.RSG_SUCCESS_RESP,
                        CSConstants.NO_REASON_CODE,
                        null
                );

                // Remove from global session map
                List<HttpSession> sessions =
                        CacheManager.userSessionMap.get(username);
                if (sessions != null) {
                    sessions.clear();
                    CacheManager.userSessionMap.remove(username);
                }

                try {
                    session.invalidate();
                } catch (IllegalStateException ignore) {
                    // already invalid
                }
            }

        } catch (Exception e) {
            CacheManager.LOGIN_LOGGER.debug(
                    "Logout failed for user: {} < {} >",
                    MasterUtil.getUserName(), MasterUtil.getUserIp(), e);

            ResponseFormatter.getResponseString(
                    responseVO,
                    CSConstants.FAILURE_CODE,
                    CSConstants.MG_SYSTEM_ERR,
                    CSConstants.NO_REASON_CODE,
                    null
            );
        }

        return responseVO;
    }
}

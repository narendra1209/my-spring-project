package com.iisl.filter;

import java.io.IOException;

import com.iisl.utilities.common.MasterUtil;
import com.iisl.util.ReqContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class ReqContextFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest httpReq) {
            MasterUtil.reqContext = new ReqContext(httpReq);
        }
        try {
            chain.doFilter(request, response);
        } finally {
            // Cleanup if needed
            MasterUtil.reqContext = null;
        }
    }
}

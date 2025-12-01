package com.iisl.filter;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

@Component
public class HSTSFilter implements Filter {

    private static final String HEADER_NAME = "Strict-Transport-Security";
    private static final String MAX_AGE_DIRECTIVE = "max-age=%s";
    private static final String INCLUDE_SUB_DOMAINS_DIRECTIVE = "includeSubDomains";
    private static final String PRELOAD_DIRECTIVE = "preload";

    private int maxAgeSeconds = 31536000; // 1 year
    private boolean includeSubDomains = true;
    private boolean preload = true;
    private String directives;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.directives = String.format(MAX_AGE_DIRECTIVE, this.maxAgeSeconds);
        if (includeSubDomains) {
            this.directives += "; " + INCLUDE_SUB_DOMAINS_DIRECTIVE;
        }
        if (preload) {
            this.directives += "; " + PRELOAD_DIRECTIVE;
        }
    }

    @Override
    public void doFilter(ServletRequest req,
                         ServletResponse res,
                         FilterChain chain)
            throws IOException, ServletException {

        if (res instanceof HttpServletResponse response) {
            response.addHeader(HEADER_NAME, this.directives);
        }

        chain.doFilter(req, res);
    }

    @Override
    public void destroy() {
    }
}

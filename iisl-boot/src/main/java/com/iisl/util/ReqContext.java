package com.iisl.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public class ReqContext {

    private final HttpServletRequest request;

    public ReqContext(HttpServletRequest request) {
        this.request = request;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public HttpSession getSession(boolean create) {
        return request.getSession(create);
    }

    public HttpSession getSession() {
        return request.getSession();
    }

    public Object getContext() {
        // Old code probably stored some context;
        // for now we just return the request itself.
        return request;
    }
}

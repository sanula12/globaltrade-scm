package com.globaltrade.web;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * CorsFilter — allows our React frontend (port 3000) to talk to
 * the Jakarta EE backend (port 8080) without browser "CORS" blocks.
 *
 * WHY IS THIS NEEDED?
 * Browsers enforce the "Same-Origin Policy": JavaScript can only call APIs
 * on the SAME origin (same host + port) as the page it came from.
 * Our React app runs on localhost:3000 but the API is on localhost:8080.
 * Different ports = different origin = browser blocks the request.
 *
 * CORS (Cross-Origin Resource Sharing) is the standard way to tell the
 * browser "it's okay, I trust this other origin".
 *
 * @WebFilter("/*") means this filter runs on every single request.
 */
@WebFilter("/*")
public class CorsFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  req  = (HttpServletRequest)  request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // Dynamically allow any localhost / 127.0.0.1 origin (any port).
        // This handles Vite bumping from 5173 → 5174 → 5175 etc. automatically.
        String origin = req.getHeader("Origin");
        if (origin != null && (origin.startsWith("http://localhost:") || origin.startsWith("http://127.0.0.1:"))) {
            resp.setHeader("Access-Control-Allow-Origin", origin);
        }

        // Allow the browser to send the Authorization header (needed for Basic Auth)
        resp.setHeader("Access-Control-Allow-Credentials", "true");

        // Allow these HTTP methods
        resp.setHeader("Access-Control-Allow-Methods",     "GET, POST, PUT, DELETE, OPTIONS");

        // Allow these headers in the request
        resp.setHeader("Access-Control-Allow-Headers",     "Content-Type, Authorization");

        // How long the browser should cache the preflight result (seconds)
        resp.setHeader("Access-Control-Max-Age",           "3600");

        // Handle the browser's "preflight" OPTIONS request
        // The browser sends OPTIONS before every cross-origin POST/PUT/DELETE
        // to ask "is this allowed?". We say yes and return immediately.
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        chain.doFilter(request, response);
    }

    @Override public void init(FilterConfig fc) {}
    @Override public void destroy() {}
}

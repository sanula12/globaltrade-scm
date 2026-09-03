package com.globaltrade.web;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * MeServlet — tells the frontend who you are and what roles you hold.
 *
 * WHY IS THIS NEEDED?
 * The browser sends a username + password, but after login the React app
 * has no idea which ROLE that user belongs to. Without knowing the role,
 * we can't show/hide buttons correctly (e.g. only ADMIN sees "Cancel").
 *
 * This servlet calls request.isUserInRole() — a built-in Jakarta EE method
 * that checks the security realm — and returns the roles as JSON.
 *
 * URL: GET /globaltrade/me
 */
@WebServlet(name = "MeServlet", urlPatterns = {"/me"})
public class MeServlet extends HttpServlet {

    private static final String[] ALL_ROLES = {
        "ADMIN",
        "LOGISTICS_COORDINATOR",
        "WAREHOUSE_MANAGER",
        "VENDOR_REPRESENTATIVE",
        "CUSTOMS_AGENT"
    };

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // request.getUserPrincipal() returns the logged-in user
        String username = request.getUserPrincipal() != null
                ? request.getUserPrincipal().getName()
                : "anonymous";

        // Check each role and collect the ones this user belongs to
        List<String> roles = new ArrayList<>();
        for (String role : ALL_ROLES) {
            if (request.isUserInRole(role)) {
                roles.add("\"" + role + "\"");
            }
        }

        out.print("{\"username\":\"" + username + "\","
                + "\"roles\":[" + String.join(",", roles) + "]}");
        response.setStatus(HttpServletResponse.SC_OK);
    }
}

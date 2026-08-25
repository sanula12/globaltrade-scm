package com.globaltrade.web;

import com.globaltrade.entity.Shipment;
import com.globaltrade.entity.ShipmentStatus;
import com.globaltrade.exception.ShipmentNotFoundException;
import com.globaltrade.service.ShipmentService;
import com.globaltrade.timer.ShipmentTrackingTimerBean;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import javax.servlet.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * ShipmentServlet — HTTP front-door for the supply chain system.
 *
 * @WebServlet maps this servlet to the URL /shipments
 * Full URL will be: http://localhost:8080/globaltrade/shipments
 *
 * KEY PATTERN: The servlet does NO business logic.
 * It only:
 *   1. Reads HTTP request parameters
 *   2. Calls EJB service methods
 *   3. Formats the response
 *
 * Business logic stays in EJBs — clean separation of concerns.
 *
 * @EJB injection works in Servlets too (not just in other EJBs).
 * GlassFish detects the @EJB annotation and injects the bean.
 */
@WebServlet(name = "ShipmentServlet", urlPatterns = {"/shipments"})
public class ShipmentServlet extends HttpServlet {

    /**
     * @EJB — container injects our stateless EJB.
     * We never call "new ShipmentService()" — the container does it.
     * This is Dependency Injection in action.
     */
    @EJB
    private ShipmentService shipmentService;

    @EJB
    private ShipmentTrackingTimerBean timerBean;

    /**
     * HTTP GET — retrieve shipment information.
     *
     * Usage:
     *   GET /globaltrade/shipments           → list all shipments
     *   GET /globaltrade/shipments?id=1      → get by ID
     *   GET /globaltrade/shipments?tracking=GT-2024-00001 → get by tracking number
     */
    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try {
            String idParam       = request.getParameter("id");
            String trackingParam = request.getParameter("tracking");
            String statusParam   = request.getParameter("status");

            if (idParam != null) {
                // GET /shipments?id=1
                Shipment s = shipmentService.findById(Long.parseLong(idParam));
                out.print(toJson(s));

            } else if (trackingParam != null) {
                // GET /shipments?tracking=GT-2024-00001
                Shipment s = shipmentService.findByTrackingNumber(trackingParam);
                out.print(toJson(s));

            } else if (statusParam != null) {
                // GET /shipments?status=DELAYED
             ShipmentStatus status =
                        ShipmentStatus.valueOf(statusParam.toUpperCase());
                List<Shipment> list = shipmentService.findByStatus(status);
                out.print(toJsonList(list));

            } else {
                // GET /shipments → all
                List<Shipment> all = shipmentService.findAllShipments();
                out.print(toJsonList(all));
            }

            response.setStatus(HttpServletResponse.SC_OK);

        } catch (ShipmentNotFoundException e) {
            // Application Exception — business error, return 404
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.print("{\"error\": \"" + e.getMessage() + "\"}");

        } catch (SecurityException | javax.ejb.EJBAccessException e) {
            // Security violation — return 403 Forbidden
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            out.print("{\"error\": \"Access denied: " + e.getMessage() + "\"}");

        } catch (Exception e) {
            // System error — return 500
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Internal server error: " + e.getMessage() + "\"}");
        }
    }

    /**
     * HTTP POST — create a new shipment.
     *
     * Usage:
     *   POST /globaltrade/shipments
     *   Parameters: trackingNumber, originCountry, destinationCountry, carrierName
     *
     * In a real system you'd parse a JSON body using JAX-RS.
     * For this prototype we use simple request parameters.
     */
    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try {
            String tracking    = request.getParameter("trackingNumber");
            String origin      = request.getParameter("originCountry");
            String destination = request.getParameter("destinationCountry");
            String carrier     = request.getParameter("carrierName");

            // Build the Shipment entity from request parameters
            Shipment shipment = new Shipment(tracking, origin, destination, carrier);

            // Delegate to EJB service — all transaction/security handled there
            Shipment created = shipmentService.createShipment(shipment);

            // Schedule a customs alert 48 hours from now
            timerBean.scheduleCustomsAlert(created.getId(), 48);

            response.setStatus(HttpServletResponse.SC_CREATED); // 201
            out.print("{\"message\": \"Shipment created\", \"id\": "
                    + created.getId() + "}");

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    // ---- Simple JSON helpers (no external library needed) ----

    private String toJson(Shipment s) {
        return "{"
                + "\"id\":"              + s.getId()                       + ","
                + "\"trackingNumber\":\"" + s.getTrackingNumber()         + "\","
                + "\"origin\":\""        + s.getOriginCountry()           + "\","
                + "\"destination\":\""   + s.getDestinationCountry()      + "\","
                + "\"status\":\""        + s.getStatus()                  + "\","
                + "\"carrier\":\""       + nullSafe(s.getCarrierName())   + "\","
                + "\"customsCleared\":"  + s.getCustomsCleared()
                + "}";
    }

    private String toJsonList(List<Shipment> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(toJson(list.get(i)));
            if (i < list.size() - 1) sb.append(",");
        }
        return sb.append("]").toString();
    }

    private String nullSafe(String val) {
        return val != null ? val : "";
    }
}
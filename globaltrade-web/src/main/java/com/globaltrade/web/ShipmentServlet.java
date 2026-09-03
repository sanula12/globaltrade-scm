package com.globaltrade.web;

import com.globaltrade.entity.Shipment;
import com.globaltrade.entity.ShipmentStatus;
import com.globaltrade.exception.ShipmentNotFoundException;
import com.globaltrade.service.ShipmentService;
import com.globaltrade.timer.ShipmentTrackingTimerBean;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBAccessException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * ShipmentServlet — HTTP front-door for the supply chain system.
 *
 * Supports:
 *   GET    /shipments              → list all
 *   GET    /shipments?id=1         → by ID
 *   GET    /shipments?tracking=X   → by tracking number
 *   GET    /shipments?status=X     → by status
 *   POST   /shipments              → create
 *   PUT    /shipments?id=1&status=DELIVERED  → update status
 *   PUT    /shipments?id=1&action=customs    → process customs
 *   DELETE /shipments?id=1         → cancel (ADMIN only)
 */
@WebServlet(name = "ShipmentServlet", urlPatterns = {"/shipments"})
public class ShipmentServlet extends HttpServlet {

    @EJB private ShipmentService shipmentService;
    @EJB private ShipmentTrackingTimerBean timerBean;

    // ── GET ────────────────────────────────────────────────────────────────────
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try {
            String idParam       = request.getParameter("id");
            String trackingParam = request.getParameter("tracking");
            String statusParam   = request.getParameter("status");

            if (idParam != null) {
                out.print(toJson(shipmentService.findById(Long.parseLong(idParam))));
            } else if (trackingParam != null) {
                out.print(toJson(shipmentService.findByTrackingNumber(trackingParam)));
            } else if (statusParam != null) {
                ShipmentStatus status = ShipmentStatus.valueOf(statusParam.toUpperCase());
                out.print(toJsonList(shipmentService.findByStatus(status)));
            } else {
                out.print(toJsonList(shipmentService.findAllShipments()));
            }
            response.setStatus(HttpServletResponse.SC_OK);

        } catch (ShipmentNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.print(err(e.getMessage()));
        } catch (SecurityException | EJBAccessException e) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            out.print(err("Access denied: " + e.getMessage()));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(err("Internal error: " + e.getMessage()));
        }
    }

    // ── POST (create) ──────────────────────────────────────────────────────────
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try {
            String tracking    = request.getParameter("trackingNumber");
            String origin      = request.getParameter("originCountry");
            String destination = request.getParameter("destinationCountry");
            String carrier     = request.getParameter("carrierName");

            Shipment shipment = new Shipment(tracking, origin, destination, carrier);
            Shipment created  = shipmentService.createShipment(shipment);
            timerBean.scheduleCustomsAlert(created.getId(), 48);

            response.setStatus(HttpServletResponse.SC_CREATED);
            out.print("{\"message\":\"Shipment created\",\"id\":" + created.getId() + "}");

        } catch (SecurityException | EJBAccessException e) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            out.print(err("Access denied: " + e.getMessage()));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(err(e.getMessage()));
        }
    }

    // ── PUT (update status / customs clearance) ────────────────────────────────
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try {
            String idParam     = request.getParameter("id");
            String statusParam = request.getParameter("status");
            String action      = request.getParameter("action");

            if (idParam == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(err("Missing id parameter"));
                return;
            }

            long id = Long.parseLong(idParam);

            if ("customs".equals(action)) {
                // Process customs clearance — only CUSTOMS_AGENT or ADMIN
                Shipment updated = shipmentService.processCustomsClearance(id);
                response.setStatus(HttpServletResponse.SC_OK);
                out.print("{\"message\":\"Customs cleared\",\"shipment\":" + toJson(updated) + "}");

            } else if (statusParam != null) {
                // Update status — LOGISTICS_COORDINATOR, WAREHOUSE_MANAGER, VENDOR_REPRESENTATIVE, ADMIN
                ShipmentStatus newStatus = ShipmentStatus.valueOf(statusParam.toUpperCase());
                Shipment updated = shipmentService.updateShipmentStatus(id, newStatus);
                response.setStatus(HttpServletResponse.SC_OK);
                out.print("{\"message\":\"Status updated\",\"shipment\":" + toJson(updated) + "}");

            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(err("Missing status or action parameter"));
            }

        } catch (ShipmentNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.print(err(e.getMessage()));
        } catch (SecurityException | EJBAccessException e) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            out.print(err("Access denied: " + e.getMessage()));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(err("Internal error: " + e.getMessage()));
        }
    }

    // ── DELETE (cancel) ────────────────────────────────────────────────────────
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try {
            String idParam = request.getParameter("id");
            if (idParam == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(err("Missing id parameter"));
                return;
            }

            shipmentService.cancelShipment(Long.parseLong(idParam));
            response.setStatus(HttpServletResponse.SC_OK);
            out.print("{\"message\":\"Shipment cancelled\"}");

        } catch (ShipmentNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.print(err(e.getMessage()));
        } catch (SecurityException | EJBAccessException e) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            out.print(err("Access denied — only ADMIN can cancel shipments"));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(err("Internal error: " + e.getMessage()));
        }
    }

    // ── JSON helpers ───────────────────────────────────────────────────────────
    private String toJson(Shipment s) {
        return "{"
                + "\"id\":"               + s.getId()                     + ","
                + "\"trackingNumber\":\"" + s.getTrackingNumber()         + "\","
                + "\"origin\":\""         + s.getOriginCountry()          + "\","
                + "\"destination\":\""    + s.getDestinationCountry()     + "\","
                + "\"status\":\""         + s.getStatus()                 + "\","
                + "\"carrier\":\""        + nullSafe(s.getCarrierName())  + "\","
                + "\"customsCleared\":"   + s.getCustomsCleared()
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

    private String err(String msg) {
        return "{\"error\":\"" + (msg != null ? msg.replace("\"", "'") : "unknown") + "\"}";
    }

    private String nullSafe(String val) { return val != null ? val : ""; }
}

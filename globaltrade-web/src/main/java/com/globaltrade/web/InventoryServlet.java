package com.globaltrade.web;

import com.globaltrade.entity.Inventory;
import com.globaltrade.service.InventoryService;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBAccessException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * InventoryServlet — REST-style endpoint for inventory management.
 *
 * GET  /inventory               → all inventory items
 * GET  /inventory?low=true      → low stock items only
 * POST /inventory               → add new inventory item (WAREHOUSE_MANAGER, ADMIN)
 * PUT  /inventory?sku=X&qty=50  → restock an item (WAREHOUSE_MANAGER, ADMIN)
 */
@WebServlet(name = "InventoryServlet", urlPatterns = {"/inventory"})
public class InventoryServlet extends HttpServlet {

    @EJB private InventoryService inventoryService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try {
            String low = request.getParameter("low");
            if ("true".equals(low)) {
                out.print(toJsonList(inventoryService.findLowStockItems()));
            } else {
                out.print(toJsonList(inventoryService.findAll()));
            }
            response.setStatus(HttpServletResponse.SC_OK);

        } catch (SecurityException | EJBAccessException e) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            out.print(err("Access denied"));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(err(e.getMessage()));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try {
            Inventory inv = new Inventory();
            inv.setProductSku(request.getParameter("sku"));
            inv.setProductName(request.getParameter("productName"));
            inv.setWarehouseLocation(request.getParameter("warehouseLocation"));

            String qtyStr = request.getParameter("quantity");
            inv.setQuantity(qtyStr != null ? Integer.parseInt(qtyStr) : 0);

            String threshStr = request.getParameter("reorderThreshold");
            inv.setReorderThreshold(threshStr != null ? Integer.parseInt(threshStr) : 10);

            Inventory created = inventoryService.addInventory(inv);
            response.setStatus(HttpServletResponse.SC_CREATED);
            out.print("{\"message\":\"Inventory added\",\"id\":" + created.getId() + "}");

        } catch (SecurityException | EJBAccessException e) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            out.print(err("Access denied"));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(err(e.getMessage()));
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try {
            String sku    = request.getParameter("sku");
            String qtyStr = request.getParameter("qty");

            if (sku == null || qtyStr == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(err("Missing sku or qty parameter"));
                return;
            }

            inventoryService.restockInventory(sku, Integer.parseInt(qtyStr));
            response.setStatus(HttpServletResponse.SC_OK);
            out.print("{\"message\":\"Restocked " + sku + " by " + qtyStr + " units\"}");

        } catch (SecurityException | EJBAccessException e) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            out.print(err("Access denied"));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(err(e.getMessage()));
        }
    }

    private String toJson(Inventory i) {
        boolean isLow = i.getQuantity() != null && i.getReorderThreshold() != null
                && i.getQuantity() <= i.getReorderThreshold();
        return "{"
                + "\"id\":"               + i.getId()                            + ","
                + "\"sku\":\""            + nullSafe(i.getProductSku())          + "\","
                + "\"productName\":\""    + nullSafe(i.getProductName())         + "\","
                + "\"warehouse\":\""      + nullSafe(i.getWarehouseLocation())   + "\","
                + "\"quantity\":"         + (i.getQuantity() != null ? i.getQuantity() : 0) + ","
                + "\"reorderThreshold\":" + (i.getReorderThreshold() != null ? i.getReorderThreshold() : 10) + ","
                + "\"isLowStock\":"       + isLow
                + "}";
    }

    private String toJsonList(List<Inventory> list) {
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

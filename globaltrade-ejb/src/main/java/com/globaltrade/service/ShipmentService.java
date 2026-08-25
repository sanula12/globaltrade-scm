package com.globaltrade.service;


import com.globaltrade.entity.Shipment;
import com.globaltrade.entity.ShipmentStatus;
import com.globaltrade.exception.CarrierSystemException;
import com.globaltrade.exception.CustomsException;
import com.globaltrade.exception.ShipmentNotFoundException;
import com.globaltrade.intercepter.AuditInterceptor;
import com.globaltrade.intercepter.PerformanceInterceptor;
import com.globaltrade.intercepter.ValidationInterceptor;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.*;
import javax.interceptor.Interceptors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

@Stateless
@Interceptors({AuditInterceptor.class, PerformanceInterceptor.class})
public class ShipmentService {

    private static final Logger logger =
            Logger.getLogger(ShipmentService.class.getName());


    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;
    
    @Resource
    private SessionContext sessionContext;

@TransactionAttribute(TransactionAttributeType.REQUIRED)
@RolesAllowed({"LOGISTICS_COORDINATOR", "ADMIN"})
public Shipment createShipment(Shipment shipment) {

    logger.info("Creating shipment" + shipment.getTrackingNumber());

    if(shipment.getTrackingNumber() == null || shipment.getTrackingNumber().isEmpty()) {
        shipment.setTrackingNumber("GT-" + UUID.randomUUID()
                .toString().substring(0, 8).toUpperCase());
    }

    List<Shipment> existing = em.createQuery(
            "SELECT s FROM Shipment s WHERE s.trackingNumber = :tn", Shipment.class
    ).setParameter("tn", shipment.getTrackingNumber()).getResultList();

    if (!existing.isEmpty()){
        throw new CarrierSystemException(
                "SYSTEM", "Duplicate tracking number: " + shipment.getTrackingNumber()
        );
        }

    em.persist(shipment);
    logger.info("Shipment created with tracking number: " + shipment.getTrackingNumber());
    return shipment;
}

@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@PermitAll
public Shipment findByTrackingNumber(String trackingNumber) {


    List<Shipment> results = em.createQuery(
            "SELECT s FROM Shipment s WHERE s.trackingNumber = :tn",
            Shipment.class
    ).setParameter("tn", trackingNumber).getResultList();

    if(results.isEmpty()){
        throw new ShipmentNotFoundException(trackingNumber);
    }
    return results.get(0);
}

@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@PermitAll
public Shipment findById(Long id) {

    Shipment shipment = em.find(Shipment.class, id);
    if(shipment == null){
        throw new ShipmentNotFoundException(id);
    }

    return shipment;
}

@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@RolesAllowed({"LOGISTICS_COORDINATOR", "ADMIN", "WAREHOUSE_MANAGER"})
public List<Shipment> findByStatus(ShipmentStatus status){

    return em.createQuery(
      "SELECT s FROM Shipment s WHERE s.status = :status ORDER BY s.createdAt DESC ",
      Shipment.class).setParameter("status", status).getResultList();
}

@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@RolesAllowed({"LOGISTICS_COORDINATOR", "ADMIN", "WAREHOUSE_MANAGER"})
public List<Shipment> findAllShipments() {

    return em.createQuery(
            "SELECT s FROM Shipment s ORDER BY s.createdAt DESC",
            Shipment.class
    ).getResultList();
}

@TransactionAttribute(TransactionAttributeType.REQUIRED)
@RolesAllowed({"LOGISTICS_COORDINATOR", "WAREHOUSE_MANAGER", "VENDOR_REPRESENTATIVE", "ADMIN"})
public Shipment updateShipmentStatus(Long shipmentId, ShipmentStatus newStatus){

    Shipment shipment = findById(shipmentId);

    if(sessionContext.isCallerInRole("VENDOR_REPRESENTATIVE")){

        if(newStatus != ShipmentStatus.CANCELLED){
            throw new EJBAccessException(
                    "Vendor representatives can only cancel shipments"
            );
        }
    }

    if(shipment.getStatus() == ShipmentStatus.DELIVERED ||
    shipment.getStatus() == ShipmentStatus.CANCELLED){
        throw new CarrierSystemException(
                "SYSTEM",
                "Cannot update a " + shipment.getStatus() + " shipment"
        );
    }

    shipment.setStatus(newStatus);

    if(newStatus == ShipmentStatus.DELIVERED){
        shipment.setActualDelivery(LocalDate.now());
    }

    Shipment updated = em.merge(shipment);
    logger.info("Shipment " + shipment.getTrackingNumber()
            + " status updated to " + newStatus);

    return updated;
}

@TransactionAttribute(TransactionAttributeType.REQUIRED)
@RolesAllowed({"CUSTOMS_AGENT", "ADMIN"})
public Shipment processCustomsClearance(Long shipmentId) {
    Shipment shipment = findById(shipmentId);
    if (shipment.getVendor() == null) {

        throw new CustomsException(
                shipment.getTrackingNumber(),
                "No vendor associated — customs documentation incomplete"
        );
    }
    if (shipment.getOriginCountry() == null) {
        throw new CustomsException(
                shipment.getTrackingNumber(),
                "Origin country missing from customs documentation"
        );
    }

    shipment.setCustomsCleared(true);
    if (shipment.getStatus() == ShipmentStatus.CUSTOMS_HOLD) {
        shipment.setStatus(ShipmentStatus.IN_TRANSIT);
    }
    return em.merge(shipment);
}

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @RolesAllowed({"ADMIN"})
    public void cancelShipment(Long shipmentId) {
        Shipment shipment = findById(shipmentId);
        shipment.setStatus(ShipmentStatus.CANCELLED);
        em.merge(shipment);
        logger.info("Shipment " + shipment.getTrackingNumber()
                + " cancelled by " + sessionContext.getCallerPrincipal().getName());
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @RolesAllowed({"ADMIN", "LOGISTICS_COORDINATOR"})
    public List<Shipment> findOverdueShipments() {
        return em.createQuery(
                        "SELECT s FROM Shipment s " +
                                "WHERE s.estimatedDelivery < CURRENT_DATE " +
                                "AND s.status NOT IN " +
                                "(:delivered, :cancelled)",
                        Shipment.class
                ).setParameter("delivered", ShipmentStatus.DELIVERED)
                .setParameter("cancelled", ShipmentStatus.CANCELLED)
                .getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @RolesAllowed({"ADMIN", "LOGISTICS_COORDINATOR"})
    public int markOverdueShipmentsAsDelayed() {
        int count = em.createQuery(
                        "UPDATE Shipment s SET s.status = :delayed " +
                                "WHERE s.estimatedDelivery < CURRENT_DATE " +
                                "AND s.status = :inTransit"
                ).setParameter("delayed",   ShipmentStatus.DELAYED)
                .setParameter("inTransit", ShipmentStatus.IN_TRANSIT)
                .executeUpdate();
        if (count > 0) {
            logger.warning("[TIMER] Marked " + count
                    + " shipments as DELAYED — customer notifications required!");
        }
        return count;
    }



}

package com.globaltrade.timer;

import com.globaltrade.entity.Shipment;
import com.globaltrade.entity.ShipmentStatus;
import com.globaltrade.service.ShipmentService;

import javax.annotation.Resource;
import javax.annotation.security.RunAs;
import javax.ejb.*;
import java.util.List;
import java.util.logging.Logger;


@Stateless
@RunAs("ADMIN")
public class ShipmentTrackingTimerBean {

    private static final Logger logger =
            Logger.getLogger(ShipmentTrackingTimerBean.class.getName());

    @EJB
    private ShipmentService shipmentService;

    @Resource
    private TimerService timerService;

    @Schedule(second = "0", minute = "0", hour = "*",
            persistent = true, info = "HOURLY_SHIPMENT_CHECK")
    public void checkOverdueShipments() {

        logger.info("[TIMER] Hourly shipment check triggered");

        try {

            int markedDelayed = shipmentService.markOverdueShipmentsAsDelayed();

            if (markedDelayed > 0) {
                logger.warning("[TIMER] " + markedDelayed
                        + " shipments marked DELAYED — escalation required!");
            }


            List<Shipment> delayedShipments =
                    shipmentService.findByStatus(ShipmentStatus.DELAYED);

            logger.info("[TIMER] Current delayed shipments: "
                    + delayedShipments.size());

            for (Shipment s : delayedShipments) {
                logger.warning("[TIMER] DELAYED: " + s.getTrackingNumber()
                        + " | From: " + s.getOriginCountry()
                        + " → " + s.getDestinationCountry()
                        + " | Carrier: " + s.getCarrierName());
            }

        } catch (Exception e) {

            logger.severe("[TIMER] Shipment check failed: " + e.getMessage());
        }
    }

    @Schedule(second = "0", minute = "0", hour = "6",
            dayOfWeek = "*", persistent = false,
            info = "DAILY_SHIPMENT_SUMMARY")
    public void generateDailyShipmentSummary() {

        logger.info("[TIMER] === Daily Shipment Summary Report ===");

        try {
            int pending    = shipmentService
                    .findByStatus(ShipmentStatus.PENDING).size();
            int inTransit  = shipmentService
                    .findByStatus(ShipmentStatus.IN_TRANSIT).size();
            int delayed    = shipmentService
                    .findByStatus(ShipmentStatus.DELAYED).size();
            int delivered  = shipmentService
                    .findByStatus(ShipmentStatus.DELIVERED).size();
            int hold       = shipmentService
                    .findByStatus(ShipmentStatus.CUSTOMS_HOLD).size();

            logger.info("[TIMER] Pending:       " + pending);
            logger.info("[TIMER] In Transit:    " + inTransit);
            logger.info("[TIMER] Customs Hold:  " + hold);
            logger.info("[TIMER] Delayed:       " + delayed);
            logger.info("[TIMER] Delivered:     " + delivered);
            logger.info("[TIMER] === End of Daily Report ===");

        } catch (Exception e) {
            logger.severe("[TIMER] Daily summary failed: " + e.getMessage());
        }
    }


    public void scheduleCustomsAlert(Long shipmentId, long hoursUntilAlert) {


        long delayMs = hoursUntilAlert * 60 * 60 * 1000;


        Timer timer = timerService.createTimer(
                delayMs,
                0,
                "CUSTOMS_DEADLINE_ALERT:" + shipmentId
        );

        logger.info("[TIMER] Customs deadline alert scheduled for shipment "
                + shipmentId + " — fires in " + hoursUntilAlert + " hours."
                + " Timer ID: " + timer.getHandle());
    }

    @Timeout
    public void handleTimerTimeout(Timer timer) {

        String timerInfo = (String) timer.getInfo();
        logger.info("[TIMER] Programmatic timer fired: " + timerInfo);

        if (timerInfo != null && timerInfo.startsWith("CUSTOMS_DEADLINE_ALERT:")) {

            String shipmentIdStr = timerInfo.replace("CUSTOMS_DEADLINE_ALERT:", "");

            try {
                Long shipmentId = Long.parseLong(shipmentIdStr);
                Shipment shipment = shipmentService.findById(shipmentId);

                logger.warning("[TIMER] ⚠ CUSTOMS DEADLINE ALERT ⚠");
                logger.warning("[TIMER] Shipment: " + shipment.getTrackingNumber());
                logger.warning("[TIMER] From: " + shipment.getOriginCountry()
                        + " → " + shipment.getDestinationCountry());
                logger.warning("[TIMER] Customs Cleared: "
                        + shipment.getCustomsCleared());

                if (!shipment.getCustomsCleared()) {
                    logger.severe("[TIMER] URGENT: Shipment "
                            + shipment.getTrackingNumber()
                            + " approaching customs deadline WITHOUT clearance!"
                            + " Immediate action required!");
                }

            } catch (Exception e) {
                logger.severe("[TIMER] Failed to process customs alert "
                        + "for shipment " + shipmentIdStr
                        + ": " + e.getMessage());
            }
        }
    }
}
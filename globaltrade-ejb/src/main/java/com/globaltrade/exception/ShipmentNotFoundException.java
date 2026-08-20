package com.globaltrade.exception;

import javax.ejb.ApplicationException;

@ApplicationException(rollback = false)
public class ShipmentNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String trackingNumber;

    public ShipmentNotFoundException(String trackingNumber ){
        super("Shipment not found for tracking number: " + trackingNumber);
        this.trackingNumber = trackingNumber;
    }

    public ShipmentNotFoundException(Long id){
        super("Shipment not found for id: " + id);
        this.trackingNumber = null;
    }

    public String getTrackingNumber(){
        return trackingNumber;
    }

}

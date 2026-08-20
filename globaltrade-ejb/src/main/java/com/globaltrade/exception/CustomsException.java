package com.globaltrade.exception;

import javax.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class CustomsException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private final String trackingNumber;
    private final String reason;

    public CustomsException(String trackingNumber, String reason) {

        super("Customs clearance failed for: " + trackingNumber + ": " + reason);
        this.trackingNumber = trackingNumber;
        this.reason = reason;

    }

    public String getTrackingNumber() { return trackingNumber; }
    public String getReason() { return reason; }

}

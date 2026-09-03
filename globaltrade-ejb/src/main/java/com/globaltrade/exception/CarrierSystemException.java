package com.globaltrade.exception;

import jakarta.ejb.ApplicationException;


public class CarrierSystemException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private final String carrierName;

    public CarrierSystemException(String carrierName,String message) {

        super("Carrier system error for: " + carrierName + ": " + message);
        this.carrierName = carrierName;
    }

    public CarrierSystemException(String carrierName, String message, Throwable cause) {

        super("Carrier system error for: " + carrierName + ": " + message, cause);
    this.carrierName = carrierName;
    }

    public String getCarrierName() { return carrierName; }
}

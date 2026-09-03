package com.globaltrade.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = false)
public class VendorValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private final String field;
    private final String reason;

    public VendorValidationException(String field, String reason) {

        super("Vendor validation error: " + field + " : " + reason);
        this.field = field;
        this.reason = reason;

    }

    public VendorValidationException(String message){
        super(message);
        this.field = "unknown";
        this.reason = message;
    }

    public String getField() {return field;}
    public String getReason() {return reason;}

}

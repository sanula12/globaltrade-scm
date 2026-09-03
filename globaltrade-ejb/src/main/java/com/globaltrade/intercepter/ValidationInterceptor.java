package com.globaltrade.intercepter;

import com.globaltrade.entity.Shipment;
import com.globaltrade.entity.Vendor;
import com.globaltrade.exception.VendorValidationException;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;
import java.util.logging.Logger;

public class ValidationInterceptor {

    private static final Logger logger = Logger.getLogger(ValidationInterceptor.class.getName());

    @AroundInvoke
    public Object validateParameter(InvocationContext context) throws Exception {

        logger.fine("[VALIDATION] Validating parameters for: "
                + context.getMethod().getName());

        Object[] parameters = context.getParameters();

        if(parameters != null){

            for(Object param : parameters){

                if(param instanceof Shipment){
                    validateShipment((Shipment) param);
                }

                if(param instanceof Vendor){
                    validateVendor((Vendor) param);
                }

                if(param instanceof String){
                    if(param == null || ((String) param).trim().isEmpty()){
                        throw new VendorValidationException(
                                "parameter",
                                "String parameter cannot be null or empty"
                        );
                    }
                }
            }
        }

        return context.proceed();
   }

private void validateShipment(Shipment shipment){

        if(shipment == null){
            throw new VendorValidationException("shipment", "Shipment cannot be null");
        }
    if (shipment.getTrackingNumber() == null
            || shipment.getTrackingNumber().trim().isEmpty()) {
        throw new VendorValidationException(
                "trackingNumber", "Tracking number is required"
        );
    }

    if (shipment.getOriginCountry() == null
            || shipment.getOriginCountry().trim().isEmpty()) {
        throw new VendorValidationException(
                "originCountry", "Origin country is required"
        );
    }
    if (shipment.getDestinationCountry() == null
            || shipment.getDestinationCountry().trim().isEmpty()) {
        throw new VendorValidationException(
                "destinationCountry", "Destination country is required"
        );
    }

    if (shipment.getOriginCountry()
            .equalsIgnoreCase(shipment.getDestinationCountry())) {
        throw new VendorValidationException(
                "destinationCountry",
                "Origin and destination countries cannot be the same"
        );
    }
    logger.fine("[VALIDATION] Shipment " + shipment.getTrackingNumber() + " is valid");
}

private void validateVendor(Vendor vendor){

        if(vendor == null){
            throw new VendorValidationException("vendor", "Vendor cannot be null");
        }

    if (vendor.getName() == null || vendor.getName().trim().isEmpty()) {
        throw new VendorValidationException("name", "Vendor name is required");
    }
    if (vendor.getVendorCode() == null
            || vendor.getVendorCode().trim().isEmpty()) {
        throw new VendorValidationException("vendorCode", "Vendor code is required");
    }
    if (vendor.getContactEmail() != null
            && !vendor.getContactEmail().contains("@")) {
        throw new VendorValidationException(
                "contactEmail", "Invalid email format"
        );
    }
    logger.fine("[VALIDATION] Vendor " + vendor.getName() + " is valid");
}

}

package com.globaltrade.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class InventoryShortageException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private final String productSku;
    private final int requested;
    private final int available;


    public InventoryShortageException(String productSku, int requested, int available) {

        super("Inventory shortage for product SKU [" +productSku+ "]:" + "requested:" +requested+ ", available:" +available);
        this.productSku = productSku;
        this.requested = requested;
        this.available = available;

    }

    public String getProductSku() {return productSku;}
    public int getRequested() {return requested;}
    public int getAvailable() {return available;}

}

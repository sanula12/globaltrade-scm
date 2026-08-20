-- ============================================================
-- GlobalTrade SCM Database Schema
-- Run this in MySQL to set up the database
-- ============================================================

CREATE DATABASE IF NOT EXISTS globaltrade_scm
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE globaltrade_scm;

-- ---- VENDORS TABLE ----
-- Represents companies that supply goods to GlobalTrade's clients
CREATE TABLE vendors (
                         id                 BIGINT PRIMARY KEY AUTO_INCREMENT,
                         vendor_code        VARCHAR(20)  UNIQUE NOT NULL,  -- e.g. VND-001
                         name               VARCHAR(100) NOT NULL,
                         country            VARCHAR(50),
                         contact_email      VARCHAR(100),
                         performance_score  DECIMAL(5,2) DEFAULT 100.00,  -- 0-100 rating
                         status             VARCHAR(20)  DEFAULT 'ACTIVE', -- ACTIVE, SUSPENDED, UNDER_REVIEW
                         created_at         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- ---- INVENTORY TABLE ----
-- Stock levels at warehouses around the world
CREATE TABLE inventory (
                           id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
                           product_sku         VARCHAR(50)  NOT NULL,   -- Stock Keeping Unit code
                           product_name        VARCHAR(100) NOT NULL,
                           warehouse_location  VARCHAR(100) NOT NULL,
                           quantity            INT          DEFAULT 0,
                           reorder_threshold   INT          DEFAULT 10,  -- Alert when stock falls below this
                           unit_price          DECIMAL(10,2),
                           last_updated        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ---- SHIPMENTS TABLE ----
-- Core table: every shipment GlobalTrade manages
CREATE TABLE shipments (
                           id                   BIGINT PRIMARY KEY AUTO_INCREMENT,
                           tracking_number      VARCHAR(50)  UNIQUE NOT NULL,  -- e.g. GT-2024-00001
                           origin_country       VARCHAR(50)  NOT NULL,
                           destination_country  VARCHAR(50)  NOT NULL,
                           status               VARCHAR(30)  DEFAULT 'PENDING',
    -- Possible statuses: PENDING, IN_TRANSIT, CUSTOMS_HOLD, DELIVERED, DELAYED, CANCELLED
                           carrier_name         VARCHAR(100),
                           estimated_delivery   DATE,
                           actual_delivery      DATE,
                           vendor_id            BIGINT,
                           customs_cleared      BOOLEAN      DEFAULT FALSE,
                           created_at           TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
                           FOREIGN KEY (vendor_id) REFERENCES vendors(id)
);

-- ---- AUDIT LOG TABLE ----
-- Every EJB method call gets logged here by our AuditInterceptor
-- This satisfies customs compliance requirements!
CREATE TABLE audit_log (
                           id             BIGINT PRIMARY KEY AUTO_INCREMENT,
                           method_name    VARCHAR(200) NOT NULL,  -- e.g. "ShipmentService.createShipment"
                           caller_user    VARCHAR(100),           -- who triggered it (logged-in user)
                           called_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           duration_ms    BIGINT,                 -- how many milliseconds the method took
                           success        BOOLEAN DEFAULT TRUE,
                           error_message  TEXT                    -- if it failed, what went wrong
);

-- ---- SAMPLE DATA ----
INSERT INTO vendors (vendor_code, name, country, contact_email, performance_score, status)
VALUES
    ('VND-001', 'AsiaTech Suppliers', 'China', 'contact@asiatech.com', 95.50, 'ACTIVE'),
    ('VND-002', 'EuroLogistics GmbH', 'Germany', 'ops@eurologistics.de', 88.00, 'ACTIVE'),
    ('VND-003', 'AmeriShip Corp', 'USA', 'support@ameriship.com', 72.30, 'UNDER_REVIEW');

INSERT INTO inventory (product_sku, product_name, warehouse_location, quantity, reorder_threshold, unit_price)
VALUES
    ('SKU-ELEC-001', 'Industrial Sensors', 'Singapore-WH1', 250, 50, 149.99),
    ('SKU-MECH-002', 'Steel Bearings Grade-A', 'Rotterdam-WH2', 8, 20, 34.50),
    ('SKU-CHEM-003', 'Industrial Lubricant 5L', 'Dubai-WH3', 5, 15, 89.00);

INSERT INTO shipments (tracking_number, origin_country, destination_country, status, carrier_name, estimated_delivery, vendor_id)
VALUES
    ('GT-2024-00001', 'China', 'Germany', 'IN_TRANSIT', 'DHL Express', '2024-09-15', 1),
    ('GT-2024-00002', 'USA', 'Singapore', 'PENDING', 'FedEx', '2024-09-20', 3),
    ('GT-2024-00003', 'Germany', 'India', 'CUSTOMS_HOLD', 'Maersk', '2024-09-10', 2);
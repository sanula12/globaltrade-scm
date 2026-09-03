package com.globaltrade.service;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@Singleton
@Startup
public class ConfigurationBean {

    private static final Logger logger =
            Logger.getLogger(ConfigurationBean.class.getName());


    private Map<String, String> config = new HashMap<>();

    @PostConstruct
    public void initialize() {
        logger.info("=== GlobalTrade SCM Configuration Bean Initializing ===");

        config.put("performance.warning.threshold.ms",  "1000");
        config.put("performance.critical.threshold.ms", "5000");
        config.put("vendor.performance.review.threshold", "60.0");
        config.put("inventory.low.stock.alert.enabled",   "true");
        config.put("shipment.overdue.check.enabled",      "true");
        config.put("audit.log.enabled",                   "true");
        config.put("max.shipments.per.vendor",            "1000");

        logger.info("=== GlobalTrade SCM Configuration Loaded — "
                + config.size() + " settings ===");
    }

    public String get(String key) {
        return config.getOrDefault(key, "");
    }

    public String get(String key, String defaultValue) {
        return config.getOrDefault(key, defaultValue);
    }

    public long getLong(String key, long defaultValue) {
        try {
            return Long.parseLong(config.getOrDefault(key,
                    String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public Map<String, String> getAllConfig() {
        return new HashMap<>(config);
    }
}

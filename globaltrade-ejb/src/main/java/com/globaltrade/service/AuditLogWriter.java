package com.globaltrade.service;

import com.globaltrade.entity.AuditLog;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;


@Stateless
public class AuditLogWriter {

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void write(String methodName, String callUser, long duration,
                      boolean success, String errorMessage) {
        AuditLog log = new AuditLog(methodName, callUser, duration, success, errorMessage);
        em.persist(log);
    }
}
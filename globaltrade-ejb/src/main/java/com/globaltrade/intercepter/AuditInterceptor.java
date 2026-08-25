package com.globaltrade.intercepter;

import com.globaltrade.entity.AuditLog;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.logging.Logger;

public class AuditInterceptor {

    private static final Logger logger = Logger.getLogger(AuditInterceptor.class.getName());

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @Resource
    private SessionContext sessionContext;

    @AroundInvoke
    public Object auditMethodCall(InvocationContext context) throws Exception{

        String methodName = context.getTarget().getClass().getSimpleName()
                + "." + context.getMethod().getName();

        String callUser = "anonymous";
        try {
            if(sessionContext != null && sessionContext.getCallerPrincipal() != null ){

                callUser = sessionContext.getCallerPrincipal().getName();
            }


        }catch (Exception e){
            callUser = "system-timer";
        }

        logger.info("[AUDIT] " + callUser + "calling " + methodName);

        long startTime = System.currentTimeMillis();
        boolean success = true;
        String errorMessage = null;

        try{

            Object result = context.proceed();

            return result;

        } catch (Exception e){

            success = false;
            errorMessage = e.getClass().getSimpleName() + ": " + e.getMessage();
            logger.warning("[AUDIT] " + methodName + " FAILED: " + errorMessage);
            throw e;
        }finally {

            long duration = System.currentTimeMillis() - startTime;

            saveAuditLog(methodName,callUser,duration,success,errorMessage);

            logger.info("[AUDIT] " + methodName + " completed in "
                    + duration + "ms, success=" + success);

        }


    }

    private void saveAuditLog(String methodName, String callUser, long duration, boolean success, String errorMessage) {

        try{
            if(em != null){
                AuditLog log = new AuditLog(methodName, callUser, duration, success, errorMessage);
                em.persist(log);
            }
        }catch (Exception e){
            logger.severe("[AUDIT] Failed to save audit log to DB: " + e.getMessage());
        }
    }
}

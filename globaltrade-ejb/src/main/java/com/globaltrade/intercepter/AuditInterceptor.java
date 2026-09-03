package com.globaltrade.intercepter;

import com.globaltrade.entity.AuditLog;

import com.globaltrade.service.AuditLogWriter;
import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.logging.Logger;

public class AuditInterceptor {

    private static final Logger logger = Logger.getLogger(AuditInterceptor.class.getName());

    @EJB
    private AuditLogWriter auditLogWriter;

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
        try {
            if (auditLogWriter != null) {
                auditLogWriter.write(methodName, callUser, duration, success, errorMessage);
            }
        } catch (Exception e) {
            logger.severe("[AUDIT] Failed to save audit log to DB: " + e.getMessage());
        }
    }
}

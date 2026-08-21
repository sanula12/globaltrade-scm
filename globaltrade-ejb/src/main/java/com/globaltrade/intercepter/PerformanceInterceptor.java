package com.globaltrade.intercepter;


import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import java.util.logging.Logger;

public class PerformanceInterceptor {

    private static final Logger logger = Logger.getLogger(PerformanceInterceptor.class.getName());

    private static final long WARNING_THRESHOLD_MS  = 1000;  // 1 second
    private static final long CRITICAL_THRESHOLD_MS = 5000;  // 5 seconds

    @AroundInvoke
    public Object monitorPerformance(InvocationContext context) throws Exception {

        String methodName = context.getTarget().getClass().getSimpleName() + "." + context.getMethod().getName();

        long startTime =  System.nanoTime();

        try{
            return context.proceed();
        }finally {

            long durationMs = (System.nanoTime() - startTime) / 1_000_000;

            if (durationMs >= CRITICAL_THRESHOLD_MS) {
                logger.severe(
                        "[PERFORMANCE] CRITICAL: " + methodName +
                                " took " + durationMs + "ms — EXCEEDS " +
                                CRITICAL_THRESHOLD_MS + "ms threshold! " +
                                "Investigate immediately — may impact delivery SLAs."
                );
            } else if (durationMs >= WARNING_THRESHOLD_MS) {
                logger.warning(
                        "[PERFORMANCE] SLOW: " + methodName +
                                " took " + durationMs + "ms — exceeds " +
                                WARNING_THRESHOLD_MS + "ms threshold."
                );
            } else {
                logger.fine(
                        "[PERFORMANCE] OK: " + methodName +
                                " completed in " + durationMs + "ms"
                );
            }
        }

    }



}


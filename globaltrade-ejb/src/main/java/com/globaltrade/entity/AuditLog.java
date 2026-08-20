package com.globaltrade.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
public class AuditLog implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "method_name", nullable = false, length = 200)
    private String methodName;

    @Column(name = "caller_user", length = 100)
    private String callerUser;

    @Column(name = "called_at")
    private LocalDateTime calledAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "success")
    private Boolean success = true;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @PrePersist
    protected void onCreate() {
        if (this.calledAt == null) {
            this.calledAt = LocalDateTime.now();
        }
    }

    public AuditLog() {}

    public AuditLog(String methodName, String callerUser,
                    Long durationMs, Boolean success, String errorMessage) {
        this.methodName   = methodName;
        this.callerUser   = callerUser;
        this.calledAt     = LocalDateTime.now();
        this.durationMs   = durationMs;
        this.success      = success;
        this.errorMessage = errorMessage;
    }

    public Long getId() { return id; }

    public String getMethodName() { return methodName; }

    public void setMethodName(String methodName) { this.methodName = methodName; }

    public String getCallerUser() { return callerUser; }

    public void setCallerUser(String callerUser) { this.callerUser = callerUser; }

    public LocalDateTime getCalledAt() { return calledAt; }

    public void setCalledAt(LocalDateTime calledAt) { this.calledAt = calledAt; }

    public Long getDurationMs() { return durationMs; }

    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

    public Boolean getSuccess() { return success; }

    public void setSuccess(Boolean success) { this.success = success; }

    public String getErrorMessage() { return errorMessage; }

    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    @Override
    public String toString() {
        return "AuditLog{method='" + methodName + "', user='" + callerUser
                + "', success=" + success + ", duration=" + durationMs + "ms}";
    }

}

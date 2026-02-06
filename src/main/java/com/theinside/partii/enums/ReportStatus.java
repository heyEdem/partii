package com.theinside.partii.enums;

/**
 * Tracks the status of a user report submitted to moderation.
 */
public enum ReportStatus {
    /**
     * Report has been submitted and is awaiting review by an admin.
     */
    PENDING,

    /**
     * Admin is currently reviewing the report.
     */
    UNDER_REVIEW,

    /**
     * Report was investigated and found to be valid - action was taken.
     */
    RESOLVED,

    /**
     * Report was investigated and found to be invalid or unsubstantiated.
     */
    DISMISSED,

    /**
     * Report is awaiting more information before decision.
     */
    ON_HOLD
}

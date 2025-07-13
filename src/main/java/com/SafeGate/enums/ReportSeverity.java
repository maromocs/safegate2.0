package com.SafeGate.enums;

public enum ReportSeverity {
    INFO,       // purely informational, no action needed
    LOW,        // minor issue, maybe noteworthy
    MEDIUM,     // requires review or some remediation
    HIGH,       // serious problem, action recommended ASAP
    CRITICAL    // urgent, potentially catastrophic
}

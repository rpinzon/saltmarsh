package com.saltmarsh.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "saltmarsh")
public record SaltmarshProperties(Cancellation cancellation, Security security) {

    public record Cancellation(int freeCancelHours, int lateFeePercent) {}

    public record Security(boolean headersEnabled) {}
}

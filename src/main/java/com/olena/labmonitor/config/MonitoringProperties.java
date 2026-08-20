package com.olena.labmonitor.config;

import com.olena.labmonitor.alert.AlertSeverity;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@ConfigurationProperties("labmonitor.monitoring")
public class MonitoringProperties {

    private final Alerts alerts = new Alerts();
    private final Readings readings = new Readings();
    private final Sensors sensors = new Sensors();

    public Alerts getAlerts() { return alerts; }
    public Readings getReadings() { return readings; }
    public Sensors getSensors() { return sensors; }

    public static class Alerts {
        private BigDecimal lowMaxPercent = new BigDecimal("5");
        private BigDecimal mediumMaxPercent = new BigDecimal("15");
        private BigDecimal highMaxPercent = new BigDecimal("30");
        private Duration autoRecoveryMaxDuration = Duration.ofMinutes(5);
        private Set<AlertSeverity> autoRecoverySeverities = new LinkedHashSet<>(
                Set.of(AlertSeverity.LOW, AlertSeverity.MEDIUM)
        );

        public BigDecimal getLowMaxPercent() { return lowMaxPercent; }
        public void setLowMaxPercent(BigDecimal value) { lowMaxPercent = value; }
        public BigDecimal getMediumMaxPercent() { return mediumMaxPercent; }
        public void setMediumMaxPercent(BigDecimal value) { mediumMaxPercent = value; }
        public BigDecimal getHighMaxPercent() { return highMaxPercent; }
        public void setHighMaxPercent(BigDecimal value) { highMaxPercent = value; }
        public Duration getAutoRecoveryMaxDuration() { return autoRecoveryMaxDuration; }
        public void setAutoRecoveryMaxDuration(Duration value) { autoRecoveryMaxDuration = value; }
        public Set<AlertSeverity> getAutoRecoverySeverities() { return autoRecoverySeverities; }
        public void setAutoRecoverySeverities(Set<AlertSeverity> value) { autoRecoverySeverities = value; }
    }

    public static class Readings {
        private Duration historyDefaultPeriod = Duration.ofHours(24);
        private int historyMaxResults = 1000;
        private List<Duration> historyPeriods = List.of(
                Duration.ofHours(1), Duration.ofHours(24), Duration.ofDays(7), Duration.ofDays(30)
        );

        public Duration getHistoryDefaultPeriod() { return historyDefaultPeriod; }
        public void setHistoryDefaultPeriod(Duration value) { historyDefaultPeriod = value; }
        public int getHistoryMaxResults() { return historyMaxResults; }
        public void setHistoryMaxResults(int value) { historyMaxResults = value; }
        public List<Duration> getHistoryPeriods() { return historyPeriods; }
        public void setHistoryPeriods(List<Duration> value) { historyPeriods = value; }
    }

    public static class Sensors {
        private Duration offlineAfter = Duration.ofMinutes(2);

        public Duration getOfflineAfter() { return offlineAfter; }
        public void setOfflineAfter(Duration value) { offlineAfter = value; }
    }
}

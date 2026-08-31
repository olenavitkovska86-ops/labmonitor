package com.olena.labmonitor.alert;

import com.olena.labmonitor.alert.dto.AlertResponse;
import com.olena.labmonitor.alert.dto.AlertCountResponse;
import com.olena.labmonitor.alert.dto.ResolveAlertRequest;
import com.olena.labmonitor.alert.dto.ReopenAlertRequest;
import com.olena.labmonitor.alert.dto.AlertHistoryResponse;
import com.olena.labmonitor.alert.history.AlertHistory;
import com.olena.labmonitor.alert.history.AlertHistoryAction;
import com.olena.labmonitor.alert.history.AlertHistoryRepository;
import com.olena.labmonitor.common.exception.InvalidOperationException;
import com.olena.labmonitor.common.exception.ResourceNotFoundException;
import com.olena.labmonitor.sensor.Sensor;
import com.olena.labmonitor.config.MonitoringProperties;
import com.olena.labmonitor.user.User;
import com.olena.labmonitor.user.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

@Service
@Transactional
public class AlertService {

    private static final List<AlertStatus> UNRESOLVED_STATUSES =
            List.of(AlertStatus.ACTIVE, AlertStatus.ACKNOWLEDGED);

    private final AlertRepository alertRepository;
    private final UserRepository userRepository;
    private final AlertHistoryRepository alertHistoryRepository;
    private final MonitoringProperties monitoringProperties;

    public AlertService(
            AlertRepository alertRepository,
            UserRepository userRepository,
            AlertHistoryRepository alertHistoryRepository,
            MonitoringProperties monitoringProperties
    ) {
        this.alertRepository = alertRepository;
        this.userRepository = userRepository;
        this.alertHistoryRepository = alertHistoryRepository;
        this.monitoringProperties = monitoringProperties;
    }

    public void processThresholdReading(Sensor sensor, BigDecimal value, LocalDateTime measuredAt) {
        boolean belowMinimum = sensor.getMinSafeValue() != null
                && value.compareTo(sensor.getMinSafeValue()) < 0;
        boolean aboveMaximum = sensor.getMaxSafeValue() != null
                && value.compareTo(sensor.getMaxSafeValue()) > 0;

        var existingAlert = findUnresolvedThresholdAlert(sensor.getId());
        if (!belowMinimum && !aboveMaximum) {
            existingAlert.ifPresent(alert -> {
                alert.markRecovered(measuredAt);
                boolean autoRecoverableSeverity = monitoringProperties.getAlerts()
                        .getAutoRecoverySeverities().contains(alert.getSeverity());
                long violationMinutes = alert.getViolationStartedAt() == null
                        ? Long.MAX_VALUE
                        : java.time.Duration.between(alert.getViolationStartedAt(), measuredAt).toMinutes();
                if (alert.getRecoveredAt() != null
                        && autoRecoverableSeverity
                        && violationMinutes >= 0
                        && violationMinutes <= monitoringProperties.getAlerts()
                                .getAutoRecoveryMaxDuration().toMinutes()) {
                    alert.resolveAutomatically(measuredAt);
                    alertHistoryRepository.save(AlertHistory.autoRecovered(alert));
                }
            });
            return;
        }

        if (existingAlert.isPresent()) {
            Alert alert = existingAlert.get();
            BigDecimal extreme = isMoreExtreme(sensor, value, alert.getMostExtremeValue())
                    ? value
                    : alert.getMostExtremeValue();
            alert.updateThresholdViolation(value, extreme, calculateThresholdSeverity(sensor, value), measuredAt);
            return;
        }

        String boundary = belowMinimum
                ? "minimum " + sensor.getMinSafeValue()
                : "maximum " + sensor.getMaxSafeValue();
        String unit = sensor.getUnit() == null ? "" : " " + sensor.getUnit();

        Alert alert = new Alert(
                sensor.getRoom(),
                sensor,
                AlertType.SENSOR_THRESHOLD,
                calculateThresholdSeverity(sensor, value),
                "Sensor value outside safe range",
                "Sensor '" + sensor.getName() + "' reported " + value + unit
                        + ", outside safe " + boundary + unit
        );
        alert.startThresholdViolation(value, measuredAt);
        alertRepository.save(alert);
    }

    public void processSensorOffline(Sensor sensor, LocalDateTime detectedAt) {
        var existingAlert = alertRepository.findFirstBySensorIdAndTypeAndStatusIn(
                sensor.getId(), AlertType.SENSOR_OFFLINE, UNRESOLVED_STATUSES
        );
        if (existingAlert.isPresent()) return;

        Alert alert = new Alert(
                sensor.getRoom(),
                sensor,
                AlertType.SENSOR_OFFLINE,
                AlertSeverity.HIGH,
                "Sensor stopped reporting",
                "Sensor '" + sensor.getName() + "' has not sent readings since "
                        + (sensor.getLastSeenAt() == null ? "activation" : sensor.getLastSeenAt())
        );
        alertRepository.save(alert);
    }

    public void processSensorOnline(Sensor sensor, LocalDateTime measuredAt) {
        alertRepository.findFirstBySensorIdAndTypeAndStatusIn(
                sensor.getId(), AlertType.SENSOR_OFFLINE, UNRESOLVED_STATUSES
        ).ifPresent(alert -> {
            alert.markRecovered(measuredAt);
            alert.resolveAutomatically(measuredAt, "Sensor resumed reporting");
            alertHistoryRepository.save(AlertHistory.sensorOnline(alert));
        });
    }

    AlertSeverity calculateThresholdSeverity(Sensor sensor, BigDecimal value) {
        BigDecimal minimum = sensor.getMinSafeValue();
        BigDecimal maximum = sensor.getMaxSafeValue();

        if (minimum == null || maximum == null) {
            return AlertSeverity.HIGH;
        }

        BigDecimal rangeWidth = maximum.subtract(minimum);
        if (rangeWidth.signum() <= 0) {
            return AlertSeverity.HIGH;
        }

        BigDecimal deviation;
        if (value.compareTo(minimum) < 0) {
            deviation = minimum.subtract(value);
        } else if (value.compareTo(maximum) > 0) {
            deviation = value.subtract(maximum);
        } else {
            return AlertSeverity.LOW;
        }

        BigDecimal deviationPercent = deviation
                .multiply(BigDecimal.valueOf(100))
                .divide(rangeWidth, 6, RoundingMode.HALF_UP);

        if (deviationPercent.compareTo(monitoringProperties.getAlerts().getLowMaxPercent()) <= 0) {
            return AlertSeverity.LOW;
        }
        if (deviationPercent.compareTo(monitoringProperties.getAlerts().getMediumMaxPercent()) <= 0) {
            return AlertSeverity.MEDIUM;
        }
        if (deviationPercent.compareTo(monitoringProperties.getAlerts().getHighMaxPercent()) <= 0) {
            return AlertSeverity.HIGH;
        }
        return AlertSeverity.CRITICAL;
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> findAll(
            Long organizationId,
            Long labId,
            Long roomId,
            Long sensorId,
            AlertStatus status,
            AlertSeverity severity
    ) {
        Specification<Alert> specification = (root, query, builder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (organizationId != null) {
                predicates.add(builder.equal(root.get("room").get("lab").get("organization").get("id"), organizationId));
            }
            if (labId != null) {
                predicates.add(builder.equal(root.get("room").get("lab").get("id"), labId));
            }
            if (roomId != null) {
                predicates.add(builder.equal(root.get("room").get("id"), roomId));
            }
            if (sensorId != null) {
                predicates.add(builder.equal(root.get("sensor").get("id"), sensorId));
            }
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            if (severity != null) {
                predicates.add(builder.equal(root.get("severity"), severity));
            }

            return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };

        return alertRepository.findAll(specification, Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(AlertResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AlertResponse findById(Long id) {
        return AlertResponse.from(getAlert(id));
    }

    @Transactional(readOnly = true)
    public AlertCountResponse countUnresolved() {
        return new AlertCountResponse(alertRepository.countByStatusIn(UNRESOLVED_STATUSES));
    }

    public AlertResponse acknowledge(Long id, String userEmail) {
        Alert alert = getAlertForUpdate(id);
        if (alert.getStatus() != AlertStatus.ACTIVE) {
            throw new InvalidOperationException("Only an active alert can be acknowledged");
        }
        User user = getUser(userEmail);
        alert.acknowledge(user);
        Alert savedAlert = alertRepository.saveAndFlush(alert);
        alertHistoryRepository.save(new AlertHistory(
                savedAlert, user, AlertHistoryAction.ACKNOWLEDGED, null, null
        ));
        return AlertResponse.from(savedAlert);
    }

    @Transactional(readOnly = true)
    public List<AlertHistoryResponse> findHistory(Long id) {
        getAlert(id);
        return alertHistoryRepository.findByAlertIdOrderByCreatedAtAsc(id).stream()
                .map(AlertHistoryResponse::from)
                .toList();
    }

    public AlertResponse resolve(Long id, String userEmail, ResolveAlertRequest request) {
        Alert alert = getAlertForUpdate(id);
        if (alert.getStatus() != AlertStatus.ACKNOWLEDGED) {
            throw new InvalidOperationException("Only an acknowledged alert can be resolved");
        }
        if (request.outcome() == AlertResolutionOutcome.AUTO_RECOVERED) {
            throw new InvalidOperationException("AUTO_RECOVERED can only be set by the system");
        }
        if (request.outcome() == AlertResolutionOutcome.FIXED && alert.getRecoveredAt() == null) {
            throw new InvalidOperationException("A fixed alert can only be resolved after sensor recovery");
        }
        if (request.outcome() == AlertResolutionOutcome.FALSE_ALARM
                && (request.comment() == null || request.comment().isBlank())) {
            throw new InvalidOperationException("A false alarm requires an explanation");
        }
        User user = getUser(userEmail);
        alert.resolve(user, request.outcome(), request.comment());
        Alert savedAlert = alertRepository.saveAndFlush(alert);
        alertHistoryRepository.save(new AlertHistory(
                savedAlert, user, AlertHistoryAction.RESOLVED, request.outcome(), request.comment()
        ));
        return AlertResponse.from(savedAlert);
    }

    public AlertResponse reopen(Long id, String userEmail, ReopenAlertRequest request) {
        Alert alert = getAlertForUpdate(id);
        if (alert.getStatus() != AlertStatus.RESOLVED) {
            throw new InvalidOperationException("Only a resolved alert can be reopened");
        }
        User user = getUser(userEmail);
        AlertResolutionOutcome previousOutcome = alert.getResolutionOutcome();
        alertHistoryRepository.save(new AlertHistory(
                alert, user, AlertHistoryAction.REOPENED, previousOutcome, request.reason()
        ));
        alert.reopen(user);
        return AlertResponse.from(alertRepository.saveAndFlush(alert));
    }

    private java.util.Optional<Alert> findUnresolvedThresholdAlert(Long sensorId) {
        return alertRepository.findFirstBySensorIdAndTypeAndStatusInAndRecoveredAtIsNull(
                sensorId,
                AlertType.SENSOR_THRESHOLD,
                UNRESOLVED_STATUSES
        );
    }

    private boolean isMoreExtreme(Sensor sensor, BigDecimal candidate, BigDecimal current) {
        if (current == null) {
            return true;
        }
        return deviationFromSafeRange(sensor, candidate).compareTo(deviationFromSafeRange(sensor, current)) > 0;
    }

    private BigDecimal deviationFromSafeRange(Sensor sensor, BigDecimal value) {
        if (sensor.getMinSafeValue() != null && value.compareTo(sensor.getMinSafeValue()) < 0) {
            return sensor.getMinSafeValue().subtract(value);
        }
        if (sensor.getMaxSafeValue() != null && value.compareTo(sensor.getMaxSafeValue()) > 0) {
            return value.subtract(sensor.getMaxSafeValue());
        }
        return BigDecimal.ZERO;
    }

    private Alert getAlert(Long id) {
        return alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert with id " + id + " was not found"));
    }

    private Alert getAlertForUpdate(Long id) {
        return alertRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert with id " + id + " was not found"));
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User with email " + email + " was not found"));
    }

}

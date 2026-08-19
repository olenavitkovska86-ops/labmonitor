package com.olena.labmonitor.alert;

import com.olena.labmonitor.alert.dto.AlertResponse;
import com.olena.labmonitor.common.exception.InvalidOperationException;
import com.olena.labmonitor.common.exception.ResourceNotFoundException;
import com.olena.labmonitor.sensor.Sensor;
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

@Service
@Transactional
public class AlertService {

    private static final BigDecimal LOW_LIMIT_PERCENT = new BigDecimal("5");
    private static final BigDecimal MEDIUM_LIMIT_PERCENT = new BigDecimal("15");
    private static final BigDecimal HIGH_LIMIT_PERCENT = new BigDecimal("30");
    private static final List<AlertStatus> UNRESOLVED_STATUSES =
            List.of(AlertStatus.ACTIVE, AlertStatus.ACKNOWLEDGED);

    private final AlertRepository alertRepository;
    private final UserRepository userRepository;

    public AlertService(AlertRepository alertRepository, UserRepository userRepository) {
        this.alertRepository = alertRepository;
        this.userRepository = userRepository;
    }

    public void createThresholdAlertIfRequired(Sensor sensor, BigDecimal value) {
        boolean belowMinimum = sensor.getMinSafeValue() != null
                && value.compareTo(sensor.getMinSafeValue()) < 0;
        boolean aboveMaximum = sensor.getMaxSafeValue() != null
                && value.compareTo(sensor.getMaxSafeValue()) > 0;

        if ((!belowMinimum && !aboveMaximum) || hasUnresolvedThresholdAlert(sensor.getId())) {
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
        alertRepository.save(alert);
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

        if (deviationPercent.compareTo(LOW_LIMIT_PERCENT) <= 0) {
            return AlertSeverity.LOW;
        }
        if (deviationPercent.compareTo(MEDIUM_LIMIT_PERCENT) <= 0) {
            return AlertSeverity.MEDIUM;
        }
        if (deviationPercent.compareTo(HIGH_LIMIT_PERCENT) <= 0) {
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

    public AlertResponse acknowledge(Long id, String userEmail) {
        Alert alert = getAlert(id);
        if (alert.getStatus() != AlertStatus.ACTIVE) {
            throw new InvalidOperationException("Only an active alert can be acknowledged");
        }
        alert.acknowledge(getUser(userEmail));
        return AlertResponse.from(alertRepository.saveAndFlush(alert));
    }

    public AlertResponse resolve(Long id, String userEmail) {
        Alert alert = getAlert(id);
        if (alert.getStatus() == AlertStatus.RESOLVED) {
            throw new InvalidOperationException("Alert is already resolved");
        }
        alert.resolve(getUser(userEmail));
        return AlertResponse.from(alertRepository.saveAndFlush(alert));
    }

    private boolean hasUnresolvedThresholdAlert(Long sensorId) {
        return alertRepository.existsBySensorIdAndTypeAndStatusIn(
                sensorId,
                AlertType.SENSOR_THRESHOLD,
                UNRESOLVED_STATUSES
        );
    }

    private Alert getAlert(Long id) {
        return alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert with id " + id + " was not found"));
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User with email " + email + " was not found"));
    }

}

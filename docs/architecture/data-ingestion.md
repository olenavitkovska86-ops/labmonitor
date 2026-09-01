# Data Ingestion

LabMonitor receives sensor readings through a single application service.
Camera ingestion is future work.

## Sources

- Browser-based data clients
- HTTP API clients and physical devices
- MQTT listener in a future version

## Design Rule

All data sources must use the same application services.

Sensor data sources should use:

```text
SensorReadingService
```

Future camera data sources should use:

```text
CameraEventService
```

Alert generation should use:

```text
AlertService
```

Data sources must not write directly to the database.

## Measurement Order and Late Data

Every accepted reading is stored, including late or out-of-order measurements.
The current reading is selected by `measured_at` descending and then by reading
ID descending, which provides a deterministic tie-breaker for equal timestamps.

Every received packet updates sensor liveness and can close an offline alert,
because receipt itself proves that the sensor is online. Threshold-alert state
is updated only when the newly stored reading is current in measurement order.
An older measurement therefore remains available in history and exports but
cannot reopen or otherwise rewrite the sensor's current threshold condition.

## Browser and authenticated HTTP flow

```text
Browser client / authenticated HTTP API
        ->
SensorReadingService
        ->
persist SensorReading
        ->
AlertService
```

## Device flow

```text
Device HTTP client
        -> Authorization: Device <token>
DeviceAuthenticationFilter
        ->
DeviceIngestionService
        -> resolve channel and enforce message idempotency
SensorReadingService
        ->
persist SensorReading with source_device_id and message_id
        ->
AlertService
```

Device timestamps must fall within the configured past-age and future-skew
limits. The device and its credential record receipt activity even when a
repeated `messageId` returns an already processed reading.

## Planned Camera Data Flow

```text
Browser client / device / HTTP API / MQTT
        ->
CameraEventService
        ->
camera_events
        ->
AlertService
```

## Current Project Version

The current implementation supports the user-authenticated HTTP API and the
device-authenticated HTTP API. Browser pages may generate or acquire values and
submit them through either route. MQTT remains future work.

## Source-neutral readings

`SensorReading` does not classify a value as simulated, mobile, or physical.
Device-originated readings do retain `source_device_id` and `message_id` for
authentication provenance and idempotency. The backend otherwise validates and
processes browser and device readings through the same application service.

Additional operational traceability should remain transport-level ingestion
metadata or audit logging rather than changing the meaning of a sensor or
splitting threshold and liveness logic.

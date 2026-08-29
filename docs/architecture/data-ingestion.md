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

## Sensor Data Flow

```text
Browser client / device / HTTP API / MQTT
        ->
SensorReadingService
        ->
sensor_readings
        ->
AlertService
```

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

The current implementation uses the HTTP API. Browser pages may generate or
acquire values and submit them to that API, while real sensors and MQTT support
can be added later without changing the main domain model.

## Source-neutral readings

`SensorReading` intentionally does not store whether a value came from a
browser client, an iPhone, an integration, or a physical device. The backend
validates and processes every accepted reading identically. Client names and
transport-specific details must not leak into the sensor domain or alert logic.

If operational traceability is required later, it should be designed as
transport-level ingestion metadata or audit logging rather than changing the
meaning of a sensor or splitting the reading flow.

## Future Device Fields

Real sensor and camera integrations may require additional device fields, for example:

```text
external_device_id
device_key_hash
```

These fields can be added later with database migrations when real device integration is implemented.

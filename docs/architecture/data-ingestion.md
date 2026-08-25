# Data Ingestion

LabMonitor currently receives sensor readings from different sources through a
single application service. Camera ingestion is future work.

## Sources

- Simulator
- HTTP API from real devices
- MQTT listener in a future version
- Manual test data during development

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

## Sensor Data Flow

```text
Simulator / HTTP API / MQTT
        ->
SensorReadingService
        ->
sensor_readings
        ->
AlertService
```

## Planned Camera Data Flow

```text
Simulator / HTTP API / MQTT
        ->
CameraEventService
        ->
camera_events
        ->
AlertService
```

## Current Project Version

The current implementation uses:

- HTTP API
- simulator data

Real sensors and MQTT support can be added later without changing the main domain model.

## Planned Source Type

The current `SensorReading` entity does not store its source. A future device
integration may add the following field to sensor readings and camera events.

Recommended field:

```text
source_type
```

Recommended tables:

```text
sensor_readings.source_type
camera_events.source_type
```

Recommended values:

```text
SIMULATOR
HTTP_DEVICE
MQTT_DEVICE
MANUAL
```

This makes it possible to use simulator data and real device data in the same system without creating separate tables for each source.

## Future Device Fields

Real sensor and camera integrations may require additional device fields, for example:

```text
external_device_id
device_key_hash
```

These fields can be added later with database migrations when real device integration is implemented.

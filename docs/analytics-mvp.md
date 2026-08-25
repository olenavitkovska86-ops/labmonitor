# Analytics MVP

## User value

The first analytics screen helps a lab administrator answer one question:

> Which rooms require attention now, why, and for how long?

It is an operational overview, not a historical reporting dashboard.

The same screen also provides lightweight alert history for the last 24 hours,
7 days, or 30 days. It shows alert volume, critical alerts, average response and
resolution times, daily activity, and the rooms with the most alerts.

## Current rules

- A room requires attention when it has at least one `ACTIVE` or
  `ACKNOWLEDGED` alert.
- Room priority is based on its most severe unresolved alert.
- `CRITICAL` maps to critical attention, `HIGH` to high attention, and `LOW`
  or `MEDIUM` to medium attention.
- The main problem is the most severe alert. An unacknowledged alert wins a tie.
- Problem duration starts at the oldest unresolved alert in the room.
- Offline sensors are counted separately. An active sensor that stops reporting
  also creates a `SENSOR_OFFLINE` alert, so it affects room priority through the
  normal unresolved-alert rules. A new reading restores the sensor and closes
  that alert automatically.
- Uptime is intentionally excluded because device status history is not stored.
- A threshold alert represents one continuous violation. Further unsafe readings
  update its latest and most extreme values; a safe reading records recovery.
- A later `SAFE` to `UNSAFE` transition creates a new alert even if an earlier
  recovered alert is still awaiting review.
- Recovered `LOW` and `MEDIUM` alerts lasting no more than five minutes close automatically. `HIGH` and
  `CRITICAL` alerts require human review; `FIXED` requires sensor recovery and
  `FALSE_ALARM` requires an explanation.

## API

```http
GET /api/analytics/organizations/{organizationId}/overview
GET /api/analytics/organizations/{organizationId}/problem-rooms
GET /api/analytics/organizations/{organizationId}/history?period=LAST_7_DAYS
```

The overview contains counts for rooms requiring attention, unresolved and
unacknowledged alerts, critical alerts, and currently offline active sensors.
The problem-room list is ordered by attention level, unacknowledged alert count,
and problem age.

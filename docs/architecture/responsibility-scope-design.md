# Responsibility Scope Design Note

Status: **PROPOSED — implementation paused until the current user/role/security
work is merged.**

This note records the initial audit and a minimal candidate model. It is not an
implemented contract. The design must be reviewed against the final `User`,
`Membership`, JWT, authority, and security configuration before migrations or
application code are added.

## Goal

Keep action permissions and data responsibility independent:

```text
Role  -> what a user may do
Scope -> where the user may do it
```

The hierarchy in scope is deliberately limited to:

```text
Organization -> Lab -> Room -> Sensor
```

There is no generic ACL engine, per-sensor assignment, permission DSL, group,
team, department, or nested organization model.

## Current-State Audit

At the time of this audit:

- `Membership` connects one user to one organization and stores the role in
  that organization.
- A database unique constraint permits only one membership per user and
  organization.
- `UserDetailsServiceImpl` converts the global role and every membership role
  into authorities. An authority does not identify the organization in which a
  membership role applies.
- `SecurityConfig` generally requires authentication but does not enforce
  responsibility scope.
- Organization, lab, room, and sensor list services use unrestricted repository
  queries.
- Their detail services use unrestricted `findById` queries.
- Alert lists, alert details, history, counts, and lifecycle actions are not
  scoped to the authenticated user's rooms.
- Sensor current reading, history, ingestion, and CSV export do not perform
  object-level scope checks.
- Monitoring Session lists, details, lifecycle actions, events, timeline, and
  ZIP export do not perform object-level scope checks.
- Organization analytics counts all rooms, sensors, and alerts in the requested
  organization.
- Consequently, frontend filtering alone would be bypassable by direct API
  requests such as `GET /api/rooms/{id}`.
- There is no existing browser administration flow for memberships and
  assignments.

## Candidate Domain Model

Retain `Membership` as the user's role-bearing connection to an organization.
The initial candidate is:

```text
Membership
- id
- user_id
- organization_id
- role
- organization_wide
- created_at

ResponsibilityAssignment
- id
- membership_id
- lab_id
- room_id nullable
- created_at
```

Proposed meaning:

- `Membership.organization_wide = true` grants the whole membership
  organization.
- An assignment with `lab_id` and no `room_id` grants the whole lab.
- An assignment with both `lab_id` and `room_id` grants one room.
- A room assignment stores its lab explicitly so the complete hierarchy can be
  validated.
- Sensors inherit room access and are never assigned directly.
- A membership without organization-wide scope or assignments grants no domain
  objects.
- `SUPER_ADMIN` bypasses assignments and has global scope.

The explicit organization-wide flag is preferred over an assignment row with
both target columns null. A null-target row has unclear domain meaning and is
awkward to protect against duplicates with ordinary MySQL unique constraints.

This model is provisional. In particular, the location of the organization-wide
flag must be reconsidered after the final Membership model is merged.

## Required Invariants

Application validation must guarantee:

1. An assignment belongs to a membership in the same organization as its lab.
2. An assigned room belongs to the assignment's lab.
3. The same lab or room is not assigned twice to one membership.
4. A room assignment is rejected as redundant when the whole lab is assigned.
5. Adding a whole-lab assignment must reject or replace existing room
   assignments in that lab; the final administration UX should make this
   behavior explicit.
6. Organization-wide membership scope makes narrower assignments redundant.
7. Assignments are not required for `SUPER_ADMIN`.

A future Flyway migration should add foreign keys, indexes, check constraints,
and practical unique constraints. Organization consistency must also be checked
in the domain/service layer because the current normalized room hierarchy does
not expose `organization_id` directly on `rooms`.

No existing migration file should be modified.

## Proposed Authorization Strategy

Introduce one scope component after the security merge. It should resolve the
authenticated user and expose predicates/checks for organization, lab, room,
and sensor access.

- `SUPER_ADMIN` receives unrestricted scope.
- Other users receive the union of scope from their memberships.
- Direct-object operations perform an object-level scope check before returning
  or mutating data.
- Inaccessible objects should normally be reported as not found so the API does
  not disclose names or existence outside the user's scope.
- Role checks continue to decide which actions are allowed; scope checks only
  decide the target objects on which those actions apply.
- Internal background operations such as sensor availability processing must
  not accidentally depend on an interactive SecurityContext.

## Proposed Filtering Strategy

List and analytics filtering must happen in repository queries or JPA
specifications, not by loading all records and filtering them in Java.

The scope must cover:

- organizations visible through scoped memberships;
- labs assigned directly, inherited from organization-wide scope, or containing
  an assigned room;
- rooms inherited from organization/lab scope or assigned directly;
- sensors and readings inherited from room scope;
- alerts inherited from their room;
- monitoring sessions inherited from their room;
- session events, timeline, and export inherited from their session's room;
- analytics calculated only from allowed rooms.

Repository filtering and direct-object checks must use the same resolved scope
semantics to prevent list/detail inconsistencies.

## Administration UX Candidate

The minimal flow is:

```text
User -> Organization membership -> Role -> Responsibility
```

For each membership, a `SUPER_ADMIN` chooses either the whole organization or:

```text
[ ] Chemistry Lab — whole lab
    [x] Room 101
    [x] Room 102
    [ ] Storage
```

This should be a compact membership/assignment editor, not a general permission
editor.

## Monitoring Navigation Direction

Navigation may be redesigned independently of the final security
implementation, provided it treats backend responses as the source of visible
objects and does not pretend to provide authorization.

- `LIMITED_EMPLOYEE`: Overview, Monitor, Alerts, Sessions.
- `LAB_ADMIN`: the same primary navigation, with allowed sensor configuration
  actions shown contextually.
- `SUPER_ADMIN`: Overview, Monitor, Alerts, Sessions, Administration.
- Monitor follows Organization -> Lab -> Room -> Sensor but only displays
  objects returned by scoped APIs.
- No frontend route or hidden button is considered a security boundary.

## Security Cases to Verify After Resuming

- Scoped lists for organizations, labs, rooms, sensors, alerts, and sessions.
- Direct access to an unassigned room, sensor, alert, session, event, timeline,
  export, reading history, or analytics target is rejected.
- A lab assignment includes every room in that lab but no sibling lab.
- A room assignment includes that room's sensors and monitoring data only.
- `LAB_ADMIN` actions remain confined to assigned objects.
- `SUPER_ADMIN` sees all objects without assignments.
- Cross-organization and duplicate assignments are rejected.
- UI filtering and backend authorization are tested separately.

## Resume Checklist

Before implementation resumes:

1. Merge the final user/role/security work into the base branch.
2. Rebase or recreate `feature/user-responsibility-scope` from that updated
   base.
3. Re-audit `User`, `Membership`, JWT claims/authorities, role checks, exception
   handling, and administration endpoints.
4. Confirm or revise the candidate model before creating a migration.
5. Implement backend enforcement before relying on scope-aware navigation.

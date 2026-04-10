# HC_GroupManager

Shim plugin that implements the HaporeLab GroupManager API expected by the ArenaPvP mod, delegating all group operations to HC_Party. Provides group creation, lookup, broadcasting, coalition management, and HUD control through a bridge service layer.

## Features

- Implements the `GroupManagerAPI` / `GroupService` interface required by ArenaPvP
- Bridges HC_Party's party system to the GroupManager API via `PartyGroupService`
- Supports permanent groups and temporary coalition groups for arena matches
- Group lookups by player UUID or group ID
- Message broadcasting to group members (with optional exclusion)
- Same-group and leader checks for PvP damage rules
- Coalition group join/leave/restore for arena match lifecycle
- Party HUD pause/resume for arena transitions
- Event listener registration for group state changes

## Dependencies

- **EntityModule** (required) -- Hytale entity system
- **HC_Party** (required) -- underlying party/group implementation

## Building

```bash
./gradlew build
```

# Datapack Guide

CustomHitboxLib supports full datapack configuration. Define custom entity parts in JSON without writing any code.

---

## Folder Structure

Datapacks go in your world's `datapacks/` folder. Part definitions live in `data/<namespace>/custom_parts/`:

```
my_datapack/
  pack.mcmeta
  data/
    mymod/
      custom_parts/
        zombie_wings.json
        skeleton_shield.json
```

---

## pack.mcmeta

Every datapack needs a `pack.mcmeta`:

```json
{
  "pack": {
    "pack_format": 15,
    "description": "Custom hitboxes for entities"
  }
}
```

---

## JSON Format

Each JSON file defines parts for one or more entity types. The file can be a single object or an array of objects.

### Simple Format (single entity)

```json
{
  "id": "minecraft:zombie",
  "parts": [
    {
      "name": "left_wing",
      "width": 1.5,
      "height": 0.5,
      "offset": [1.0, 1.0, -0.2]
    }
  ]
}
```

### Selector Format (advanced matching)

```json
{
  "selector": {
    "id": "minecraft:zombie",
    "nbt": "Health:20.0"
  },
  "main_hitbox_pickable": false,
  "parts": [
    {
      "name": "armored_shield",
      "width": 2.0,
      "height": 2.0,
      "offset": [0, 0, 0],
      "collision": true,
      "pushable": true
    }
  ]
}
```

### Array Format (multiple entities in one file)

```json
[
  {
    "id": "minecraft:zombie",
    "parts": [
      { "name": "left_wing", "width": 1.5, "height": 0.5, "offset": [1.0, 1.0, -0.2] }
    ]
  },
  {
    "id": "minecraft:skeleton",
    "parts": [
      { "name": "shield", "width": 1.0, "height": 1.8, "offset": [0, 0, -0.5] }
    ]
  }
]
```

---

## Root Fields

| Field | Required | Default | Description |
|---|---|---|---|
| `id` | One of `id` or `selector` | -- | Entity type ID (e.g. `minecraft:zombie`) |
| `selector` | Alternative to `id` | -- | Object with `id` and/or `nbt` subfields |
| `nbt` | No | -- | NBT string to match (e.g. `"Health:20.0"`) |
| `pose` | No | `"standing"` | Entity pose this definition applies to (e.g. `"standing"`, `"crouching"`, `"swimming"`) |
| `main_hitbox_pickable` | No | `true` | Can the player target the main hitbox |
| `main_hitbox_pushable` | No | `true` | Does the main hitbox push entities |
| `main_hitbox_collision` | No | `true` | Does the main hitbox collide with blocks |
| `parts` | Yes | -- | Array of part definitions |

---

## Pose-Specific Parts

Parts can be defined per entity pose. When the entity changes pose, CustomHitboxLib automatically switches to the matching parts definition.

### Per-Pose Definition

Add a `"pose"` field to the root of your definition:

```json
{
  "id": "minecraft:player",
  "pose": "crouching",
  "parts": [
    {
      "name": "turtle_shell",
      "width": 1.5,
      "height": 0.5,
      "offset": [0, 0, 0]
    }
  ]
}
```

### Fallback Behavior

- If at least one part is defined for the current pose, only those pose-specific parts are used.
- If no parts are defined for the current pose at all, the `"standing"` definition set is used instead.
- Datapacks without a `"pose"` field default to `"standing"` and remain fully backward-compatible.

### Multiple Poses in One File

You can define multiple pose variants in a single JSON file:

```json
[
  {
    "id": "minecraft:player",
    "pose": "standing",
    "parts": [
      { "name": "cape", "width": 1.0, "height": 1.5, "offset": [0, 1.0, 0.3] }
    ]
  },
  {
    "id": "minecraft:player",
    "pose": "crouching",
    "parts": [
      { "name": "cape", "width": 1.0, "height": 1.0, "offset": [0, 0.5, 0.3] }
    ]
  }
]
```

### Supported Poses

Use the lowercase Minecraft pose names:

| Pose Name | Description |
|---|---|
| `standing` | Default standing pose |
| `crouching` | Sneaking / crouching |
| `swimming` | Swimming / in water |
| `fall_flying` | Elytra gliding |
| `sleeping` | Sleeping in a bed |
| `spin_attack` | Trident spin attack |
| `long_jumping` | Frog long jump |
| `dying` | Death animation |
| `sitting` | Sitting on a vehicle / saddle |
| `crawling` | Crawling (1.20.1+ limited) |

---

## Part Fields

Each entry in the `parts` array:

| Field | Required | Default | Description |
|---|---|---|---|
| `name` | Yes | -- | Unique name for the part |
| `width` | Yes | -- | Hitbox width in blocks |
| `height` | Yes | -- | Hitbox height in blocks |
| `offset` | No | `[0, 0, 0]` | `[x, y, z]` offset from entity origin (world-relative, does not rotate) |
| `positioner` | No | -- | Object specifying a positioner type and offset (see below) |
| `pickable` | No | `true` | Can the player target this part |
| `pushable` | No | `false` | Does this part push other entities |
| `collision` | No | `false` | Does this part collide with blocks |
| `suffocate` | No | `false` | Does this part cause suffocation damage inside blocks |

### Positioner Object

Instead of a plain `offset` array, you can use a `positioner` object to control how the offset behaves:

```json
{
  "positioner": {
    "type": "rotating",
    "offset": [1.0, 0.5, 0.0]
  }
}
```

| Field | Required | Default | Description |
|---|---|---|---|
| `type` | No | `"offset"` | Positioner type (see table below) |
| `offset` | No | `[0, 0, 0]` | `[x, y, z]` offset values |

**Positioner types:**

| Type | Behavior |
|---|---|
| `"offset"` | Static offset from entity origin. Does not rotate. (Default) |
| `"rotating"` | Offset rotates with the entity's **body yaw** (`yBodyRot`) |
| `"rotating_head"` | Offset rotates with the entity's **head yaw** (`yHeadRot`) |

The legacy `"offset": [x, y, z]` shorthand is still supported and behaves the same as `"type": "offset"`.

---

## Reloading

After editing your datapack, run:

```
/reload
```

Parts update on all entities immediately. No restart required.

---

## Example

A zombie with wings:

```json
{
  "id": "minecraft:zombie",
  "main_hitbox_pickable": true,
  "main_hitbox_pushable": true,
  "main_hitbox_collision": true,
  "parts": [
    {
      "name": "left_wing",
      "width": 1.5,
      "height": 0.5,
      "positioner": {
        "type": "rotating",
        "offset": [1.0, 1.0, -0.2]
      },
      "pickable": true,
      "pushable": false,
      "collision": false,
      "suffocate": false
    },
    {
      "name": "right_wing",
      "width": 1.5,
      "height": 0.5,
      "positioner": {
        "type": "rotating",
        "offset": [-1.0, 1.50, -0.2]
      },
      "pickable": true,
      "pushable": false,
      "collision": false,
      "suffocate": false
    }
  ]
}
```

See [Examples](Examples.md) for more.

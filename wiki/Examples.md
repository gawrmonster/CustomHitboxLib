# Examples

## Datapack Examples

### Zombie with Wings

Adds two wing parts that extend outward from the zombie's body.

**File:** `data/mymod/custom_parts/zombie_wings.json`

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
      "offset": [1.0, 1.0, -0.2],
      "pickable": true,
      "pushable": false,
      "collision": false,
      "suffocate": false
    },
    {
      "name": "right_wing",
      "width": 1.5,
      "height": 0.5,
      "offset": [-1.0, 1.50, -0.2],
      "pickable": true,
      "pushable": false,
      "collision": false,
      "suffocate": false
    }
  ]
}
```

### Skeleton with Shield

Adds a shield part in front of the skeleton that blocks player movement.

**File:** `data/mymod/custom_parts/skeleton_shield.json`

```json
{
  "id": "minecraft:skeleton",
  "main_hitbox_collision": true,
  "parts": [
    {
      "name": "shield",
      "width": 1.0,
      "height": 1.8,
      "offset": [0, 0, -0.5],
      "pickable": true,
      "pushable": true,
      "collision": true,
      "suffocate": false
    }
  ]
}
```

### Multiple Entities in One File

Use an array to define parts for multiple entity types.

**File:** `data/mymod/custom_parts/multi_entity.json`

```json
[
  {
    "id": "minecraft:zombie",
    "parts": [
      { "name": "left_wing", "width": 1.5, "height": 0.5, "offset": [1.0, 1.0, 0] }
    ]
  },
  {
    "id": "minecraft:skeleton",
    "parts": [
      { "name": "shield", "width": 1.0, "height": 1.8, "offset": [0, 0, -0.5], "collision": true }
    ]
  },
  {
    "id": "minecraft:creeper",
    "parts": [
      { "name": "antenna", "width": 0.3, "height": 0.5, "offset": [0, 2.0, 0] }
    ]
  }
]
```

### NBT-Based Selector

Only apply parts when the entity has specific NBT data.

**File:** `data/mymod/custom_parts/heavy_zombie.json`

```json
{
  "selector": {
    "id": "minecraft:zombie",
    "nbt": "Health:20.0"
  },
  "parts": [
    {
      "name": "heavy_armor",
      "width": 1.2,
      "height": 2.2,
      "positioner": {
        "type": "rotating",
        "offset": [0, 0, 0]
      },
      "collision": true,
      "pushable": true
    }
  ]
}
```

### Rotating Parts (Body Yaw)

Parts that follow the entity's body rotation. Useful for limbs, wings, or armor that should swing when the entity turns.

**File:** `data/mymod/custom_parts/spider_legs.json`

```json
{
  "id": "minecraft:spider",
  "parts": [
    {
      "name": "left_leg_front",
      "width": 0.3,
      "height": 0.3,
      "positioner": {
        "type": "rotating",
        "offset": [1.0, 0.5, 0.5]
      }
    },
    {
      "name": "right_leg_front",
      "width": 0.3,
      "height": 0.3,
      "positioner": {
        "type": "rotating",
        "offset": [-1.0, 0.5, 0.5]
      }
    }
  ]
}
```

### Rotating Parts (Head Yaw)

Parts that follow the entity's head rotation. Useful for accessories that should track where the entity is looking.

**File:** `data/mymod/custom_parts/head_accessory.json`

```json
{
  "id": "minecraft:zombie",
  "parts": [
    {
      "name": "head_lantern",
      "width": 0.3,
      "height": 0.3,
      "positioner": {
        "type": "rotating_head",
        "offset": [0.4, 0.2, 0.0]
      }
    }
  ]
}
```

---

## Java API Examples

### Adding Parts on Entity Spawn

Listen for entity join and add parts:

```java
@SubscribeEvent
public void onEntityJoin(EntityJoinLevelEvent event) {
    Entity entity = event.getEntity();
    if (entity instanceof Zombie zombie && zombie instanceof ICustomMultipart mp && !mp.hasCustomParts()) {
        MultipartHelper.addPart(zombie, "left_wing", 1.5F, 0.5F,
            PartPositioners.atOffset(1.0, 1.0, -0.2), true, false, false, false);
        MultipartHelper.addPart(zombie, "right_wing", 1.5F, 0.5F,
            PartPositioners.atOffset(-1.0, 1.0, -0.2), true, false, false, false);
    }
}
```

### Custom Positioner (Facing Direction)

A part that always appears 0.5 blocks in front of the entity's eyes:

```java
PartPositioner facePositioner = (entity, partialTick) -> {
    float yaw = entity.getYRot();
    float yawRad = (float) Math.toRadians(yaw);
    double forwardX = -Mth.sin(yawRad);
    double forwardZ = Mth.cos(yawRad);
    double dist = 0.5D;
    return new Vec3(
        entity.getX() + forwardX * dist,
        entity.getY() + entity.getEyeHeight(),
        entity.getZ() + forwardZ * dist
    );
};

MultipartHelper.addPart(entity, "face", 0.3F, 0.3F, facePositioner);
```

### Built-in Rotating Positioner

A part that rotates with the entity using `PartPositioners.rotating()`. The offset is applied relative to `yBodyRot`, so the part swings with the body:

```java
// 2 blocks behind the entity, rotates with body yaw
MultipartHelper.addPart(entity, "back_part", 2.0F, 2.0F,
    PartPositioners.rotating(0, 0, -2), true, true, true, false);
```

### Built-in Head-Rotating Positioner

A part that rotates with the entity's head using `PartPositioners.rotatingHead()`. The offset is applied relative to `yHeadRot`, so the part follows where the entity is looking:

```java
// 0.4 blocks to the right of the head, rotates with head yaw
MultipartHelper.addPart(entity, "head_accessory", 0.3F, 0.3F,
    PartPositioners.rotatingHead(0.4, 0.2, 0), true, false, false, false);
```

### Custom Positioner (Orbiting Part)

A part that orbits around the entity's head:

```java
PartPositioner orbitPositioner = (entity, partialTick) -> {
    float time = (float)(System.currentTimeMillis() % 4000L) / 4000.0F * 360.0F;
    double radians = Math.toRadians(time);
    return new Vec3(
        entity.getX() + Math.cos(radians) * 1.5,
        entity.getY() + entity.getEyeHeight() + 0.5,
        entity.getZ() + Math.sin(radians) * 1.5
    );
};

MultipartHelper.addPart(entity, "orbit", 0.2F, 0.2F, orbitPositioner);
```

### Disabling Main Hitbox

Use custom parts as the only collision for an entity:

```java
if (entity instanceof ICustomMultipart mp) {
    MultipartHelper.addPart(entity, "body", 0.6F, 1.8F,
        PartPositioners.atOffset(0, 0, 0), true, true, true, false);
    mp.setMainHitboxCollision(false);
    mp.setMainHitboxPickable(false);
}
```

### Dynamic Part Management

Add and remove parts at runtime:

```java
if (entity instanceof ICustomMultipart mp) {
    // Add a part
    mp.addCustomPart("shield", PartDefinition.of("shield", 1.0F, 1.8F,
        PartPositioners.atOffset(0, 0, -0.5), true, true, true));

    // Check if entity has parts
    if (mp.hasCustomParts()) {
        PartEntity<?>[] parts = mp.getCustomParts();
        // ...
    }

    // Remove a part
    mp.removeCustomPart("shield");
}
```

### Controlling Main Hitbox Behavior

```java
if (entity instanceof ICustomMultipart mp) {
    // Make main hitbox intangible but visible
    mp.setMainHitboxCollision(false);
    mp.setMainHitboxPushable(false);
    mp.setMainHitboxPickable(true);
}
```

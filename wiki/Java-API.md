# Java API Reference

CustomHitboxLib provides a full Java API for mods that want to add multipart hitboxes programmatically.

---

## Core Interface: `ICustomMultipart`

All `LivingEntity` instances automatically implement this interface via mixin. It provides methods for managing custom parts.

```java
import dev.customhitboxlib.api.ICustomMultipart;
```

### Methods

| Method | Return | Description |
|---|---|---|
| `getCustomParts()` | `PartEntity<?>[]` | Returns all custom parts, or `null` if none |
| `hasCustomParts()` | `boolean` | Whether this entity has any custom parts |
| `addCustomPart(String name, PartDefinition def)` | `void` | Adds a new part by name |
| `removeCustomPart(String name)` | `void` | Removes a part by name |
| `tickCustomParts()` | `void` | Repositions all parts using their positioners |
| `setMainHitboxPickable(boolean)` | `void` | Sets whether the main hitbox can be targeted |
| `isMainHitboxPickable()` | `boolean` | Whether the main hitbox can be targeted |
| `setMainHitboxPushable(boolean)` | `void` | Sets whether the main hitbox pushes entities |
| `isMainHitboxPushable()` | `boolean` | Whether the main hitbox pushes entities |
| `setMainHitboxCollision(boolean)` | `void` | Sets whether the main hitbox collides with blocks |
| `isMainHitboxCollision()` | `boolean` | Whether the main hitbox collides with blocks |

### Usage

```java
// Cast any LivingEntity to ICustomMultipart
Entity entity = ...;
if (entity instanceof ICustomMultipart mp) {
    mp.addCustomPart("my_part", PartDefinition.of("my_part", 1.0F, 1.0F, myPositioner));
    mp.setMainHitboxCollision(false);
}
```

---

## Part Definition: `PartDefinition`

A record that defines all properties of a custom part.

```java
import dev.customhitboxlib.api.PartDefinition;

PartDefinition def = PartDefinition.of(
    "left_wing",    // name
    1.5F,           // width
    0.5F,           // height
    myPositioner,   // PartPositioner
    true,           // pickable
    false,          // pushable
    false,          // collision
    false           // suffocate
);
```

### Factory Methods

`PartDefinition.of()` has 10 overloads:

```java
// Minimum (name, width, height, positioner)
PartDefinition.of(name, width, height, positioner)

// With pickable
PartDefinition.of(name, width, height, positioner, pickable)

// With pickable, pushable
PartDefinition.of(name, width, height, positioner, pickable, pushable)

// With pickable, pushable, collision
PartDefinition.of(name, width, height, positioner, pickable, pushable, collision)

// Full (all flags)
PartDefinition.of(name, width, height, positioner, pickable, pushable, collision, suffocate)
```

Overloads accepting `EntityDimensions` instead of `width`/`height` also exist.

---

## Positioner: `PartPositioner`

A functional interface that determines where a part is positioned each tick.

```java
import dev.customhitboxlib.api.PartPositioner;

@FunctionalInterface
public interface PartPositioner {
    Vec3 getPosition(Entity entity, float partialTick);
}
```

### Built-in Positioners: `PartPositioners`

```java
import dev.customhitboxlib.api.PartPositioners;

// Static offset from entity origin (world-relative, does not rotate)
PartPositioners.atOffset(x, y, z)

// Rotation-aware offset from entity origin (uses yBodyRot for LivingEntity)
PartPositioners.rotating(offsetX, offsetY, offsetZ)

// Rotation-aware offset from entity origin (uses yHeadRot for LivingEntity)
PartPositioners.rotatingHead(offsetX, offsetY, offsetZ)

// Fixed offset from another entity
PartPositioners.relativeTo(anchor, x, y, z)

// Trails behind the entity based on look direction
PartPositioners.following(distanceBehind, heightOffset)

// Smooth interpolation toward a target position
PartPositioners.lerpTo(target, x, y, z, speed)
```

### Custom Positioner Example

```java
// Part that orbits around the entity's head
PartPositioner orbitPositioner = (entity, partialTick) -> {
    float time = (float)(System.currentTimeMillis() % 4000L) / 4000.0F * 360.0F;
    double radians = Math.toRadians(time);
    double x = entity.getX() + Math.cos(radians) * 1.5;
    double y = entity.getY() + entity.getEyeHeight() + 0.5;
    double z = entity.getZ() + Math.sin(radians) * 1.5;
    return new Vec3(x, y, z);
};
```

---

## Helper Class: `MultipartHelper`

Static convenience methods that wrap `ICustomMultipart`. Safe to call on any entity.

```java
import dev.customhitboxlib.api.MultipartHelper;

// Add a part
MultipartHelper.addPart(entity, "name", 1.0F, 1.0F, positioner);
MultipartHelper.addPart(entity, "name", 1.0F, 1.0F, positioner, true, false, true, false);

// Remove a part
MultipartHelper.removePart(entity, "name");

// Check parts
MultipartHelper.hasParts(entity);

// Control main hitbox
MultipartHelper.setMainHitboxPickable(entity, false);
MultipartHelper.setMainHitboxPushable(entity, false);
MultipartHelper.setMainHitboxCollision(entity, false);
```

All methods return sensible defaults if the entity does not implement `ICustomMultipart`.

---

## Part Entity: `CustomEntityPart`

The actual part entity that gets attached to the parent. You rarely need to create these directly -- use `MultipartHelper` or `ICustomMultipart` instead.

```java
import dev.customhitboxlib.api.CustomEntityPart;

CustomEntityPart part = new CustomEntityPart(parent, "name", 1.0F, 1.0F);
part.setPositioner(myPositioner);
part.setHasCollision(true);
part.setPushable(false);
part.setSuffocate(false);
```

### Key Behaviors

- **Damage forwarding:** `hurt()` forwards all damage to the parent entity
- **Push forwarding:** `push()` forwards knockback to the parent entity
- **Identity:** `is()` returns true for both the part and its parent (prevents double-hits)
- **Interpolation:** `getInterpolatedPosition(partialTick)` uses the positioner for smooth per-frame positions

---

## Render State: `HitboxLibRenderState`

Controls multipart rendering during vanilla debug hitbox rendering.

```java
import dev.customhitboxlib.api.HitboxLibRenderState;

// Suppress multipart rendering (used internally during F3+B)
HitboxLibRenderState.suppressMultipart = true;
```

---

## Entity Part Properties

Each part can independently control:

| Property | Default | Description |
|---|---|---|
| `pickable` | `true` | Can the player target this part with crosshair |
| `pushable` | `false` | Does this part push other entities on collision |
| `collision` | `false` | Does this part collide with blocks (prevents passing through walls) |
| `suffocate` | `false` | Does this part cause suffocation damage inside blocks |

When `collision` is `true`, the mod's pathfinding integration automatically accounts for the part's position during mob navigation.

---

## Rotation Safety

The mod prevents custom parts from clipping into blocks when the entity rotates. Two mechanisms are used:

**Head-body angle limit (`setYRot`):** When `setYRot` is called, the mod checks if the requested yaw would put the head more than 75 degrees away from `yBodyRot`. If so, the rotation is blocked. This prevents parts with rotating positioners from swinging into walls.

**Collision check (`setXRot`, `setYHeadRot`, `setYBodyRot`):** These methods are intercepted and checked against actual block collisions. If applying the new rotation would cause any custom part to intersect a solid block, the rotation is cancelled.

### Datapack vs Java API Positioning

Datapack parts support three positioner types via the `"positioner"` object:

- `"offset"` -- Static offset (world-relative, does not rotate). Equivalent to `PartPositioners.atOffset()`.
- `"rotating"` -- Offset rotates with the entity's body yaw. Equivalent to `PartPositioners.rotating()`.
- `"rotating_head"` -- Offset rotates with the entity's head yaw. Equivalent to `PartPositioners.rotatingHead()`.

The legacy `"offset": [x, y, z]` shorthand defaults to `"type": "offset"`. For more advanced positioners (e.g. `following`, `lerpTo`, custom lambdas), use the Java API.

---

## Mixin Architecture

For advanced users who want to understand what the mod modifies:

| Mixin | Target | Purpose |
|---|---|---|
| `LivingEntityMixin` | `LivingEntity` | Implements `ICustomMultipart` on all living entities |
| `EntityMixin` | `Entity` | Part ticking, block collision, suffocation checks |
| `EntityRotationMixin` | `Entity` | Limits `setYRot` to 75-degree head-body diff; prevents `setXRot` from clipping parts into blocks |
| `LivingEntityRotationMixin` | `LivingEntity` | Prevents `setYHeadRot`/`setYBodyRot` from clipping parts into blocks |
| `MoveControlMixin` | `MoveControl` | Part-aware movement for mob AI |
| `PathNavigationMixin` | `PathNavigation` | Part-aware path following |
| `GroundPathNavigationMixin` | `GroundPathNavigation` | Part-aware surface detection |
| `WalkNodeEvaluatorMixin` | `WalkNodeEvaluator` | Part-aware pathfinding start node |
| `ServerGamePacketListenerImplMixin` | `ServerGamePacketListenerImpl` | Skip block validation for multipart players |
| `GameRendererMixin` | `GameRenderer` | Crosshair targeting of custom parts |
| `MixinEntityRenderDispatcher` | `EntityRenderDispatcher` | Wireframe rendering for debug hitboxes |
| `LocalPlayerMixin` | `LocalPlayer` | Skip suffocation push for custom parts |

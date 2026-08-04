# FAQ

## General

### What is CustomHitboxLib?

A library mod that adds multipart entity hitboxes to any living entity. Think of it like the Ender Dragon's multi-part system, but available for any entity.

### Does it work on servers?

Yes. It must be installed on **both client and server**. The server handles collision, pathfinding, and rotation logic. The client handles rendering wireframe hitboxes and crosshair targeting.

### What Minecraft versions are supported?

**Forge 1.20.1** and **NeoForge 1.21.1**. Other versions may be added in the future.

### Is it compatible with other mods?

It should be compatible with most mods. It uses Mixin to modify vanilla behavior, and the changes are focused on entity collision and rendering. If you encounter compatibility issues, report them on the issue tracker.

### What license is it under?

**MIT License** -- free to use, modify, and redistribute in any modpack or project.

---

## Datapacks

### Do I need to write code to use it?

No. You can configure everything via datapacks using JSON files. See [Datapack Guide](Datapack-Guide.md).

### Where do I put my datapack?

In your world's `datapacks/` folder. On a server, this is usually `world/datapacks/`.

### How do I update parts after editing my datapack?

Run `/reload` in-game. Note: `/reload` updates the datapack configuration, but existing entities may not reflect changes until they are reloaded (rejoin the world). New entities spawned after `/reload` will use the updated configuration.

### Can I apply parts to specific entities only?

Yes. Use the `selector` field with NBT matching to target specific entities. See the [Datapack Guide](Datapack-Guide.md) for examples.

### What does `collision: true` do on a part?

The part will collide with blocks, preventing the entity from passing through walls with that part. Mob pathfinding also accounts for collidable parts.

### What does `suffocate: true` do?

The part causes suffocation damage when inside a solid block, similar to how the player suffocates in walls.

---

## Java API

### How do I add parts to my custom entity?

Implement `ICustomMultipart` on your entity class, or use `MultipartHelper.addPart()` which works on any entity.

### Can I add parts to vanilla entities?

Yes. All `LivingEntity` instances automatically implement `ICustomMultipart` via mixin. You can add parts to zombies, skeletons, players, etc.

### How do positioners work?

A `PartPositioner` is a function that receives the parent entity and partial tick, and returns a `Vec3` world position. Called every tick to determine where the part should be.

### Can parts have custom damage handling?

All damage to parts is automatically forwarded to the parent entity via `hurt()`. There's no per-part damage system built in, but you can implement custom logic by extending `CustomEntityPart`.

### How do I make a part follow the entity's rotation?

Use `PartPositioners.rotating(offsetX, offsetY, offsetZ)` which rotates the offset around the entity's `yBodyRot`. For custom rotation logic, write a positioner that reads `entity.getYRot()` and computes position based on the facing direction. See the [Examples](Examples.md) page.

### What is the head-body rotation limit?

The mod limits `setYRot` to a 75-degree difference between the head yaw and body yaw. This prevents parts with rotating positioners from swinging into walls when the player turns quickly. The limit only applies to entities with custom parts.

---

## Troubleshooting

### Parts aren't showing up

1. Make sure CustomHitboxLib is installed on both client and server
2. Check that your datapack is in the correct folder
3. Run `/reload`
4. Press F3+B to see wireframe hitboxes
5. Check the logs for error messages

### Parts are in the wrong position

- Datapack `offset` is relative to the entity's origin (feet position)
- `offset: [0, 0, 0]` places the part at the entity's feet
- `offset: [0, 1.8, 0]` places it 1.8 blocks above the entity's feet

### Entity can't move through walls even with `collision: false`

The **main hitbox** might still have collision enabled. Set `main_hitbox_collision: false` in your datapack, or call `mp.setMainHitboxCollision(false)` in Java.

### Mob pathfinding seems broken

Custom parts with `collision: true` affect mob navigation. If the part is large or poorly positioned, it can confuse the pathfinder. Try adjusting the part size or disabling collision.

### Parts don't render

Wireframe rendering only shows in **F3+B debug mode**. Parts don't have their own textures or models -- they use the debug wireframe visualization.

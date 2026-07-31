# CustomHitboxLib

**Break free from the vanilla square.**

CustomHitboxLib is a Forge 1.20.1 library mod that adds full **multipart entity hitbox** support to any living entity. Just like the vanilla Ender Dragon, any entity can now have multiple independent collision parts with their own size, position, and behavior.

---

## Features

- **Any entity, any shape** -- Attach any number of sub-hitboxes to any `LivingEntity`
- **Datapack support** -- Configure parts via JSON, no code required. Live-reload with `/reload`
- **Developer API** -- Full Java API with built-in positioners, helper methods, and the `ICustomMultipart` interface
- **Smart collision** -- Parts independently control block collision, entity pushing, suffocation, and pick targeting
- **AI integration** -- Mob pathfinding and navigation automatically account for custom part positions
- **Rotation safety** -- Head-body angle limited to 75 degrees; parts can't clip into blocks when the entity rotates (head, body, or pitch)
- **Damage forwarding** -- Hitting any part deals damage to the parent entity, no double-hits
- **Client rendering** -- Green wireframe hitboxes shown in F3+B debug mode, crosshair targeting of individual parts

---

## Quick Start

### For Players (Datapack)

1. Create a datapack with a `data/<namespace>/custom_parts/` folder
2. Add a JSON file defining your parts (see [Datapack Guide](Datapack-Guide.md))
3. Drop it in your world's `datapacks/` folder or server's `world/datapacks/`
4. Run `/reload`

### For Mod Developers (Java API)

1. Add CustomHitboxLib as a dependency in your `build.gradle`
2. Use `MultipartHelper.addPart()` or `ICustomMultipart.addCustomPart()` to define parts
3. See [Java API](Java-API.md) for full documentation

---

## Links

- [Getting Started](Getting-Started.md) -- Installation and setup
- [Datapack Guide](Datapack-Guide.md) -- JSON configuration reference
- [Java API](Java-API.md) -- Developer documentation
- [Examples](Examples.md) -- Code and datapack examples
- [FAQ](FAQ.md) -- Common questions

---

## Requirements

| Dependency | Version |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 47.4.10+ |
| Java | 17 |
| Sides | Client + Server (required on both) |

---

## License

MIT License -- free to use, modify, and redistribute in any modpack or project.

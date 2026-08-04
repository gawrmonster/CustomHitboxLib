# Getting Started

## Installation

### As a Mod Dependency (Java API)

If you're developing a mod that uses CustomHitboxLib:

1. Download the JAR from Modrinth or CurseForge
2. Place it in your `mods/` folder alongside your mod loader
3. Add it as a dependency in your `build.gradle`:

#### Forge 1.20.1

```groovy
repositories {
    maven { url = "https://modrinth.com/maven" }
}

dependencies {
    implementation fg.deobf("com.gawrmonster:customhitboxlib:1.0.0")
}
```

#### NeoForge 1.21.1

```groovy
repositories {
    maven { url = "https://modrinth.com/maven" }
}

dependencies {
    implementation "com.gawrmonster:customhitboxlib:2.0.0"
}
```

### As a Datapack (No Code Required)

If you just want to add custom hitboxes to existing entities:

1. Download the JAR from Modrinth or CurseForge
2. Place it in your `mods/` folder (both client and server need it)
3. Create a datapack -- see [Datapack Guide](Datapack-Guide.md)
4. Drop the datapack in your world's `datapacks/` folder
5. Run `/reload`

---

## Sides

CustomHitboxLib must be installed on **both client and server**:

- **Server side** handles collision detection, pathfinding, rotation safety, and entity logic
- **Client side** handles rendering wireframe hitboxes and crosshair targeting of parts

If only installed on one side, the mod will not function correctly.

---

## Verifying Installation

1. Start Minecraft with the mod installed
2. Open the logs and look for:
   ```
   Loaded X custom part definitions from datapacks
   ```
3. Spawn an entity that has parts defined
4. Press F3+B to see green wireframe hitboxes on the custom parts

---

## Dependencies

| Dependency | Forge 1.20.1 | NeoForge 1.21.1 |
|---|---|---|
| Minecraft | 1.20.1 | 1.21.1 |
| Mod Loader | Forge 47.4.10+ | NeoForge 21.1.77+ |
| Java | 17+ | 21+ |

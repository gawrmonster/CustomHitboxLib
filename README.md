# 🧊 CustomHitboxLib

_Break free from the vanilla square._

**CustomHitboxLib** is a lightweight, powerful library mod for Forge 1.20.1 that revolutionizes entity bounding boxes. It completely bypasses the limitations of Minecraft's hardcoded single, square-based hitboxes (width and height), introducing full **multipart hitbox** support for any entity—just like the vanilla Ender Dragon!

Whether you're developing massive, complex bosses or simply want precise hitboxes for existing creatures, CustomHitboxLib provides a seamless, robust framework to make it happen.

***

## ❓ At a Glance

*   **What does this project add?** CustomHitboxLib is a library mod that brings full **multipart bounding box** support to Forge 1.20.1, overriding the vanilla limitation of single, square-shaped entity hitboxes.
*   **What features does it have?** You can attach any number of independent, precisely sized sub-hitboxes to a single entity (custom entities, vanilla mobs, or players). It handles complex positioning and automatically forwards damage to the main entity.
*   **Why would a user want to download it?** For players, simply install this if another amazing boss or combat mod requires it to function! For developers, this library gives you the tools to easily build massive, dynamically shaped creatures and realistic interactions without fighting hardcoded Minecraft mechanics.

![Demonstration](https://cdn.modrinth.com/data/cached_images/6077f076b3f62cf6838c9db571f29c988f48c8f1.png)

***

## 📁 Full Datapack Support

You don't need to be a programmer to use CustomHitboxLib! Modpack creators and server owners can completely overhaul entity shapes using simple JSON files.

### Shape Your World with JSON
Through the power of Minecraft's built-in Datapack system, you can attach entirely new hitboxes—like wings, tails, or extra heads—to **any existing entity in the game**, whether it's a vanilla zombie or a massive beast from another mod. 

*   **No Code Required:** Define part dimensions, offsets, and behaviors purely through JSON.
*   **Universal Capability:** Works seamlessly on any loaded entity out of the box.
*   **Live Reloading:** Tweak your custom hitboxes and use `/reload` to see the changes instantly in-game!

![Datapack JSON Example](https://cdn.modrinth.com/data/cached_images/d123ebd7199c3488f7c69ee5346511eaf732aa49.png)

***

## 🛠️ Powerful Developer API

While Datapacks are perfect for configuring existing mobs, CustomHitboxLib also offers a robust API for mod developers!

*   **Capability Driven:** Dynamically attach new sub-hitboxes to any entity using Forge Capabilities without needing to modify base classes or write invasive mixins.
*   **Built-in Positioners:** Utilize provided helper methods to easily animate and position parts (e.g., following behind, relative offsets, or smooth interpolation).
*   **Absolute Control:** For fully custom bosses, simply implement our interfaces on your entities to gain direct, fine-grained control over part lifecycles and update logic.

***

## ⚙️ Seamless Integration

CustomHitboxLib is designed for performance and maximum compatibility.

*   **Advanced Collision Overrides:** Completely disable the vanilla hitbox's pick interactions, pushing, block collisions, and suffocation mechanics, transferring all of those crucial checks directly to your new custom multipart hitboxes!
*   **Precise Damage Forwarding:** Striking any custom sub-hitbox flawlessly registers damage and directional knockback onto the main entity.
*   **Non-Intrusive:** Carefully built to run silently in the background, making it the perfect lightweight dependency for complex boss and combat mods. 

Just drop it in your mods folder, load up a supported mod or custom datapack, and experience Minecraft combat in a whole new dimension!

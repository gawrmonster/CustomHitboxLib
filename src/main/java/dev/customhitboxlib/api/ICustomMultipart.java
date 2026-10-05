package dev.customhitboxlib.api;

import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;

public interface ICustomMultipart {
    @Nullable
    PartEntity<?>[] getCustomParts();

    void tickCustomParts();

    void addCustomPart(String name, PartDefinition definition);

    void addCustomPart(String name, PartDefinition definition, String pose);

    void removeCustomPart(String name);

    void removeCustomPart(String name, String pose);

    boolean hasCustomParts();

    void setMainHitboxPickable(boolean pickable);

    boolean isMainHitboxPickable();

    void setMainHitboxPushable(boolean pushable);

    boolean isMainHitboxPushable();

    void setMainHitboxCollision(boolean collision);

    boolean isMainHitboxCollision();

    void setSyncedRotation(float yRot, float xRot, float yBodyRot, float yHeadRot);
}

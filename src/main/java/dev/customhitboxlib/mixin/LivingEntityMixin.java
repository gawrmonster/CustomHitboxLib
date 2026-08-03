package dev.customhitboxlib.mixin;

import dev.customhitboxlib.CustomHitboxLib;
import dev.customhitboxlib.api.CustomEntityPart;
import dev.customhitboxlib.api.HitboxLibRenderState;
import dev.customhitboxlib.api.ICustomMultipart;
import dev.customhitboxlib.api.PartDefinition;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements ICustomMultipart {
    @Unique
    private final Map<String, PartDefinition> hitboxlib$definitions = new LinkedHashMap<>();

    @Unique
    private final Map<String, CustomEntityPart> hitboxlib$instances = new LinkedHashMap<>();

    @Unique
    private PartEntity<?>[] hitboxlib$partArray = null;

    @Unique
    private boolean hitboxlib$initialized = false;

    @Unique
    private boolean hitboxlib$mainHitboxPickable = true;

    @Unique
    private boolean hitboxlib$mainHitboxPushable = true;

    @Unique
    private boolean hitboxlib$mainHitboxCollision = true;

    private LivingEntityMixin() {
        super(null, null);
    }

    @Nullable
    @Override
    public PartEntity<?>[] getParts() {
        if (hitboxlib$partArray != null && hitboxlib$partArray.length > 0) {
            return hitboxlib$partArray;
        }
        return null;
    }

    @Override
    public boolean isMultipartEntity() {
        if (HitboxLibRenderState.suppressMultipart)
            return false;
        return hitboxlib$partArray != null && hitboxlib$partArray.length > 0;
    }

    @Inject(method = "isPickable", at = @At("HEAD"), cancellable = true)
    private void hitboxlib$isPickable(CallbackInfoReturnable<Boolean> cir) {
        if (!hitboxlib$mainHitboxPickable && hitboxlib$partArray != null && hitboxlib$partArray.length > 0) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private void hitboxlib$isPushable(CallbackInfoReturnable<Boolean> cir) {
        if (!hitboxlib$mainHitboxPushable) {
            cir.setReturnValue(false);
        }
    }

    @Override
    public void addCustomPart(String name, PartDefinition definition) {
        hitboxlib$definitions.put(name, definition);
        hitboxlib$initialized = false;
        if (!hitboxlib$initialized) {
            hitboxlib$initialize();
        }
    }

    @Override
    public void removeCustomPart(String name) {
        hitboxlib$definitions.remove(name);
        hitboxlib$instances.remove(name);
        hitboxlib$initialized = false;
        hitboxlib$partArray = null;
    }

    @Override
    public boolean hasCustomParts() {
        return !hitboxlib$definitions.isEmpty();
    }

    @Override
    public PartEntity<?>[] getCustomParts() {
        return getParts();
    }

    @Override
    public void setMainHitboxPickable(boolean pickable) {
        hitboxlib$mainHitboxPickable = pickable;
    }

    @Override
    public boolean isMainHitboxPickable() {
        return hitboxlib$mainHitboxPickable;
    }

    @Override
    public void setMainHitboxPushable(boolean pushable) {
        hitboxlib$mainHitboxPushable = pushable;
    }

    @Override
    public boolean isMainHitboxPushable() {
        return hitboxlib$mainHitboxPushable;
    }

    @Override
    public void setMainHitboxCollision(boolean collision) {
        hitboxlib$mainHitboxCollision = collision;
    }

    @Override
    public boolean isMainHitboxCollision() {
        return hitboxlib$mainHitboxCollision;
    }

    @Override
    public void tickCustomParts() {
        Entity self = (Entity) (Object) this;
        if (hitboxlib$definitions.isEmpty()) {
            return;
        }
        if (!hitboxlib$initialized) {
            hitboxlib$initialize();
        }

        for (Map.Entry<String, PartDefinition> entry : hitboxlib$definitions.entrySet()) {
            CustomEntityPart part = hitboxlib$instances.get(entry.getKey());
            if (part == null) {
                continue;
            }
            PartDefinition def = entry.getValue();
            Vec3 pos = def.positioner().getPosition(self, 1.0F);
            part.xo = part.getX();
            part.yo = part.getY();
            part.zo = part.getZ();
            part.setPos(pos.x, pos.y, pos.z);
            part.setPushable(def.pushable());
            part.setHasCollision(def.collision());
            part.setSuffocate(def.suffocate());
        }

        hitboxlib$partArray = hitboxlib$instances.values().toArray(new PartEntity<?>[0]);
    }

    private void hitboxlib$initialize() {
        Entity self = (Entity) (Object) this;
        for (Map.Entry<String, PartDefinition> entry : hitboxlib$definitions.entrySet()) {
            if (!hitboxlib$instances.containsKey(entry.getKey())) {
                PartDefinition def = entry.getValue();
                CustomEntityPart part = new CustomEntityPart(self, entry.getKey(), def.dimensions(), def.pickable());
                part.setPositioner(def.positioner());
                hitboxlib$instances.put(entry.getKey(), part);
            }
        }
        hitboxlib$initialized = true;
    }

    @Inject(method = "pushEntities", at = @At("HEAD"))
    private void hitboxlib$pushCustomParts(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.noPhysics)
            return;

        Level level = self.level();
        AABB selfAabb = self.getBoundingBox();
        PartEntity<?>[] selfParts = self.getParts();

        Set<Entity> pushedEntities = Collections.newSetFromMap(new IdentityHashMap<>());
        List<CustomEntityPart> selfPushableParts = new ArrayList<>();

        if (selfParts != null) {
            for (PartEntity<?> sp : selfParts) {
                if (sp instanceof CustomEntityPart scp && scp.isPushable()) {
                    selfPushableParts.add(scp);
                }
            }
        }

        AABB searchBox = selfAabb.inflate(4.0);
        level.getEntities(self, searchBox).forEach(other -> {
            if (pushedEntities.contains(other))
                return;
            if (other.noPhysics)
                return;

            // Skip if other is owner of self's parts, or self is owner of other's parts
            if (selfParts != null) {
                for (PartEntity<?> sp : selfParts) {
                    if (sp == other)
                        return;
                    Entity owner = ((PartEntity<?>) sp).getParent();
                    if (owner == other)
                        return;
                }
            }
            if (other instanceof ICustomMultipart otherMp && otherMp.hasCustomParts()) {
                PartEntity<?>[] otherParts = other.getParts();
                if (otherParts != null) {
                    for (PartEntity<?> op : otherParts) {
                        Entity owner = ((PartEntity<?>) op).getParent();
                        if (owner == self)
                            return;
                    }
                }
            }

            if (isMainHitboxPushable() && selfAabb.intersects(other.getBoundingBox())) {
                hitboxlib$pushBoth(self, other, self.getX(), self.getZ(), pushedEntities);
                return;
            }

            if (!selfPushableParts.isEmpty()) {
                for (CustomEntityPart selfPart : selfPushableParts) {
                    AABB partBox = selfPart.getBoundingBox();

                    if (other instanceof ICustomMultipart otherMp && otherMp.hasCustomParts()) {
                        PartEntity<?>[] otherParts = other.getParts();
                        if (otherParts != null) {
                            for (PartEntity<?> otherPart : otherParts) {
                                if (!(otherPart instanceof CustomEntityPart otherCp) || !otherCp.isPushable())
                                    continue;
                                if (!partBox.intersects(otherCp.getBoundingBox()))
                                    continue;

                                hitboxlib$pushBoth(selfPart, otherCp, selfPart.getX(), selfPart.getZ(), pushedEntities);
                                pushedEntities.add(other);
                                return;
                            }
                        }
                    }

                    if (partBox.intersects(other.getBoundingBox())) {
                        hitboxlib$pushPartWithEntity(selfPart, other, selfPart.getX(), selfPart.getZ(), pushedEntities);
                        pushedEntities.add(other);
                        return;
                    }
                }
            }
        });
    }

    private void hitboxlib$pushBoth(Entity self, Entity other, double srcX, double srcZ, Set<Entity> pushedEntities) {
        if (!other.isPushable())
            return;
        if (other.isPassenger())
            return;

        double dx = other.getX() - srcX;
        double dz = other.getZ() - srcZ;
        double d2 = Math.max(Math.abs(dx), Math.abs(dz));
        if (d2 < 0.01)
            return;

        d2 = Math.sqrt(d2);
        dx /= d2;
        dz /= d2;
        double pushX = dx * 0.02;
        double pushZ = dz * 0.02;

        if (!self.isPassenger())
            self.push(-pushX, 0.0, -pushZ);
        other.push(pushX, 0.0, pushZ);
        pushedEntities.add(other);
    }

    private void hitboxlib$pushPartWithEntity(Entity selfPart, Entity other, double srcX, double srcZ,
            Set<Entity> pushedEntities) {
        if (!other.isPushable())
            return;
        if (other.isPassenger())
            return;

        double dx = other.getX() - srcX;
        double dz = other.getZ() - srcZ;
        double d2 = Math.max(Math.abs(dx), Math.abs(dz));
        if (d2 < 0.01)
            return;

        d2 = Math.sqrt(d2);
        dx /= d2;
        dz /= d2;
        double pushX = dx * 0.02;
        double pushZ = dz * 0.02;

        if (!selfPart.isPassenger())
            selfPart.push(-pushX, 0.0, -pushZ);
        other.push(pushX, 0.0, pushZ);
        pushedEntities.add(other);
    }
}
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
import java.util.HashMap;
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

    @Unique
    private final Map<String, Vec3> hitboxlib$syncedPartPositions = new HashMap<>();

    private LivingEntityMixin() {
        super(null, null);
    }

    @Nullable
    @Override
    public PartEntity<?>[] getParts() {
        if (hitboxlib$partArray != null && hitboxlib$partArray.length > 0) {
            return hitboxlib$partArray;
        }
        return super.getParts();
    }

    @Override
    public boolean isMultipartEntity() {
        if (HitboxLibRenderState.suppressMultipart)
            return false;
        if(hasCustomParts()) {
            return hitboxlib$partArray != null && hitboxlib$partArray.length > 0;
        }
        return super.isMultipartEntity();
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
        if (!hitboxlib$initialized) {
            hitboxlib$initialize();
        }
        if (hasCustomParts() && (hitboxlib$partArray == null || hitboxlib$partArray.length == 0)) {
            hitboxlib$partArray = hitboxlib$instances.values().toArray(new PartEntity<?>[0]);
        }
        return hitboxlib$partArray;
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
    public java.util.Map<String, Vec3> getSyncedPartPositions() {
        return hitboxlib$syncedPartPositions;
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

            if (!self.level().isClientSide)
                continue;

            if (!part.hasCollision())
                continue;

            Vec3 lastSynced = hitboxlib$syncedPartPositions.get(entry.getKey());
            if (lastSynced == null || lastSynced.distanceToSqr(pos) > 1.0E-6D) {
                hitboxlib$syncedPartPositions.put(entry.getKey(), pos);
                dev.customhitboxlib.network.CustomPartPositionSyncPacket.send(entry.getKey(), pos);
            }
        }

        hitboxlib$partArray = hitboxlib$instances.values().toArray(new PartEntity<?>[0]);
    }

    private void hitboxlib$initialize() {
        Entity self = (Entity) (Object) this;
        for (Map.Entry<String, PartDefinition> entry : hitboxlib$definitions.entrySet()) {
            if (!hitboxlib$instances.containsKey(entry.getKey())) {
                PartDefinition def = entry.getValue();
                CustomEntityPart part = new CustomEntityPart(self, entry.getKey(), def.dimensions(), def.pickable(), def.pushable(), def.collision(), def.suffocate());
                part.setPositioner(def.positioner());
                Vec3 pos = def.positioner().getPosition(self, 1.0F);
                part.xo = part.getX();
                part.yo = part.getY();
                part.zo = part.getZ();
                part.setPos(pos.x, pos.y, pos.z);
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
        PartEntity<?>[] selfParts = ((ICustomMultipart)self).getCustomParts();

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
                PartEntity<?>[] otherParts = otherMp.getCustomParts();
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
                        PartEntity<?>[] otherParts = otherMp.getCustomParts();
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
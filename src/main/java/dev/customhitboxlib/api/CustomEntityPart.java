package dev.customhitboxlib.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;

public class CustomEntityPart extends PartEntity<Entity> {
    private final EntityDimensions size;
    private final boolean pickable;
    private boolean pushable;
    private boolean hasCollision;
    private boolean isSuffocate;
    private PartPositioner positioner;

    public CustomEntityPart(Entity parent, String name, EntityDimensions size, boolean pickable) {
        super(parent);
        this.size = size;
        this.pickable = pickable;
        if (name != null) {
            setCustomName(net.minecraft.network.chat.Component.literal(name));
        }
        this.refreshDimensions();
    }

    public CustomEntityPart(Entity parent, String name, EntityDimensions size) {
        this(parent, name, size, true);
    }

    public CustomEntityPart(Entity parent, String name, float width, float height, boolean pickable) {
        this(parent, name, EntityDimensions.scalable(width, height), pickable);
    }

    public CustomEntityPart(Entity parent, String name, float width, float height) {
        this(parent, name, EntityDimensions.scalable(width, height), true);
    }

    public void setPositioner(PartPositioner positioner) {
        this.positioner = positioner;
    }

    public PartPositioner getPositioner() {
        return positioner;
    }

    public Vec3 getInterpolatedPosition(float partialTick) {
        if (positioner != null) {
            Entity parent = getParent();
            if (parent != null) {
                return positioner.getPosition(parent, partialTick);
            }
        }
        return position();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public boolean isPickable() {
        return pickable;
    }

    @Override
    public boolean isPushable() {
        return pushable;
    }

    public void setPushable(boolean pushable) {
        this.pushable = pushable;
    }

    public boolean hasCollision() {
        return hasCollision;
    }

    public void setHasCollision(boolean hasCollision) {
        this.hasCollision = hasCollision;
    }

    public boolean isSuffocate() {
        return isSuffocate;
    }

    public void setSuffocate(boolean suffocate) {
        this.isSuffocate = suffocate;
    }

    @Override
    public boolean canBeCollidedWith() {
        return getParent().canBeCollidedWith();
    }

    @Override
    public void push(double pX, double pY, double pZ) {
        Entity parent = getParent();
        if (parent != null && pushable) {
            parent.setDeltaMovement(parent.getDeltaMovement().add(pX, pY, pZ));
            parent.hurtMarked = true;
        }
    }

    @Override
    public boolean is(Entity entity) {
        return this == entity || getParent() == entity;
    }

    @Override
    public boolean isMultipartEntity() {
        return false;
    }

    @Override
    public PartEntity<?>[] getParts() {
        return null;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity sourceEntity = source.getEntity();
        if (sourceEntity == getParent()) return false;
        return getParent().hurt(source, amount);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return size;
    }
}

package dev.customhitboxlib.mixin;

import dev.customhitboxlib.api.CustomEntityPart;
import dev.customhitboxlib.api.ICustomMultipart;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.entity.PartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WalkNodeEvaluator.class)
public abstract class WalkNodeEvaluatorMixin extends net.minecraft.world.level.pathfinder.NodeEvaluator {

    @Shadow
    protected abstract boolean canStartAt(BlockPos p_262596_);

    @Shadow
    protected abstract Node getStartNode(BlockPos p_230632_);

    private double hitboxlib$getLowestCollisionMinY() {
        Mob entity = this.mob;
        PartEntity<?>[] parts = entity.getParts();
        double lowest = Double.MAX_VALUE;
        if (parts != null) {
            for (PartEntity<?> part : parts) {
                if (part instanceof CustomEntityPart cp && cp.hasCollision()) {
                    double minY = cp.getBoundingBox().minY;
                    if (minY < lowest) {
                        lowest = minY;
                    }
                }
            }
        }
        if (entity instanceof ICustomMultipart mp && mp.isMainHitboxCollision()) {
            double mainMinY = entity.getY();
            if (mainMinY < lowest) {
                lowest = mainMinY;
            }
        }
        return lowest == Double.MAX_VALUE ? entity.getY() : lowest;
    }

    @Inject(
        method = "getStart()Lnet/minecraft/world/level/pathfinder/Node;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void hitboxlib$useLowestCollisionMinYForStart(CallbackInfoReturnable<Node> cir) {
        Mob entity = this.mob;
        if (!(entity instanceof ICustomMultipart mp)) {
            return;
        }

        double partMinY = hitboxlib$getLowestCollisionMinY();
        int i = (int) Math.floor(partMinY);

        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
        net.minecraft.world.level.block.state.BlockState blockstate = this.currentContext.getBlockState(blockpos$mutableblockpos.set(entity.getX(), (double)i, entity.getZ()));

        if (!entity.canStandOnFluid(blockstate.getFluidState())) {
            if (this.canFloat() && entity.isInWater()) {
                while (true) {
                    if (!blockstate.is(net.minecraft.world.level.block.Blocks.WATER) && blockstate.getFluidState() != net.minecraft.world.level.material.Fluids.WATER.getSource(false)) {
                        --i;
                        break;
                    }
                    ++i;
                    blockstate = this.currentContext.getBlockState(blockpos$mutableblockpos.set(entity.getX(), (double)i, entity.getZ()));
                }
            } else if (entity.onGround()) {
                i = Mth.floor(partMinY + 0.5D);
            } else {
                BlockPos blockpos;
                for (blockpos = entity.blockPosition(); (this.currentContext.getBlockState(blockpos).isAir() || this.currentContext.getBlockState(blockpos).isPathfindable(net.minecraft.world.level.pathfinder.PathComputationType.LAND)) && blockpos.getY() > entity.level().getMinBuildHeight(); blockpos = blockpos.below()) {
                }
                i = blockpos.above().getY();
            }
        } else {
            while (entity.canStandOnFluid(blockstate.getFluidState())) {
                ++i;
                blockstate = this.currentContext.getBlockState(blockpos$mutableblockpos.set(entity.getX(), (double)i, entity.getZ()));
            }
            --i;
        }

        BlockPos blockpos1 = entity.blockPosition();
        if (!this.canStartAt(blockpos$mutableblockpos.set(blockpos1.getX(), i, blockpos1.getZ()))) {
            AABB aabb = entity.getBoundingBox();
            if (this.canStartAt(blockpos$mutableblockpos.set(aabb.minX, (double)i, aabb.minZ)) || this.canStartAt(blockpos$mutableblockpos.set(aabb.minX, (double)i, aabb.maxZ)) || this.canStartAt(blockpos$mutableblockpos.set(aabb.maxX, (double)i, aabb.minZ)) || this.canStartAt(blockpos$mutableblockpos.set(aabb.maxX, (double)i, aabb.maxZ))) {
                cir.setReturnValue(this.getStartNode(blockpos$mutableblockpos));
                return;
            }
        }
        cir.setReturnValue(this.getStartNode(new BlockPos(blockpos1.getX(), i, blockpos1.getZ())));
    }

}

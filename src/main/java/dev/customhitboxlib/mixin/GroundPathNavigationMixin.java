package dev.customhitboxlib.mixin;

import dev.customhitboxlib.api.CustomEntityPart;
import dev.customhitboxlib.api.ICustomMultipart;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.entity.PartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GroundPathNavigation.class)
public abstract class GroundPathNavigationMixin extends net.minecraft.world.entity.ai.navigation.PathNavigation {

    protected GroundPathNavigationMixin(net.minecraft.world.entity.Mob p_148416_, net.minecraft.world.level.Level p_148417_) {
        super(p_148416_, p_148417_);
    }

    @Inject(
        method = "getSurfaceY()I",
        at = @At("HEAD"),
        cancellable = true
    )
    private void hitboxlib$useLowestCollisionMinYForSurfaceY(CallbackInfoReturnable<Integer> cir) {
        if (this.mob instanceof ICustomMultipart mp) {
            double partMinY = hitboxlib$getLowestCollisionMinY();
            if (partMinY != Double.MAX_VALUE) {
                int blockY = (int) Math.floor(partMinY);
                if (this.mob.isInWater() && this.canFloat()) {
                    BlockState blockstate = this.level.getBlockState(BlockPos.containing(this.mob.getX(), (double)blockY, this.mob.getZ()));
                    int j = 0;

                    while(blockstate.is(net.minecraft.world.level.block.Blocks.WATER)) {
                        ++blockY;
                        blockstate = this.level.getBlockState(BlockPos.containing(this.mob.getX(), (double)blockY, this.mob.getZ()));
                        ++j;
                        if (j > 16) {
                            blockY = (int) Math.floor(partMinY);
                            break;
                        }
                    }
                } else {
                    blockY = (int) Math.floor(partMinY + 0.5D);
                }
                cir.setReturnValue(blockY);
            }
        }
    }

    private double hitboxlib$getLowestCollisionMinY() {
        PartEntity<?>[] parts = ((ICustomMultipart)this.mob).getCustomParts();
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
        if (this.mob instanceof ICustomMultipart mp && mp.isMainHitboxCollision()) {
            double mainMinY = this.mob.getY();
            if (mainMinY < lowest) {
                lowest = mainMinY;
            }
        }
        return lowest;
    }
}

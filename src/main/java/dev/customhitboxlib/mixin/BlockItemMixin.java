package dev.customhitboxlib.mixin;

import dev.customhitboxlib.api.CustomEntityPart;
import dev.customhitboxlib.api.ICustomMultipart;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.entity.PartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {

    @Shadow
    protected abstract boolean mustSurvive();

    @Inject(method = "canPlace", at = @At("HEAD"), cancellable = true)
    private void hitboxlib$checkPartsInsteadOfMainHitbox(BlockPlaceContext context, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        Player player = context.getPlayer();
        if (!(player instanceof ICustomMultipart mp) || mp.isMainHitboxCollision()) {
            return;
        }

        Level level = (Level) context.getLevel();

        if (this.mustSurvive() && !state.canSurvive(level, context.getClickedPos())) {
            cir.setReturnValue(false);
            return;
        }

        CollisionContext collisionContext = CollisionContext.of(player);
        VoxelShape blockShape = state.getCollisionShape(level, context.getClickedPos(), collisionContext);
        if (blockShape.isEmpty()) {
            cir.setReturnValue(true);
            return;
        }

        BlockPos pos = context.getClickedPos();
        VoxelShape movedShape = blockShape.move(pos.getX(), pos.getY(), pos.getZ());

        for (Entity entity : level.getEntities(player, movedShape.bounds())) {
            if (!entity.isRemoved() && entity.blocksBuilding
                    && Shapes.joinIsNotEmpty(movedShape, Shapes.create(entity.getBoundingBox()), BooleanOp.AND)) {
                cir.setReturnValue(false);
                return;
            }
        }

        PartEntity<?>[] parts = player.getParts();
        if (parts != null) {
            for (PartEntity<?> part : parts) {
                if (part instanceof CustomEntityPart cp && cp.hasCollision()) {
                    if (Shapes.joinIsNotEmpty(movedShape, Shapes.create(cp.getBoundingBox()), BooleanOp.AND)) {
                        cir.setReturnValue(false);
                        return;
                    }
                }
            }
        }

        cir.setReturnValue(true);
    }
}

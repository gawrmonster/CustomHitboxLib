package dev.customhitboxlib.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.customhitboxlib.api.CustomEntityPart;
import dev.customhitboxlib.api.ICustomMultipart;
import dev.customhitboxlib.api.MultipartHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ScreenEffectRenderer.class)
public class ScreenEffectRendererMixin {

    @ModifyVariable(method = "renderScreenEffect", at = @At("STORE"), ordinal = 0)
    private static Pair<BlockState, BlockPos> hitboxlib$customPartOverlayCheck(Pair<BlockState, BlockPos> original) {
        if (original == null) return null;

        Player player = Minecraft.getInstance().player;
        if (player == null) return original;

        if (!(player instanceof ICustomMultipart mp)) return original;
        if (!mp.hasCustomParts()) return original;
        if (MultipartHelper.isMainHitboxCollision(player)) return original;

        var parts = mp.getCustomParts();
        if (parts == null) return null;

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (var part : parts) {
            if (!(part instanceof CustomEntityPart cp)) continue;
            if (!cp.hasCollision()) continue;

            Vec3 pos = cp.getInterpolatedPosition(1.0F);
            float width = cp.getDimensions(net.minecraft.world.entity.Pose.STANDING).width;
            float height = cp.getDimensions(net.minecraft.world.entity.Pose.STANDING).height;

            for (int i = 0; i < 8; ++i) {
                double x = pos.x + (((float)((i >> 0) % 2) - 0.5F) * width * 0.8F);
                double y = pos.y + height * 0.5 + (((float)((i >> 1) % 2) - 0.5F) * 0.1F);
                double z = pos.z + (((float)((i >> 2) % 2) - 0.5F) * width * 0.8F);
                mutablePos.set(x, y, z);
                BlockState blockstate = player.level().getBlockState(mutablePos);
                if (blockstate.getRenderShape() != RenderShape.INVISIBLE && blockstate.isViewBlocking(player.level(), mutablePos)) {
                    return Pair.of(blockstate, mutablePos.immutable());
                }
            }
        }

        return null;
    }
}

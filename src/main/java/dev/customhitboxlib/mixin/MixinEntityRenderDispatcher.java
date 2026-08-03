package dev.customhitboxlib.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.customhitboxlib.api.CustomEntityPart;
import dev.customhitboxlib.api.HitboxLibRenderState;
import dev.customhitboxlib.api.ICustomMultipart;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({EntityRenderDispatcher.class})
public abstract class MixinEntityRenderDispatcher {

    @Inject(
        method = {"renderHitbox"},
        at = {@At("HEAD")}
    )
    private static void hitboxlib$onRenderHitboxStart(
        PoseStack pPoseStack,
        VertexConsumer pBuffer,
        Entity pEntity,
        float pRed,
        float pGreen,
        float pBlue,
        float pAlpha,
        CallbackInfo ci
    ) {
        if (pEntity instanceof ICustomMultipart mp && mp.hasCustomParts()) {
            HitboxLibRenderState.suppressMultipart = true;
        }
    }

    @Inject(
        method = {"renderHitbox"},
        at = {@At("TAIL")}
    )
    private static void hitboxlib$renderHitbox(
        PoseStack pPoseStack,
        VertexConsumer pBuffer,
        Entity pEntity,
        float pRed,
        float pGreen,
        float pBlue,
        float pAlpha,
        CallbackInfo ci
    ) {
        PartEntity<?>[] parts = pEntity.getParts();
        HitboxLibRenderState.suppressMultipart = false;
        if (parts == null) return;

        double entityX = pEntity.xo + (pEntity.getX() - pEntity.xo) * pRed;
        double entityY = pEntity.yo + (pEntity.getY() - pEntity.yo) * pRed;
        double entityZ = pEntity.zo + (pEntity.getZ() - pEntity.zo) * pRed;

        for (PartEntity<?> part : parts) {
            if (part == pEntity) continue;

            Vec3 partPos;
            if (part instanceof CustomEntityPart cp && cp.getPositioner() != null) {
                partPos = cp.getInterpolatedPosition(pRed);
                Vec3 tickOffset = new Vec3(
                    pEntity.getX() - entityX,
                    pEntity.getY() - entityY,
                    pEntity.getZ() - entityZ
                );
                partPos = partPos.subtract(tickOffset);
            } else {
                partPos = new Vec3(
                    part.xo + (part.getX() - part.xo) * pRed,
                    part.yo + (part.getY() - part.yo) * pRed,
                    part.zo + (part.getZ() - part.zo) * pRed
                );
            }

            double dx = partPos.x - entityX;
            double dy = partPos.y - entityY;
            double dz = partPos.z - entityZ;

            pPoseStack.pushPose();
            pPoseStack.translate(dx, dy, dz);
            LevelRenderer.renderLineBox(
                pPoseStack, pBuffer,
                part.getBoundingBox().move(-part.getX(), -part.getY(), -part.getZ()),
                0.0F, 1.0F, 0.0F, 1.0F
            );
            pPoseStack.popPose();
        }
    }
}

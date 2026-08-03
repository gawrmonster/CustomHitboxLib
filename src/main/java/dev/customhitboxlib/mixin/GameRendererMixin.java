package dev.customhitboxlib.mixin;

import dev.customhitboxlib.api.CustomEntityPart;
import dev.customhitboxlib.api.ICustomMultipart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "pick", at = @At("RETURN"))
    private void hitboxlib$pickMultipartParts(float pPartialTick, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;

        Entity player = mc.player;
        Vec3 eyePos = player.getEyePosition(pPartialTick);
        Vec3 lookVec = player.getViewVector(pPartialTick);

        double blockReach = mc.gameMode.getPickRange();
        double entityReach = mc.player.getEntityReach();
        double searchRange = Math.max(blockReach, entityReach);

        Vec3 endPos = eyePos.add(lookVec.scale(searchRange));

        AABB searchAABB = player.getBoundingBox()
            .expandTowards(lookVec.scale(searchRange))
            .inflate(1.0D, 1.0D, 1.0D);

        HitResult currentHit = mc.hitResult;
        double bestDistSq = searchRange * searchRange;
        if (currentHit != null && currentHit.getType() != HitResult.Type.MISS) {
            bestDistSq = currentHit.getLocation().distanceToSqr(eyePos);
        }

        for (Entity entity : mc.level.getEntities(player, searchAABB)) {
            if (!(entity instanceof ICustomMultipart mp) || !mp.hasCustomParts()) continue;

            PartEntity<?>[] parts = entity.getParts();
            if (parts == null) continue;

            for (PartEntity<?> part : parts) {
                if (!(part instanceof CustomEntityPart customPart)) continue;

                AABB partAABB = customPart.getBoundingBox();
                Optional<Vec3> intercept = partAABB.clip(eyePos, endPos);
                if (intercept.isPresent()) {
                    double distSq = eyePos.distanceToSqr(intercept.get());
                    if (distSq < bestDistSq) {
                        bestDistSq = distSq;
                        currentHit = new EntityHitResult(entity, intercept.get());
                    }
                } else if (partAABB.contains(eyePos)) {
                    bestDistSq = 0.0D;
                    currentHit = new EntityHitResult(entity, eyePos);
                }
            }
        }

        mc.hitResult = currentHit;
    }
}

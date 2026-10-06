package dev.customhitboxlib.mixin;

import dev.customhitboxlib.api.CustomEntityPart;
import dev.customhitboxlib.util.CustomPartTracker;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Predicate;

@Mixin(Level.class)
public abstract class LevelMixin {

    @Inject(method = "getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;", at = @At("RETURN"), cancellable = true)
    private void hitboxlib$getEntities(Entity excluded, AABB area, Predicate<? super Entity> predicate, CallbackInfoReturnable<List<Entity>> cir) {
        List<Entity> result = cir.getReturnValue();

        Level level = (Level) (Object) this;
        List<CustomEntityPart> matchingParts = CustomPartTracker.getIntersectingParts(level, area);
        if (matchingParts.isEmpty()) return;

        for (CustomEntityPart part : matchingParts) {
            Entity parent = part.getParent();
            if (parent == null || parent == excluded) continue;

            if (predicate != null && !predicate.test(parent)) continue;
            if (!result.contains(parent)) {
                result.add(parent);
            }
        }
    }
}
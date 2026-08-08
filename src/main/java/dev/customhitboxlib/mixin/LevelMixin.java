package dev.customhitboxlib.mixin;

import dev.customhitboxlib.api.CustomEntityPart;
import dev.customhitboxlib.api.ICustomMultipart;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.entity.PartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Predicate;

@Mixin(Level.class)
public abstract class LevelMixin {

    @Unique
    private static final ThreadLocal<Boolean> hitboxlib$inGetEntities = ThreadLocal.withInitial(() -> false);

    @Inject(method = "getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;", at = @At("RETURN"), cancellable = true)
    private void hitboxlib$getEntities(Entity excluded, AABB area, Predicate<? super Entity> predicate, CallbackInfoReturnable<List<Entity>> cir) {
        if (hitboxlib$inGetEntities.get()) return;

        List<Entity> result = cir.getReturnValue();
        AABB inflated = area.inflate(16.0);

        hitboxlib$inGetEntities.set(true);
        try {
            List<Entity> nearby = ((Level)(Object)this).getEntities(excluded, inflated, e -> true);
            for (Entity entity : nearby) {
                if (result.contains(entity)) continue;
                if (!(entity instanceof ICustomMultipart mp) || !mp.hasCustomParts()) continue;

                PartEntity<?>[] parts = entity.getParts();
                if (parts == null) continue;

                for (PartEntity<?> part : parts) {
                    if (!(part instanceof CustomEntityPart cp) || !cp.isPickable()) continue;
                    if (cp.getBoundingBox().intersects(area)) {
                        result.add(entity);
                        break;
                    }
                }
            }
        } finally {
            hitboxlib$inGetEntities.set(false);
        }
    }
}

package ganymedes01.etfuturum.mixins.early.movement;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ganymedes01.etfuturum.movement.PlayerMovement;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collections;
import java.util.List;

@Mixin(World.class)
public abstract class MixinWorld {
    @WrapOperation(method = "getCollidingBoundingBoxes", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;getEntitiesWithinAABBExcludingEntity(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/AxisAlignedBB;)Ljava/util/List;"), require = 1)
    private List<Entity> etfu$ignoreEntitiesForPose(World world, Entity mover, AxisAlignedBB searchBounds,
                                                     Operation<List<Entity>> original, Entity collisionEntity,
                                                     AxisAlignedBB collisionBounds) {
        // A block can force a smaller pose. An overlapping entity cannot.
        if (PlayerMovement.isPoseCollisionQuery(collisionBounds)) return Collections.emptyList();
        return original.call(world, mover, searchBounds);
    }
}

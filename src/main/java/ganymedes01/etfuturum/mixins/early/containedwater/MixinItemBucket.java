package ganymedes01.etfuturum.mixins.early.containedwater;

import ganymedes01.etfuturum.ModernMapParityBlocks;
import ganymedes01.etfuturum.Tags;
import ganymedes01.etfuturum.core.utils.DripleafWater;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBucket;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemBucket.class)
public abstract class MixinItemBucket {
    @Shadow private Block isFull;

    @Inject(method = "tryPlaceContainedLiquid", at = @At("HEAD"), cancellable = true)
    private void etfu$fillAquatic(World world, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
        if (isFull != Blocks.flowing_water) return;

        // The bucket event can hit the block *behind* a thin plant, and vanilla
        // will then try to pour water into the plant's cell. The FillBucketEvent
        // handler cannot protect this path: it only sees the ray-traced block.
        // Vanilla ItemBucket.tryPlaceContainedLiquid would drop a Material.plants
        // block and replace it with water, deleting both halves of small dripleaf.
        // Since Java 1.19.3 (MC-127110), a full bucket may also be emptied
        // INTO an already-waterlogged block. Report success without changing
        // that block; never fall through to vanilla, which can break plants.
        if (DripleafWater.isDripleaf(world, x, y, z)) {
            boolean alreadyWet = DripleafWater.contains(world, x, y, z);
            boolean placed = alreadyWet || DripleafWater.fill(world, x, y, z);
            if (placed) etfu$playEmptySound(world, x, y, z);
            cir.setReturnValue(placed);
            return;
        }

        if (ModernMapParityBlocks.containsWater(world, x, y, z)) {
            etfu$playEmptySound(world, x, y, z);
            cir.setReturnValue(true);
            return;
        }

        if (ModernMapParityBlocks.fillContainedWater(world, x, y, z)) {
            etfu$playEmptySound(world, x, y, z);
            cir.setReturnValue(true);
        }
    }

    private static void etfu$playEmptySound(World world, int x, int y, int z) {
        if (!world.isRemote)
            world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D,
                    Tags.MC_ASSET_VER + ":item.bucket.empty", 1.0F, 1.0F);
    }
}

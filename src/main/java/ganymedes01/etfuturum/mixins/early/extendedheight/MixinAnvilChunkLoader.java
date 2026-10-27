package ganymedes01.etfuturum.mixins.early.extendedheight;

import ganymedes01.etfuturum.core.utils.WorldHeightCompat;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Restores all configured Overworld sections while leaving other providers unchanged. */
@Mixin(AnvilChunkLoader.class)
public class MixinAnvilChunkLoader {

    @ModifyConstant(method = "readChunkFromNBT", constant = @Constant(intValue = WorldHeightCompat.LEGACY_SECTION_COUNT))
    private int etfu$allocateExtendedSectionArray(int original, World world, NBTTagCompound tag) {
        return WorldHeightCompat.extended(world) ? (WorldHeightCompat.height(world) / 16) : original;
    }
    @Inject(method = "readChunkFromNBT", at = @At("HEAD"))
    private void etfu$refuseTruncatedSections(World world, NBTTagCompound tag, CallbackInfoReturnable<Chunk> cir) {
        if (world.provider == null || world.provider.dimensionId != 0) return;
        int sections = WorldHeightCompat.height(world) / 16;
        NBTTagList stored = tag.getTagList("Sections", 10);
        for (int i = 0; i < stored.tagCount(); i++) {
            if ((stored.getCompoundTagAt(i).getByte("Y") & 255) >= sections)
                throw new IllegalStateException("Saved chunk exceeds this world's configured height; restore its original EFR height before loading");
        }
    }
}

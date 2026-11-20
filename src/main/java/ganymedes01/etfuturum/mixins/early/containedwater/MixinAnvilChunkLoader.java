package ganymedes01.etfuturum.mixins.early.containedwater;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ganymedes01.etfuturum.ModernMapParityBlocks;
import ganymedes01.etfuturum.core.utils.KelpAgeMigration;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AnvilChunkLoader.class)
public abstract class MixinAnvilChunkLoader {
    // Wrap the call so migration surrounds both vanilla's loader and ChunkAPI's replacement.
    @WrapOperation(method = "checkedReadChunkFromNBT__Async", remap = false, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/storage/AnvilChunkLoader;readChunkFromNBT(Lnet/minecraft/world/World;Lnet/minecraft/nbt/NBTTagCompound;)Lnet/minecraft/world/chunk/Chunk;",
            remap = true))
    private Chunk etfu$migrateKelp(AnvilChunkLoader loader, World world, NBTTagCompound level, Operation<Chunk> original) {
        boolean migrated = KelpAgeMigration.migrate(level,
                Block.getIdFromBlock(ModernMapParityBlocks.KELP.get()),
                Block.getIdFromBlock(ModernMapParityBlocks.KELP_AGE_16.get()));
        Chunk chunk = original.call(loader, world, level);
        if (migrated) chunk.setChunkModified();
        return chunk;
    }
}

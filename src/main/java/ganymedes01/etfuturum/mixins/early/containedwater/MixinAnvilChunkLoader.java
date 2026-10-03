package ganymedes01.etfuturum.mixins.early.containedwater;

import ganymedes01.etfuturum.ModernMapParityBlocks;
import ganymedes01.etfuturum.core.utils.KelpAgeMigration;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnvilChunkLoader.class)
public abstract class MixinAnvilChunkLoader {
    @Unique private final ThreadLocal<Boolean> etfu$migratedKelp = new ThreadLocal<Boolean>();
    @Inject(method = "readChunkFromNBT", at = @At("HEAD"))
    private void etfu$migrateKelp(World world, NBTTagCompound level, CallbackInfoReturnable<Chunk> cir) {
        etfu$migratedKelp.set(KelpAgeMigration.migrate(level,
                Block.getIdFromBlock(ModernMapParityBlocks.KELP.get()),
                Block.getIdFromBlock(ModernMapParityBlocks.KELP_AGE_16.get())));
    }
    @Inject(method = "readChunkFromNBT", at = @At("RETURN"))
    private void etfu$saveMigration(World world, NBTTagCompound level, CallbackInfoReturnable<Chunk> cir) {
        if (Boolean.TRUE.equals(etfu$migratedKelp.get())) cir.getReturnValue().setChunkModified();
        etfu$migratedKelp.remove();
    }
}

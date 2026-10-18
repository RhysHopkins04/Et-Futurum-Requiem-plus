package ganymedes01.etfuturum.mixins.early.imported;

import cpw.mods.fml.common.registry.GameRegistry;
import ganymedes01.etfuturum.world.imported.ImportedPopulationGuard;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChunkProviderServer.class)
public class MixinChunkProviderServer {
    @Shadow public WorldServer worldObj;
    @Redirect(method="populate",at=@At(value="INVOKE",target="Lnet/minecraft/world/chunk/IChunkProvider;populate(Lnet/minecraft/world/chunk/IChunkProvider;II)V"),require=1)
    private void etfu$guardVanillaPopulation(IChunkProvider generator,IChunkProvider provider,int x,int z) {
        ImportedPopulationGuard.run(worldObj,()->generator.populate(provider,x,z));
    }
    @Redirect(method="populate",at=@At(value="INVOKE",target="Lcpw/mods/fml/common/registry/GameRegistry;generateWorld(IILnet/minecraft/world/World;Lnet/minecraft/world/chunk/IChunkProvider;Lnet/minecraft/world/chunk/IChunkProvider;)V",remap=false),require=1)
    private void etfu$guardForgePopulation(int x,int z,World world,IChunkProvider generator,IChunkProvider provider) {
        ImportedPopulationGuard.run(world,()->GameRegistry.generateWorld(x,z,world,generator,provider));
    }
}

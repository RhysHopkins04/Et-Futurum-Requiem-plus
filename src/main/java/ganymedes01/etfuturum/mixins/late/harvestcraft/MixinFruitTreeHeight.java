package ganymedes01.etfuturum.mixins.late.harvestcraft;

import ganymedes01.etfuturum.core.utils.WorldHeightCompat;
import net.minecraft.world.World;
import java.util.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Native HarvestCraft saplings must also grow on imported high terrain. */
@Pseudo
@Mixin(targets={"com.pam.harvestcraft.WorldGenPamFruitTree","com.pam.harvestcraft.WorldGenPamFruitLogTree"},remap=false)
public abstract class MixinFruitTreeHeight {
    // The inherited vanilla method is SRG-named in the shipped mod and MCP-named
    // after development remapping. No optional-mod classes enter the classpath.
    @ModifyConstant(method={"generate","func_76484_a"},constant=@Constant(intValue=256),remap=false,require=1)
    private int etfu$treeHeight(int original,World world,Random random,int x,int y,int z) {
        return WorldHeightCompat.heightOf(world);
    }
}

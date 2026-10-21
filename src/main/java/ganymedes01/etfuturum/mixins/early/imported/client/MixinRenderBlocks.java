package ganymedes01.etfuturum.mixins.early.imported.client;

import ganymedes01.etfuturum.world.imported.ImportedWaterProfiles;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.init.Blocks;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Vanilla multiplies top/sides by biome RGB, but renders the bottom in grey. */
@Mixin(RenderBlocks.class)
public abstract class MixinRenderBlocks {
    @Shadow public IBlockAccess blockAccess;

    @Redirect(method="renderBlockLiquid", at=@At(value="INVOKE",
        target="Lnet/minecraft/client/renderer/Tessellator;setColorOpaque_F(FFF)V",ordinal=1),require=1)
    private void etfu$tintWaterUnderside(Tessellator tessellator,float r,float g,float b,
                                       Block block,int x,int y,int z) {
        if(ImportedWaterProfiles.enabled() && (block==Blocks.water || block==Blocks.flowing_water)) {
            int color=block.colorMultiplier(blockAccess,x,y,z);
            r*=((color>>16)&255)/255.0F;
            g*=((color>>8)&255)/255.0F;
            b*=(color&255)/255.0F;
        }
        tessellator.setColorOpaque_F(r,g,b);
    }
}

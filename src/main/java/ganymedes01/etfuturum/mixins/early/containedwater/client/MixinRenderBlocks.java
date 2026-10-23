package ganymedes01.etfuturum.mixins.early.containedwater.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import ganymedes01.etfuturum.ModernMapParityBlocks;
import ganymedes01.etfuturum.blocks.BlockBubbleColumn;
import ganymedes01.etfuturum.client.renderer.block.ContainedWaterAccess;
import ganymedes01.etfuturum.client.renderer.block.ContainedWaterShaderCompat;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.RenderBlocks;
import com.gtnewhorizon.gtnhlib.client.renderer.TessellatorManager;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RenderBlocks.class)
public abstract class MixinRenderBlocks {
    @Shadow public IBlockAccess blockAccess;

    @WrapMethod(method = "renderBlockLiquid")
    private boolean etfu$fluidView(Block block, int x, int y, int z, Operation<Boolean> original) {
        IBlockAccess source = blockAccess;
        // Renderers may already supply a contained-water view. Its queried block is
        // water too, so scope the actual emitted fluid rather than reclassifying plants.
        boolean contained = block.getMaterial() == Material.water
                && (source instanceof ContainedWaterAccess || source.getBlock(x, y, z) instanceof BlockBubbleColumn
                    || ModernMapParityBlocks.containsWater(source, x, y, z));
        try (ContainedWaterShaderCompat.Scope shader = ContainedWaterShaderCompat.water(contained ? TessellatorManager.get() : null)) {
            if (!(source instanceof ContainedWaterAccess)) blockAccess = new ContainedWaterAccess(source);
            return original.call(block, x, y, z);
        } finally {
            blockAccess = source;
        }
    }
}

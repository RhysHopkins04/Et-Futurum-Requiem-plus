package ganymedes01.etfuturum.mixins.early.containedwater.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import ganymedes01.etfuturum.ModernMapParityBlocks;
import ganymedes01.etfuturum.blocks.BlockBubbleColumn;
import ganymedes01.etfuturum.client.renderer.block.ContainedWaterAccess;
import ganymedes01.etfuturum.client.renderer.block.ContainedWaterShaderCompat;
import ganymedes01.etfuturum.core.utils.NativeWaterlogging;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockEnderChest;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.RenderBlocks;
import com.gtnewhorizon.gtnhlib.client.renderer.TessellatorManager;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RenderBlocks.class)
public abstract class MixinRenderBlocks {
    @Shadow public IBlockAccess blockAccess;

    @WrapMethod(method = "renderBlockByRenderType")
    private boolean etfu$dispatchContainedWater(Block block, int x, int y, int z, Operation<Boolean> original) {
        IBlockAccess source = blockAccess;
        if (block.getMaterial() != Material.water || !NativeWaterlogging.contains(source, x, y, z))
            return original.call(block, x, y, z);

        // BetterFoliage chooses a render type by looking up the block at this position.
        // Show it the liquid during the overlay draw so it does not select the host's renderer.
        blockAccess = new ContainedWaterAccess(source);
        try {
            return original.call(block, x, y, z);
        } finally {
            blockAccess = source;
        }
    }

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

    @WrapOperation(method = "renderBlockLiquid", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/block/Block;shouldSideBeRendered(Lnet/minecraft/world/IBlockAccess;IIII)Z"))
    private boolean etfu$hideCoveredFace(Block liquid, IBlockAccess access, int nx, int ny, int nz, int side,
                                         Operation<Boolean> original) {
        if (liquid.getMaterial() == Material.water && access instanceof ContainedWaterAccess) {
            IBlockAccess host = ((ContainedWaterAccess) access).source();
            ForgeDirection face = ForgeDirection.getOrientation(side);
            int x = nx - face.offsetX, y = ny - face.offsetY, z = nz - face.offsetZ;
            if (NativeWaterlogging.contains(host, x, y, z) && NativeWaterlogging.waterFaceHidden(host, x, y, z, face))
                return false;
        }
        return original.call(liquid, access, nx, ny, nz, side);
    }

    @ModifyExpressionValue(method = "renderBlockLiquid", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/renderer/RenderBlocks;getLiquidHeight(IIILnet/minecraft/block/material/Material;)F"))
    private float etfu$containedSurfaceHeight(float height, @Local(argsOnly = true) Block liquid,
            @Local(argsOnly = true, ordinal = 0) int x, @Local(argsOnly = true, ordinal = 1) int y,
            @Local(argsOnly = true, ordinal = 2) int z) {
        if (liquid.getMaterial() != Material.water || !(blockAccess instanceof ContainedWaterAccess)) return height;
        ContainedWaterAccess water = (ContainedWaterAccess) blockAccess;
        IBlockAccess host = water.source();
        if (!NativeWaterlogging.contains(host, x, y, z)) return height;
        Block block = host.getBlock(x, y, z);
        if (block instanceof BlockChest || block instanceof BlockEnderChest)
            return water.chestWaterHeight(x, y, z, height);
        if (block instanceof BlockSlab && (host.getBlockMetadata(x, y, z) & 8) != 0)
            return Math.min(height, .5F);
        return height;
    }
}

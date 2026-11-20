package ganymedes01.etfuturum.mixins.late.containedwater.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import ganymedes01.etfuturum.client.renderer.block.WaterloggedSectionSnapshot;
import ganymedes01.etfuturum.core.utils.WaterloggedAccess;
import net.minecraft.block.Block;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

@Pseudo
@Mixin(targets = "com.gtnewhorizons.angelica.rendering.celeritas.world.WorldSlice", remap = false)
public abstract class MixinAngelicaWaterloggedSlice implements WaterloggedAccess {
    @Shadow private int baseX;
    @Shadow private int baseY;
    @Shadow private int baseZ;
    @Shadow public static int getLocalSectionIndex(int x, int y, int z) { throw new AssertionError(); }
    @Unique private final WaterloggedAccess[] etfu$water = new WaterloggedAccess[64];

    @WrapOperation(method = "copyData", at = @At(value = "INVOKE", target =
            "Lcom/gtnewhorizons/angelica/rendering/celeritas/world/WorldSlice;unpackBlockData([Lnet/minecraft/block/Block;[ILcom/gtnewhorizons/angelica/rendering/celeritas/world/cloned/ClonedChunkSection;Lnet/minecraft/world/gen/structure/StructureBoundingBox;)V"))
    private void etfu$copyWater(@Coerce Object slice, Block[] blocks, int[] meta, @Coerce Object section,
                               StructureBoundingBox volume, Operation<Void> original, @Local(name = "idx") int index) {
        original.call(slice, blocks, meta, section, volume);
        etfu$water[index] = ((WaterloggedSectionSnapshot) section).etfu$waterloggingSnapshot();
    }

    @Override public boolean etfu$isWaterlogged(int x, int y, int z) {
        int sx = (x - baseX) >> 4, sy = (y - baseY) >> 4, sz = (z - baseZ) >> 4;
        if (sx < 0 || sy < 0 || sz < 0 || sx >= 3 || sy >= 3 || sz >= 3) return false;
        WaterloggedAccess section = etfu$water[getLocalSectionIndex(sx, sy, sz)];
        return section != null && section.etfu$isWaterlogged(x, y, z);
    }
}

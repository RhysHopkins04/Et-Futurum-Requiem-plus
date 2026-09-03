package ganymedes01.etfuturum.client.model;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.model.ModelChest;
import net.minecraft.client.model.ModelRenderer;

/**
 * One modern double-chest half using the 1.21.11 15x14 geometry/64x64 UV contract.
 * Modern Copper Chests use independent left/right 64x64 sprites rather than 1.7's
 * single 128x64 double-chest texture.
 */
@SideOnly(Side.CLIENT)
public final class ModelCopperChestHalf extends ModelChest {

    public ModelCopperChestHalf(boolean left) {
        float bodyX = left ? 0.0F : 1.0F;

        chestLid = (new ModelRenderer(this, 0, 0)).setTextureSize(64, 64);
        chestLid.addBox(0.0F, -5.0F, -14.0F, 15, 5, 14, 0.0F);
        chestLid.rotationPointX = bodyX;
        chestLid.rotationPointY = 7.0F;
        chestLid.rotationPointZ = 15.0F;

        chestBelow = (new ModelRenderer(this, 0, 19)).setTextureSize(64, 64);
        chestBelow.addBox(0.0F, 0.0F, 0.0F, 15, 10, 14, 0.0F);
        chestBelow.rotationPointX = bodyX;
        chestBelow.rotationPointY = 6.0F;
        chestBelow.rotationPointZ = 1.0F;

        chestKnob = (new ModelRenderer(this, 0, 0)).setTextureSize(64, 64);
        chestKnob.addBox(0.0F, -2.0F, -15.0F, 1, 4, 1, 0.0F);
        chestKnob.rotationPointX = left ? 0.0F : 15.0F;
        chestKnob.rotationPointY = 7.0F;
        chestKnob.rotationPointZ = 15.0F;
    }
}

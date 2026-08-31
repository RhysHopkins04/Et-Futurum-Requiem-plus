package ganymedes01.etfuturum.client.renderer.tileentity;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.ModernMapParityBlocks;
import ganymedes01.etfuturum.client.OpenGLHelper;
import ganymedes01.etfuturum.Tags;
import net.minecraft.client.model.ModelChest;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.ResourceLocation;

/**
 * Basic functional Copper Chest renderer. The block/TileEntity reuse vanilla 1.7 chest inventory,
 * adjacency and lid logic; this renderer only swaps in the correct 1.21.11 copper chest texture.
 * Copper Golem sorting/automation is intentionally outside the visual-map-parity layer.
 */
@SideOnly(Side.CLIENT)
public final class TileEntityParityCopperChestRenderer extends TileEntitySpecialRenderer {
    private final ModelChest model = new ModelChest();

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks) {
        if (!(tile instanceof TileEntityChest)) return;
        TileEntityChest chest = (TileEntityChest) tile;

        ModernMapParityBlocks entry = ModernMapParityBlocks.fromBlock(chest.getBlockType());
        if (entry == null || !entry.getRegistryName().endsWith("copper_chest")) return;

        bindTexture(textureFor(entry));

        int metadata = chest.hasWorldObj() ? chest.getBlockMetadata() : 3;
        short yaw = 0;
        if (metadata == 2) yaw = 180;
        else if (metadata == 4) yaw = 90;
        else if (metadata == 5) yaw = -90;

        OpenGLHelper.pushMatrix();
        OpenGLHelper.enableRescaleNormal();
        OpenGLHelper.colour(1.0F, 1.0F, 1.0F);
        OpenGLHelper.translate((float) x, (float) y + 1.0F, (float) z + 1.0F);
        OpenGLHelper.scale(1.0F, -1.0F, -1.0F);
        OpenGLHelper.translate(0.5F, 0.5F, 0.5F);
        OpenGLHelper.rotate(yaw, 0.0F, 1.0F, 0.0F);
        OpenGLHelper.translate(-0.5F, -0.5F, -0.5F);

        float lid = chest.prevLidAngle + (chest.lidAngle - chest.prevLidAngle) * partialTicks;
        lid = 1.0F - lid;
        lid = 1.0F - lid * lid * lid;
        model.chestLid.rotateAngleX = -(lid * (float) Math.PI / 2.0F);
        model.renderAll();

        OpenGLHelper.disableRescaleNormal();
        OpenGLHelper.popMatrix();
        OpenGLHelper.colour(1.0F, 1.0F, 1.0F);
    }

    private static ResourceLocation textureFor(ModernMapParityBlocks entry) {
        String name = entry.getRegistryName();
        String texture;
        if (name.contains("oxidized")) texture = "copper_oxidized";
        else if (name.contains("weathered")) texture = "copper_weathered";
        else if (name.contains("exposed")) texture = "copper_exposed";
        else texture = "copper";
        return new ResourceLocation(Tags.MC_ASSET_VER, "textures/entity/chest/" + texture + ".png");
    }
}

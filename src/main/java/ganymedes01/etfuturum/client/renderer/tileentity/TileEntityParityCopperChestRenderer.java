package ganymedes01.etfuturum.client.renderer.tileentity;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.ModernMapParityBlocks;
import ganymedes01.etfuturum.Tags;
import ganymedes01.etfuturum.client.OpenGLHelper;
import ganymedes01.etfuturum.client.model.ModelCopperChestHalf;
import net.minecraft.client.model.ModelChest;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * Copper Chest renderer with the modern single/left/right texture contract.
 * Pairing is provided by ParityCopperChestTileEntity and is restricted to the
 * exact same Copper Chest block identity.
 */
@SideOnly(Side.CLIENT)
public final class TileEntityParityCopperChestRenderer extends TileEntitySpecialRenderer {
    private enum Half { SINGLE, LEFT, RIGHT }

    private final ModelChest singleModel = new ModelChest();
    private final ModelChest leftModel = new ModelCopperChestHalf(true);
    private final ModelChest rightModel = new ModelCopperChestHalf(false);

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks) {
        if (!(tile instanceof ModernMapParityBlocks.ParityCopperChestTileEntity)) return;
        TileEntityChest chest = (TileEntityChest) tile;

        ModernMapParityBlocks entry = ModernMapParityBlocks.fromBlock(chest.getBlockType());
        if (entry == null || !entry.getRegistryName().endsWith("copper_chest")) return;

        if (chest.hasWorldObj()) chest.checkForAdjacentChests();
        int metadata = chest.hasWorldObj() ? chest.getBlockMetadata() : 3;
        Half half = halfFor(chest, metadata);
        ModelChest model = half == Half.LEFT ? leftModel : half == Half.RIGHT ? rightModel : singleModel;
        bindTexture(textureFor(entry, half));

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

        float lid = interpolatedLid(chest, partialTicks);
        model.chestLid.rotateAngleX = -(lid * (float) Math.PI / 2.0F);
        renderWithoutClosedSeamFight(model);

        OpenGLHelper.disableRescaleNormal();
        OpenGLHelper.popMatrix();
        OpenGLHelper.colour(1.0F, 1.0F, 1.0F);
    }

    private static float interpolatedLid(TileEntityChest chest, float partialTicks) {
        float lid = chest.prevLidAngle + (chest.lidAngle - chest.prevLidAngle) * partialTicks;
        TileEntityChest other = chest.adjacentChestXNeg != null ? chest.adjacentChestXNeg
                : chest.adjacentChestXPos != null ? chest.adjacentChestXPos
                : chest.adjacentChestZNeg != null ? chest.adjacentChestZNeg : chest.adjacentChestZPos;
        if (other != null) {
            float otherLid = other.prevLidAngle + (other.lidAngle - other.prevLidAngle) * partialTicks;
            if (otherLid > lid) lid = otherLid;
        }
        lid = 1.0F - lid;
        return 1.0F - lid * lid * lid;
    }

    /**
     * The modern chest lid and base overlap by one model pixel. Rendering the base first and
     * applying a tiny depth bias to the moving lid prevents the closed-state texture shimmer/
     * overlap that occurs with 1.7's ModelChest render order.
     */
    private static void renderWithoutClosedSeamFight(ModelChest model) {
        model.chestKnob.rotateAngleX = model.chestLid.rotateAngleX;
        model.chestBelow.render(0.0625F);
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-1.0F, -1.0F);
        model.chestLid.render(0.0625F);
        model.chestKnob.render(0.0625F);
        GL11.glPolygonOffset(0.0F, 0.0F);
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
    }

    /** Returns the modern visual half relative to the chest's front-facing direction. */
    private static Half halfFor(TileEntityChest chest, int metadata) {
        if (chest.adjacentChestXNeg == null && chest.adjacentChestXPos == null
                && chest.adjacentChestZNeg == null && chest.adjacentChestZPos == null) return Half.SINGLE;

        switch (metadata) {
            case 2: // north: east is player's left
                return chest.adjacentChestXPos != null ? Half.LEFT : Half.RIGHT;
            case 3: // south: west is player's left
                return chest.adjacentChestXNeg != null ? Half.LEFT : Half.RIGHT;
            case 4: // west: north is player's left
                return chest.adjacentChestZNeg != null ? Half.LEFT : Half.RIGHT;
            case 5: // east: south is player's left
                return chest.adjacentChestZPos != null ? Half.LEFT : Half.RIGHT;
            default:
                return Half.SINGLE;
        }
    }

    private static ResourceLocation textureFor(ModernMapParityBlocks entry, Half half) {
        String name = entry.getRegistryName();
        String texture;
        if (name.contains("oxidized")) texture = "copper_oxidized";
        else if (name.contains("weathered")) texture = "copper_weathered";
        else if (name.contains("exposed")) texture = "copper_exposed";
        else texture = "copper";
        if (half == Half.LEFT) texture += "_left";
        else if (half == Half.RIGHT) texture += "_right";
        return new ResourceLocation(Tags.MC_ASSET_VER, "textures/entity/chest/" + texture + ".png");
    }
}

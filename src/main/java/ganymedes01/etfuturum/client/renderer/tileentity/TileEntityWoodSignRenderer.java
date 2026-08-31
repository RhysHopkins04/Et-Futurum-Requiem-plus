package ganymedes01.etfuturum.client.renderer.tileentity;

import ganymedes01.etfuturum.blocks.BlockWoodSign;
import ganymedes01.etfuturum.client.gui.inventory.GuiEditWoodSign;
import ganymedes01.etfuturum.tileentities.TileEntityWoodSign;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.model.ModelSign;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/** EFR wood-sign TESR with modern front/back text, per-side dye and glow state. */
public class TileEntityWoodSignRenderer extends TileEntitySpecialRenderer {

    private final ModelSign model = new ModelSign();

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks) {
        if (!(tile instanceof TileEntityWoodSign)) return;
        boolean vanilla = tile.getBlockType() == Blocks.standing_sign || tile.getBlockType() == Blocks.wall_sign;
        BlockWoodSign block = tile.getBlockType() instanceof BlockWoodSign ? (BlockWoodSign) tile.getBlockType() : null;
        if (!vanilla && block == null) return;

        TileEntityWoodSign sign = (TileEntityWoodSign) tile;
        boolean standing = vanilla ? tile.getBlockType() == Blocks.standing_sign : block.standing;

        GL11.glPushMatrix();
        float modelScale = 0.6666667F;
        float yaw;

        if (standing) {
            GL11.glTranslatef((float) x + 0.5F, (float) y + 0.75F * modelScale, (float) z + 0.5F);
            yaw = tile.getBlockMetadata() * 360.0F / 16.0F;
            GL11.glRotatef(-yaw, 0.0F, 1.0F, 0.0F);
            model.signStick.showModel = true;
        } else {
            int meta = tile.getBlockMetadata();
            yaw = wallYaw(meta);
            GL11.glTranslatef((float) x + 0.5F, (float) y + 0.75F * modelScale, (float) z + 0.5F);
            GL11.glRotatef(-yaw, 0.0F, 1.0F, 0.0F);
            GL11.glTranslatef(0.0F, -0.3125F, -0.4375F);
            model.signStick.showModel = false;
        }

        // Literal 1.7 oak signs keep the vanilla entity texture; EFR wood variants retain
        // their existing per-wood sign texture path.
        bindTexture(vanilla
                ? new ResourceLocation("textures/entity/sign.png")
                : new ResourceLocation("textures/entity/signs/" + block.type + ".png"));
        GL11.glPushMatrix();
        GL11.glScalef(modelScale, -modelScale, -modelScale);
        model.renderSign();
        GL11.glPopMatrix();

        FontRenderer font = func_147498_b();
        float textScale = 0.016666668F * modelScale;
        GL11.glTranslatef(0.0F, 0.5F * modelScale, 0.0F);
        GL11.glScalef(textScale, -textScale, textScale);
        GL11.glNormal3f(0.0F, 0.0F, -textScale);
        GL11.glDepthMask(false);

        boolean editor = Minecraft.getMinecraft().currentScreen instanceof GuiEditWoodSign;
        if (editor) GL11.glDisable(GL11.GL_DEPTH_TEST);

        // 0.07 * modelScale was the original vanilla-derived EFR front plane. Expressing it after
        // the text scale keeps it just outside the board and makes the same geometry usable on back.
        float zPixels = (0.07F * modelScale) / textScale;
        if (editor) {
            renderFace(font, sign, sign.isEditingBack(), zPixels, true);
        } else {
            renderFace(font, sign, false, zPixels, false);
            renderFace(font, sign, true, zPixels, false);
        }

        if (editor) GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
    }

    private static void renderFace(FontRenderer font, TileEntityWoodSign sign, boolean back, float zPixels, boolean editor) {
        GL11.glPushMatrix();
        if (back) GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
        GL11.glTranslatef(0.0F, 0.0F, zPixels);
        String[] lines = sign.getText(back);
        int colour = sign.getTextColour(back);
        boolean glowing = sign.isGlowing(back);
        int drawColour = glowing ? colour : darken(colour);
        int outline = colour == 0 ? 0xF0EBCC : darken(colour);
        for (int i = 0; i < 4; i++) {
            String text = lines[i] == null ? "" : lines[i];
            if (editor && i == sign.lineBeingEdited) text = "> " + text + " <";
            int tx = -font.getStringWidth(text) / 2;
            int ty = i * 10 - 20;
            if (glowing) {
                font.drawString(text, tx - 1, ty, outline);
                font.drawString(text, tx + 1, ty, outline);
                font.drawString(text, tx, ty - 1, outline);
                font.drawString(text, tx, ty + 1, outline);
            }
            font.drawString(text, tx, ty, drawColour);
        }
        GL11.glPopMatrix();
    }

    private static int darken(int colour) {
        int r = (int) (((colour >> 16) & 255) * 0.4F);
        int g = (int) (((colour >> 8) & 255) * 0.4F);
        int b = (int) ((colour & 255) * 0.4F);
        return r << 16 | g << 8 | b;
    }

    private static float wallYaw(int meta) {
        if (meta == 2) return 180.0F;
        if (meta == 4) return 90.0F;
        if (meta == 5) return -90.0F;
        return 0.0F;
    }
}

package ganymedes01.etfuturum.client.renderer.tileentity;

import ganymedes01.etfuturum.ModernMapParityBlocks;
import ganymedes01.etfuturum.client.model.ModernJsonModelBridge;
import ganymedes01.etfuturum.client.model.ModernJsonModelBridge.Model;
import ganymedes01.etfuturum.client.model.ModernJsonModelBridge.Quad;
import ganymedes01.etfuturum.client.model.ModernJsonModelBridge.Vertex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiEditSign;
import ganymedes01.etfuturum.client.gui.inventory.GuiEditWoodSign;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import org.lwjgl.opengl.GL11;

/** Extra tile rendering layered over the JSON parity block model. */
public final class TileEntityModernParityRenderer {
    private TileEntityModernParityRenderer() {}

    /** Renders sign text; the modern wooden board/chains are normally drawn by the JSON block model. */
    public static final class Sign extends TileEntitySpecialRenderer {
        @Override
        public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks) {
            if (!(tile instanceof ModernMapParityBlocks.ParitySignTileEntity)) return;
            ModernMapParityBlocks parity = ModernMapParityBlocks.fromBlock(tile.getBlockType());
            if (parity == null) return;
            boolean hanging = parity.getStyle() == ModernMapParityBlocks.Style.HANGING_SIGN;
            if (!hanging && parity.getStyle() != ModernMapParityBlocks.Style.SIGN) return;

            int meta = tile.getBlockMetadata() & 15;
            boolean hangingWallIdentity = hanging && parity.getRegistryName().endsWith("_wall_hanging_sign");
            boolean hangingWallForm = hanging && (hangingWallIdentity || (!hangingWallIdentity && meta >= 8));
            float stateYaw = hanging && !hangingWallIdentity ? hangingYaw(meta) : wallYaw(meta);
            boolean nativeEditorPreview = (Minecraft.getMinecraft().currentScreen instanceof GuiEditSign
                    || Minecraft.getMinecraft().currentScreen instanceof GuiEditWoodSign)
                    && Math.abs(x + 0.5D) < 1.0E-4D
                    && Math.abs(y + 0.75D) < 1.0E-4D
                    && Math.abs(z + 0.5D) < 1.0E-4D;

            if (nativeEditorPreview) renderEditorPreview(parity, meta, stateYaw, x, y, z);

            ModernMapParityBlocks.ParitySignTileEntity sign = (ModernMapParityBlocks.ParitySignTileEntity) tile;
            FontRenderer font = func_147498_b();
            GL11.glPushMatrix();
            GL11.glTranslatef((float) x + 0.5F, (float) y + (hanging ? 0.3125F : 0.625F), (float) z + 0.5F);

            // The JSON bridge rotates ceiling hanging-sign geometry in block coordinates. OpenGL's
            // positive Y rotation is opposite that convention, so the text must use -yaw on the
            // ceiling forms. N/S masked the old error because +/-180 are identical; E/W exposed it.
            float worldYaw = hanging && !hangingWallForm ? -stateYaw : stateYaw;
            GL11.glRotatef(nativeEditorPreview ? -stateYaw : worldYaw, 0.0F, 1.0F, 0.0F);

            float textOffset = hanging ? 0.078F : -0.050F;
            float scale = 0.015625F * (hanging ? 0.9F : 0.6666667F);
            int lineHeight = hanging ? 9 : 10;
            int textCenter = 4 * lineHeight / 2;
            GL11.glTranslatef(0.0F, hanging ? -0.0075F : 0.0F, 0.0F);
            GL11.glScalef(scale, -scale, scale);
            GL11.glNormal3f(0.0F, 0.0F, -1.0F * scale);
            GL11.glDepthMask(false);
            if (nativeEditorPreview) GL11.glDisable(GL11.GL_DEPTH_TEST);

            if (nativeEditorPreview) {
                renderTextFace(font, sign, sign.isEditingBack(), textOffset / scale, lineHeight, textCenter, true);
            } else {
                renderTextFace(font, sign, false, textOffset / scale, lineHeight, textCenter, false);
                renderTextFace(font, sign, true, textOffset / scale, lineHeight, textCenter, false);
            }

            if (nativeEditorPreview) GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(true);
            GL11.glColor4f(1, 1, 1, 1);
            GL11.glPopMatrix();
        }

        private static void renderTextFace(FontRenderer font, ModernMapParityBlocks.ParitySignTileEntity sign,
                boolean back, float zPixels, int lineHeight, int textCenter, boolean editor) {
            GL11.glPushMatrix();
            if (back) GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
            GL11.glTranslatef(0.0F, 0.0F, zPixels);
            String[] lines = sign.getText(back);
            int colour = sign.getTextColour(back);
            boolean glowing = sign.isGlowing(back);
            int drawColour = glowing ? colour : darken(colour);
            int outline = colour == 0 ? 0xF0EBCC : darken(colour);
            for (int i = 0; i < lines.length; i++) {
                String text = lines[i] == null ? "" : lines[i];
                if (editor && i == sign.lineBeingEdited) text = "> " + text + " <";
                int tx = -font.getStringWidth(text) / 2;
                int ty = i * lineHeight - textCenter;
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

        private static float hangingYaw(int meta) {
            if (meta >= 8 && meta <= 11) return wallYaw(2 + (meta - 8));
            return ((meta >> 1) & 3) * 90.0F;
        }

        private static void renderEditorPreview(ModernMapParityBlocks parity, int meta, float yaw,
                double x, double y, double z) {
            ModernJsonModelBridge.PreparedModels prepared = ModernJsonModelBridge.get(parity);
            Model model;
            boolean hanging = parity.getStyle() == ModernMapParityBlocks.Style.HANGING_SIGN;
            boolean hangingWallIdentity = hanging && parity.getRegistryName().endsWith("_wall_hanging_sign");
            if (hanging && !hangingWallIdentity && meta < 8) {
                int attachment = meta & 1;
                model = prepared.facingModels[attachment] != null ? prepared.facingModels[attachment] : prepared.block;
            } else if ((hanging && !hangingWallIdentity && meta >= 8 && meta <= 11) || (meta >= 2 && meta <= 5)) {
                int neutral = hanging && !hangingWallIdentity ? 9 : 3;
                model = prepared.facingModels[neutral] != null ? prepared.facingModels[neutral] : prepared.block;
            } else {
                model = prepared.facingModels[meta] != null ? prepared.facingModels[meta] : prepared.block;
            }
            if (model == null || model.quads.isEmpty()) return;

            Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
            GL11.glPushMatrix();
            GL11.glTranslated(x + 0.5D, y, z + 0.5D);
            GL11.glRotatef(-yaw, 0.0F, 1.0F, 0.0F);
            GL11.glTranslated(-0.5D, 0.0D, -0.5D);
            Tessellator tess = Tessellator.instance;
            tess.startDrawingQuads();
            tess.setBrightness(0x00F000F0);
            tess.setColorOpaque_F(1.0F, 1.0F, 1.0F);
            for (Quad quad : model.quads) {
                IIcon icon = quad.icon;
                if (icon == null) continue;
                for (int i = 0; i < 4; i++) {
                    Vertex vertex = quad.vertices[i];
                    tess.addVertexWithUV(vertex.x, vertex.y, vertex.z,
                            icon.getInterpolatedU(quad.uv[i][0]), icon.getInterpolatedV(quad.uv[i][1]));
                }
            }
            tess.draw();
            GL11.glColor4f(1, 1, 1, 1);
            GL11.glPopMatrix();
        }
    }

    /** Renders the four independently cooking campfire items above the logs. */
    public static final class Campfire extends TileEntitySpecialRenderer {
        private final EntityItem[] renderItems = new EntityItem[4];
        // Modern campfire food occupies four distinct quadrants close to the log corners.
        private static final double[][] POSITIONS = {
                {0.22D, 0.455D, 0.22D}, {0.78D, 0.455D, 0.22D},
                {0.78D, 0.455D, 0.78D}, {0.22D, 0.455D, 0.78D}
        };

        @Override
        public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks) {
            if (!(tile instanceof ModernMapParityBlocks.ParityCampfireTileEntity) || !tile.hasWorldObj()) return;
            ModernMapParityBlocks.ParityCampfireTileEntity campfire = (ModernMapParityBlocks.ParityCampfireTileEntity) tile;
            int facing = tile.getBlockMetadata() & 3;
            for (int slot = 0; slot < 4; slot++) {
                ItemStack stack = campfire.getCookingItem(slot);
                if (stack == null) continue;
                if (renderItems[slot] == null) {
                    renderItems[slot] = new EntityItem(tile.getWorldObj());
                    renderItems[slot].hoverStart = 0.0F;
                }
                EntityItem entity = renderItems[slot];
                entity.setWorld(tile.getWorldObj());
                ItemStack renderStack = stack.copy();
                renderStack.stackSize = 1;
                entity.setEntityItemStack(renderStack);
                entity.age = 0;

                boolean block3d = stack.getItem() instanceof ItemBlock && stack.getItemSpriteNumber() == 0
                        && RenderBlocks.renderItemIn3d(net.minecraft.block.Block.getBlockFromItem(stack.getItem()).getRenderType());
                double[] pos = rotateQuadrant(POSITIONS[slot][0], POSITIONS[slot][2], facing);
                GL11.glPushMatrix();
                GL11.glTranslated(x + pos[0], y + POSITIONS[slot][1], z + pos[1]);
                GL11.glRotatef((slot * 90.0F) - (facing * 90.0F), 0.0F, 1.0F, 0.0F);
                if (!block3d) GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
                GL11.glScalef(0.55F, 0.55F, 0.55F);
                RenderItem.renderInFrame = true;
                RenderManager.instance.renderEntityWithPosYaw(entity, 0.0D, 0.0D, 0.0D, 0.0F, 0.0F);
                RenderItem.renderInFrame = false;
                GL11.glPopMatrix();
            }
        }

        private static double[] rotateQuadrant(double px, double pz, int facing) {
            double x = px - 0.5D;
            double z = pz - 0.5D;
            switch (facing & 3) {
                case 1: return new double[]{0.5D - z, 0.5D + x};
                case 2: return new double[]{0.5D - x, 0.5D - z};
                case 3: return new double[]{0.5D + z, 0.5D - x};
                default: return new double[]{px, pz};
            }
        }
    }
}

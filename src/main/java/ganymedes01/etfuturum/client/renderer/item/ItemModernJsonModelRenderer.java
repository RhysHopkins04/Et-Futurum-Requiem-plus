package ganymedes01.etfuturum.client.renderer.item;

import ganymedes01.etfuturum.ModernMapParityBlocks;
import ganymedes01.etfuturum.client.OpenGLHelper;
import ganymedes01.etfuturum.client.model.ModernJsonModelBridge;
import ganymedes01.etfuturum.client.model.ModernJsonModelBridge.Model;
import ganymedes01.etfuturum.client.model.ModernJsonModelBridge.PreparedModels;
import ganymedes01.etfuturum.client.model.ModernJsonModelBridge.Quad;
import ganymedes01.etfuturum.client.model.ModernJsonModelBridge.Transform;
import ganymedes01.etfuturum.client.model.ModernJsonModelBridge.Vertex;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraftforge.client.IItemRenderer;
import org.lwjgl.opengl.GL11;

/**
 * Inventory/held renderer for modern parity blocks. The geometry and display transforms come from
 * the actual 1.21.11 item-model graph; generated items remain flat instead of being forced through
 * the block-model GUI transform.
 */
public final class ItemModernJsonModelRenderer implements IItemRenderer {
    private final ModernMapParityBlocks entry;

    public ItemModernJsonModelRenderer(ModernMapParityBlocks entry) {
        this.entry = entry;
    }

    @Override
    public boolean handleRenderType(ItemStack stack, ItemRenderType type) {
        return type != ItemRenderType.FIRST_PERSON_MAP;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack stack, ItemRendererHelper helper) {
        if (type == ItemRenderType.INVENTORY && helper == ItemRendererHelper.INVENTORY_BLOCK) {
            PreparedModels prepared = ModernJsonModelBridge.get(entry);
            Model model = prepared.item;
            // Generated sprites use vanilla 16x16 GUI rendering; 3D models keep Forge's standard
            // inventory-block camera. Do not stack a second modern GUI camera on top of it.
            return model == null || model.flatIcon == null;
        }
        return true;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack stack, Object... data) {
        PreparedModels prepared = ModernJsonModelBridge.get(entry);
        Model model = prepared.item;

        OpenGLHelper.pushMatrix();

        // Preserve all caller GL state. This renderer is used by inventory,
        // equipped-item and EntityItem render paths.
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        OpenGLHelper.enableRescaleNormal();
        OpenGLHelper.enableBlend();
        OpenGLHelper.blendFunc(770, 771);
        OpenGLHelper.colour(1.0F, 1.0F, 1.0F);

        boolean flat = model != null && model.flatIcon != null;

        /*
         * Forge 1.7.10 has already applied its legacy EntityItem presentation
         * scale before IItemRenderer receives ENTITY.
         *
         * Remove that legacy scale so Mojang's modern ground/fixed transform is
         * the only item-model scale.
         *
         * Forge also performs ENTITY_ROTATION around local (0,0,0), while the
         * modern model geometry occupies 0..1. Move the centre of that geometry
         * onto the EntityItem origin so it spins in place instead of orbiting
         * around a corner.
         */
        if (type == ItemRenderType.ENTITY) {
            undoForgeEntityPresentation(stack);
            OpenGLHelper.translate(-0.5F, -0.5F, -0.5F);
        }

        /*
         * IMPORTANT: exactly ONE modern display transform is applied here.
         *
         * For ENTITY:
         *   normal dropped item -> "ground"
         *   item frame          -> "fixed"
         *
         * For 3D inventory models Forge already supplied its inventory-block
         * camera, so do not stack Mojang's GUI transform on top of it.
         */
        if (!(type == ItemRenderType.INVENTORY && !flat)) {
            applyModernDisplayTransform(model, type, flat);
        }

        if (type == ItemRenderType.INVENTORY && !flat) {
            applyParityGuiCorrection(entry, model);
        }

        // Modern item models provide their own shading.
        OpenGLHelper.disableLighting();

        if (flat && type == ItemRenderType.INVENTORY) {
            renderFlatInventory(model.flatIcon);
        } else if (flat) {
            /*
             * ItemRenderer.renderItemIn2D builds legacy flat geometry from Z=0
             * backwards to Z=-1/16, giving it a depth centre of -1/32.
             *
             * Modern display transforms assume item geometry is centred on the
             * normal model-space centre Z=0.5. Move only ENTITY flat geometry into
             * that modern basis before it is drawn.
             *
             * 0.5 + 1/32 = 17/32 = 0.53125.
             *
             * This correction is intentionally ENTITY-only so already-correct
             * first/third-person rendering is not changed.
             */
            if (type == ItemRenderType.ENTITY) {
                OpenGLHelper.translate(0.0F, 0.0F, 17.0F / 32.0F);
            }

            renderFlat(model.flatIcon);
        } else if (model != null && !model.quads.isEmpty()) {
            renderQuads(model);
        } else {
            renderFallback(stack);
        }

        GL11.glPopAttrib();
        OpenGLHelper.popMatrix();
    }

    private static Block getItemBlock(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemBlock)) return null;
        return Block.getBlockFromItem(stack.getItem());
    }

    /** Mirrors ForgeHooksClient.renderEntityItem's branch decision for this renderer. */
    private boolean usesForgeEntity3D(ItemStack stack) {
        Block block = getItemBlock(stack);
        return shouldUseRenderHelper(ItemRenderType.ENTITY, stack, ItemRendererHelper.BLOCK_3D)
                || (block != null && RenderBlocks.renderItemIn3d(block.getRenderType()));
    }

    /** Mirrors Forge 1.7.10's EntityItem scale exactly; do not replace with a blanket x4. */
    private float getForgeEntityScale(ItemStack stack) {
        Block block = getItemBlock(stack);
        if (usesForgeEntity3D(stack)) {
            int renderType = block != null ? block.getRenderType() : 1;
            return renderType == 1 || renderType == 19 || renderType == 12 || renderType == 2
                    ? 0.5F : 0.25F;
        }
        return 0.5F;
    }

    /**
     * Cancels only the legacy Forge EntityItem/model-presentation transform which precedes this
     * renderer. Entity bobbing/spin and the outer RenderItemFrame world placement remain intact.
     */
    private void undoForgeEntityPresentation(ItemStack stack) {
        boolean forge3D = usesForgeEntity3D(stack);
        float forgeScale = getForgeEntityScale(stack);
        float inverseScale = 1.0F / forgeScale;
        OpenGLHelper.scale(inverseScale, inverseScale, inverseScale);

        if (forge3D && RenderItem.renderInFrame) {
            // Forge applied: scale(1.25), translate(+0.05Y), rotate(-90Y), then entity scale.
            // Current-matrix operations append on the right, so cancel them in reverse order.
            OpenGLHelper.rotate(90.0F, 0.0F, 1.0F, 0.0F);
            OpenGLHelper.translate(0.0F, -0.05F, 0.0F);
            OpenGLHelper.scale(0.8F, 0.8F, 0.8F);
        }
    }


    /**
     * Small presentation-only corrections for model families whose modern GUI camera is expressed
     * relative to the newer item-model coordinate convention. The world model is untouched.
     */
    private static void applyParityGuiCorrection(ModernMapParityBlocks entry, Model model) {
        String name = entry.getRegistryName();
        float yaw = 0.0F;
        Transform authoredGui = null;
        if ("pale_oak_pressure_plate".equals(name)) {
            // 1.7 pressure-plate inventory rendering deliberately uses a full 16x16 footprint
            // and a 4px-thick centred presentation even though the placed plate is 14x14x1px.
            // Match the neighbouring legacy pressure-plate atlas icons without changing the
            // exact modern world model: [1..15,0..1,1..15] -> [0..16,6..10,0..16].
            OpenGLHelper.translate(0.0F, 6.0F / 16.0F, 0.0F);
            OpenGLHelper.translate(0.5F, 0.0F, 0.5F);
            OpenGLHelper.scale(8.0F / 7.0F, 4.0F, 8.0F / 7.0F);
            OpenGLHelper.translate(-0.5F, 0.0F, -0.5F);
        } else if ("lectern".equals(name) || "dried_ghast".equals(name)) {
            yaw = 180.0F;
        } else if (name.endsWith("_shelf")) {
            // Forge already supplied the standard block GUI rotation/scale. The Shelf template uses
            // those same values but adds its own [2.5, -1.5, 0] GUI translation, so preserve the
            // confirmed -90 degree presentation and apply the translation authored by Mojang's model
            // rather than the guessed Pass 6 recentering offset.
            yaw = -90.0F;
            authoredGui = model == null ? null : model.display.get("gui");
        } else if ("pale_oak_stairs".equals(name) || "resin_brick_stairs".equals(name)) {
            // Reverse the previous correction: the modern stair item camera is mirrored relative
            // to the 1.7 inventory-block camera on this axis.
            yaw = 90.0F;
        } else if ("pale_oak_fence_gate".equals(name)) {
            // Runtime testing confirmed the gate is already correct; keep its existing correction.
            yaw = -90.0F;
        }
        if (authoredGui != null) {
            /*
             * Forge's INVENTORY_BLOCK helper has already installed the normal block GUI camera.
             * Shelf's modern template uses that same camera and only adds [2.5, -1.5, 0].  This
             * translation therefore has to be appended BEFORE our compatibility yaw.  Pass 7 did
             * the opposite and also ran it through the generic model-axis swizzle; the -90 degree
             * yaw consequently rotated +X into the screen-left direction seen at runtime.
             *
             * Keep the authored X/Y/Z values in block-model axes here.  With Forge's existing
             * inventory camera this produces the intended small right/down presentation offset,
             * while the following yaw changes only the shelf's orientation.
             */
            OpenGLHelper.translate(
                    (float) (authoredGui.translation[0] / 16.0D),
                    (float) (authoredGui.translation[1] / 16.0D),
                    (float) (authoredGui.translation[2] / 16.0D));
        }
        if (yaw != 0.0F) {
            OpenGLHelper.translate(0.5F, 0.5F, 0.5F);
            OpenGLHelper.rotate(yaw, 0.0F, 1.0F, 0.0F);
            OpenGLHelper.translate(-0.5F, -0.5F, -0.5F);
        }
    }

    private static void applyModernDisplayTransform(Model model, ItemRenderType type, boolean generatedFlatItem) {
        if (model == null) return;

        String key;
        switch (type) {
            case INVENTORY: key = "gui"; break;
            case ENTITY: key = RenderItem.renderInFrame ? "fixed" : "ground"; break;
            case EQUIPPED_FIRST_PERSON: key = "firstperson_righthand"; break;
            case EQUIPPED: key = "thirdperson_righthand"; break;
            default: key = "fixed"; break;
        }

        Transform transform = model.display.get(key);
        if (transform == null) {
            // minecraft:item/generated intentionally has no GUI transform. In modern Minecraft its
            // layer0 sprite therefore stays front-facing and fills the slot. Treating that absence
            // as the block default was the reason bars/plants/torches became tiny 3D fragments.
            if (type == ItemRenderType.INVENTORY && generatedFlatItem) return;
            applyVanillaDefaultTransform(type);
            return;
        }

        // Coordinate conversion used by GTNHLib's proven modern-model renderer. Modern model
        // display transforms are authored in Blockbench/Minecraft coordinates, not 1.7's GL axes.
        OpenGLHelper.translate(
                (float) (-transform.translation[2] / 16.0D),
                (float) ( transform.translation[1] / 16.0D),
                (float) ( transform.translation[0] / 16.0D));

        OpenGLHelper.translate(0.5F, 0.5F, 0.5F);
        OpenGLHelper.rotate((float) transform.rotation[0], 0.0F, 0.0F, 1.0F);
        OpenGLHelper.rotate((float) transform.rotation[1], 0.0F, 1.0F, 0.0F);
        OpenGLHelper.rotate((float) -transform.rotation[2], 1.0F, 0.0F, 0.0F);
        OpenGLHelper.scale(
                (float) transform.scale[2],
                (float) transform.scale[1],
                (float) transform.scale[0]);
        OpenGLHelper.translate(-0.5F, -0.5F, -0.5F);
    }

    private static void applyVanillaDefaultTransform(ItemRenderType type) {
        OpenGLHelper.translate(0.5F, 0.5F, 0.5F);
        switch (type) {
            case INVENTORY:
                OpenGLHelper.rotate(30.0F, 0.0F, 0.0F, 1.0F);
                OpenGLHelper.rotate(-135.0F, 0.0F, 1.0F, 0.0F);
                OpenGLHelper.scale(0.625F, 0.625F, 0.625F);
                break;
            case EQUIPPED:
                OpenGLHelper.translate(0.0F, 2.5F / 16.0F, 0.0F);
                OpenGLHelper.rotate(75.0F, 0.0F, 0.0F, 1.0F);
                OpenGLHelper.rotate(45.0F, 0.0F, 1.0F, 0.0F);
                OpenGLHelper.scale(0.375F, 0.375F, 0.375F);
                break;
            case EQUIPPED_FIRST_PERSON:
                OpenGLHelper.rotate(45.0F, 0.0F, 1.0F, 0.0F);
                OpenGLHelper.scale(0.4F, 0.4F, 0.4F);
                break;
            case ENTITY:
                OpenGLHelper.translate(0.0F, 3.0F / 16.0F, 0.0F);
                OpenGLHelper.scale(0.25F, 0.25F, 0.25F);
                break;
            default:
                break;
        }
        OpenGLHelper.translate(-0.5F, -0.5F, -0.5F);
    }

    private static void renderQuads(Model model) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.setBrightness(0x00F000F0);

        for (Quad q : model.quads) {
            if (q.icon == null) continue;
            Normal normal = Normal.from(q);
            float shade = diffuseLight(normal.x, normal.y, normal.z);
            t.setColorOpaque_F(shade, shade, shade);
            t.setNormal(normal.x, normal.y, normal.z);
            for (int i = 0; i < 4; i++) {
                Vertex v = q.vertices[i];
                t.addVertexWithUV(
                        v.x,
                        v.y,
                        v.z,
                        q.icon.getInterpolatedU(q.uv[i][0]),
                        q.icon.getInterpolatedV(q.uv[i][1]));
            }
        }
        t.draw();
    }

    private static void renderFlatInventory(IIcon icon) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        // Match vanilla RenderItem exactly. This preserves the source sprite's upright orientation
        // and avoids the 3D ItemRenderer coordinate system that previously flipped generated items.
        new net.minecraft.client.renderer.entity.RenderItem().renderIcon(0, 0, icon, 16, 16);
    }

    private static void renderFlat(IIcon icon) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        ItemRenderer.renderItemIn2D(
                Tessellator.instance,
                icon.getMaxU(), icon.getMinV(), icon.getMinU(), icon.getMaxV(),
                Math.max(1, icon.getIconWidth()), Math.max(1, icon.getIconHeight()), 0.0625F);
    }

    private static void renderFallback(ItemStack stack) {
        IIcon icon = stack.getIconIndex();
        if (icon != null) renderFlat(icon);
    }

    private static float diffuseLight(float nx, float ny, float nz) {
        return Math.min(nx * nx * 0.6F + ny * ny * ((3.0F + ny) / 4.0F) + nz * nz * 0.8F, 1.0F);
    }

    private static final class Normal {
        final float x, y, z;

        Normal(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        static Normal from(Quad q) {
            Vertex v0 = q.vertices[0], v1 = q.vertices[1], v2 = q.vertices[2];
            double ax = v1.x - v0.x, ay = v1.y - v0.y, az = v1.z - v0.z;
            double bx = v2.x - v0.x, by = v2.y - v0.y, bz = v2.z - v0.z;
            double nx = ay * bz - az * by;
            double ny = az * bx - ax * bz;
            double nz = ax * by - ay * bx;
            double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (length < 1.0E-12D) return new Normal(0, 1, 0);
            return new Normal((float) (nx / length), (float) (ny / length), (float) (nz / length));
        }
    }
}

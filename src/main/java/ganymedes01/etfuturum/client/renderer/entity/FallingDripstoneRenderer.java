package ganymedes01.etfuturum.client.renderer.entity;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.blocks.BlockPointedDripstone;
import ganymedes01.etfuturum.entities.EntityFallingDripstone;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

/** Renders the whole collapsed stalactite column carried by {@link EntityFallingDripstone}. */
public class FallingDripstoneRenderer extends Render {
    private final RenderBlocks renderBlocks = new RenderBlocks();

    public FallingDripstoneRenderer() {
        this.shadowSize = 0.5F;
    }

    public void doRender(EntityFallingDripstone entity, double x, double y, double z, float yaw, float partialTicks) {
        World world = entity.func_145807_e();
        Block block = ModBlocks.POINTED_DRIPSTONE.get();
        int blockX = MathHelper.floor_double(entity.posX);
        int blockY = MathHelper.floor_double(entity.posY);
        int blockZ = MathHelper.floor_double(entity.posZ);

        if (block == null) {
            return;
        }

        GL11.glPushMatrix();
        GL11.glTranslatef((float) x, (float) y, (float) z);
        this.bindEntityTexture(entity);
        GL11.glDisable(GL11.GL_LIGHTING);

        this.renderBlocks.blockAccess = world;
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.setTranslation(-blockX - 0.5D, -blockY - 0.5D, -blockZ - 0.5D);

        int count = Math.max(1, entity.getCount());
        for (int index = 0; index < count; index++) {
            int py = blockY - index;
            int meta = BlockPointedDripstone.metadataForGeneratedColumn(false, index, count);
            renderBlocks.drawCrossedSquares(block.getIcon(0, meta), blockX, py, blockZ, 1.0F);
        }

        tessellator.setTranslation(0.0D, 0.0D, 0.0D);
        tessellator.draw();
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glPopMatrix();
    }

    protected ResourceLocation getEntityTexture(EntityFallingBlock entity) {
        return TextureMap.locationBlocksTexture;
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return this.getEntityTexture((EntityFallingBlock) entity);
    }

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        this.doRender((EntityFallingDripstone) entity, x, y, z, yaw, partialTicks);
    }
}

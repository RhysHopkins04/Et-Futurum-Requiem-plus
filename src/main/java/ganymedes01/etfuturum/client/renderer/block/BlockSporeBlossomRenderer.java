package ganymedes01.etfuturum.client.renderer.block;

import com.gtnewhorizons.angelica.api.ThreadSafeISBRH;
import ganymedes01.etfuturum.blocks.BlockSporeBlossom;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

/** Faithful tessellation of the modern spore_blossom block model. */
@ThreadSafeISBRH(perThread = false)
public class BlockSporeBlossomRenderer extends BlockModelBase {

    private static final double P = 1.0D / 16.0D;

    public BlockSporeBlossomRenderer(int modelID) {
        super(modelID);
    }

    @Override
    protected void renderInventoryModel(Block block, int meta, int modelId, RenderBlocks renderer,
                                        double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        Tessellator tess = Tessellator.instance;
        // Custom inventory tessellation does not pass through RenderBlocks' per-face normal setup.
        // Without an explicit normal the GUI model inherits a stale normal from the previous draw
        // and foliage appears much darker than the identical placed block. An upward-facing normal
        // gives thin vegetation models the same neutral inventory lighting baseline as vanilla.
        tess.setNormal(0.0F, 1.0F, 0.0F);
        tess.setColorOpaque_F(1.0F, 1.0F, 1.0F);
        renderModel((BlockSporeBlossom) block, 0, 0, 0, tess);
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {
        Tessellator tess = Tessellator.instance;
        tess.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
        tess.setColorOpaque_F(1.0F, 1.0F, 1.0F);
        renderModel((BlockSporeBlossom) block, x, y, z, tess);
        return true;
    }

    private static void renderModel(BlockSporeBlossom block, double x, double y, double z, Tessellator tess) {
        // Central 14x14 attachment plate: [1,15.9,1] -> [15,15.9,15]
        double[][] base = plane(1 * P, 15.9 * P, 1 * P, 15 * P, 15 * P);
        emitDoubleSided(tess, base, x, y, z, uv(block.getBaseIcon(), 1, 1, 15, 15, 0));

        // Petals mirror the modern JSON exactly: each is a full 16x16 plane extending
        // half a block beyond the owning block and hinged at the central attachment.
        double[][] east = plane(8 * P, 15.7 * P, 0, 24 * P, 16 * P);
        rotateZ(east, 8 * P, 16 * P, -22.5D);
        emitDoubleSided(tess, east, x, y, z, uv(block.getFlowerIcon(), 0, 0, 16, 16, 90));

        double[][] west = plane(-8 * P, 15.7 * P, 0, 8 * P, 16 * P);
        rotateZ(west, 8 * P, 16 * P, 22.5D);
        emitDoubleSided(tess, west, x, y, z, uv(block.getFlowerIcon(), 0, 0, 16, 16, 270));

        double[][] south = plane(0, 15.7 * P, 8 * P, 16 * P, 24 * P);
        rotateX(south, 16 * P, 8 * P, 22.5D);
        emitDoubleSided(tess, south, x, y, z, uv(block.getFlowerIcon(), 16, 16, 0, 0, 0));

        double[][] north = plane(0, 15.7 * P, -8 * P, 16 * P, 8 * P);
        rotateX(north, 16 * P, 8 * P, -22.5D);
        emitDoubleSided(tess, north, x, y, z, uv(block.getFlowerIcon(), 0, 0, 16, 16, 0));
    }

    /** Horizontal plane with coordinates x0/y/z0 to x1/y/z1. */
    private static double[][] plane(double x0, double y, double z0, double x1, double z1) {
        return new double[][]{
                {x0, y, z0},
                {x1, y, z0},
                {x1, y, z1},
                {x0, y, z1}
        };
    }

    private static void rotateZ(double[][] points, double pivotX, double pivotY, double degrees) {
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        for (double[] p : points) {
            double dx = p[0] - pivotX;
            double dy = p[1] - pivotY;
            p[0] = pivotX + dx * cos - dy * sin;
            p[1] = pivotY + dx * sin + dy * cos;
        }
    }

    private static void rotateX(double[][] points, double pivotY, double pivotZ, double degrees) {
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        for (double[] p : points) {
            double dy = p[1] - pivotY;
            double dz = p[2] - pivotZ;
            p[1] = pivotY + dy * cos - dz * sin;
            p[2] = pivotZ + dy * sin + dz * cos;
        }
    }

    private static double[][] uv(IIcon icon, double u0, double v0, double u1, double v1, int rotation) {
        double[][] result = new double[][]{
                {icon.getInterpolatedU(u0), icon.getInterpolatedV(v0)},
                {icon.getInterpolatedU(u1), icon.getInterpolatedV(v0)},
                {icon.getInterpolatedU(u1), icon.getInterpolatedV(v1)},
                {icon.getInterpolatedU(u0), icon.getInterpolatedV(v1)}
        };
        int turns = ((rotation / 90) % 4 + 4) % 4;
        for (int t = 0; t < turns; t++) {
            double[] last = result[3];
            result[3] = result[2];
            result[2] = result[1];
            result[1] = result[0];
            result[0] = last;
        }
        return result;
    }

    private static void emitDoubleSided(Tessellator tess, double[][] p, double x, double y, double z, double[][] uv) {
        for (int i = 0; i < 4; i++) {
            tess.addVertexWithUV(x + p[i][0], y + p[i][1], z + p[i][2], uv[i][0], uv[i][1]);
        }
        for (int i = 3; i >= 0; i--) {
            tess.addVertexWithUV(x + p[i][0], y + p[i][1], z + p[i][2], uv[i][0], uv[i][1]);
        }
    }
}

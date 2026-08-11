package ganymedes01.etfuturum.client.renderer.block;

import com.gtnewhorizons.angelica.api.ThreadSafeISBRH;
import ganymedes01.etfuturum.blocks.BlockSmallDripleaf;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

/**
 * Faithful 1.7.10 tessellation of the modern Java Edition small-dripleaf models.
 * The lower half is the crossed lower stem model; the upper half reproduces the
 * three 7x7 leaves, their 1-pixel side skirts, and the cropped upper stem UVs.
 */
@ThreadSafeISBRH(perThread = false)
public class BlockSmallDripleafRenderer extends BlockModelBase {

    private static final double P = 1.0D / 16.0D;

    public BlockSmallDripleafRenderer(int modelID) {
        super(modelID);
    }

    @Override
    protected void renderInventoryModel(Block block, int meta, int modelId, RenderBlocks renderer,
                                        double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        // Modern small_dripleaf item model inherits small_dripleaf_top, not the complete two-block plant.
        BlockSmallDripleaf leaf = (BlockSmallDripleaf) block;
        Tessellator tess = Tessellator.instance;
        tess.setColorOpaque_F(1.0F, 1.0F, 1.0F);
        renderUpper(leaf, 0, 0, 0, 0, tess);
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {
        BlockSmallDripleaf leaf = (BlockSmallDripleaf) block;
        Tessellator tess = Tessellator.instance;
        int meta = world.getBlockMetadata(x, y, z);
        int facing = BlockSmallDripleaf.getFacing(meta);

        tess.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
        tess.setColorOpaque_F(1.0F, 1.0F, 1.0F);

        if (BlockSmallDripleaf.isUpper(meta)) {
            renderUpper(leaf, facing, x, y, z, tess);
        } else {
            renderLower(leaf, facing, x, y, z, tess);
        }
        return true;
    }

    private static void renderLower(BlockSmallDripleaf leaf, int facing, double x, double y, double z, Tessellator tess) {
        // small_dripleaf_bottom.json: [4.5,0,8] -> [11.5,16,8], +/-45 degrees, rescale=false.
        renderSmallStem(leaf.getStemIcon(false), 4.5D, 11.5D, 16.0D, 3.0D, 14.0D, facing, x, y, z, tess);
    }

    private static void renderUpper(BlockSmallDripleaf leaf, int facing, double x, double y, double z, Tessellator tess) {
        // small_dripleaf_top.json stem: [4.5,0,8] -> [11.5,14,8], UV [4,0,12,14].
        renderSmallStem(leaf.getStemIcon(true), 4.5D, 11.5D, 14.0D, 4.0D, 12.0D, facing, x, y, z, tess);

        // Exact three leaf plates and 1-pixel side skirts from the modern model.
        renderLeafPlate(leaf, 8, 15, 2, 3, 8, 15, facing, x, y, z,
                new double[][] {{8, 8}, {0, 8}, {0, 0}, {8, 0}}, tess);
        renderLeafPlate(leaf, 1, 8, 7, 8, 1, 8, facing, x, y, z,
                new double[][] {{0, 0}, {8, 0}, {8, 8}, {0, 8}}, tess);
        renderLeafPlate(leaf, 1, 8, 11, 12, 8, 15, facing, x, y, z,
                // UV rotation 270 from small_dripleaf_top.json.
                new double[][] {{0, 8}, {0, 0}, {8, 0}, {8, 8}}, tess);
    }

    private static void renderSmallStem(IIcon icon,
                                        double fromXpx, double toXpx, double heightPx,
                                        double u0px, double u1px, int facing,
                                        double x, double y, double z, Tessellator tess) {
        double halfWidth = (toXpx - fromXpx) * P * 0.5D;
        double component = halfWidth * Math.cos(Math.toRadians(45.0D));

        double[][] a = new double[][] {
                {0.5D - component, 0, 0.5D + component},
                {0.5D + component, 0, 0.5D - component},
                {0.5D + component, heightPx * P, 0.5D - component},
                {0.5D - component, heightPx * P, 0.5D + component}
        };
        double[][] b = new double[][] {
                {0.5D - component, 0, 0.5D - component},
                {0.5D + component, 0, 0.5D + component},
                {0.5D + component, heightPx * P, 0.5D + component},
                {0.5D - component, heightPx * P, 0.5D - component}
        };
        rotateY(a, facing);
        rotateY(b, facing);

        double[][] uv = uvRect(icon, u0px, heightPx, u1px, 0);
        emitDoubleSided(tess, a, x, y, z, uv);
        emitDoubleSided(tess, b, x, y, z, uv);
    }

    private static void renderLeafPlate(BlockSmallDripleaf leaf,
                                        double minXpx, double maxXpx,
                                        double sideBottomYpx, double topYpx,
                                        double minZpx, double maxZpx,
                                        int facing, double x, double y, double z,
                                        double[][] topUvPixels, Tessellator tess) {
        // The top planes in vanilla sit fractionally below the side-skirt ceiling to avoid z-fighting.
        double planeY = topYpx == 3.0D ? 2.99D * P : topYpx * P;
        double[][] top = new double[][] {
                {minXpx * P, planeY, minZpx * P},
                {maxXpx * P, planeY, minZpx * P},
                {maxXpx * P, planeY, maxZpx * P},
                {minXpx * P, planeY, maxZpx * P}
        };
        rotateY(top, facing);
        emitDoubleSided(tess, top, x, y, z, uvPixels(leaf.getLeafTopIcon(), topUvPixels));

        double minX = minXpx * P;
        double maxX = maxXpx * P;
        double minY = sideBottomYpx * P;
        double maxY = topYpx * P;
        double minZ = minZpx * P;
        double maxZ = maxZpx * P;

        double[][] north = new double[][] {{minX,minY,minZ},{maxX,minY,minZ},{maxX,maxY,minZ},{minX,maxY,minZ}};
        double[][] south = new double[][] {{maxX,minY,maxZ},{minX,minY,maxZ},{minX,maxY,maxZ},{maxX,maxY,maxZ}};
        double[][] west  = new double[][] {{minX,minY,maxZ},{minX,minY,minZ},{minX,maxY,minZ},{minX,maxY,maxZ}};
        double[][] east  = new double[][] {{maxX,minY,minZ},{maxX,minY,maxZ},{maxX,maxY,maxZ},{maxX,maxY,minZ}};
        rotateY(north, facing);
        rotateY(south, facing);
        rotateY(west, facing);
        rotateY(east, facing);

        double[][] sideUv = uvRect(leaf.getLeafSideIcon(), 0, 1, 8, 0);
        emitDoubleSided(tess, north, x, y, z, sideUv);
        emitDoubleSided(tess, south, x, y, z, sideUv);
        emitDoubleSided(tess, west, x, y, z, sideUv);
        emitDoubleSided(tess, east, x, y, z, sideUv);
    }

    /** Modern blockstate Y rotations: north=0, east=90, south=180, west=270. */
    private static void rotateY(double[][] vertices, int facing) {
        int turns = facing & 3;
        for (double[] vertex : vertices) {
            for (int i = 0; i < turns; i++) {
                double ox = vertex[0] - 0.5D;
                double oz = vertex[2] - 0.5D;
                vertex[0] = 0.5D + oz;
                vertex[2] = 0.5D - ox;
            }
        }
    }

    private static double[][] uvRect(IIcon icon, double u0, double v0, double u1, double v1) {
        return new double[][] {
                {icon.getInterpolatedU(u0), icon.getInterpolatedV(v0)},
                {icon.getInterpolatedU(u1), icon.getInterpolatedV(v0)},
                {icon.getInterpolatedU(u1), icon.getInterpolatedV(v1)},
                {icon.getInterpolatedU(u0), icon.getInterpolatedV(v1)}
        };
    }

    private static double[][] uvPixels(IIcon icon, double[][] uvPixels) {
        double[][] uv = new double[4][2];
        for (int i = 0; i < 4; i++) {
            uv[i][0] = icon.getInterpolatedU(uvPixels[i][0]);
            uv[i][1] = icon.getInterpolatedV(uvPixels[i][1]);
        }
        return uv;
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

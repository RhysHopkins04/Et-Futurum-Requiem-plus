package ganymedes01.etfuturum.client.renderer.block;

import com.gtnewhorizons.angelica.api.ThreadSafeISBRH;
import ganymedes01.etfuturum.blocks.BlockBigDripleaf;
import ganymedes01.etfuturum.blocks.BlockBigDripleafStem;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

/**
 * Faithful 1.7.10 tessellation of the modern Java Edition big-dripleaf block models.
 *
 * The geometry and UV windows mirror the modern vanilla model family:
 * big_dripleaf.json, big_dripleaf_partial_tilt.json, big_dripleaf_full_tilt.json,
 * and big_dripleaf_stem.json. In particular, only the leaf pivots when tilting;
 * the stem remains vertical and anchored near the rear edge of the leaf.
 */
@ThreadSafeISBRH(perThread = false)
public class BlockBigDripleafRenderer extends BlockModelBase {

    private static final double P = 1.0D / 16.0D;

    public BlockBigDripleafRenderer(int modelID) {
        super(modelID);
    }

    @Override
    protected void renderInventoryModel(Block block, int meta, int modelId, RenderBlocks renderer,
                                        double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        Tessellator tess = Tessellator.instance;
        tess.setColorOpaque_F(1.0F, 1.0F, 1.0F);

        if (block instanceof BlockBigDripleaf) {
            BlockBigDripleaf leaf = (BlockBigDripleaf) block;
            renderStem(leaf.getStemIcon(), 0, 0, 0, 15.0D * P, 0, tess);
            renderLeaf(leaf, 0, BlockBigDripleaf.TILT_NONE, 0, 0, 0, tess);
        } else if (block instanceof BlockBigDripleafStem) {
            renderStem(((BlockBigDripleafStem) block).getStemIcon(), 0, 0, 0, 1.0D, 0, tess);
        }
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {
        Tessellator tess = Tessellator.instance;
        tess.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
        tess.setColorOpaque_F(1.0F, 1.0F, 1.0F);

        int meta = world.getBlockMetadata(x, y, z);
        int facing = meta & 3;

        if (block instanceof BlockBigDripleaf) {
            BlockBigDripleaf leaf = (BlockBigDripleaf) block;
            renderStem(leaf.getStemIcon(), x, y, z, 15.0D * P, facing, tess);
            renderLeaf(leaf, facing, BlockBigDripleaf.getTilt(meta), x, y, z, tess);
            return true;
        }

        if (block instanceof BlockBigDripleafStem) {
            renderStem(((BlockBigDripleafStem) block).getStemIcon(), x, y, z, 1.0D, facing, tess);
            return true;
        }

        return false;
    }

    /**
     * Vanilla big-dripleaf stem element: [5,0,12] -> [11,h,12], rotated +/-45 degrees
     * around [8,8,12] with rescale=true. At 45 degrees the rescale produces the exact
     * X footprint below, centred at z=12/16 rather than at the centre of the block.
     */
    private static void renderStem(IIcon icon, double x, double y, double z, double height, int facing, Tessellator tess) {
        double[][] a = new double[][] {
                {5.0D * P, 0, 15.0D * P},
                {11.0D * P, 0, 9.0D * P},
                {11.0D * P, height, 9.0D * P},
                {5.0D * P, height, 15.0D * P}
        };
        double[][] b = new double[][] {
                {5.0D * P, 0, 9.0D * P},
                {11.0D * P, 0, 15.0D * P},
                {11.0D * P, height, 15.0D * P},
                {5.0D * P, height, 9.0D * P}
        };

        rotateY(a, facing);
        rotateY(b, facing);

        // Modern model explicitly crops away the transparent margins of big_dripleaf_stem.png.
        double[][] uv = uvRect(icon, 3, 16, 14, 0);
        emitDoubleSided(tess, a, x, y, z, uv);
        emitDoubleSided(tess, b, x, y, z, uv);
    }

    private static void renderLeaf(BlockBigDripleaf leaf, int facing, int tilt,
                                   double x, double y, double z, Tessellator tess) {
        double angle;
        if (tilt == BlockBigDripleaf.TILT_PARTIAL) {
            angle = -22.5D;
        } else if (tilt == BlockBigDripleaf.TILT_FULL) {
            angle = -45.0D;
        } else {
            // Vanilla's UNSTABLE state deliberately uses the upright model.
            angle = 0.0D;
        }

        // Top plane: [0,15,0] -> [16,15,16].
        double[][] top = new double[][] {
                {0, 15.0D * P, 0},
                {1, 15.0D * P, 0},
                {1, 15.0D * P, 1},
                {0, 15.0D * P, 1}
        };
        transformLeaf(top, angle, facing);

        double[][] topUv = uvRect(leaf.getTopIcon(), 0, 0, 16, 16);
        emitDoubleSided(tess, top, x, y, z, topUv);

        // Front/tip curtain: [0,11,0] -> [16,15,0].
        double[][] tip = new double[][] {
                {0, 11.0D * P, 0},
                {1, 11.0D * P, 0},
                {1, 15.0D * P, 0},
                {0, 15.0D * P, 0}
        };
        transformLeaf(tip, angle, facing);
        emitDoubleSided(tess, tip, x, y, z, uvRect(leaf.getTipIcon(), 0, 4, 16, 0));

        // Side curtains. The modern model uses only the first four V pixels of the side texture;
        // using the full 16x16 icon is why P002 looked almost transparent/wrong in-game.
        double[][] west = new double[][] {
                {0, 11.0D * P, 1},
                {0, 11.0D * P, 0},
                {0, 15.0D * P, 0},
                {0, 15.0D * P, 1}
        };
        double[][] east = new double[][] {
                {1, 11.0D * P, 0},
                {1, 11.0D * P, 1},
                {1, 15.0D * P, 1},
                {1, 15.0D * P, 0}
        };
        transformLeaf(west, angle, facing);
        transformLeaf(east, angle, facing);
        emitDoubleSided(tess, west, x, y, z, uvRect(leaf.getSideIcon(), 0, 4, 16, 0));
        emitDoubleSided(tess, east, x, y, z, uvRect(leaf.getSideIcon(), 16, 4, 0, 0));
    }

    private static void transformLeaf(double[][] vertices, double angleDegrees, int facing) {
        if (angleDegrees != 0.0D) {
            double radians = Math.toRadians(angleDegrees);
            double cos = Math.cos(radians);
            double sin = Math.sin(radians);
            double pivotY = 15.0D * P;
            double pivotZ = 1.0D;

            for (double[] vertex : vertices) {
                double dy = vertex[1] - pivotY;
                double dz = vertex[2] - pivotZ;
                vertex[1] = pivotY + dy * cos - dz * sin;
                vertex[2] = pivotZ + dy * sin + dz * cos;
            }
        }
        rotateY(vertices, facing);
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

    private static void emitDoubleSided(Tessellator tess, double[][] p, double x, double y, double z, double[][] uv) {
        emit(tess, p, x, y, z, uv, false);
        emit(tess, p, x, y, z, uv, true);
    }

    private static void emit(Tessellator tess, double[][] p, double x, double y, double z, double[][] uv, boolean reverse) {
        if (!reverse) {
            for (int i = 0; i < 4; i++) {
                tess.addVertexWithUV(x + p[i][0], y + p[i][1], z + p[i][2], uv[i][0], uv[i][1]);
            }
        } else {
            for (int i = 3; i >= 0; i--) {
                tess.addVertexWithUV(x + p[i][0], y + p[i][1], z + p[i][2], uv[i][0], uv[i][1]);
            }
        }
    }
}

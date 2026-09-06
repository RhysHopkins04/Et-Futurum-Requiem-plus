package ganymedes01.etfuturum.client.renderer.block;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.ModernMapParityBlocks;
import ganymedes01.etfuturum.Tags;
import ganymedes01.etfuturum.client.model.ModernJsonModelBridge;
import ganymedes01.etfuturum.client.model.ModernJsonModelBridge.Model;
import ganymedes01.etfuturum.client.model.ModernJsonModelBridge.Quad;
import ganymedes01.etfuturum.client.model.ModernJsonModelBridge.Vertex;
import ganymedes01.etfuturum.lib.RenderIDs;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/** Draws AssetDirector-loaded modern JSON models directly into the legacy block atlas. */
public final class BlockModernJsonModelRenderer implements ISimpleBlockRenderingHandler {
    private static final double FACE_EPSILON = 1.0E-5D;
    private static volatile int[] dryFoliagePixels;
    private static volatile int dryFoliageWidth;
    private static volatile int dryFoliageHeight;

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
        // BlockModernSapling historically used vanilla crossed-square inventory rendering. Its
        // world render ID is redirected here only so Mangrove Propagule can select modern
        // hanging/age models; preserve the existing Mangrove/Cherry item presentation.
        if (ModBlocks.SAPLING.isEnabled() && block == ModBlocks.SAPLING.get()) {
            Tessellator t = Tessellator.instance;
            t.startDrawingQuads();
            t.setNormal(0.0F, -1.0F, 0.0F);
            renderer.drawCrossedSquares(block.getIcon(0, metadata), -0.5D, -0.5D, -0.5D, 1.0F);
            t.draw();
            return;
        }
        // Normal parity items use ItemModernJsonModelRenderer. This path only exists for callers
        // that bypass Forge's IItemRenderer hook, so keep a conservative vanilla cube fallback.
        renderer.setRenderBoundsFromBlock(block);
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        renderFallbackCube(block, metadata, renderer);
        t.draw();
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
            RenderBlocks renderer) {
        ModernMapParityBlocks entry;
        if (ModBlocks.SAPLING.isEnabled() && block == ModBlocks.SAPLING.get()) {
            int saplingMeta = world.getBlockMetadata(x, y, z);
            if ((saplingMeta & 7) == 0) {
                entry = ModernMapParityBlocks.MANGROVE_PROPAGULE;
            } else {
                renderer.renderCrossedSquares(block, x, y, z);
                return true;
            }
        } else {
            entry = ModernMapParityBlocks.fromBlock(block);
        }
        if (entry == null && ModBlocks.LIGHTNING_ROD.isEnabled() && block == ModBlocks.LIGHTNING_ROD.get()) {
            // The historical Et Futurum lightning_rod registry identity predates the parity layer.
            // Its waxed-default modern sibling is visually identical, so use that exact 1.21.11
            // JSON model graph instead of maintaining a second, incomplete renderer.
            entry = ModernMapParityBlocks.WAXED_LIGHTNING_ROD;
        }
        boolean vanillaCommandBlock = block == Blocks.command_block;
        if (entry == null && !vanillaCommandBlock) return false;

        Model model = vanillaCommandBlock
                ? ModernJsonModelBridge.getVanillaCommandBlockWorldModel(world, x, y, z)
                : ModernJsonModelBridge.getWorldModel(entry, world, x, y, z);
        if (model == null || model.quads.isEmpty()) return renderer.renderStandardBlock(block, x, y, z);

        Tessellator t = Tessellator.instance;
        int meta = world.getBlockMetadata(x, y, z);
        for (Quad q : model.quads) {
            IIcon icon = q.icon != null ? q.icon : block.getIcon(0, meta);
            if (icon == null) continue;

            FaceInfo face = FaceInfo.from(q);
            t.setBrightness(sampleBrightness(block, world, x, y, z, face));
            String registryName = vanillaCommandBlock ? "command_block" : entry.getRegistryName();
            boolean coralFanNoShade = registryName.endsWith("_coral_fan")
                    || registryName.endsWith("_coral_wall_fan");
            float shade = "wildflowers".equals(registryName) || coralFanNoShade
                    ? 1.0F : diffuseLight(face.nx, face.ny, face.nz);
            int tint = vanillaCommandBlock ? 0xFFFFFF : tintColour(entry, q, world, x, y, z);
            float red = ((tint >> 16) & 255) / 255.0F;
            float green = ((tint >> 8) & 255) / 255.0F;
            float blue = (tint & 255) / 255.0F;
            t.setColorOpaque_F(shade * red, shade * green, shade * blue);
            t.setNormal(face.nx, face.ny, face.nz);
            emit(t, q, icon, x, y, z);
        }
        return true;
    }

    /**
     * Mirrors the important part of vanilla/GTNHLib model lighting: an exterior face samples
     * the light in the adjacent block, rather than the (often fully opaque/dark) modeled block.
     * Inset faces use self-light for transparent models and the brightest neighbour for opaque
     * models. This prevents modern JSON geometry from looking uniformly black/brown in-world.
     */
    private static int sampleBrightness(Block block, IBlockAccess world, int x, int y, int z, FaceInfo face) {
        int dx = 0, dy = 0, dz = 0;
        boolean exterior = false;

        if (face.axis == 0) {
            if (face.nx > 0.0F && face.avgX >= 1.0D - FACE_EPSILON) { dx = 1; exterior = true; }
            else if (face.nx < 0.0F && face.avgX <= FACE_EPSILON) { dx = -1; exterior = true; }
        } else if (face.axis == 1) {
            if (face.ny > 0.0F && face.avgY >= 1.0D - FACE_EPSILON) { dy = 1; exterior = true; }
            else if (face.ny < 0.0F && face.avgY <= FACE_EPSILON) { dy = -1; exterior = true; }
        } else {
            if (face.nz > 0.0F && face.avgZ >= 1.0D - FACE_EPSILON) { dz = 1; exterior = true; }
            else if (face.nz < 0.0F && face.avgZ <= FACE_EPSILON) { dz = -1; exterior = true; }
        }

        if (exterior) return block.getMixedBrightnessForBlock(world, x + dx, y + dy, z + dz);

        int self = block.getMixedBrightnessForBlock(world, x, y, z);
        if (!block.isOpaqueCube() || block.getLightOpacity(world, x, y, z) < 255) return self;

        int light = self;
        light = maxLight(light, block.getMixedBrightnessForBlock(world, x + 1, y, z));
        light = maxLight(light, block.getMixedBrightnessForBlock(world, x - 1, y, z));
        light = maxLight(light, block.getMixedBrightnessForBlock(world, x, y + 1, z));
        light = maxLight(light, block.getMixedBrightnessForBlock(world, x, y - 1, z));
        light = maxLight(light, block.getMixedBrightnessForBlock(world, x, y, z + 1));
        light = maxLight(light, block.getMixedBrightnessForBlock(world, x, y, z - 1));
        return light;
    }

    private static int tintColour(ModernMapParityBlocks entry, Quad quad, IBlockAccess world, int x, int y, int z) {
        if (quad.tintIndex < 0) return 0xFFFFFF;
        String name = entry.getRegistryName();
        // Modern Pale Oak leaves have a tint index in the JSON model but no BlockColors provider:
        // their authored texture is already the final pale colour.
        if ("pale_oak_leaves".equals(name)) return 0xFFFFFF;
        // Modern wildflowers only biome-tint the stem layer (tint 1); the flowerbed pixels stay authored.
        if ("wildflowers".equals(name) && quad.tintIndex == 0) return 0xFFFFFF;
        // Leaf Litter uses Minecraft's dedicated modern dry-foliage colormap. AssetDirector already
        // exposes that 1.21.11 resource under Tags.MC_ASSET_VER, so sample the real map from the
        // legacy biome's temperature/downfall instead of approximating it with a fixed tan.
        if ("leaf_litter".equals(name)) return dryFoliageColour(world, x, y, z);
        if (entry.getStyle() == ModernMapParityBlocks.Style.LEAVES) {
            return world.getBiomeGenForCoords(x, z).getBiomeFoliageColor(x, y, z);
        }
        return world.getBiomeGenForCoords(x, z).getBiomeGrassColor(x, y, z);
    }

    private static int dryFoliageColour(IBlockAccess world, int x, int y, int z) {
        BiomeGenBase biome = world.getBiomeGenForCoords(x, z);
        if (biome == null) return 0xD6AA74;

        float temperature = clamp01(biome.getFloatTemperature(x, y, z));
        float downfall = clamp01((float) biome.getIntRainfall() / 65536.0F);
        // Mojang's foliage/dry-foliage lookup multiplies downfall by temperature before sampling.
        downfall *= temperature;

        int[] pixels = dryFoliagePixels();
        int width = dryFoliageWidth;
        int height = dryFoliageHeight;
        if (pixels == null || width <= 0 || height <= 0) return 0xD6AA74;

        int px = clamp((int) ((1.0F - temperature) * (width - 1)), 0, width - 1);
        int py = clamp((int) ((1.0F - downfall) * (height - 1)), 0, height - 1);
        return pixels[py * width + px] & 0x00FFFFFF;
    }

    private static int[] dryFoliagePixels() {
        int[] cached = dryFoliagePixels;
        if (cached != null) return cached;

        synchronized (BlockModernJsonModelRenderer.class) {
            if (dryFoliagePixels != null) return dryFoliagePixels;
            InputStream stream = null;
            try {
                stream = Minecraft.getMinecraft().getResourceManager().getResource(
                        new ResourceLocation(Tags.MC_ASSET_VER, "textures/colormap/dry_foliage.png"))
                        .getInputStream();
                BufferedImage image = ImageIO.read(stream);
                if (image == null) return null;
                int width = image.getWidth();
                int height = image.getHeight();
                int[] pixels = new int[width * height];
                image.getRGB(0, 0, width, height, pixels, 0, width);
                dryFoliageWidth = width;
                dryFoliageHeight = height;
                dryFoliagePixels = pixels;
                return pixels;
            } catch (Throwable ignored) {
                // Do not cache failures: AssetDirector resources may not yet be ready on the first
                // render attempt during startup and a later atlas/render pass should retry.
                return null;
            } finally {
                if (stream != null) {
                    try { stream.close(); } catch (Throwable ignored) { }
                }
            }
        }
    }

    private static float clamp01(float value) {
        return value < 0.0F ? 0.0F : value > 1.0F ? 1.0F : value;
    }

    private static int clamp(int value, int min, int max) {
        return value < min ? min : value > max ? max : value;
    }

    private static int maxLight(int a, int b) {
        int blockA = a & 0xFFFF;
        int skyA = a & 0xFFFF0000;
        int blockB = b & 0xFFFF;
        int skyB = b & 0xFFFF0000;
        return Math.max(blockA, blockB) | Math.max(skyA, skyB);
    }

    private static float diffuseLight(float nx, float ny, float nz) {
        // Same directional weighting used by the modern-model support in GTNHLib, calculated
        // from the actually rotated quad rather than the pre-rotation JSON face declaration.
        return Math.min(nx * nx * 0.6F + ny * ny * ((3.0F + ny) / 4.0F) + nz * nz * 0.8F, 1.0F);
    }

    private static void emit(Tessellator t, Quad q, IIcon icon, double ox, double oy, double oz) {
        for (int i = 0; i < 4; i++) {
            Vertex v = q.vertices[i];
            double u = icon.getInterpolatedU(q.uv[i][0]);
            double vv = icon.getInterpolatedV(q.uv[i][1]);
            t.addVertexWithUV(ox + v.x, oy + v.y, oz + v.z, u, vv);
        }
    }

    private static void renderFallbackCube(Block block, int meta, RenderBlocks renderer) {
        Tessellator t = Tessellator.instance;
        t.setNormal(0, -1, 0); renderer.renderFaceYNeg(block, 0, 0, 0, block.getIcon(0, meta));
        t.setNormal(0, 1, 0); renderer.renderFaceYPos(block, 0, 0, 0, block.getIcon(1, meta));
        t.setNormal(0, 0, -1); renderer.renderFaceZNeg(block, 0, 0, 0, block.getIcon(2, meta));
        t.setNormal(0, 0, 1); renderer.renderFaceZPos(block, 0, 0, 0, block.getIcon(3, meta));
        t.setNormal(-1, 0, 0); renderer.renderFaceXNeg(block, 0, 0, 0, block.getIcon(4, meta));
        t.setNormal(1, 0, 0); renderer.renderFaceXPos(block, 0, 0, 0, block.getIcon(5, meta));
    }

    @Override public boolean shouldRender3DInInventory(int modelId) { return true; }
    @Override public int getRenderId() { return RenderIDs.MODERN_MAP_PARITY; }

    private static final class FaceInfo {
        final float nx, ny, nz;
        final int axis;
        final double avgX, avgY, avgZ;

        FaceInfo(float nx, float ny, float nz, int axis, double avgX, double avgY, double avgZ) {
            this.nx = nx;
            this.ny = ny;
            this.nz = nz;
            this.axis = axis;
            this.avgX = avgX;
            this.avgY = avgY;
            this.avgZ = avgZ;
        }

        static FaceInfo from(Quad q) {
            Vertex v0 = q.vertices[0], v1 = q.vertices[1], v2 = q.vertices[2];
            double ax = v1.x - v0.x, ay = v1.y - v0.y, az = v1.z - v0.z;
            double bx = v2.x - v0.x, by = v2.y - v0.y, bz = v2.z - v0.z;
            double nx = ay * bz - az * by;
            double ny = az * bx - ax * bz;
            double nz = ax * by - ay * bx;
            double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (length < 1.0E-12D) { nx = 0; ny = 1; nz = 0; length = 1; }
            nx /= length; ny /= length; nz /= length;

            double sx = 0, sy = 0, sz = 0;
            for (Vertex v : q.vertices) { sx += v.x; sy += v.y; sz += v.z; }
            sx *= 0.25D; sy *= 0.25D; sz *= 0.25D;

            double axn = Math.abs(nx), ayn = Math.abs(ny), azn = Math.abs(nz);
            int axis = ayn >= axn && ayn >= azn ? 1 : (azn >= axn ? 2 : 0);
            return new FaceInfo((float) nx, (float) ny, (float) nz, axis, sx, sy, sz);
        }
    }
}

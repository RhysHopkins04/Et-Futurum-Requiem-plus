package ganymedes01.etfuturum.blocks;

import java.util.Random;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.client.particle.CustomParticles;
import ganymedes01.etfuturum.client.sound.ModSounds;
import ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems;
import ganymedes01.etfuturum.core.utils.Utils;
import ganymedes01.etfuturum.entities.EntityFallingDripstone;
import ganymedes01.etfuturum.lib.RenderIDs;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * Plus P008e pointed-dripstone implementation.
 *
 * <p>The ten vanilla-style shape/orientation states fit entirely in 1.7.10 metadata. This avoids
 * depending on newer GTNHLib BlockState APIs while preserving the existing metadata/icon layout:
 * 0..4 point down, 5..9 point up; within each half 0=tip, 1=frustum, 2=middle, 3=base,
 * 4=merged tip.</p>
 */
public class BlockPointedDripstone extends Block {

    public static final int STATE_TIP = 0;
    public static final int STATE_FRUSTUM = 1;
    public static final int STATE_MIDDLE = 2;
    public static final int STATE_BASE = 3;
    public static final int STATE_TIP_MERGE = 4;
    private static final int STATE_COUNT = 5;

    private static final int MAX_GROWTH_LENGTH = 8;
    private static final int MAX_DRIP_SCAN = 10;
    // Match vanilla 1.7 liquid-through-block visual cadence instead of emitting on every
    // randomDisplayTick selection. These gates are client-only and do not alter cauldron/growth ticks.
    private static final int SOURCE_DRIP_DISPLAY_INTERVAL = 10;

    private IIcon[] downIcons;
    private IIcon[] upIcons;

    public static final DamageSource STALACTITE_DAMAGE = new DamageSource("stalactite");
    public static final DamageSource STALAGMITE_DAMAGE = new DamageSource("stalagmite");

    public BlockPointedDripstone() {
        super(Material.rock);
        Utils.setBlockSound(this, ModSounds.soundPointedDripstone);
        this.setHardness(1.5F);
        this.setResistance(3F);
        this.setHarvestLevel("pickaxe", 0);
        this.setBlockName(Utils.getUnlocalisedName("pointed_dripstone"));
        this.setBlockTextureName("pointed_dripstone");
        this.setCreativeTab(EtFuturum.creativeTabBlocks);
        this.setTickRandomly(true);
    }

    public static boolean isPointingUp(int meta) {
        return meta >= STATE_COUNT;
    }

    public static int getState(int meta) {
        int state = meta % STATE_COUNT;
        return state < 0 ? state + STATE_COUNT : state;
    }

    public static int composeMeta(boolean pointingUp, int state) {
        int clamped = state < 0 ? STATE_TIP : (state >= STATE_COUNT ? STATE_TIP_MERGE : state);
        return clamped + (pointingUp ? STATE_COUNT : 0);
    }

    /** Returns the correct metadata for one piece in a generated/falling column. */
    public static int metadataForGeneratedColumn(boolean pointingUp, int indexFromSupport, int length) {
        if (length <= 1) {
            return composeMeta(pointingUp, STATE_TIP);
        }
        if (indexFromSupport <= 0) {
            return composeMeta(pointingUp, STATE_BASE);
        }
        if (indexFromSupport >= length - 1) {
            return composeMeta(pointingUp, STATE_TIP);
        }
        if (indexFromSupport == length - 2) {
            return composeMeta(pointingUp, STATE_FRUSTUM);
        }
        return composeMeta(pointingUp, STATE_MIDDLE);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg) {
        downIcons = new IIcon[STATE_COUNT];
        downIcons[STATE_TIP] = reg.registerIcon(getTextureName() + "_down_tip");
        downIcons[STATE_FRUSTUM] = reg.registerIcon(getTextureName() + "_down_frustum");
        downIcons[STATE_MIDDLE] = reg.registerIcon(getTextureName() + "_down_middle");
        downIcons[STATE_BASE] = reg.registerIcon(getTextureName() + "_down_base");
        downIcons[STATE_TIP_MERGE] = reg.registerIcon(getTextureName() + "_down_tip_merge");

        upIcons = new IIcon[STATE_COUNT];
        upIcons[STATE_TIP] = reg.registerIcon(getTextureName() + "_up_tip");
        upIcons[STATE_FRUSTUM] = reg.registerIcon(getTextureName() + "_up_frustum");
        upIcons[STATE_MIDDLE] = reg.registerIcon(getTextureName() + "_up_middle");
        upIcons[STATE_BASE] = reg.registerIcon(getTextureName() + "_up_base");
        upIcons[STATE_TIP_MERGE] = reg.registerIcon(getTextureName() + "_up_tip_merge");
        this.blockIcon = downIcons[STATE_BASE];
    }

    @Override
    public int onBlockPlaced(World world, int x, int y, int z, int side, float subX, float subY, float subZ, int meta) {
        boolean pointingUp;
        if (side == ForgeDirection.UP.ordinal()) {
            pointingUp = true;
        } else if (side == ForgeDirection.DOWN.ordinal()) {
            pointingUp = false;
        } else if (canAttach(world, x, y - 1, z, true)) {
            pointingUp = true;
        } else {
            pointingUp = false;
        }
        return composeMeta(pointingUp, STATE_TIP);
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        if (!world.isRemote) {
            refreshColumn(world, x, y, z);
        }
    }

    @Override
    public boolean canPlaceBlockOnSide(World world, int x, int y, int z, int side) {
        ForgeDirection dir = ForgeDirection.getOrientation(side);
        if (dir == ForgeDirection.UP) {
            return canAttach(world, x, y - 1, z, true);
        }
        if (dir == ForgeDirection.DOWN) {
            return canAttach(world, x, y + 1, z, false);
        }
        return false;
    }

    private boolean canAttach(World world, int x, int y, int z, boolean pointingUp) {
        if (world.getBlock(x, y, z) == this) {
            return isPointingUp(world.getBlockMetadata(x, y, z)) == pointingUp;
        }
        return world.isSideSolid(x, y, z, pointingUp ? ForgeDirection.UP : ForgeDirection.DOWN);
    }

    @Override
    public boolean canBlockStay(World world, int x, int y, int z) {
        boolean pointingUp = isPointingUp(world.getBlockMetadata(x, y, z));
        int supportY = y + (pointingUp ? -1 : 1);
        return canAttach(world, x, supportY, z, pointingUp);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block block) {
        if (world.isRemote || world.getBlock(x, y, z) != this) {
            return;
        }

        if (!canBlockStay(world, x, y, z)) {
            if (isPointingUp(world.getBlockMetadata(x, y, z))) {
                world.setBlockToAir(x, y, z);
                this.dropBlockAsItem(world, x, y, z, 0, 0);
            } else {
                collapseHangingColumn(world, x, y, z);
            }
            return;
        }

        refreshColumn(world, x, y, z);
    }

    /**
     * Refreshes nearby metadata after placement, growth or world generation without causing a
     * notification cascade. Public so the P008e cave decorator can finalize generated columns.
     */
    public void refreshColumn(World world, int x, int y, int z) {
        for (int dy = -12; dy <= 12; dy++) {
            int py = y + dy;
            if (py < 0 || py >= world.getActualHeight() || world.getBlock(x, py, z) != this) {
                continue;
            }
            boolean pointingUp = isPointingUp(world.getBlockMetadata(x, py, z));
            int state = computeState(world, x, py, z, pointingUp);
            int meta = composeMeta(pointingUp, state);
            if (world.getBlockMetadata(x, py, z) != meta) {
                world.setBlockMetadataWithNotify(x, py, z, meta, 2);
            }
        }
    }

    private int computeState(World world, int x, int y, int z, boolean pointingUp) {
        int tipDir = pointingUp ? 1 : -1;
        int supportDir = -tipDir;
        boolean tipNeighbor = sameOrientation(world, x, y + tipDir, z, pointingUp);
        boolean supportNeighbor = sameOrientation(world, x, y + supportDir, z, pointingUp);

        if (!tipNeighbor) {
            if (world.getBlock(x, y + tipDir, z) == this
                    && isPointingUp(world.getBlockMetadata(x, y + tipDir, z)) != pointingUp) {
                return STATE_TIP_MERGE;
            }
            return STATE_TIP;
        }
        if (!supportNeighbor) {
            return STATE_BASE;
        }
        if (!sameOrientation(world, x, y + tipDir * 2, z, pointingUp)) {
            return STATE_FRUSTUM;
        }
        return STATE_MIDDLE;
    }

    private boolean sameOrientation(World world, int x, int y, int z, boolean pointingUp) {
        return y >= 0 && y < world.getActualHeight() && world.getBlock(x, y, z) == this
                && isPointingUp(world.getBlockMetadata(x, y, z)) == pointingUp;
    }

    private void collapseHangingColumn(World world, int x, int y, int z) {
        int topY = y;
        while (topY + 1 < world.getActualHeight() && sameOrientation(world, x, topY + 1, z, false)) {
            topY++;
        }

        int count = 0;
        int scanY = topY;
        int originalMeta = world.getBlockMetadata(x, topY, z);
        while (scanY >= 0 && sameOrientation(world, x, scanY, z, false)) {
            world.setBlock(x, scanY, z, Blocks.air, 0, 2);
            count++;
            scanY--;
        }

        if (count > 0) {
            EntityFallingDripstone falling = new EntityFallingDripstone(world,
                    x + 0.5D, topY + 0.5D, z + 0.5D, originalMeta, count);
            world.spawnEntityInWorld(falling);
        }
    }

    @Override
    public void onFallenUpon(World world, int x, int y, int z, Entity entity, float fallDistance) {
        int meta = world.getBlockMetadata(x, y, z);
        int state = getState(meta);
        if (!isPointingUp(meta) || (state != STATE_TIP && state != STATE_TIP_MERGE)) {
            super.onFallenUpon(world, x, y, z, entity, fallDistance);
            return;
        }

        entity.fallDistance = 0.0F;
        if (entity.isEntityInvulnerable()) {
            return;
        }
        if (entity instanceof EntityPlayer && ((EntityPlayer) entity).capabilities.allowFlying) {
            return;
        }
        int damage = MathHelper.ceiling_float_int(fallDistance * 2.0F - 2.0F);
        if (damage > 0) {
            entity.attackEntityFrom(STALAGMITE_DAMAGE, damage);
        }
    }

    private static float boundsOffset(int state) {
        switch (state) {
            case STATE_BASE:
                return 0.125F;
            case STATE_MIDDLE:
                return 0.1875F;
            case STATE_FRUSTUM:
                return 0.25F;
            case STATE_TIP:
            case STATE_TIP_MERGE:
            default:
                return 0.3125F;
        }
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        int meta = world.getBlockMetadata(x, y, z);
        int state = getState(meta);
        float offset = boundsOffset(state);
        if (state == STATE_TIP) {
            boolean pointingUp = isPointingUp(meta);
            float minY = pointingUp ? 0.0F : 0.3125F;
            float maxY = pointingUp ? 0.6875F : 1.0F;
            return AxisAlignedBB.getBoundingBox(x + offset, y + minY, z + offset,
                    x + 1.0F - offset, y + maxY, z + 1.0F - offset);
        }
        return AxisAlignedBB.getBoundingBox(x + offset, y, z + offset,
                x + 1.0F - offset, y + 1.0F, z + 1.0F - offset);
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess access, int x, int y, int z) {
        int meta = access.getBlockMetadata(x, y, z);
        int state = getState(meta);
        float offset = boundsOffset(state);
        if (state == STATE_TIP) {
            boolean pointingUp = isPointingUp(meta);
            float minY = pointingUp ? 0.0F : 0.3125F;
            float maxY = pointingUp ? 0.6875F : 1.0F;
            this.setBlockBounds(offset, minY, offset, 1.0F - offset, maxY, 1.0F - offset);
        } else {
            this.setBlockBounds(offset, 0.0F, offset, 1.0F - offset, 1.0F, 1.0F - offset);
        }
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        int state = getState(meta);
        return isPointingUp(meta) ? upIcons[state] : downIcons[state];
    }

    @Override
    public int damageDropped(int meta) {
        return 0;
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random rand) {
        if (world.isRemote || world.getBlock(x, y, z) != this) {
            return;
        }

        int meta = world.getBlockMetadata(x, y, z);
        if (isPointingUp(meta)) {
            return;
        }

        int state = getState(meta);
        if (state == STATE_TIP) {
            tryGrow(world, x, y, z, rand);
        }

        if (isTopmostStalactiteBlock(world, x, y, z)) {
            tryMudToClay(world, x, y, z, rand);
            tryFillCauldron(world, x, y, z, rand);
        }
    }

    private boolean isTopmostStalactiteBlock(World world, int x, int y, int z) {
        return !sameOrientation(world, x, y + 1, z, false);
    }

    private int findStalactiteBaseY(World world, int x, int tipY, int z) {
        int y = tipY;
        for (int i = 0; i < 12; i++) {
            if (!sameOrientation(world, x, y, z, false)) {
                return -1;
            }
            if (!sameOrientation(world, x, y + 1, z, false)) {
                return y;
            }
            y++;
        }
        return -1;
    }

    private int findStalactiteTipY(World world, int x, int baseY, int z) {
        int y = baseY;
        for (int i = 0; i < 12; i++) {
            if (!sameOrientation(world, x, y, z, false)) {
                return -1;
            }
            int state = getState(world.getBlockMetadata(x, y, z));
            if (state == STATE_TIP || state == STATE_TIP_MERGE) {
                return y;
            }
            y--;
        }
        return -1;
    }

    private int fluidSourceType(World world, int x, int y, int z) {
        if (y < 0 || y >= world.getActualHeight() || world.getBlockMetadata(x, y, z) != 0) {
            return 0;
        }
        Material material = world.getBlock(x, y, z).getMaterial();
        if (material == Material.water) {
            return 1;
        }
        if (material == Material.lava) {
            return 2;
        }
        return 0;
    }

    /**
     * Returns the source fluid directly above the solid block supporting the top of a stalactite.
     * Dripping/cauldron transfer works through any valid solid support (stone, deepslate, dripstone
     * block, etc.); natural pointed-dripstone growth remains intentionally stricter and requires
     * a Dripstone Block in {@link #tryGrow}.
     */
    private int drippingFluidSourceType(World world, int x, int baseY, int z) {
        int supportY = baseY + 1;
        if (supportY < 0 || supportY + 1 >= world.getActualHeight()
                || !world.isSideSolid(x, supportY, z, ForgeDirection.DOWN)) {
            return 0;
        }
        return fluidSourceType(world, x, supportY + 1, z);
    }

    /**
     * Modern-style Mud drying is not a "droplet lands on Mud" interaction. The Mud block sits
     * above the stalactite's solid support (Mud -> support -> downward pointed dripstone) and is
     * drained into Clay by random ticks. A Mud floor below a water-fed stalactite remains a valid
     * surface for stalagmite growth and is intentionally handled by {@link #tryGrow}.
     */
    private void tryMudToClay(World world, int x, int baseY, int z, Random rand) {
        if (!ModBlocks.MUD.isEnabled() || world.provider.isHellWorld || rand.nextInt(256) >= 45) {
            return;
        }
        int supportY = baseY + 1;
        if (!world.isSideSolid(x, supportY, z, ForgeDirection.DOWN)) {
            return;
        }
        if (world.getBlock(x, supportY + 1, z) == ModBlocks.MUD.get()) {
            world.setBlock(x, supportY + 1, z, Blocks.clay, 0, 3);
        }
    }

    private void tryFillCauldron(World world, int x, int baseY, int z, Random rand) {
        int fluid = drippingFluidSourceType(world, x, baseY, z);
        if (fluid == 0) {
            return;
        }
        int tipY = findStalactiteTipY(world, x, baseY, z);
        if (tipY < 0 || baseY - tipY + 1 > MAX_DRIP_SCAN) {
            return;
        }
        int cauldronY = findCauldronY(world, x, tipY, z);
        if (cauldronY < 0) {
            return;
        }

        if (fluid == 1 && world.getBlock(x, cauldronY, z) == Blocks.cauldron) {
            int level = world.getBlockMetadata(x, cauldronY, z);
            if (level < 3 && rand.nextInt(256) < 45) {
                world.setBlockMetadataWithNotify(x, cauldronY, z, level + 1, 3);
            }
        } else if (fluid == 2 && world.getBlock(x, cauldronY, z) == Blocks.cauldron
                && world.getBlockMetadata(x, cauldronY, z) == 0
                && ConfigBlocksItems.enableLavaCauldrons && ModBlocks.LAVA_CAULDRON.isEnabled()
                && rand.nextInt(256) < 15) {
            world.setBlock(x, cauldronY, z, ModBlocks.LAVA_CAULDRON.get(), 3, 3);
        }
    }

    private int findCauldronY(World world, int x, int tipY, int z) {
        for (int y = tipY - 1; y >= tipY - MAX_DRIP_SCAN && y >= 0; y--) {
            Block block = world.getBlock(x, y, z);
            if (block == Blocks.cauldron) {
                return y;
            }
            if (!world.isAirBlock(x, y, z) && block.getCollisionBoundingBoxFromPool(world, x, y, z) != null) {
                return -1;
            }
        }
        return -1;
    }

    private void tryGrow(World world, int x, int tipY, int z, Random rand) {
        int baseY = findStalactiteBaseY(world, x, tipY, z);
        if (baseY < 0 || world.getBlock(x, baseY + 1, z) != ModBlocks.DRIPSTONE_BLOCK.get()
                || drippingFluidSourceType(world, x, baseY, z) != 1) {
            return;
        }
        int length = baseY - tipY + 1;
        if (length >= MAX_GROWTH_LENGTH || rand.nextInt(5625) >= 64) {
            return;
        }

        int stalagmiteY = findStalagmiteGrowthTarget(world, x, tipY, z);
        boolean canGrowDown = tipY > 1 && world.isAirBlock(x, tipY - 1, z);
        boolean canGrowUp = stalagmiteY >= 0;
        if (!canGrowDown && !canGrowUp) {
            return;
        }

        if (canGrowDown && (!canGrowUp || rand.nextBoolean())) {
            world.setBlock(x, tipY - 1, z, this, composeMeta(false, STATE_TIP), 2);
            refreshColumn(world, x, tipY, z);
            return;
        }

        if (world.getBlock(x, stalagmiteY, z) == this) {
            if (stalagmiteY + 1 < tipY && world.isAirBlock(x, stalagmiteY + 1, z)) {
                world.setBlock(x, stalagmiteY + 1, z, this, composeMeta(true, STATE_TIP), 2);
                refreshColumn(world, x, stalagmiteY, z);
            }
        } else if (world.isAirBlock(x, stalagmiteY, z)) {
            world.setBlock(x, stalagmiteY, z, this, composeMeta(true, STATE_TIP), 2);
            refreshColumn(world, x, stalagmiteY, z);
        }
    }

    private int findStalagmiteGrowthTarget(World world, int x, int tipY, int z) {
        for (int y = tipY - 1; y >= tipY - MAX_DRIP_SCAN && y >= 1; y--) {
            Block block = world.getBlock(x, y, z);
            if (block == this && isPointingUp(world.getBlockMetadata(x, y, z))) {
                int state = getState(world.getBlockMetadata(x, y, z));
                return (state == STATE_TIP || state == STATE_TIP_MERGE) ? y : -1;
            }
            if (!world.isAirBlock(x, y, z)) {
                if (world.isSideSolid(x, y, z, ForgeDirection.UP) && world.isAirBlock(x, y + 1, z)) {
                    return y + 1;
                }
                return -1;
            }
        }
        return -1;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random rand) {
        int meta = world.getBlockMetadata(x, y, z);
        if (isPointingUp(meta) || getState(meta) != STATE_TIP) {
            return;
        }
        int baseY = findStalactiteBaseY(world, x, y, z);
        if (baseY < 0 || baseY - y + 1 > MAX_DRIP_SCAN) {
            return;
        }

        int fluid = drippingFluidSourceType(world, x, baseY, z);
        // Plus P008e-h intentionally uses strict source-only visuals: a pointed-dripstone tip
        // must have an actual water/lava source above its solid support. Do not synthesize
        // ambient Overworld-water/Nether-lava droplets when no source exists.
        if (fluid == 0 || rand.nextInt(SOURCE_DRIP_DISPLAY_INTERVAL) != 0) {
            return;
        }

        int color = fluid == 2 ? 0xFFFF7813 : 0xFF3F76E4;
        setBlockBoundsBasedOnState(world, x, y, z);
        double px = x + minX + (maxX - minX) * 0.5D;
        double pz = z + minZ + (maxZ - minZ) * 0.5D;
        CustomParticles.spawnDripstoneDrippingParticle(world, px, y + minY - 0.05D, pz, color);
    }

    @Override
    public int getRenderType() {
        return RenderIDs.POINTED_DRIPSTONE;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public String getItemIconName() {
        return "pointed_dripstone";
    }
}

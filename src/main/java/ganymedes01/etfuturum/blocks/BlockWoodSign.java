package ganymedes01.etfuturum.blocks;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.Tags;
import ganymedes01.etfuturum.client.particle.CustomParticles;
import ganymedes01.etfuturum.client.sound.ModSounds;
import ganymedes01.etfuturum.core.utils.Utils;
import ganymedes01.etfuturum.network.WoodSignOpenMessage;
import ganymedes01.etfuturum.tileentities.TileEntityWoodSign;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSign;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import java.lang.reflect.Field;
import java.util.Random;

/**
 * EFR wood sign with modern two-sided editing, dyes, glow ink and shared waxing.
 *
 * <p>Pass 18 also installs the same data tile under the vanilla 1.7 oak standing/wall sign
 * blocks. That lets ordinary {@link Items#sign} placements use the modern compatibility layer
 * without replacing the vanilla block IDs, which keeps existing worlds/map data stable.</p>
 */
public class BlockWoodSign extends BlockSign {

    protected static BlockWoodSign prevSign;
    private BlockWoodSign wallSign;
    private final BlockWoodSign standingSign;

    private final Block baseBlock;
    private final int meta;
    public final String type;

    public final boolean standing;

    private static boolean vanillaTileFactoryInstalled;

    public BlockWoodSign(Class<? extends TileEntity> p_i45426_1_, boolean p_i45426_2_, String type, Block block, int meta) {
        super(p_i45426_1_, p_i45426_2_);
        this.meta = meta;
        baseBlock = block;
        this.type = type;
        standing = p_i45426_2_;
        if (standing) {
            prevSign = this;
            standingSign = this;
        } else {
            standingSign = prevSign;
            wallSign = this;
            prevSign.wallSign = this;
        }
        setHardness(1.0F);
        disableStats();
        setBlockName(Utils.getUnlocalisedName(type + "_sign"));
        if (type.equals("crimson") || type.equals("warped")) {
            Utils.setBlockSound(this, ModSounds.soundNetherWood);
        } else if (type.equals("cherry")) {
            Utils.setBlockSound(this, ModSounds.soundCherryWood);
        } else if (type.equals("bamboo")) {
            Utils.setBlockSound(this, ModSounds.soundBambooWood);
        } else {
            setStepSound(Block.soundTypeWood);
        }
        if (block != Blocks.planks && standing) {
            setCreativeTab(EtFuturum.creativeTabBlocks);
        }
    }

    /**
     * Make future vanilla oak sign placements instantiate {@link TileEntityWoodSign} while retaining
     * the exact vanilla standing_sign / wall_sign block IDs. BlockSign only has one Class-valued
     * instance field, so discovering it by type is stable in both deobfuscated and obfuscated 1.7.10.
     */
    public static void installVanillaSignTileEntityCompatibility() {
        if (vanillaTileFactoryInstalled) return;
        try {
            Field tileClassField = null;
            for (Field field : BlockSign.class.getDeclaredFields()) {
                if (field.getType() == Class.class) {
                    tileClassField = field;
                    break;
                }
            }
            if (tileClassField == null) {
                throw new IllegalStateException("Unable to locate BlockSign tile-entity class field");
            }
            tileClassField.setAccessible(true);
            tileClassField.set(Blocks.standing_sign, TileEntityWoodSign.class);
            tileClassField.set(Blocks.wall_sign, TileEntityWoodSign.class);
            vanillaTileFactoryInstalled = true;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Unable to install modern vanilla-sign tile compatibility", e);
        }
    }

    /**
     * Upgrade an already-saved vanilla TileEntitySign in place. Old front text is preserved exactly;
     * the newly introduced back side starts empty and both sides start black/non-glowing/unwaxed.
     */
    public static TileEntityWoodSign upgradeVanillaSignTile(World world, int x, int y, int z) {
        if (world == null) return null;
        Block block = world.getBlock(x, y, z);
        if (block != Blocks.standing_sign && block != Blocks.wall_sign) return null;

        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof TileEntityWoodSign) return (TileEntityWoodSign) tile;
        if (!(tile instanceof TileEntitySign)) return null;

        TileEntitySign old = (TileEntitySign) tile;
        TileEntityWoodSign replacement = new TileEntityWoodSign();
        for (int i = 0; i < 4; i++) {
            replacement.signText[i] = old.signText[i] == null ? "" : old.signText[i];
        }
        replacement.lineBeingEdited = old.lineBeingEdited;
        world.setTileEntity(x, y, z, replacement);
        replacement.markDirty();
        if (!world.isRemote) world.markBlockForUpdate(x, y, z);
        return replacement;
    }

    /**
     * Server/client Forge-event bridge for the literal vanilla oak sign blocks. This gives them the
     * same front/back editing, dye, glow-ink and shared wax behavior as EFR's wood sign families.
     */
    public static boolean handleVanillaSignActivation(World world, int x, int y, int z, EntityPlayer player) {
        if (world == null || player == null) return false;
        Block block = world.getBlock(x, y, z);
        boolean isStanding = block == Blocks.standing_sign;
        if (!isStanding && block != Blocks.wall_sign) return false;

        TileEntityWoodSign sign = upgradeVanillaSignTile(world, x, y, z);
        if (sign == null) return false;

        boolean back = isBackSide(player, x, z, world.getBlockMetadata(x, y, z) & 15, isStanding);
        ItemStack held = player.getHeldItem();
        if (tryApplyModifierCommon(world, x, y, z, player, held, sign, back)) return true;

        if (!world.isRemote && player instanceof EntityPlayerMP) {
            openEditor((EntityPlayerMP) player, sign, back, block);
        }
        return true;
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        return baseBlock.getIcon(side, this.meta);
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        if (baseBlock == Blocks.planks) {
            return ModItems.OLD_SIGN_ITEMS[this.meta - 1].get();
        }
        return Item.getItemFromBlock(standingSign);
    }

    @Override
    public Item getItem(World worldIn, int x, int y, int z) {
        return getItemDropped(0, null, 0);
    }

    public BlockWoodSign getWallSign() {
        return wallSign;
    }

    @Override
    public String getItemIconName() {
        return type + "_sign";
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side,
            float hitX, float hitY, float hitZ) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof TileEntityWoodSign)) return true;
        TileEntityWoodSign sign = (TileEntityWoodSign) tile;
        boolean back = isBackSide(player, x, z, world.getBlockMetadata(x, y, z) & 15, standing);
        ItemStack held = player.getHeldItem();

        if (tryApplyModifierCommon(world, x, y, z, player, held, sign, back)) return true;
        if (!world.isRemote && player instanceof EntityPlayerMP) {
            openEditor((EntityPlayerMP) player, sign, back, this);
        }
        return true;
    }

    private static void openEditor(EntityPlayerMP player, TileEntityWoodSign sign, boolean back, Block block) {
        if (sign.isWaxed()) {
            playModernSound(player.worldObj, sign.xCoord, sign.yCoord, sign.zCoord, "block.sign.waxed_interact_fail");
            return;
        }
        sign.func_145912_a(player);
        EtFuturum.networkWrapper.sendTo(new WoodSignOpenMessage(sign, Block.getIdFromBlock(block), back), player);
    }

    private static boolean isBackSide(EntityPlayer player, int x, int z, int blockMeta, boolean standing) {
        double fx;
        double fz;
        if (standing) {
            double yaw = Math.toRadians((blockMeta & 15) * 22.5D);
            fx = -Math.sin(yaw);
            fz = Math.cos(yaw);
        } else {
            switch (blockMeta) {
                case 2: fx = 0.0D; fz = -1.0D; break;
                case 3: fx = 0.0D; fz = 1.0D; break;
                case 4: fx = -1.0D; fz = 0.0D; break;
                case 5: fx = 1.0D; fz = 0.0D; break;
                default: fx = 0.0D; fz = 1.0D; break;
            }
        }
        double dx = player.posX - (x + 0.5D);
        double dz = player.posZ - (z + 0.5D);
        return dx * fx + dz * fz < 0.0D;
    }

    private static boolean tryApplyModifierCommon(World world, int x, int y, int z, EntityPlayer player,
            ItemStack held, TileEntityWoodSign sign, boolean back) {
        if (sign.isWaxed()) {
            if (!world.isRemote) playModernSound(world, x, y, z, "block.sign.waxed_interact_fail");
            return true;
        }
        if (held == null) return false;

        if (held.getItem() == ModItems.HONEYCOMB.get()) {
            if (world.isRemote) {
                spawnSignWaxOnParticles(world, x, y, z);
            } else {
                sign.setWaxed(true);
                consumeOne(player, held);
                playModernSound(world, x, y, z, "item.honeycomb.wax_on");
            }
            return true;
        }
        if (held.getItem() == ModItems.GLOW_INK_SAC.get()) {
            if (sign.isGlowing(back)) return true;
            if (!world.isRemote) {
                sign.setGlowing(back, true);
                consumeOne(player, held);
                playModernSound(world, x, y, z, "item.glow_ink_sac.use");
            }
            return true;
        }
        if (held.getItem() == Items.dye && held.getItemDamage() == 0 && sign.isGlowing(back)) {
            if (!world.isRemote) {
                sign.setGlowing(back, false);
                consumeOne(player, held);
                playModernSound(world, x, y, z, "item.ink_sac.use");
            }
            return true;
        }

        Integer colour = dyeColour(held);
        if (colour != null) {
            if (!world.isRemote) {
                sign.setTextColour(back, colour);
                consumeOne(player, held);
                playModernSound(world, x, y, z, "item.dye.use");
            }
            return true;
        }
        return false;
    }

    private static Integer dyeColour(ItemStack held) {
        if (held == null) return null;
        if (held.getItem() == Items.dye) {
            int dyeMeta = MathHelper.clamp_int(held.getItemDamage(), 0, 15);
            if (dyeMeta == 0) return null;
            return modernSignTextColourForLegacyDyeMeta(dyeMeta);
        }
        if (held.getItem() == ModItems.DYE.get()) {
            int[] legacyMeta = {15, 4, 3, 0};
            int dyeMeta = MathHelper.clamp_int(held.getItemDamage(), 0, legacyMeta.length - 1);
            return modernSignTextColourForLegacyDyeMeta(legacyMeta[dyeMeta]);
        }
        return null;
    }

    private static int modernSignTextColourForLegacyDyeMeta(int meta) {
        final int[] colours = {
                0x000000, 0xFF0000, 0x00FF00, 0x8B4513,
                0x0000FF, 0xA020F0, 0x00FFFF, 0xD3D3D3,
                0x808080, 0xFF69B4, 0xBFFF00, 0xFFFF00,
                0x9AC0CD, 0xFF00FF, 0xFF681F, 0xFFFFFF
        };
        return colours[MathHelper.clamp_int(meta, 0, colours.length - 1)];
    }

    @SideOnly(Side.CLIENT)
    private static void spawnSignWaxOnParticles(World world, int x, int y, int z) {
        Random random = world.rand;
        for (int i = 0; i < 10; i++) {
            double px = x + 0.15D + random.nextDouble() * 0.70D;
            double py = y + 0.20D + random.nextDouble() * 0.65D;
            double pz = z + 0.15D + random.nextDouble() * 0.70D;
            CustomParticles.spawnCopperWaxOnParticle(world, px, py, pz);
        }
    }

    private static void consumeOne(EntityPlayer player, ItemStack held) {
        if (player.capabilities.isCreativeMode || held == null) return;
        if (--held.stackSize <= 0) player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
    }

    private static void playModernSound(World world, int x, int y, int z, String sound) {
        world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, Tags.MC_ASSET_VER + ":" + sound, 1.0F, 1.0F);
    }
}

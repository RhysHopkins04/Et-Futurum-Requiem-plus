package ganymedes01.etfuturum.blocks.ores.modded;

import cpw.mods.fml.common.registry.GameRegistry;
import ganymedes01.etfuturum.api.DeepslateOreRegistry;
import ganymedes01.etfuturum.blocks.ores.BaseSubtypesDeepslateOre;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Random;

/**
 * Earth/Overworld NTM ores with 1.7.10-safe (0..15) metadata and a deepslate host texture.
 *
 * The underlying NTM ore remains the mining result when it would normally drop itself:
 * NTM processing often recognises the original block ItemStack, not only OreDictionary entries.
 * No compile-time HBM dependency is required. Do not add NTM reactive/outgassing ores,
 * custom depth deposits or block-entity deposits to this simple variant block.
 */
public final class BlockDeepslateHBMOre extends BaseSubtypesDeepslateOre {
    /** Retained only to decode existing test saves; new ores have separate IDs. */
    @Override
    public void getSubBlocks(Item item, net.minecraft.creativetab.CreativeTabs tab, java.util.List<ItemStack> list) {}
    private static final String[] BASE_NAMES = {
                "ore_thorium",
                "ore_titanium",
                "ore_sulfur",
                "ore_aluminium",
                "ore_copper",
                "ore_zinc",
                "ore_fluorite",
                "ore_niter",
                "ore_tungsten",
                "ore_lead",
                "ore_beryllium",
                "ore_rare",
                "ore_lignite",
                "ore_cinnebar",
                "ore_cobalt",
                "ore_coltan"
    };

    public BlockDeepslateHBMOre() {
        super(
                "deepslate_hbm_thorium_ore",
                "deepslate_hbm_titanium_ore",
                "deepslate_hbm_sulfur_ore",
                "deepslate_hbm_aluminium_ore",
                "deepslate_hbm_copper_ore",
                "deepslate_hbm_zinc_ore",
                "deepslate_hbm_fluorite_ore",
                "deepslate_hbm_niter_ore",
                "deepslate_hbm_tungsten_ore",
                "deepslate_hbm_lead_ore",
                "deepslate_hbm_beryllium_ore",
                "deepslate_hbm_rare_ore",
                "deepslate_hbm_lignite_ore",
                "deepslate_hbm_cinnebar_ore",
                "deepslate_hbm_cobalt_ore",
                "deepslate_hbm_coltan_ore"
        );
    }

    @Override
    public String getTextureSubfolder() {
        return "hbm";
    }

    @Override
    public Block getBase(int metadata) {
        final String name = BASE_NAMES[Math.floorMod(metadata, BASE_NAMES.length)];
        // NTM: Space registers ores using block.getUnlocalizedName() (tile.ore_*).
        Block ore = GameRegistry.findBlock("hbm", "tile." + name);
        if (ore == null) {
            // Accept forks which register the conventional name without tile. prefix.
            ore = GameRegistry.findBlock("hbm", name);
        }
        if (ore == null) {
            throw new IllegalStateException("NTM ore missing from Forge registry: hbm:" + name);
        }
        return ore;
    }

    @Override
    public Item getItemDropped(int metadata, Random random, int fortune) {
        return getBase(metadata).getItemDropped(getBaseMeta(metadata), random, fortune);
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        ArrayList<ItemStack> drops = getBase(metadata).getDrops(world, x, y, z, getBaseMeta(metadata), fortune);
        // Old combined blocks migrate to the active individual identity when mined.
        ganymedes01.etfuturum.api.mappings.BlockMetaPair target = DeepslateOreRegistry.getOre(getBase(metadata), 0);
        if (target != null && target.get() instanceof BlockDeepslateNtmOre) {
            for (ItemStack drop : drops) if (Block.getBlockFromItem(drop.getItem()) == getBase(metadata) && drop.getItemDamage() == 0) {
                drop.func_150996_a(Item.getItemFromBlock(target.get()));
                drop.setItemDamage(0);
            }
        }
        return drops;
    }

    /**
     * NTM uranium and asbestos emit gases and perform other block-specific actions.
     * Keep those exact NTM blocks native even when generic OreDictionary conversion
     * is enabled; a retextured wrapper would otherwise silently delete their behaviour.
     */
    public static void preserveReactiveOres() {
        preserve("ore_uranium");
        preserve("ore_asbestos");
        // Clusters are enriched crystal deposits, not ordinary ore, and basalt
        // ores have their own metadata and (asbestos) gas behaviour.
        preserve("cluster_iron");
        preserve("cluster_titanium");
        preserve("cluster_aluminium");
        preserve("cluster_copper");
        preserve("cluster_depth_iron");
        preserve("cluster_depth_titanium");
        preserve("cluster_depth_tungsten");
        preserve("ore_basalt");
    }

    private static void preserve(String name) {
        Block nativeOre = GameRegistry.findBlock("hbm", "tile." + name);
        if (nativeOre == null) nativeOre = GameRegistry.findBlock("hbm", name);
        if (nativeOre != null) {
            for (int meta = 0; meta < 16; meta++) {
                if (meta == 0 && DeepslateOreRegistry.getOre(nativeOre, 0) != null
                        && DeepslateOreRegistry.getOre(nativeOre, 0).get() instanceof BlockDeepslateNtmOre) continue;
                if (!nativeOre.hasTileEntity(meta)) DeepslateOreRegistry.addOre(nativeOre, meta, nativeOre, meta);
            }
        }
    }
}

package ganymedes01.etfuturum;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.client.ModernAssetResourcePack;
import ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.Locale;

/** Modern registry identities for the 23 pottery sherd patterns available through 1.21.11. */
public enum ModernPotterySherds {
    ANGLER, ARCHER, ARMS_UP, BLADE, BREWER, BURN, DANGER, EXPLORER, FRIEND, HEART,
    HEARTBREAK, HOWL, MINER, MOURNER, PLENTY, PRIZE, SHEAF, SHELTER, SKULL, SNORT,
    FLOW, GUSTER, SCRAPE;

    private PotterySherdItem item;

    public String getRegistryName() {
        return name().toLowerCase(Locale.ROOT) + "_pottery_sherd";
    }

    public String getPatternName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Item get() {
        return item;
    }

    public static void init() {
        if (!ConfigBlocksItems.enableModernMapParityBlocks) return;
        for (ModernPotterySherds entry : values()) {
            if (GameRegistry.findItem(Tags.MOD_ID, entry.getRegistryName()) != null) continue;
            entry.item = new PotterySherdItem(entry);
            GameRegistry.registerItem(entry.item, entry.getRegistryName());
        }
    }

    public static ModernPotterySherds fromItem(Item item) {
        if (item == null) return null;
        for (ModernPotterySherds entry : values()) if (entry.item == item) return entry;
        return null;
    }

    public static ItemStack ingredientFor(String pattern) {
        if (pattern == null || "minecraft:brick".equals(pattern) || "brick".equals(pattern)) {
            return new ItemStack(net.minecraft.init.Items.brick);
        }
        String clean = pattern;
        int colon = clean.indexOf(':');
        if (colon >= 0) clean = clean.substring(colon + 1);
        if (clean.endsWith("_pottery_sherd")) clean = clean.substring(0, clean.length() - "_pottery_sherd".length());
        for (ModernPotterySherds entry : values()) {
            if (entry.getPatternName().equals(clean) && entry.item != null) return new ItemStack(entry.item);
        }
        return new ItemStack(net.minecraft.init.Items.brick);
    }

    private static final class PotterySherdItem extends Item {
        private final ModernPotterySherds entry;

        PotterySherdItem(ModernPotterySherds entry) {
            this.entry = entry;
            setUnlocalizedName(Tags.MOD_ID + "." + entry.getRegistryName());
            setCreativeTab(EtFuturum.creativeTabItems);
        }

        @Override
        public EnumRarity getRarity(ItemStack stack) {
            return EnumRarity.uncommon;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void registerIcons(IIconRegister register) {
            String synthetic = "modern_item/" + entry.getRegistryName();
            String target = "textures/item/" + entry.getRegistryName() + ".png";
            ModernAssetResourcePack.registerDynamicAlias("textures/items/" + synthetic + ".png", target);
            itemIcon = register.registerIcon("minecraft:" + synthetic);
        }
    }
}

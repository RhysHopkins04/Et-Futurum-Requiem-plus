package ganymedes01.etfuturum.compat;

import cpw.mods.fml.common.registry.GameRegistry;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.blocks.ores.modded.BlockDeepslateHBMOre;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Collections;
import java.util.IdentityHashMap;

/** Optional NTM integration; never links HBM classes into a build without NTM. */
public final class NtmOreCompat {
    private NtmOreCompat() {}
    private static final ThreadLocal<ganymedes01.etfuturum.blocks.ores.modded.BlockDeepslateNtmOre> NATIVE_HARVEST = new ThreadLocal<>();

    public static Block breakEffectSource(Block block, int meta) {
        if (block == ModBlocks.DEEPSLATE_COAL_ORE.get()) return net.minecraft.init.Blocks.coal_ore;
        Block source = block;
        if (block instanceof ganymedes01.etfuturum.blocks.ores.modded.BlockDeepslateNtmOre)
            source = ((ganymedes01.etfuturum.blocks.ores.modded.BlockDeepslateNtmOre) block).getBase();
        else if (block instanceof BlockDeepslateHBMOre && meta >= 0 && meta < 16)
            source = ((BlockDeepslateHBMOre) block).getBase(meta);
        Block lignite = GameRegistry.findBlock("hbm", "tile.ore_lignite");
        return source == lignite ? source : block;
    }

    public static void nativeHarvest(ganymedes01.etfuturum.blocks.ores.modded.BlockDeepslateNtmOre variant, Runnable harvest) {
        ganymedes01.etfuturum.blocks.ores.modded.BlockDeepslateNtmOre previous = NATIVE_HARVEST.get();
        NATIVE_HARVEST.set(variant);
        try { harvest.run(); }
        finally { if (previous == null) NATIVE_HARVEST.remove(); else NATIVE_HARVEST.set(previous); }
    }

    public static void retainReactiveOreDrop(net.minecraftforge.event.world.BlockEvent.HarvestDropsEvent event) {
        ganymedes01.etfuturum.blocks.ores.modded.BlockDeepslateNtmOre variant = NATIVE_HARVEST.get();
        if (variant == null || event.block != variant.getBase() || event.blockMetadata != 0) return;
        for (ItemStack drop : event.drops) if (Block.getBlockFromItem(drop.getItem()) == event.block && drop.getItemDamage() == 0) {
            drop.func_150996_a(Item.getItemFromBlock(variant));
            drop.setItemDamage(0);
        }
    }

    public static boolean isNative(ItemStack stack) {
        GameRegistry.UniqueIdentifier id = stack == null || stack.getItem() == null ? null : GameRegistry.findUniqueIdentifierFor(stack.getItem());
        return id != null && "hbm".equals(id.modId);
    }

    public static boolean preservesDrops(Block block, ItemStack drop) {
        return block instanceof BlockDeepslateHBMOre || block instanceof ganymedes01.etfuturum.blocks.ores.modded.BlockDeepslateNtmOre || isNative(drop)
                || isNative(new ItemStack(block));
    }

    public static ItemStack processingInput(ItemStack input) {
        if (input == null) return null;
        Block block = Block.getBlockFromItem(input.getItem());
        Block nativeOre;
        if (block instanceof ganymedes01.etfuturum.blocks.ores.modded.BlockDeepslateNtmOre) {
            nativeOre = ((ganymedes01.etfuturum.blocks.ores.modded.BlockDeepslateNtmOre) block).getBase();
        } else if (block instanceof BlockDeepslateHBMOre && input.getItemDamage() >= 0 && input.getItemDamage() < 16) {
            nativeOre = ((BlockDeepslateHBMOre) block).getBase(input.getItemDamage());
        } else return input;
        ItemStack nativeInput = new ItemStack(nativeOre, input.stackSize, 0);
        if (input.hasTagCompound()) nativeInput.setTagCompound((net.minecraft.nbt.NBTTagCompound) input.getTagCompound().copy());
        return nativeInput;
    }

    /** Hide only redundant menu entries. Keep every existing registry ID and recipe. */
    public static boolean hasOnlyNtmSources(String tag) {
        if (!ModBlocks.DEEPSLATE_HBM_ORE.isEnabled()) return false;
        boolean ntm = false;
        for (ItemStack stack : OreDictionary.getOres(tag)) {
            if (isNative(stack)) { ntm = true; continue; }
            GameRegistry.UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(stack.getItem());
            if (id != null && !"etfuturum".equals(id.modId)) return false;
        }
        return ntm;
    }

}

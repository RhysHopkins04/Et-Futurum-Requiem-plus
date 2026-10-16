package ganymedes01.etfuturum.blocks.ores.modded;

import cpw.mods.fml.common.registry.GameRegistry;
import ganymedes01.etfuturum.blocks.ores.BaseDeepslateOre;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import java.util.ArrayList;
import java.util.Random;

/** One block/item identity per material. NTM planetary metadata is never reused as a material ID. */
public final class BlockDeepslateNtmOre extends BaseDeepslateOre {
    private final String material;
    private final String nativeName;
    private final boolean reactive;
    private final boolean nativeOverlay;

    public BlockDeepslateNtmOre(String material) {
        this(material, "ore_" + material, false, false);
    }

    public BlockDeepslateNtmOre(String material, String nativeName, boolean reactive) {
        this(material, nativeName, reactive, true);
    }

    private BlockDeepslateNtmOre(String material, String nativeName, boolean reactive, boolean nativeOverlay) {
        this.material = material;
        this.nativeName = nativeName;
        this.reactive = reactive;
        this.nativeOverlay = nativeOverlay;
        setNames("deepslate_hbm_" + material + "_ore");
    }

    @Override public String getTextureSubfolder() { return "hbm"; }

    @Override public Block getBase() {
        Block block = GameRegistry.findBlock("hbm", "tile." + nativeName);
        if (block == null) block = GameRegistry.findBlock("hbm", nativeName);
        if (block == null) throw new IllegalStateException("Missing NTM ore: " + material);
        return block;
    }

    @Override public void onLoadAction() {
        super.onLoadAction();
        if (reactive) setTickRandomly(getBase().getTickRandomly());
    }

    @Override public int tickRate(World world) { return getBase().tickRate(world); }

    @Override public void onNeighborBlockChange(World world, int x, int y, int z, Block changed) {
        getBase().onNeighborBlockChange(world, x, y, z, changed);
    }

    @Override public void dropBlockAsItemWithChance(World world, int x, int y, int z, int meta, float chance, int fortune) {
        // Invoke the actual native instance: its gas selection uses identity checks.
        if (reactive) ganymedes01.etfuturum.compat.NtmOreCompat.nativeHarvest(this,
                () -> getBase().dropBlockAsItemWithChance(world, x, y, z, 0, chance, fortune));
        else super.dropBlockAsItemWithChance(world, x, y, z, 0, chance, fortune);
    }

    @Override
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public void registerBlockIcons(net.minecraft.client.renderer.texture.IIconRegister register) {
        if (nativeOverlay) {
            blockIcon = ganymedes01.etfuturum.client.texture.NtmDeepslateSprite.register(register, getTextureName(), nativeName);
        } else super.registerBlockIcons(register);
    }

    @Override public Item getItemDropped(int meta, Random random, int fortune) {
        Item item = getBase().getItemDropped(0, random, fortune);
        return item == Item.getItemFromBlock(getBase()) ? Item.getItemFromBlock(this) : item;
    }

    @Override public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int meta, int fortune) {
        return super.getDrops(world, x, y, z, 0, fortune);
    }

    @Override public int damageDropped(int meta) { return getBase().damageDropped(0); }
    @Override public int quantityDropped(int meta, int fortune, Random random) { return getBase().quantityDropped(0, fortune, random); }
    @Override public boolean canSilkHarvest(World world, EntityPlayer player, int x, int y, int z, int meta) {
        return getBase().canSilkHarvest(world, player, x, y, z, 0);
    }
}

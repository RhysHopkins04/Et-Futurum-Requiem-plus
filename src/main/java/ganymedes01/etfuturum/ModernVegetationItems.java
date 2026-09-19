package ganymedes01.etfuturum;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.client.ModernAssetResourcePack;
import ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Pass 37 block-local vegetation items that were missing from the parity shell.
 * These are deliberately tiny planting items; they do not register any world generation.
 */
public final class ModernVegetationItems {
    private static Item torchflowerSeeds;
    private static Item pitcherPod;

    private ModernVegetationItems() { }

    public static void init() {
        if (!ConfigBlocksItems.enableModernMapParityBlocks) return;
        if (GameRegistry.findItem(Tags.MOD_ID, "torchflower_seeds") == null) {
            torchflowerSeeds = new AncientPlantingItem("torchflower_seeds", ModernMapParityBlocks.TORCHFLOWER_CROP);
            GameRegistry.registerItem(torchflowerSeeds, "torchflower_seeds");
        } else {
            torchflowerSeeds = GameRegistry.findItem(Tags.MOD_ID, "torchflower_seeds");
        }
        if (GameRegistry.findItem(Tags.MOD_ID, "pitcher_pod") == null) {
            pitcherPod = new AncientPlantingItem("pitcher_pod", ModernMapParityBlocks.PITCHER_CROP);
            GameRegistry.registerItem(pitcherPod, "pitcher_pod");
        } else {
            pitcherPod = GameRegistry.findItem(Tags.MOD_ID, "pitcher_pod");
        }
    }

    public static Item getTorchflowerSeeds() { return torchflowerSeeds; }
    public static Item getPitcherPod() { return pitcherPod; }

    private static final class AncientPlantingItem extends Item {
        private final String registryName;
        private final ModernMapParityBlocks crop;

        private AncientPlantingItem(String registryName, ModernMapParityBlocks crop) {
            this.registryName = registryName;
            this.crop = crop;
            setUnlocalizedName(Tags.MOD_ID + "." + registryName);
            setCreativeTab(EtFuturum.creativeTabItems);
        }

        @Override
        public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z,
                int side, float hitX, float hitY, float hitZ) {
            if (stack == null || stack.stackSize <= 0 || side != 1 || world.getBlock(x, y, z) != Blocks.farmland)
                return false;
            int ty = y + 1;
            if (!player.canPlayerEdit(x, ty, z, side, stack) || !world.isAirBlock(x, ty, z)) return false;
            Block cropBlock = crop.get();
            if (cropBlock == null || !cropBlock.canPlaceBlockAt(world, x, ty, z)) return false;
            if (!world.isRemote) {
                if (!world.setBlock(x, ty, z, cropBlock, 0, 3)) return false;
                world.playSoundEffect(x + 0.5D, ty + 0.5D, z + 0.5D,
                        cropBlock.stepSound.func_150496_b(),
                        (cropBlock.stepSound.getVolume() + 1.0F) / 2.0F,
                        cropBlock.stepSound.getPitch() * 0.8F);
                if (!player.capabilities.isCreativeMode && --stack.stackSize <= 0)
                    player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
            }
            return true;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void registerIcons(IIconRegister register) {
            String synthetic = "modern_item/" + registryName;
            ModernAssetResourcePack.registerDynamicAlias(
                    "textures/items/" + synthetic + ".png",
                    "textures/item/" + registryName + ".png");
            itemIcon = register.registerIcon("minecraft:" + synthetic);
        }
    }
}

package efrtest;

import cpw.mods.fml.common.Loader;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.ModItems;
import ganymedes01.etfuturum.compat.CompatIronChests;
import ganymedes01.etfuturum.tileentities.TileEntityShulkerBox.ShulkerBoxType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.world.World;
import static efrtest.EarthImportContracts.check;

public final class IronChestShulkerProbe {
    public static void run(World world) throws Exception {
        if(!Loader.isModLoaded("IronChest"))return;
        Class<?> chestType=Class.forName("cpw.mods.ironchest.IronChestType");int checked=0;
        for(ShulkerBoxType type:ShulkerBoxType.VALUES) {
            if(type.ordinal()==0 || !CompatIronChests.tierExists(type.name()))continue;
            Object actual=null;for(Object constant:chestType.getEnumConstants())if(((Enum<?>)constant).name().equals(type.name()))actual=constant;
            check(actual!=null,"enabled shulker tier has no matching chest");
            ItemStack chest=(ItemStack)CompatIronChests.class.getMethod("registeredChest",chestType).invoke(null,actual);
            check(chest!=null,"registered chest missing for "+type);
            InventoryCrafting grid=new InventoryCrafting(new Container(){@Override public boolean canInteractWith(EntityPlayer p){return true;}},3,3);
            grid.setInventorySlotContents(1,ModItems.SHULKER_SHELL.newItemStack());grid.setInventorySlotContents(4,chest);
            grid.setInventorySlotContents(7,ModItems.SHULKER_SHELL.newItemStack());
            ItemStack result=CraftingManager.getInstance().findMatchingRecipe(grid,world);
            check(result!=null && result.getItem()==ModBlocks.SHULKER_BOX.getItem() && result.stackSize==1
                    && result.hasTagCompound() && result.getTagCompound().getByte("Type")==type.ordinal()
                    && !result.getTagCompound().hasKey("Color"),"wrong chest-shell recipe output for "+type+": "+result);
            grid.setInventorySlotContents(8,new ItemStack(net.minecraft.init.Items.diamond));
            check(CraftingManager.getInstance().findMatchingRecipe(grid,world)==null,"tier recipe consumes extra ingredients");checked++;
        }
        check(checked>=3,"expected several enabled Iron Chests tiers");
        System.out.println("IRON_CHESTS_SHULKERS PASSED: "+checked+" named registered chest tiers, direct two-shell crafting and extra-input rejection");
    }
}

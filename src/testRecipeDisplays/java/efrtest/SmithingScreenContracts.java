package efrtest;

import ganymedes01.etfuturum.Tags;
import ganymedes01.etfuturum.client.gui.inventory.GuiSmithingTable;
import ganymedes01.etfuturum.entities.EntityArmourStand;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

public final class SmithingScreenContracts {
    private static void check(boolean ok, String why) { if (!ok) throw new AssertionError(why); }
    public static void run() {
        // AssetDirector deliberately checks unversioned mod-pack overrides before its jar.
        // A bundled historical background therefore wins even for minecraft_1.21.11 requests.
        check(Tags.MC_ASSET_VER.equals("minecraft_1.21.11"), "smithing reference version changed");
        check(GuiSmithingTable.texturePath.getResourceDomain().equals(Tags.MC_ASSET_VER), "smithing bypasses AssetDirector");
        check(SmithingScreenContracts.class.getResource("/assets/minecraft/textures/gui/container/smithing.png") == null,
                "bundled smithing background shadows modern AssetDirector art");
        EntityArmourStand stand = new EntityArmourStand(null);
        ItemStack chest = new ItemStack(Items.diamond_chestplate);
        GuiSmithingTable.equipPreview(stand, chest);
        check(stand.getEquipmentInSlot(3) != null && stand.getEquipmentInSlot(3) != chest, "armor preview missing or aliases result");
        GuiSmithingTable.equipPreview(stand, new ItemStack(Items.diamond_pickaxe));
        for (int slot = 0; slot < 5; slot++) check(stand.getEquipmentInSlot(slot) == null, "tool equipped or stale armor retained");
        GuiSmithingTable.equipPreview(stand, new ItemStack(Items.diamond_helmet));
        check(stand.getEquipmentInSlot(4) != null, "helmet preview wrong slot");
        GuiSmithingTable.equipPreview(stand, new ItemStack(Items.diamond_leggings));
        check(stand.getEquipmentInSlot(2) != null && stand.getEquipmentInSlot(4) == null, "leggings preview wrong slot or stale helmet");
        GuiSmithingTable.equipPreview(stand, new ItemStack(Items.diamond_boots));
        check(stand.getEquipmentInSlot(1) != null, "boots preview wrong slot");
        GuiSmithingTable.equipPreview(stand, null);
        for (int slot = 0; slot < 5; slot++) check(stand.getEquipmentInSlot(slot) == null, "empty result retains preview equipment");
        System.out.println("SMITHING_SCREEN PASSED: versioned resource, no legacy override, copied armor in correct slots, tools and empty results clear all equipment");
    }
}

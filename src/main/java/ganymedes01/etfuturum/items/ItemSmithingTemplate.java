package ganymedes01.etfuturum.items;

public final class ItemSmithingTemplate extends BaseSubtypesItem {
    public static final String[] NAMES={"netherite_upgrade_smithing_template","sentry_armor_trim_smithing_template","dune_armor_trim_smithing_template","coast_armor_trim_smithing_template","wild_armor_trim_smithing_template","ward_armor_trim_smithing_template","eye_armor_trim_smithing_template","vex_armor_trim_smithing_template","tide_armor_trim_smithing_template","snout_armor_trim_smithing_template","rib_armor_trim_smithing_template","spire_armor_trim_smithing_template","wayfinder_armor_trim_smithing_template","shaper_armor_trim_smithing_template","silence_armor_trim_smithing_template","raiser_armor_trim_smithing_template","host_armor_trim_smithing_template","flow_armor_trim_smithing_template","bolt_armor_trim_smithing_template"};
    public ItemSmithingTemplate() { super(NAMES);setNames("smithing_template"); }
    @Override @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public net.minecraft.item.EnumRarity getRarity(net.minecraft.item.ItemStack stack) {
        // Vanilla 1.21.11: Silence is epic; Ward, Eye, Vex and Spire are rare.
        int meta=stack.getItemDamage();
        return meta==14?net.minecraft.item.EnumRarity.epic:
                meta==5 || meta==6 || meta==7 || meta==11?net.minecraft.item.EnumRarity.rare:net.minecraft.item.EnumRarity.uncommon;
    }
    @Override @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public void addInformation(net.minecraft.item.ItemStack stack,net.minecraft.entity.player.EntityPlayer player,java.util.List<String> tooltip,boolean advanced) {
        String kind=stack.getItemDamage()==0?"efr.template.upgrade":"efr.template.trim";
        tooltip.add(net.minecraft.util.EnumChatFormatting.GRAY+net.minecraft.util.StatCollector.translateToLocal("efr.template.type"));
        tooltip.add("");
        tooltip.add(net.minecraft.util.EnumChatFormatting.GRAY+net.minecraft.util.StatCollector.translateToLocal("efr.template.applies_to"));
        tooltip.add(net.minecraft.util.EnumChatFormatting.BLUE+" "+net.minecraft.util.StatCollector.translateToLocal(kind+".applies_to"));
        tooltip.add(net.minecraft.util.EnumChatFormatting.GRAY+net.minecraft.util.StatCollector.translateToLocal("efr.template.ingredients"));
        tooltip.add(net.minecraft.util.EnumChatFormatting.BLUE+" "+net.minecraft.util.StatCollector.translateToLocal(kind+".ingredients"));
    }
}

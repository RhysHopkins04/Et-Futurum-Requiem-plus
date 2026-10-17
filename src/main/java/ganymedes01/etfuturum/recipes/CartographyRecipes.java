package ganymedes01.etfuturum.recipes;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.api.LockedMapData;
import ganymedes01.etfuturum.configuration.configs.ConfigMapCompatibility;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapData;

/** Preview never creates/changes saved maps. New identities are allocated only on actual take. */
public final class CartographyRecipes {
    private static final String OP="etfuturum:cartography_operation";
    private CartographyRecipes() {}
    public static boolean enabled() { return ModBlocks.CARTOGRAPHY_TABLE.isEnabled() && !ConfigMapCompatibility.mapCompatibilityMode; }
    public static boolean addition(ItemStack stack) { return stack!=null && stack.stackSize>0 && (stack.getItem()==Items.paper || stack.getItem()==Items.map || stack.getItem()==Item.getItemFromBlock(Blocks.glass_pane)); }
    public static MapData data(World world,ItemStack stack) { return stack==null || stack.getItem()!=Items.filled_map?null:(MapData)world.loadItemData(MapData.class,"map_"+stack.getItemDamage()); }
    public static String operation(World world,ItemStack map,ItemStack extra) {
        if(map==null || map.stackSize<=0 || !addition(extra))return "";
        MapData data=data(world,map);if(data==null)return "";
        boolean locked=((LockedMapData)data).etfuturum$isLocked();
        if(extra.getItem()==Items.map)return "copy";
        if(locked)return "";
        if(extra.getItem()==Items.paper)return data.scale<4?"expand":"";
        return "lock";
    }
    public static ItemStack preview(World world,ItemStack map,ItemStack extra) {
        String op=operation(world,map,extra);if(op.isEmpty())return null;
        ItemStack result=map.copy();result.stackSize=op.equals("copy")?2:1;
        if(result.hasTagCompound())result.getTagCompound().removeTag("map_is_scaling");
        if(!op.equals("copy"))result.setTagInfo(OP,new net.minecraft.nbt.NBTTagString(op));
        return result;
    }
    public static int center(int coordinate,int scale) {
        int size=128*(1<<scale);return (int)(Math.floor((coordinate+64D)/size)*size+size/2D-64);
    }
    public static ItemStack finish(World world,ItemStack map,ItemStack extra) {
        String op=operation(world,map,extra);ItemStack result=preview(world,map,extra);if(result==null || world.isRemote)return result;
        if(!op.equals("copy")) {
            MapData source=data(world,map);int id=world.getUniqueDataId("map");MapData target=new MapData("map_"+id);
            target.scale=(byte)(source.scale+(op.equals("expand")?1:0));target.dimension=source.dimension;
            target.xCenter=op.equals("expand")?center(source.xCenter,target.scale):source.xCenter;
            target.zCenter=op.equals("expand")?center(source.zCenter,target.scale):source.zCenter;
            if(op.equals("lock")) { target.colors=source.colors.clone();((LockedMapData)target).etfuturum$setLocked(true); }
            target.markDirty();world.setItemData("map_"+id,target);result.setItemDamage(id);
            result.getTagCompound().removeTag(OP);result.getTagCompound().removeTag("map_is_scaling");
            if(result.getTagCompound().hasNoTags())result.setTagCompound(null);
        }
        return result;
    }
}

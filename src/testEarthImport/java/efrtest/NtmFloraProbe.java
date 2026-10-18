package efrtest;

import java.util.ArrayList;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldServer;

/** Executes native growth, crop drops and registered cadmium processing. */
public final class NtmFloraProbe {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void run(WorldServer world, boolean restart) throws Exception {
        int x=-224, y=270, z=-224;
        Class<?> modBlocks=Class.forName("com.hbm.blocks.ModBlocks");
        Class<?> modItems=Class.forName("com.hbm.items.ModItems");
        Block flower=(Block)modBlocks.getField("plant_flower").get(null);
        Block tall=(Block)modBlocks.getField("plant_tall").get(null);
        Block oily=(Block)modBlocks.getField("dirt_oily").get(null);
        Block reeds=(Block)modBlocks.getField("reeds").get(null);
        if (restart && new java.io.File("ntm-flora-fixture-created").exists()) check(world.getBlock(x,y,z)==flower && world.getBlockMetadata(x,y,z)==4
            && world.getBlock(x,y-1,z)==oily,"willow fixture failed save/reload");
        world.setBlock(x,y-1,z,Blocks.grass,0,2);
        world.setBlockToAir(x,y,z); world.setBlockToAir(x,y+1,z);
        for(int meta : new int[]{0,1,2,3,4,5,6,7}) {
            check(flower.canPlaceBlockAt(world,x,y,z),"native flower cannot stay on imported grass");
            world.setBlock(x,y,z,flower,meta,2);
            check(world.getBlockMetadata(x,y,z)==meta,"resource flower metadata mismatch");
            check(!flower.getDrops(world,x,y,z,meta,0).isEmpty(),"resource flower has no native drops");
            world.setBlockToAir(x,y,z);
        }
        world.setBlock(x,y-1,z,Blocks.water,0,2);
        check(reeds.canBlockStay(world,x,y,z),"native reeds need water beneath");
        world.setBlock(x,y-1,z,Blocks.grass,0,2);
        check(!reeds.canBlockStay(world,x,y,z),"native reeds unexpectedly accept soil");
        world.setBlock(x,y-1,z,oily,0,2);
        world.setBlock(x,y,z,flower,4,2);
        for (int[] delta : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) world.setBlockToAir(x+delta[0],y-1,z+delta[1]);
        check(!((IGrowable)flower).func_149851_a(world,x,y,z,false),"willow grew without water");
        world.setBlock(x+1,y-1,z,Blocks.water,0,2);
        for (int stage=0;stage<4;stage++) {
            Block current=world.getBlock(x,y,z);
            check(current instanceof IGrowable && ((IGrowable)current).func_149851_a(world,x,y,z,false),"native willow growth stalled at stage "+stage);
            ((IGrowable)current).func_149853_b(world,world.rand,x,y,z);
        }
        check(world.getBlock(x,y,z)==tall && world.getBlockMetadata(x,y,z)==3
            && world.getBlockMetadata(x,y+1,z)==11,"native willow did not mature correctly");
        check(world.getBlock(x,y-1,z)==Blocks.dirt,"mature willow did not consume oily soil");
        Item plantItem=(Item)modItems.getField("plant_item").get(null);
        Item cadmium=(Item)modItems.getField("powder_cadmium").get(null);
        ArrayList<ItemStack> drops=tall.getDrops(world,x,y+1,z,11,0);
        ItemStack crop=null;
        for(ItemStack stack:drops) if(stack.getItem()==plantItem && stack.getItemDamage()==2) crop=stack;
        check(crop!=null && crop.stackSize>=3 && crop.stackSize<=6,"mature native willow lacks cadmium crop");
        Class<?> recipes=Class.forName("com.hbm.inventory.recipes.CrystallizerRecipes");
        java.lang.reflect.Field recipeField=recipes.getDeclaredField("recipes"); recipeField.setAccessible(true);
        Map<?,?> all=(Map<?,?>)recipeField.get(null);
        boolean found=false;
        for(Map.Entry<?,?> entry:all.entrySet()) {
            Object pair=entry.getKey();
            Object key=pair.getClass().getMethod("getKey").invoke(pair);
            // Inspect the actual registered native input, not an invented fallback recipe.
            if(!key.getClass().getSimpleName().equals("ComparableStack")) continue;
            ItemStack input=(ItemStack)key.getClass().getMethod("toStack").invoke(key);
            if(input.getItem()!=plantItem || input.getItemDamage()!=2) continue;
            Object recipe=entry.getValue();
            ItemStack output=(ItemStack)recipe.getClass().getField("output").get(recipe);
            check(output.getItem()==cadmium && recipe.getClass().getField("itemAmount").getInt(recipe)==10
                && recipe.getClass().getField("acidAmount").getInt(recipe)==250,"native cadmium processing mismatch");
            found=true;
        }
        check(found,"native willow crystallizer recipe not registered");
        // Restore a seed crop for persistence; block updates are native-owned.
        world.setBlockToAir(x,y+1,z); world.setBlockToAir(x+1,y-1,z);
        world.setBlock(x,y-1,z,oily,0,2); world.setBlock(x,y,z,flower,4,2);
        new java.io.File("ntm-flora-fixture-created").createNewFile();
        System.out.println("NTM_NATIVE_FLORA PASSED: resource metadata/drops, reed support, willow growth/soil consumption, cadmium recipe and restart");
    }
}

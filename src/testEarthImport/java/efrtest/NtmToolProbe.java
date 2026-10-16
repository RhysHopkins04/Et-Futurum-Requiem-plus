package efrtest;

import ganymedes01.etfuturum.api.DeepslateOreRegistry;
import ganymedes01.etfuturum.blocks.ores.modded.BlockDeepslateHBMOre;
import ganymedes01.etfuturum.blocks.ores.modded.BlockDeepslateNtmOre;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import java.util.*;
import java.lang.reflect.Method;
import static efrtest.EarthImportContracts.check;

/** Runs NTM's actual processing abilities, not an imitation of their recipes. */
public final class NtmToolProbe {
    private static final int X=160, Y=90, Z=160;
    public static void run(WorldServer world, BlockDeepslateHBMOre legacy) throws Exception {
        Class<?> api=Class.forName("com.hbm.handler.ability.IToolHarvestAbility");
        Method ability=api.getMethod("onHarvestBlock",int.class,World.class,int.class,int.class,int.class,EntityPlayer.class,Block.class,int.class);
        Class<?> tool=Class.forName("com.hbm.items.tool.ItemToolAbility");
        tool.getField("dropX").setInt(null,X);tool.getField("dropY").setInt(null,Y);tool.getField("dropZ").setInt(null,Z);
        FakePlayer player=FakePlayerFactory.getMinecraft(world);player.capabilities.isCreativeMode=false;
        Set<Block> blocks=new HashSet<>();
        java.io.File fixture=new java.io.File("ntm-earth-coverage-fixture-created");
        List<ItemStack> menu=new ArrayList<>();legacy.getSubBlocks(net.minecraft.item.Item.getItemFromBlock(legacy),null,menu);
        check(menu.isEmpty(),"old metadata family still in menu");
        List<BlockDeepslateNtmOre> variants=new ArrayList<>();
        for(ganymedes01.etfuturum.ModBlocks entry:ganymedes01.etfuturum.ModBlocks.values())
            if(entry.isEnabled() && entry.get() instanceof BlockDeepslateNtmOre) variants.add((BlockDeepslateNtmOre)entry.get());
        check(variants.size()==28,"missing Earth ore variants");
        for(int material=0;material<variants.size();material++) {
            BlockDeepslateNtmOre block=variants.get(material);
            Block nativeOre=block.getBase();
            check(DeepslateOreRegistry.getOre(nativeOre,0).get()==block,"native replacement mapping missing "+material);
            check(block instanceof BlockDeepslateNtmOre && blocks.add(block),"material lacks unique block identity "+material);
            int fx=176+(material%4), fz=176+(material/4);
            if (fixture.exists()) {
                check(world.getBlock(fx,Y,fz)==block && world.getBlockMetadata(fx,Y,fz)==0,"individual ID did not survive save/reload "+material);
                if(material<16)check(world.getBlock(fx,Y+1,fz)==legacy && world.getBlockMetadata(fx,Y+1,fz)==material,"legacy subtype did not survive save/reload "+material);
            }
            world.setBlock(fx,Y,fz,block,0,2);if(material<16)world.setBlock(fx,Y+1,fz,legacy,material,2);
            ItemStack input=new ItemStack(block);
            check(!input.getHasSubtypes() && input.getItemDamage()==0,"ore still uses material metadata "+material);
            for(int fortune:new int[]{0,3,9}) {
                check(remapSelfDrop(standard(world,player,nativeOre,fortune),nativeOre,block).equals(standard(world,player,block,fortune)),"real ordinary/Fortune harvest differs "+material+":"+fortune);
            }
            for(String name:new String[]{"SMELTER","SHREDDER","CENTRIFUGE","CRYSTALLIZER"}) {
                Object operation=api.getField(name).get(null);
                Map<String,Integer> expected=harvest(world,player,ability,operation,nativeOre);
                Map<String,Integer> actual=harvest(world,player,ability,operation,block);
                check(remapSelfDrop(expected,nativeOre,block).equals(actual),"NTM "+name+" output mismatch material="+material+" native="+expected+" actual="+actual);
            }
            // Execute NTM's silk ability toggles and vanilla/Forge's real silk harvest.
            player.inventory.setInventorySlotContents(0,new ItemStack(Items.diamond_pickaxe));player.inventory.currentItem=0;
            Object silk=api.getField("SILK").get(null);
            api.getMethod("preHarvestAll",int.class,World.class,EntityPlayer.class).invoke(silk,0,world,player);
            clear(world);world.setBlock(X,Y,Z,block,0,2);world.rand.setSeed(12345L);
            check(block.canSilkHarvest(world,player,X,Y,Z,0),"silk harvest rejected "+material);
            block.harvestBlock(world,player,X,Y,Z,0);
            List<ItemStack> silkDrops=collect(world);
            check(silkDrops.size()==1 && ItemStack.areItemStacksEqual(silkDrops.get(0),input),"silk lost material identity "+material);
            api.getMethod("postHarvestAll",int.class,World.class,EntityPlayer.class).invoke(silk,0,world,player);
            // Real machine/tool queries must also decode the old family without rewriting saves.
            if (material<16) {
            ItemStack old=new ItemStack(legacy,1,material);
            ItemStack normal=ganymedes01.etfuturum.compat.NtmOreCompat.processingInput(old);
            check(ItemStack.areItemStacksEqual(normal,new ItemStack(nativeOre)),"old metadata decoder failed "+material);
            }
        }
        clear(world);
        NtmGasProbe.run(world);
        fixture.createNewFile();
        check(ganymedes01.etfuturum.compat.NtmOreCompat.processingInput(null)==null,"null recipe input changed");
        ItemStack unrelated=new ItemStack(Items.diamond);
        check(ganymedes01.etfuturum.compat.NtmOreCompat.processingInput(unrelated)==unrelated,"unrelated input rewritten");
        ItemStack tagged=new ItemStack(blocks.iterator().next(),7);
        tagged.setTagCompound(new net.minecraft.nbt.NBTTagCompound());tagged.getTagCompound().setString("probe","kept");
        ItemStack normalized=ganymedes01.etfuturum.compat.NtmOreCompat.processingInput(tagged);
        check(normalized!=tagged && normalized.stackSize==7 && "kept".equals(normalized.getTagCompound().getString("probe")),"normalization lost count or NBT");
        normalized.getTagCompound().setString("probe","changed");
        check("kept".equals(tagged.getTagCompound().getString("probe")),"normalization mutated source NBT");
        System.out.println("EARTH_IMPORT_NTM_TOOLS PASSED: 28 unique meta-zero IDs, actual silk harvest + smelter/shredder/centrifuge/crystallizer abilities, native output parity");
    }
    private static Map<String,Integer> remapSelfDrop(Map<String,Integer> original,Block nativeOre,Block variant) {
        String from=net.minecraft.item.Item.itemRegistry.getNameForObject(net.minecraft.item.Item.getItemFromBlock(nativeOre))+":0:";
        String to=net.minecraft.item.Item.itemRegistry.getNameForObject(net.minecraft.item.Item.getItemFromBlock(variant))+":0:";
        Map<String,Integer> result=new TreeMap<>();
        for(Map.Entry<String,Integer> entry:original.entrySet()) result.put(entry.getKey().startsWith(from)?to+entry.getKey().substring(from.length()):entry.getKey(),entry.getValue());
        return result;
    }
    private static Map<String,Integer> standard(WorldServer world,FakePlayer player,Block block,int fortune) {
        clear(world);player.inventory.currentItem=0;
        ItemStack pick=new ItemStack(Items.diamond_pickaxe);
        if(fortune>0)pick.addEnchantment(net.minecraft.enchantment.Enchantment.fortune,fortune);
        player.inventory.setInventorySlotContents(0,pick);
        world.setBlock(X,Y,Z,block,0,2);world.rand.setSeed(12345L);
        block.harvestBlock(world,player,X,Y,Z,0);world.setBlockToAir(X,Y,Z);
        return summary(collect(world));
    }
    private static Map<String,Integer> harvest(WorldServer world,FakePlayer player,Method method,Object operation,Block block) throws Exception {
        clear(world);player.inventory.currentItem=0;player.inventory.setInventorySlotContents(0,new ItemStack(Items.diamond_pickaxe));
        world.setBlock(X,Y,Z,block,0,2);world.rand.setSeed(12345L);
        method.invoke(operation,0,world,X,Y,Z,player,block,0);
        check(world.getBlock(X,Y,Z)!=block,"NTM ability did not break "+block.getUnlocalizedName());
        Map<String,Integer> output=summary(collect(world));
        output.put("@remaining:"+Block.blockRegistry.getNameForObject(world.getBlock(X,Y,Z))+":"+world.getBlockMetadata(X,Y,Z),1);
        return output;
    }
    private static Map<String,Integer> summary(List<ItemStack> stacks) {
        Map<String,Integer> result=new TreeMap<>();
        for(ItemStack stack:stacks) {
            String key=net.minecraft.item.Item.itemRegistry.getNameForObject(stack.getItem())+":"+stack.getItemDamage()+":"+stack.getTagCompound();
            result.put(key,result.containsKey(key)?result.get(key)+stack.stackSize:stack.stackSize);
        }
        return result;
    }
    private static List<ItemStack> collect(WorldServer world) {
        List<ItemStack> result=new ArrayList<>();
        for(Object entity:new ArrayList<>(world.loadedEntityList)) if(entity instanceof EntityItem && !((EntityItem)entity).isDead) {
            EntityItem drop=(EntityItem)entity;
            if(Math.abs(drop.posX-X)<5 && Math.abs(drop.posZ-Z)<5) result.add(drop.getEntityItem().copy());
        }
        return result;
    }
    private static void clear(WorldServer world) {
        for(Object entity:new ArrayList<>(world.loadedEntityList)) if(entity instanceof EntityItem) {
            EntityItem drop=(EntityItem)entity;
            if(Math.abs(drop.posX-X)<5 && Math.abs(drop.posZ-Z)<5) world.removeEntity(drop);
        }
    }
}

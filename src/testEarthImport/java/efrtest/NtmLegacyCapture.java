package efrtest;

import java.lang.reflect.*;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

/** Bounded reproduction of HbmWorldGen's individual Overworld entry points. */
final class NtmLegacyCapture {
    static final String[] NAMES=("antenna desert_atom library_dungeon arctic_vault jungle_dungeon ancient_tomb locked_safe blueprint_obsidian_cache stone_keyhole soyuz_capsule corrupt_broadcaster broken_behemoth_lantern_and_booklet waste_tank unexploded_dud landmines bosnia_minefield chlorine_geyser glyphid_hive pink_oak_conversion ntm_meteorite spaceship gas_bubble explosive_gas_bubble").split(" ");
    static Block block(String name)throws Exception{return (Block)Class.forName("com.hbm.blocks.ModBlocks").getField(name).get(null);}
    static void set(Object target,String name,Object value)throws Exception{target.getClass().getField(name).set(target,value);}
    static String pool(String owner,String name)throws Exception{return (String)Class.forName("com.hbm.itempool."+owner).getField(name).get(null);}
    static void fill(Random rng,IInventory inventory,String pool,int count)throws Exception{
        WeightedRandomChestContent[] contents=(WeightedRandomChestContent[])Class.forName("com.hbm.itempool.ItemPool").getMethod("getPool",String.class).invoke(null,pool);
        WeightedRandomChestContent.generateChestContents(rng,contents,inventory,count);
    }
    static void generate(NtmRecordingWorld world,Random rng,String name,int variant)throws Exception{
        if(name.equals("ancient_tomb_high"))name="ancient_tomb";
        int y=64;
        if(name.equals("arctic_vault")){
            Object generator=Class.forName("com.hbm.world.dungeon.ArcticVault").getConstructor().newInstance();
            generator.getClass().getMethod("trySpawn",World.class,int.class,int.class,int.class).invoke(generator,world,0,32,0);
        }else if(name.equals("jungle_dungeon")){
            Object generator=Class.forName("com.hbm.world.generator.CellularDungeonFactory").getField("jungle").get(null);
            Method generate=generator.getClass().getMethod("generate",World.class,int.class,int.class,int.class,Random.class);
            Method drain=NtmStructureCaptureProbe.class.getDeclaredMethod("drainTimedJobs",NtmRecordingWorld.class);drain.setAccessible(true);
            for(int floor:new int[]{20,24,28}){generate.invoke(generator,world,0,floor,0,world.rand);drain.invoke(null,world);}
            int marker=world.getHeightValue(0,0);for(int n=0;n<3;n++)world.setBlock(0,marker+n,0,block("deco_titanium"));world.setBlock(0,marker+3,0,Blocks.redstone_block);
        }else if(name.equals("locked_safe")){
            world.setBlock(0,y,0,block("safe"),rng.nextInt(4)+2,2);TileEntity safe=world.getTileEntity(0,y,0);
            double[] mods={1.0,0.1,0.02,0};String[] pools={"POOL_VAULT_RUSTY","POOL_VAULT_STANDARD","POOL_VAULT_REINFORCED","POOL_VAULT_UNBREAKABLE"};
            safe.getClass().getMethod("setMod",double.class).invoke(safe,mods[variant]);
            fill(rng,(IInventory)safe,pool("ItemPoolsSingle",pools[variant]),variant==0?3+rng.nextInt(4):variant==1?2+rng.nextInt(3):variant==2?1+rng.nextInt(3):1+rng.nextInt(2));
            safe.getClass().getMethod("setPins",int.class).invoke(safe,1+rng.nextInt(999));safe.getClass().getMethod("lock").invoke(safe);
            if(variant%2==0)safe.getClass().getMethod("fillWithSpiders").invoke(safe);
        }else if(name.equals("blueprint_obsidian_cache")){
            world.setBlock(0,y,0,Blocks.chest);
            for(int x=-1;x<=1;x++)for(int yy=y-1;yy<=y+1;yy++)for(int z=-1;z<=1;z++)if(x!=0||yy!=y||z!=0)world.setBlock(x,yy,z,Blocks.obsidian);
            fill(rng,(IInventory)world.getTileEntity(0,y,0),pool("ItemPoolsSingle","POOL_BLUEPRINTS"),50);
        }else if(name.equals("stone_keyhole")){world.setBlock(0,12,0,block("stone_keyhole"));
        }else if(name.equals("soyuz_capsule")){
            world.setBlock(0,y-4,0,block("soyuz_capsule"),3,2);IInventory inventory=(IInventory)world.getTileEntity(0,y-4,0);
            Item record=(Item)Class.forName("com.hbm.items.ModItems").getField("record_glass").get(null);
            inventory.setInventorySlotContents(rng.nextInt(inventory.getSizeInventory()),new ItemStack(record));
        }else if(name.equals("corrupt_broadcaster")){world.setBlock(0,y,0,block("broadcaster_pc"),rng.nextInt(4)+2,2);
        }else if(name.equals("broken_behemoth_lantern_and_booklet")){
            Block lantern=block("lantern_behemoth");world.setBlock(0,y,0,lantern,12,3);
            Class.forName("com.hbm.handler.MultiblockHandlerXR").getMethod("fillSpace",World.class,int.class,int.class,int.class,int[].class,Block.class,ForgeDirection.class)
                .invoke(null,world,0,y,0,new int[]{4,0,0,0,0,0},lantern,ForgeDirection.NORTH);
            set(world.getTileEntity(0,y,0),"isBroken",true);
            if(variant%2==0){
                Class<?> loot=Class.forName("com.hbm.util.LootGenerator");loot.getMethod("setBlock",World.class,int.class,int.class,int.class).invoke(null,world,0,y,-2);
                loot.getMethod("lootBooklet",World.class,int.class,int.class,int.class).invoke(null,world,0,y,-2);
            }
        }else if(name.equals("landmines")||name.equals("bosnia_minefield")){
            world.setBlock(0,y,0,block(name.equals("landmines")?"mine_ap":"mine_he"));set(world.getTileEntity(0,y,0),"waitingForPlayer",true);
        }else if(name.equals("glyphid_hive")){
            Class.forName("com.hbm.world.feature.GlyphidHive").getMethod("generateSmall",World.class,int.class,int.class,int.class,Random.class,boolean.class,boolean.class)
                .invoke(null,world,0,y,0,rng,variant%2==0,true);
        }else if(name.equals("pink_oak_conversion")){
            for(int yy=y;yy<y+6;yy++)world.setBlock(0,yy,0,block("pink_log"));
        }else if(name.equals("gas_bubble")||name.equals("explosive_gas_bubble")){
            Class.forName("com.hbm.world.generator.DungeonToolbox").getMethod("generateOre",World.class,Random.class,int.class,int.class,int.class,int.class,int.class,int.class,Block.class,int.class)
                .invoke(null,world,rng,0,0,1,32,30,10,block(name.equals("gas_bubble")?"gas_flammable":"gas_explosive"),1);
        }else if(name.equals("ntm_meteorite")){
            Object generator=Class.forName("com.hbm.world.feature.Meteorite").getConstructor().newInstance();
            generator.getClass().getMethod("generate",World.class,Random.class,int.class,int.class,int.class,boolean.class,boolean.class,boolean.class)
                .invoke(generator,world,rng,0,y-4,0,false,false,false);
        }else{
            String type=name.equals("antenna")?"dungeon.Antenna":name.equals("desert_atom")?"dungeon.DesertAtom001":
                name.equals("library_dungeon")?"dungeon.LibraryDungeon":name.equals("ancient_tomb")?"dungeon.AncientTomb":
                name.equals("waste_tank")?"dungeon.Barrel":name.equals("unexploded_dud")?"feature.Dud":
                name.equals("chlorine_geyser")?"feature.Geyser":"dungeon.Spaceship";
            Object generator=Class.forName("com.hbm.world."+type).getConstructor().newInstance();
            generator.getClass().getMethod(name.equals("ancient_tomb")?"build":"generate",World.class,Random.class,int.class,int.class,int.class)
                .invoke(generator,world,rng,0,name.equals("library_dungeon")?32:y,0);
        }
    }
}

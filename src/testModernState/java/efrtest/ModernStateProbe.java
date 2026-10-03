package efrtest;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import ganymedes01.etfuturum.ModernMapParityBlocks;
import ganymedes01.etfuturum.ModernPowderSnowBucket;
import ganymedes01.etfuturum.blocks.BlockPowderSnow;
import ganymedes01.etfuturum.core.utils.PowderSnowPhysics;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.monster.EntityBlaze;
import net.minecraft.entity.monster.EntitySnowman;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import static efrtest.ModernStateContracts.check;

@Mod(modid="modern_state_probe",name="Modern state runtime tests",version="test",dependencies="required-after:etfuturum",acceptableRemoteVersions="*")
public final class ModernStateProbe {
    @Mod.EventHandler public void start(FMLServerStartedEvent event) throws Exception {
        if (!Boolean.getBoolean("efr.modernStateProbe")) return;
        MinecraftServer server=MinecraftServer.getServer();WorldServer world=server.worldServerForDimension(0);
        try {
            check(!ganymedes01.etfuturum.configuration.configs.ConfigWorld.extendedWorldHeight,"extended height enabled");
            check(!ganymedes01.etfuturum.configuration.configs.ConfigWorld.modernOverworldGeneration,"modern worldgen enabled");
            int x=96,y=70,z=96;
            boolean restart=world.getBlock(x-1,y,z)==Blocks.gold_block;
            if (restart) {
                for (int age=0;age<=25;age++) {check(ModernMapParityBlocks.getKelpAge(world,x+age,y,z)==age,"restart age "+age);check(world.getTileEntity(x+age,y,z)==null,"restart TE "+age);}
                world.getChunkFromBlockCoords(140,100);
                boolean frozenPersisted=false;
                for (Object entity:world.loadedEntityList) {
                    net.minecraft.entity.Entity e=(net.minecraft.entity.Entity)entity;
                    if (e.getEntityData().getBoolean("efrProbeFrozen")) {
                        check(e.getEntityData().getInteger(PowderSnowPhysics.FROZEN)==140,"freeze persistence after restart");
                        frozenPersisted=true;
                    }
                }
                // The first probe predates the entity fixture; subsequent runs must preserve it.
                if (new java.io.File("frozen-fixture-created").exists()) check(frozenPersisted,"frozen entity missing after restart");
                System.out.println("MODERN_STATE_PROBE restart persistence PASSED (all 26 ages, zero head TEs, entity freeze state="+frozenPersisted+")");
            }
            for (int age=0;age<=25;age++) {
                world.setBlock(x+age,y-1,z,Blocks.stone,0,2);
                world.setBlock(x+age,y,z,ModernMapParityBlocks.KELP.get(),0,2);
                ModernMapParityBlocks.setKelpAge(world,x+age,y,z,age);
                check(ModernMapParityBlocks.getKelpAge(world,x+age,y,z)==age,"runtime age "+age);
                check(world.getTileEntity(x+age,y,z)==null,"head creates TE");
            }
            world.setBlock(x-1,y,z,Blocks.gold_block,0,2);
            // Exercise transformed Anvil loading, including >255 block IDs and negative coordinates.
            int low=Block.getIdFromBlock(ModernMapParityBlocks.KELP.get()),high=Block.getIdFromBlock(ModernMapParityBlocks.KELP_AGE_16.get());
            NBTTagCompound level=ModernStateContracts.fixture(low,high);level.setIntArray("HeightMap",new int[256]);level.setByteArray("Biomes",new byte[256]);
            AnvilChunkLoader loader=new AnvilChunkLoader(new java.io.File("world"));
            Method read=AnvilChunkLoader.class.getDeclaredMethod("readChunkFromNBT",net.minecraft.world.World.class,NBTTagCompound.class);read.setAccessible(true);
            Chunk migrated=(Chunk)read.invoke(loader,world,level);
            for(int age=0;age<=25;age++) {
                check(migrated.getBlock(age&15,64,age>>4)==(age<16?ModernMapParityBlocks.KELP.get():ModernMapParityBlocks.KELP_AGE_16.get()),"Anvil migration block");
                check(migrated.getBlockMetadata(age&15,64,age>>4)==(age&15),"Anvil migration meta");}
            check(migrated.chunkTileEntityMap.isEmpty(),"Anvil migration allocated TEs");check(migrated.isModified,"migrated chunk not dirty");
            // These calls execute the installed Forge material hooks rather than helper-only checks.
            // A two-cell contained-water volume permits body and eye immersion with no real water blocks.
            int fx=140,fy=70,fz=100;
            for (int dx=-1;dx<=1;dx++) for (int dz=-1;dz<=1;dz++) {
                world.setBlock(fx+dx,fy-1,fz+dz,Blocks.stone,0,2);
                world.setBlock(fx+dx,fy,fz+dz,Blocks.air,0,2);
                world.setBlock(fx+dx,fy+1,fz+dz,Blocks.air,0,2);
            }
            world.setBlock(fx,fy-1,fz,Blocks.stone,0,2);
            world.setBlock(fx,fy,fz,ModernMapParityBlocks.KELP_PLANT.get(),0,2);
            world.setBlock(fx,fy+1,fz,ModernMapParityBlocks.KELP.get(),0,2);
            EntityZombie zombie=new EntityZombie(world);zombie.setPosition(fx+0.5,fy,fz+0.5);
            check(world.handleMaterialAcceleration(zombie.boundingBox,Material.water,zombie),"contained body water");
            check(zombie.isInsideOfMaterial(Material.water),"contained eye water");
            zombie.setPosition(fx+0.5,fy+1+8.0D/9.0D-zombie.getEyeHeight()+0.01D,fz+0.5);
            check(!zombie.isInsideOfMaterial(Material.water),"eye above exposed source surface submerged");
            zombie.setPosition(fx+0.5,fy,fz+0.5);
            zombie.setAir(1);float beforeDrowning=zombie.getHealth();
            for (int tick=0;tick<25;tick++) zombie.onEntityUpdate();
            check(zombie.getHealth()<beforeDrowning,"contained water did not drown living entity");
            zombie.handleWaterMovement();check(zombie.isInWater(),"contained water swimming state missing");
            world.setBlock(fx,fy,fz,ModernMapParityBlocks.SEA_PICKLE.get(),3,2);world.setBlock(fx,fy+1,fz,Blocks.air,0,2);
            check(!ModernMapParityBlocks.containsWater(world,fx,fy,fz),"dry pickle water");
            check(ModernMapParityBlocks.fillContainedWater(world,fx,fy,fz),"water bucket fill");
            check(world.getBlockMetadata(fx,fy,fz)==7,"pickle count lost on fill");
            check(ModernMapParityBlocks.drainContainedWater(world,fx,fy,fz),"water bucket pickup");
            check(world.getBlockMetadata(fx,fy,fz)==3,"pickle count lost on pickup");
            ((ganymedes01.etfuturum.core.utils.IContainedWaterFlow)Blocks.flowing_water).etfu$spreadSource(world,fx,fy,fz);
            check(world.getBlockMetadata(fx,fy,fz)==3,"dry source flow changed pickle");
            ModernMapParityBlocks.fillContainedWater(world,fx,fy,fz);
            ((ganymedes01.etfuturum.core.utils.IContainedWaterFlow)Blocks.flowing_water).etfu$spreadSource(world,fx,fy,fz);
            check(world.getBlock(fx-1,fy,fz)==Blocks.flowing_water && world.getBlockMetadata(fx-1,fy,fz)==1,"occupied source failed to spread");
            Blocks.flowing_water.updateTick(world,fx-1,fy,fz,world.rand);
            check(world.getBlock(fx-1,fy,fz).getMaterial()==Material.water && world.getBlockMetadata(fx-1,fy,fz)==1,"flow lost contained source decay");
            world.setBlock(fx-1,fy,fz,Blocks.lava,0,3);
            check(world.getBlock(fx-1,fy,fz)==Blocks.obsidian,"contained water did not cool lava");
            check(world.getBlock(fx,fy,fz)==ModernMapParityBlocks.SEA_PICKLE.get() && world.getBlockMetadata(fx,fy,fz)==7,"fluid propagation replaced source block/state");
            BlockPowderSnow snow=(BlockPowderSnow)ModernMapParityBlocks.POWDER_SNOW.get();world.setBlock(fx,fy,fz,snow,0,2);
            check(ModernPowderSnowBucket.get()!=null,"powder snow bucket missing");
            zombie.setPosition(fx+0.5,fy+1,fz+0.5);ArrayList<AxisAlignedBB> boxes=new ArrayList<AxisAlignedBB>();
            AxisAlignedBB mask=AxisAlignedBB.getBoundingBox(fx,fy,fz,fx+1,fy+2,fz+1);
            snow.addCollisionBoxesToList(world,fx,fy,fz,mask,boxes,zombie);check(boxes.isEmpty(),"bare feet solid snow");
            zombie.setCurrentItemOrArmor(1,new ItemStack(Items.leather_boots));snow.addCollisionBoxesToList(world,fx,fy,fz,mask,boxes,zombie);
            check(boxes.size()==1 && boxes.get(0).maxY==fy+1,"leather collision");check(!PowderSnowPhysics.canFreeze(zombie),"leather immunity");
            zombie.fallDistance=3;boxes.clear();snow.addCollisionBoxesToList(world,fx,fy,fz,mask,boxes,zombie);
            check(boxes.size()==1 && Math.abs(boxes.get(0).maxY-(fy+0.9))<0.00001,"falling collision");
            zombie.setCurrentItemOrArmor(1,null);zombie.setPosition(fx+0.5,fy,fz+0.5);
            zombie.hurtResistantTime=0;zombie.getEntityData().setInteger(PowderSnowPhysics.FROZEN,139);zombie.ticksExisted=40;float health=zombie.getHealth();PowderSnowPhysics.tick(zombie);
            check(zombie.getEntityData().getInteger(PowderSnowPhysics.FROZEN)==140 && zombie.getHealth()==health-1,"freeze damage");
            check(!PowderSnowPhysics.canFreeze(new EntitySnowman(world)),"snow golem immunity");
            EntityBlaze blaze=new EntityBlaze(world);blaze.setPosition(fx+0.5,fy,fz+0.5);blaze.getEntityData().setInteger(PowderSnowPhysics.FROZEN,139);blaze.ticksExisted=40;
            health=blaze.getHealth();PowderSnowPhysics.tick(blaze);check(blaze.getHealth()==health-5,"blaze extra freeze damage");
            double previousX=zombie.posX;
            zombie.motionX=0.2D; zombie.moveEntity(0.2D,0,0);
            check(Math.abs(zombie.posX-previousX-0.18D)<0.00001 && zombie.motionX==0,"powder snow movement multiplier/drag");
            zombie.setFire(10);snow.onEntityCollidedWithBlock(world,fx,fy,fz,zombie);check(world.isAirBlock(fx,fy,fz) && !zombie.isBurning(),"burning entity melt/extinguish");
            net.minecraft.entity.player.EntityPlayerMP player=new net.minecraft.entity.player.EntityPlayerMP(server,world,
                    new com.mojang.authlib.GameProfile(java.util.UUID.fromString("48647269-7479-4000-8000-000000000001"),"StateProbe"),new net.minecraft.server.management.ItemInWorldManager(world));
            world.setBlock(fx,fy,fz,snow,0,2);
            net.minecraftforge.event.entity.player.FillBucketEvent pickup=new net.minecraftforge.event.entity.player.FillBucketEvent(player,new ItemStack(Items.bucket),world,new net.minecraft.util.MovingObjectPosition(fx,fy,fz,1,net.minecraft.util.Vec3.createVectorHelper(fx+0.5,fy+1,fz+0.5)));
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(pickup);
            check(pickup.getResult()==cpw.mods.fml.common.eventhandler.Event.Result.ALLOW && pickup.result.getItem()==ModernPowderSnowBucket.get() && world.isAirBlock(fx,fy,fz),"powder snow bucket pickup event");
            world.setBlock(fx,fy,fz,ModernMapParityBlocks.TALL_SEAGRASS.get(),0,2);
            world.setBlock(fx,fy+1,fz,ModernMapParityBlocks.TALL_SEAGRASS.get(),1,2);
            check(ModernMapParityBlocks.drainContainedWater(world,fx,fy,fz),"tall seagrass drain");
            check(world.isAirBlock(fx,fy,fz) && world.getBlock(fx,fy+1,fz)==Blocks.water,"drain destroyed neighboring contained source water");
            zombie.getEntityData().setBoolean("efrProbeFrozen",true);
            world.spawnEntityInWorld(zombie);
            world.saveAllChunks(true,null);
            new java.io.File("frozen-fixture-created").createNewFile();
            System.out.println("MODERN_STATE_PROBE PASSED: real Forge block registry, no kelp TEs, transformed migration/immersion, wet/dry counts, snow collision, freezing damage/immunity/melting");
        } catch(Throwable failure) {failure.printStackTrace();System.out.println("MODERN_STATE_PROBE FAILED");throw new RuntimeException(failure);}
        finally {server.initiateShutdown();}
    }
}

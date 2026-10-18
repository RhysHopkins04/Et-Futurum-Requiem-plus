package efrtest;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.blocks.BlockBamboo;
import ganymedes01.etfuturum.tileentities.TileEntityCaveVines;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldServer;
import java.io.File;
import static efrtest.EarthImportContracts.check;

/** Native placement/state decoding and disk save/restart for the Earth audit corrections. */
public final class FlaggedStateProbe {
    public static void run(WorldServer world) throws Exception {
        boolean restart=new File("flagged-state-fixture-created").isFile();
        for(int meta=0;meta<16;meta++) {
            check(BlockBamboo.setStage(meta,false)==(meta&7),"bamboo stage clearing changed other bits");
            check(BlockBamboo.setStage(meta,true)==(meta|8),"bamboo stage setting changed other bits");
            check(BlockBamboo.setStalkSize(meta,false)==(meta&14),"bamboo stalk clearing changed leaves/stage");
            check(BlockBamboo.setStalkSize(meta,true)==(meta|1),"bamboo stalk setting changed leaves/stage");
        }
        Block infested=ModBlocks.INFESTED_DEEPSLATE.get();
        check(infested.getRenderType()==31,"infested deepslate must use pillar UV rotation");
        int[] axes={0,4,8};int[] sides={1,5,3};
        for(int i=0;i<3;i++) {
            check(infested.onBlockPlaced(world,-176+i,128,112,sides[i],0.5F,0.5F,0.5F,0)==axes[i],"infested axis placement");
            if(!restart)world.setBlock(-176+i,128,112,infested,axes[i],2);
            check(world.getBlock(-176+i,128,112)==infested && world.getBlockMetadata(-176+i,128,112)==axes[i],"infested axis lost on restart");
        }
        for(int age=0;age<=25;age++) {
            NBTTagCompound tag=new NBTTagCompound();tag.setInteger("Age",age);tag.setInteger("MaxLength",27);
            TileEntityCaveVines vine=new TileEntityCaveVines();vine.readFromNBT(tag);
            NBTTagCompound out=new NBTTagCompound();vine.writeToNBT(out);
            check(out.getInteger("Age")==age,"canonical vine age NBT roundtrip");
        }
        int i=0;
        for(int stalk=0;stalk<2;stalk++)for(int leaves=0;leaves<3;leaves++)for(int stage=0;stage<2;stage++) {
            int meta=stalk|(leaves<<1)|(stage<<3);int x=-176+(i&15),z=116+(i>>4);i++;
            if(!restart) {
                world.setBlock(x,127,z,Blocks.grass,0,2);
                world.setBlock(x,128,z,ModBlocks.BAMBOO.get(),meta,2);
            }
            int actual=world.getBlockMetadata(x,128,z);
            check(world.getBlock(x,128,z)==ModBlocks.BAMBOO.get() && actual==meta,"bamboo disk state changed");
            check(BlockBamboo.getStalkSize(actual)==stalk && BlockBamboo.getLeavesSize(actual)==leaves && BlockBamboo.getStage(actual)==stage,"bamboo native decoding");
        }
        world.saveAllChunks(true,null);
        new File("flagged-state-fixture-created").createNewFile();
        System.out.println("FLAGGED_STATES_"+(restart?"RESTART":"SAVE")+" PASSED");
    }
}

package ganymedes01.etfuturum.tileentities;

import ganymedes01.etfuturum.Tags;

import ganymedes01.etfuturum.core.handlers.SculkEventHandler;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

public class TileEntitySculkCatalyst extends TileEntity {
    private final SculkSpreaderLegacy spreader=new SculkSpreaderLegacy();
    private long bloomEndsAt;
    // 1.7 calls validate() while the chunk is still installing TileEntities. World/block
    // reads from validate() can recursively load the same chunk, so defer all runtime work.
    private boolean runtimeInitPending = true;
    private boolean runtimeRegistered;

    @Override public void validate(){
        super.validate();
        runtimeInitPending = true;
        runtimeRegistered = false;
    }
    @Override public void invalidate(){unregisterRuntime();super.invalidate();}
    @Override public void onChunkUnload(){unregisterRuntime();super.onChunkUnload();}

    private void unregisterRuntime(){
        if(runtimeRegistered)SculkEventHandler.INSTANCE.unregisterCatalyst(this);
        runtimeRegistered=false;
        runtimeInitPending=true;
    }

    private void ensureRuntimeReady(){
        if(!runtimeInitPending||worldObj==null||worldObj.isRemote)return;
        SculkEventHandler.INSTANCE.registerCatalyst(this);
        runtimeRegistered=true;
        runtimeInitPending=false;
        long now=worldObj.getTotalWorldTime();
        // Imported/saved bloom metadata still gets a coherent transient timer, but only
        // after the TE is fully installed and ordinary world reads are safe.
        if(worldObj.getBlockMetadata(xCoord,yCoord,zCoord)==1&&bloomEndsAt<=now){
            bloomEndsAt=now+8L;
            markDirty();
        }
    }

    public void bloomWithExperience(int experience){bloomWithExperience(experience,xCoord,yCoord+1,zCoord);}
    public void bloomWithExperience(int experience,int sourceX,int sourceY,int sourceZ){
        if(worldObj==null||worldObj.isRemote||experience<=0)return;
        spreader.addCursors(sourceX,sourceY,sourceZ,experience);
        bloomEndsAt=worldObj.getTotalWorldTime()+8L;
        worldObj.setBlockMetadataWithNotify(xCoord,yCoord,zCoord,1,3);
        worldObj.playSoundEffect(xCoord+.5,yCoord+.5,zCoord+.5,Tags.MC_ASSET_VER+":block.sculk_catalyst.bloom",2.0F,1.0F);
        if(worldObj instanceof net.minecraft.world.WorldServer)((net.minecraft.world.WorldServer)worldObj).func_147487_a("portal",sourceX+.5,sourceY+.5,sourceZ+.5,8,.3,.3,.3,.02);
        markDirty();
    }
    @Override public void updateEntity(){
        if(worldObj==null||worldObj.isRemote)return;
        ensureRuntimeReady();
        spreader.tick(worldObj,xCoord,yCoord,zCoord,worldObj.rand);
        if(worldObj.getBlockMetadata(xCoord,yCoord,zCoord)==1&&bloomEndsAt>0&&worldObj.getTotalWorldTime()>=bloomEndsAt){
            worldObj.setBlockMetadataWithNotify(xCoord,yCoord,zCoord,0,3);
            bloomEndsAt=0;
            markDirty();
        }
    }
    @Override public void writeToNBT(NBTTagCompound tag){super.writeToNBT(tag);tag.setLong("BloomEndsAt",bloomEndsAt);spreader.writeToNBT(tag);}
    @Override public void readFromNBT(NBTTagCompound tag){super.readFromNBT(tag);bloomEndsAt=tag.getLong("BloomEndsAt");spreader.readFromNBT(tag);}
}

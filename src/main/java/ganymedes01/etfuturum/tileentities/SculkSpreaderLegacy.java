package ganymedes01.etfuturum.tileentities;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.ModernMapParityBlocks;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** Bounded 1.7 adaptation of the modern level SculkSpreader charge/cursor model. */
public final class SculkSpreaderLegacy {
    public static final int MAX_CHARGE=1000, MAX_CURSORS=32, MAX_GROWTH_RATE_RADIUS=24, SHRIEKER_PLACEMENT_RATE=11;
    private static final class Cursor { int x,y,z,charge,decayDelay,updateDelay; Cursor(int x,int y,int z,int c){this.x=x;this.y=y;this.z=z;charge=c;updateDelay=1;} }
    private final List<Cursor> cursors=new ArrayList<Cursor>();

    public void addCursors(int x,int y,int z,int charge){while(charge>0&&cursors.size()<MAX_CURSORS){int c=Math.min(MAX_CHARGE,charge);cursors.add(new Cursor(x,y,z,c));charge-=c;}}
    public boolean isEmpty(){return cursors.isEmpty();}

    public void tick(World world,int catalystX,int catalystY,int catalystZ,Random random){
        if(world==null||world.isRemote)return;
        Iterator<Cursor>it=cursors.iterator();
        while(it.hasNext()){
            Cursor c=it.next(); if(c.charge<=0){it.remove();continue;} if(c.updateDelay-->0)continue; c.updateDelay=1;
            if(distanceSq(c.x,c.y,c.z,catalystX,catalystY,catalystZ)>MAX_GROWTH_RATE_RADIUS*MAX_GROWTH_RATE_RADIUS){it.remove();continue;}
            // Move through a small local neighbourhood without ever probing an unloaded chunk.
            int nx=c.x+random.nextInt(3)-1, ny=c.y+random.nextInt(3)-1, nz=c.z+random.nextInt(3)-1;
            if(!loaded(world,nx,nz)) continue;
            if(Math.abs(nx-catalystX)+Math.abs(ny-catalystY)+Math.abs(nz-catalystZ)>MAX_GROWTH_RATE_RADIUS*2)continue;
            c.x=nx;c.y=ny;c.z=nz;
            boolean spent=false;
            if(canReplace(world,nx,ny,nz)){
                world.setBlock(nx,ny,nz,ModBlocks.SCULK.get(),0,3);c.charge--;spent=true;
                tryPlaceVeinAround(world,nx,ny,nz,random);
            }else if(world.isAirBlock(nx,ny,nz) || world.getBlock(nx,ny,nz)==ModernMapParityBlocks.SCULK_VEIN.get()){
                spent=tryPlaceVein(world,nx,ny,nz,random); if(spent)c.charge--;
            }
            double d2=distanceSq(nx,ny,nz,catalystX,catalystY,catalystZ);
            // Modern level-spreader special growth happens after charge has travelled beyond the no-growth radius.
            if(d2>16.0D && c.charge>=10 && random.nextInt(18)==0 && trySpecialGrowth(world,nx,ny,nz,random)){c.charge-=10;spent=true;}
            if(!spent && d2>16.0D && random.nextInt(2)==0)c.charge--; // bounded distance-dependent decay
            if(c.charge<=0)it.remove();
        }
    }

    private static boolean loaded(World w,int x,int z){return w.getChunkProvider()!=null&&w.getChunkProvider().chunkExists(x>>4,z>>4);}
    private static double distanceSq(int x,int y,int z,int ox,int oy,int oz){double dx=x-ox,dy=y-oy,dz=z-oz;return dx*dx+dy*dy+dz*dz;}
    private static boolean canReplace(World w,int x,int y,int z){
        if(!loaded(w,x,z)||y<0||y>=256)return false; TileEntity te=w.getTileEntity(x,y,z);if(te!=null)return false;
        Block b=w.getBlock(x,y,z); if(b==null||b==Blocks.air||b==ModBlocks.SCULK.get())return false;
        return b==Blocks.stone||b==Blocks.dirt||b==Blocks.grass||b==Blocks.gravel||b==Blocks.sand||b==Blocks.sandstone||b==Blocks.clay||b==Blocks.hardened_clay||b==Blocks.stained_hardened_clay||b==Blocks.netherrack||b==Blocks.end_stone||b==Blocks.mycelium;
    }
    private static boolean tryPlaceVeinAround(World w,int x,int y,int z,Random r){for(ForgeDirection d:ForgeDirection.VALID_DIRECTIONS){int ax=x+d.offsetX,ay=y+d.offsetY,az=z+d.offsetZ;if((w.isAirBlock(ax,ay,az)||w.getBlock(ax,ay,az)==ModernMapParityBlocks.SCULK_VEIN.get())&&tryPlaceVein(w,ax,ay,az,r))return true;}return false;}
    private static boolean tryPlaceVein(World w,int x,int y,int z,Random r){
        if(!loaded(w,x,z)||ModernMapParityBlocks.SCULK_VEIN.get()==null)return false;
        int mask=0;for(ForgeDirection d:ForgeDirection.VALID_DIRECTIONS){int sx=x+d.offsetX,sy=y+d.offsetY,sz=z+d.offsetZ;if(!loaded(w,sx,sz))continue;Block support=w.getBlock(sx,sy,sz);if(support!=null&&support!=Blocks.air&&!support.getMaterial().isLiquid()&&w.isSideSolid(sx,sy,sz,d.getOpposite(),false))mask|=1<<d.ordinal();}
        if(mask==0)return false;
        if(w.getBlock(x,y,z)==ModernMapParityBlocks.SCULK_VEIN.get()){
            TileEntity te=w.getTileEntity(x,y,z);if(te instanceof ModernMapParityBlocks.ParityMultifaceTileEntity){ModernMapParityBlocks.ParityMultifaceTileEntity m=(ModernMapParityBlocks.ParityMultifaceTileEntity)te;m.setFaceMask(m.getFaceMask()|mask);return true;}return false;
        }
        if(!w.isAirBlock(x,y,z))return false; w.setBlock(x,y,z,ModernMapParityBlocks.SCULK_VEIN.get(),0,3);TileEntity te=w.getTileEntity(x,y,z);if(te instanceof ModernMapParityBlocks.ParityMultifaceTileEntity)((ModernMapParityBlocks.ParityMultifaceTileEntity)te).setFaceMask(mask);return true;
    }
    private static boolean trySpecialGrowth(World w,int x,int y,int z,Random r){
        if(!w.isAirBlock(x,y,z))return false;
        Block target=r.nextInt(SHRIEKER_PLACEMENT_RATE)==0?ModernMapParityBlocks.SCULK_SHRIEKER.get():ModernMapParityBlocks.SCULK_SENSOR.get();if(target==null)return false;
        if(y<=0||!loaded(w,x,z))return false;Block below=w.getBlock(x,y-1,z);if(below!=ModBlocks.SCULK.get())return false;
        // Mojang 22w13a: only worldgen shriekers may can_summon; player-placed and
        // Sculk-spread-generated shriekers are explicitly can_summon=false.
        int meta=0;
        return w.setBlock(x,y,z,target,meta,3);
    }

    public void writeToNBT(NBTTagCompound tag){NBTTagList list=new NBTTagList();for(Cursor c:cursors){NBTTagCompound n=new NBTTagCompound();n.setInteger("X",c.x);n.setInteger("Y",c.y);n.setInteger("Z",c.z);n.setInteger("Charge",c.charge);n.setInteger("DecayDelay",c.decayDelay);n.setInteger("UpdateDelay",c.updateDelay);list.appendTag(n);}tag.setTag("cursors",list);}
    public void readFromNBT(NBTTagCompound tag){cursors.clear();NBTTagList list=tag.getTagList("cursors",10);for(int i=0;i<list.tagCount()&&cursors.size()<MAX_CURSORS;i++){NBTTagCompound n=list.getCompoundTagAt(i);Cursor c=new Cursor(n.getInteger("X"),n.getInteger("Y"),n.getInteger("Z"),Math.min(MAX_CHARGE,Math.max(0,n.getInteger("Charge"))));c.decayDelay=n.getInteger("DecayDelay");c.updateDelay=n.getInteger("UpdateDelay");if(c.charge>0)cursors.add(c);}}
}

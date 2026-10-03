package ganymedes01.etfuturum.tileentities;

import ganymedes01.etfuturum.Tags;

import ganymedes01.etfuturum.ModernMapParityBlocks;
import ganymedes01.etfuturum.core.handlers.SculkEventHandler;
import ganymedes01.etfuturum.core.handlers.SculkVibrationDispatcher;
import ganymedes01.etfuturum.core.handlers.SculkVibrationDispatcher.Vibration;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldServer;

import java.util.List;
import java.util.UUID;

/** Runtime-only gameplay state for Pass-38 Sensor/Calibrated Sensor/Shrieker identities. */
public class TileEntitySculkRuntime extends TileEntity {
    private int power;
    private int lastFrequency;
    private long phaseEndsAt;
    private long pendingArrivalAt = -1L;
    private int pendingFrequency;
    private double pendingX, pendingY, pendingZ;
    private UUID pendingPlayer;
    private int shriekerWarningLevel;
    // TileEntity#validate is invoked while 1.7 is still inserting chunk TileEntities.
    // Never query the world or expose this listener to vibration dispatch from validate(),
    // because either path can synchronously re-enter the same chunk load.
    private boolean runtimeInitPending = true;
    private boolean runtimeRegistered;

    @Override public void validate() {
        super.validate();
        runtimeInitPending = true;
        runtimeRegistered = false;
    }
    @Override public void invalidate() {
        unregisterRuntime();
        super.invalidate();
    }
    @Override public void onChunkUnload() {
        unregisterRuntime();
        super.onChunkUnload();
    }

    private void unregisterRuntime() {
        if (runtimeRegistered) SculkVibrationDispatcher.unregister(this);
        runtimeRegistered = false;
        runtimeInitPending = true;
    }

    private void ensureRuntimeReady() {
        if (!runtimeInitPending || worldObj == null || worldObj.isRemote) return;
        // updateEntity runs only after the TE has been installed in its owning chunk, so
        // identity/metadata reads performed by normalization are now chunk-load safe.
        SculkVibrationDispatcher.register(this);
        runtimeRegistered = true;
        runtimeInitPending = false;
        normalizeImportedTransientState();
    }

    private ModernMapParityBlocks identity() {
        return worldObj == null ? null : ModernMapParityBlocks.fromBlock(worldObj.getBlock(xCoord,yCoord,zCoord));
    }
    private boolean sensor() { return identity()==ModernMapParityBlocks.SCULK_SENSOR; }
    private boolean calibrated() { return identity()==ModernMapParityBlocks.CALIBRATED_SCULK_SENSOR; }
    private boolean shrieker() { return identity()==ModernMapParityBlocks.SCULK_SHRIEKER; }
    private int phase() {
        int m=worldObj.getBlockMetadata(xCoord,yCoord,zCoord)&15;
        return calibrated()?m/4:sensor()?m:0;
    }
    private int facing() { return calibrated() ? (worldObj.getBlockMetadata(xCoord,yCoord,zCoord)&3) : 0; }
    private void setPhase(int p) {
        if (calibrated()) worldObj.setBlockMetadataWithNotify(xCoord,yCoord,zCoord,(p*4)+(facing()&3),3);
        else if(sensor()) worldObj.setBlockMetadataWithNotify(xCoord,yCoord,zCoord,p,3);
    }

    private void normalizeImportedTransientState() {
        if (worldObj==null || worldObj.isRemote) return;
        long now=worldObj.getTotalWorldTime();
        if ((sensor()||calibrated()) && phase()!=0 && phaseEndsAt<=now)
            phaseEndsAt=now+(phase()==1?(calibrated()?10:30):10);
        if ((sensor()||calibrated()) && phase()!=0) clearPendingVibration();
        if (shrieker() && isShrieking() && phaseEndsAt<=now) phaseEndsAt=now+90;
    }

    public int getPower() { return power; }
    public int getLastFrequency() { return lastFrequency; }
    public int getComparatorFrequency() { return (sensor() || calibrated()) && phase() == 1 ? lastFrequency : 0; }

    public void offer(Vibration v) {
        if (worldObj==null || worldObj.isRemote || v==null || pendingArrivalAt>=0) return;
        if (sensor() || calibrated()) {
            if (v.event==SculkVibrationDispatcher.Event.SCULK_SENSOR_TENDRILS_CLICKING || phase()!=0 || v.event.frequency<=0) return;
            int radius=calibrated()?16:8;
            double dx=(xCoord+0.5D)-v.x, dy=(yCoord+0.5D)-v.y, dz=(zCoord+0.5D)-v.z;
            double dist=Math.sqrt(dx*dx+dy*dy+dz*dz);
            if (dist>radius) return;
            // A sensor does not respond to its own placement/destruction event.
            if ((v.event==SculkVibrationDispatcher.Event.BLOCK_PLACE || v.event==SculkVibrationDispatcher.Event.BLOCK_DESTROY)
                    && Math.floor(v.x)==xCoord && Math.floor(v.y)==yCoord && Math.floor(v.z)==zCoord) return;
            if (calibrated()) {
                int filter=calibrationInput();
                if (filter!=0 && filter!=v.event.frequency) return;
            }
            if (SculkVibrationDispatcher.isOccludedByDampening(worldObj,v.x,v.y,v.z,xCoord+0.5D,yCoord+0.5D,zCoord+0.5D)) return;
            schedule(v, Math.max(1,(int)Math.floor(dist)));
        } else if (shrieker()) {
            if (v.event!=SculkVibrationDispatcher.Event.SCULK_SENSOR_TENDRILS_CLICKING || isShrieking() || v.responsiblePlayer==null) return;
            double dx=(xCoord+0.5D)-v.x, dy=(yCoord+0.5D)-v.y, dz=(zCoord+0.5D)-v.z;
            double dist=Math.sqrt(dx*dx+dy*dy+dz*dz);
            if (dist>8.0D) return;
            schedule(v, Math.max(1,(int)Math.floor(dist)));
        }
    }

    private void schedule(Vibration v, int delay) {
        pendingArrivalAt=worldObj.getTotalWorldTime()+delay;
        pendingFrequency=v.event.frequency;
        pendingX=v.x; pendingY=v.y; pendingZ=v.z; pendingPlayer=v.responsiblePlayer;
        markDirty();
    }

    private void clearPendingVibration() {
        pendingArrivalAt=-1L; pendingFrequency=0; pendingX=pendingY=pendingZ=0.0D; pendingPlayer=null;
    }

    private int calibrationInput() {
        // metadata facing index north/east/south/west. Modern calibrated input is the rear face.
        int f=facing(), dx=0,dz=0;
        if(f==0) dz=1; else if(f==1) dx=-1; else if(f==2) dz=-1; else dx=1;
        return worldObj.getIndirectPowerLevelTo(xCoord+dx,yCoord,zCoord+dz, f==0?2:f==1?5:f==2?3:4);
    }

    private boolean isShrieking() { return (worldObj.getBlockMetadata(xCoord,yCoord,zCoord)&2)!=0; }
    private boolean canSummon() { return (worldObj.getBlockMetadata(xCoord,yCoord,zCoord)&1)!=0; }
    private void setShrieking(boolean b) {
        int m=worldObj.getBlockMetadata(xCoord,yCoord,zCoord)&3;
        m=b?(m|2):(m&~2);
        worldObj.setBlockMetadataWithNotify(xCoord,yCoord,zCoord,m,3);
    }

    @Override public void updateEntity() {
        if (worldObj==null || worldObj.isRemote) return;
        ensureRuntimeReady();
        long now=worldObj.getTotalWorldTime();
        if (pendingArrivalAt>=0 && now>=pendingArrivalAt) {
            // A vibration accepted while INACTIVE may activate only if the listener is still
            // listening when it arrives. Never carry a stale vibration through ACTIVE/COOLDOWN.
            if(sensor()||calibrated()) {
                if(phase()==0) activateSensor();
                clearPendingVibration();
            } else if(shrieker()) {
                activateShrieker();
                clearPendingVibration();
            }
        }
        if(sensor()||calibrated()) {
            if(phase()==1 && phaseEndsAt>0 && now>=phaseEndsAt) {
                clearPendingVibration();
                power=0; setPhase(2); phaseEndsAt=now+10; notifyPower();
                worldObj.playSoundEffect(xCoord+.5,yCoord+.5,zCoord+.5,Tags.MC_ASSET_VER+":block.sculk_sensor.clicking_stop",1.0F,1.0F);
                markDirty();
            } else if(phase()==2 && phaseEndsAt>0 && now>=phaseEndsAt) {
                clearPendingVibration();
                setPhase(0); phaseEndsAt=0; markDirty();
            }
        } else if(shrieker() && isShrieking() && phaseEndsAt>0 && now>=phaseEndsAt) {
            setShrieking(false); phaseEndsAt=0;
            respondAfterShriek();
            markDirty();
        }
    }

    private void activateSensor() {
        if(phase()!=0) return;
        double dx=(xCoord+0.5D)-pendingX,dy=(yCoord+0.5D)-pendingY,dz=(zCoord+0.5D)-pendingZ;
        int radius=calibrated()?16:8;
        power=SculkVibrationDispatcher.redstoneStrength(Math.sqrt(dx*dx+dy*dy+dz*dz),radius);
        lastFrequency=Math.max(1,Math.min(15,pendingFrequency));
        setPhase(1); phaseEndsAt=worldObj.getTotalWorldTime()+(calibrated()?10:30);
        notifyPower(); markDirty();
        worldObj.playSoundEffect(xCoord+.5,yCoord+.5,zCoord+.5,Tags.MC_ASSET_VER+":block.sculk_sensor.clicking",1.0F,0.8F+lastFrequency*0.02F);
        if(worldObj instanceof WorldServer) ((WorldServer)worldObj).func_147487_a("reddust",xCoord+.5,yCoord+.8,zCoord+.5,4,.25,.15,.25,0.0);
        SculkVibrationDispatcher.post(worldObj,xCoord+.5,yCoord+.5,zCoord+.5,
                SculkVibrationDispatcher.Event.SCULK_SENSOR_TENDRILS_CLICKING,null,pendingPlayer);
    }

    private void notifyPower() {
        worldObj.notifyBlocksOfNeighborChange(xCoord,yCoord,zCoord,getBlockType());
        worldObj.notifyBlocksOfNeighborChange(xCoord,yCoord-1,zCoord,getBlockType());
        worldObj.markBlockForUpdate(xCoord,yCoord,zCoord);
    }

    private void activateShrieker() {
        if(pendingPlayer==null) return;
        EntityPlayer player=findPlayer(pendingPlayer);
        if(player!=null) tryShriekFromPlayer(player);
    }

    /** Direct-step and vibration paths converge here, matching modern SculkShriekerBlockEntity#tryShriek. */
    public boolean tryShriekFromPlayer(EntityPlayer player) {
        if(worldObj==null||worldObj.isRemote||!shrieker()||player==null||isShrieking())return false;
        int warning=0;
        if(canRespond()) {
            warning=SculkEventHandler.INSTANCE.tryWarnPlayer(player,xCoord,yCoord,zCoord);
            // Modern can_summon Shriekers do not begin another shriek while the shared
            // WardenSpawnTracker warning state is still inside its 200-tick cooldown.
            if(warning<0)return false;
        }
        shriekerWarningLevel=warning;
        setShrieking(true);
        phaseEndsAt=worldObj.getTotalWorldTime()+90;
        worldObj.playSoundEffect(xCoord+.5,yCoord+.5,zCoord+.5,Tags.MC_ASSET_VER+":block.sculk_shrieker.shriek",2.0F,1.0F);
        if(worldObj instanceof WorldServer) ((WorldServer)worldObj).func_147487_a("portal",xCoord+.5,yCoord+1.0,zCoord+.5,12,.4,.2,.4,.02);
        markDirty();
        return true;
    }

    private boolean canRespond() {
        // 1.7.10 has no doWardenSpawning gamerule. Preserve the modern CAN_SUMMON + non-Peaceful
        // gate, then stop exactly at the unavailable Warden entity endpoint.
        return canSummon() && worldObj!=null && worldObj.difficultySetting!=EnumDifficulty.PEACEFUL;
    }

    private void respondAfterShriek() {
        if(!canRespond()||shriekerWarningLevel<=0)return;
        // Modern attempts a Warden spawn at warning level 4, then plays a warning reply if the
        // spawn attempt fails. EFR has no Warden entity, so the attempt is intentionally absent
        // and therefore always reaches the reply-sound path without retrying.
        String event=warningSound(shriekerWarningLevel);
        if(event==null)return;
        int sx=xCoord+worldObj.rand.nextInt(21)-10;
        int sy=yCoord+worldObj.rand.nextInt(21)-10;
        int sz=zCoord+worldObj.rand.nextInt(21)-10;
        worldObj.playSoundEffect(sx+.5D,sy+.5D,sz+.5D,Tags.MC_ASSET_VER+":"+event,5.0F,1.0F);
        // Deliberate Pass-38 boundary: no Warden entity exists in EFR and no spawn/retry is attempted.
        // Modern Darkness (radius 40) and actual Warden creation remain the explicit Pass-38
        // entity/effect boundary; neither is replaced with legacy Blindness or a fake spawn.
    }

    private static String warningSound(int level) {
        if(level==1)return "entity.warden.nearby_close";
        if(level==2)return "entity.warden.nearby_closer";
        if(level==3)return "entity.warden.nearby_closest";
        if(level==4)return "entity.warden.listening_angry";
        return null;
    }

    private EntityPlayer findPlayer(UUID id) {
        @SuppressWarnings("unchecked") List<EntityPlayer> players=worldObj.playerEntities;
        for(EntityPlayer p:players) if(id.equals(p.getUniqueID())) return p;
        return null;
    }

    @Override public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        power=nbt.getInteger("SculkPower"); lastFrequency=nbt.getInteger("LastVibrationFrequency");
        phaseEndsAt=nbt.getLong("PhaseEndsAt"); pendingArrivalAt=nbt.hasKey("PendingArrivalAt")?nbt.getLong("PendingArrivalAt"):-1L;
        pendingFrequency=nbt.getInteger("PendingFrequency"); pendingX=nbt.getDouble("PendingX"); pendingY=nbt.getDouble("PendingY"); pendingZ=nbt.getDouble("PendingZ");
        if(nbt.hasKey("PendingPlayerMost")&&nbt.hasKey("PendingPlayerLeast")) pendingPlayer=new UUID(nbt.getLong("PendingPlayerMost"),nbt.getLong("PendingPlayerLeast"));
        shriekerWarningLevel=nbt.getInteger("WarningLevel");
    }
    @Override public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setInteger("SculkPower",power); nbt.setInteger("LastVibrationFrequency",lastFrequency); nbt.setLong("PhaseEndsAt",phaseEndsAt);
        nbt.setLong("PendingArrivalAt",pendingArrivalAt); nbt.setInteger("PendingFrequency",pendingFrequency); nbt.setDouble("PendingX",pendingX); nbt.setDouble("PendingY",pendingY); nbt.setDouble("PendingZ",pendingZ);
        if(pendingPlayer!=null){nbt.setLong("PendingPlayerMost",pendingPlayer.getMostSignificantBits());nbt.setLong("PendingPlayerLeast",pendingPlayer.getLeastSignificantBits());}
        nbt.setInteger("WarningLevel",shriekerWarningLevel);
    }
}

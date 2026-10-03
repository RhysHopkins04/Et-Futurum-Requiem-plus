package ganymedes01.etfuturum.core.handlers;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import ganymedes01.etfuturum.tileentities.TileEntitySculkCatalyst;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerUseItemEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.WorldEvent;

import java.lang.reflect.Method;
import java.util.*;

/** Pass-38 bounded event bridge for Sculk gameplay on 1.7.10. */
public class SculkEventHandler {
    public static final SculkEventHandler INSTANCE = new SculkEventHandler();
    private static final String WARN_TAG="EFRSculkWarning";
    private final Method getXpMethod = ReflectionHelper.findMethod(EntityLivingBase.class, null, new String[]{"getExperiencePoints", "func_70693_a"}, EntityPlayer.class);

    private static final class ConsumedXp { final World world; final double x,y,z; int ticks=3; ConsumedXp(World w,double x,double y,double z){this.world=w;this.x=x;this.y=y;this.z=z;} }
    private static final class PendingBlockInteraction {
        final World world; final int x,y,z; final EntityPlayer player;
        PendingBlockInteraction(World world,int x,int y,int z,EntityPlayer player){this.world=world;this.x=x;this.y=y;this.z=z;this.player=player;}
    }
    private final List<ConsumedXp> consumedXp = new ArrayList<ConsumedXp>();
    private final Map<EntityPlayer,double[]> lastPlayerPos = new WeakHashMap<EntityPlayer,double[]>();
    private final Map<EntityPlayer,PendingBlockInteraction> pendingBlockInteractions = new WeakHashMap<EntityPlayer,PendingBlockInteraction>();
    private final Set<EntityArrow> trackedArrows = Collections.newSetFromMap(new WeakHashMap<EntityArrow,Boolean>());
    private final Map<World,Map<Long,Set<TileEntitySculkCatalyst>>> catalysts = Collections.synchronizedMap(new WeakHashMap<World,Map<Long,Set<TileEntitySculkCatalyst>>>());

    private static long key(int cx,int cz){return (((long)cx)<<32)^(cz&0xffffffffL);}
    public void registerCatalyst(TileEntitySculkCatalyst te){
        if(te==null||te.getWorldObj()==null||te.getWorldObj().isRemote)return;
        synchronized(catalysts){Map<Long,Set<TileEntitySculkCatalyst>> m=catalysts.get(te.getWorldObj()); if(m==null){m=new HashMap<Long,Set<TileEntitySculkCatalyst>>();catalysts.put(te.getWorldObj(),m);} long k=key(te.xCoord>>4,te.zCoord>>4);Set<TileEntitySculkCatalyst>s=m.get(k);if(s==null){s=Collections.newSetFromMap(new WeakHashMap<TileEntitySculkCatalyst,Boolean>());m.put(k,s);}s.add(te);}
    }
    public void unregisterCatalyst(TileEntitySculkCatalyst te){if(te==null||te.getWorldObj()==null)return;synchronized(catalysts){Map<Long,Set<TileEntitySculkCatalyst>>m=catalysts.get(te.getWorldObj());if(m!=null){Set<TileEntitySculkCatalyst>s=m.get(key(te.xCoord>>4,te.zCoord>>4));if(s!=null)s.remove(te);}}}

    private TileEntitySculkCatalyst findNearbyCatalyst(Entity entity) {
        TileEntitySculkCatalyst best=null; double bestD=64.0D;
        int cx=((int)Math.floor(entity.posX))>>4,cz=((int)Math.floor(entity.posZ))>>4;
        synchronized(catalysts){Map<Long,Set<TileEntitySculkCatalyst>>m=catalysts.get(entity.worldObj);if(m==null)return null;for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){Set<TileEntitySculkCatalyst>s=m.get(key(cx+dx,cz+dz));if(s==null)continue;for(TileEntitySculkCatalyst te:s){if(te==null||te.isInvalid())continue;double d=te.getDistanceFrom(entity.posX,entity.posY,entity.posZ);if(d<=64.0D&&d<bestD){bestD=d;best=te;}}}}
        return best;
    }

    @SubscribeEvent public void onEntityJoin(EntityJoinWorldEvent event) {
        if (event.world.isRemote) return;
        if (event.entity instanceof EntityArrow) { trackedArrows.add((EntityArrow)event.entity); return; }
        if (!(event.entity instanceof EntityXPOrb)) return;
        synchronized(consumedXp){for(ConsumedXp c:consumedXp) if(c.world==event.world&&event.entity.getDistanceSq(c.x,c.y,c.z)<1.0D){event.setCanceled(true);break;}}
    }

    @SubscribeEvent public void onTick(TickEvent.ServerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        synchronized(consumedXp){Iterator<ConsumedXp>it=consumedXp.iterator();while(it.hasNext())if(--it.next().ticks<=0)it.remove();}
        Iterator<EntityArrow> arrows=trackedArrows.iterator();
        while(arrows.hasNext()){
            EntityArrow a=arrows.next();
            if(a==null||a.isDead){arrows.remove();continue;}
            if(a.inGround){
                Entity src=a.shootingEntity;UUID id=src instanceof EntityPlayer?src.getUniqueID():null;
                if(!arrowImpactIsDampened(a))
                    SculkVibrationDispatcher.post(a.worldObj,a.posX,a.posY,a.posZ,SculkVibrationDispatcher.Event.PROJECTILE_LAND,src,id);
                arrows.remove();
            }
        }
        // Forge fires RIGHT_CLICK_BLOCK before it knows whether an ItemBlock use becomes a
        // placement. Defer only those ambiguous clicks until the end of the server tick:
        // a successful PlaceEvent consumes the candidate, while a genuine block activation
        // with an ItemBlock still produces BLOCK_ACTIVATE if no placement followed.
        if(!pendingBlockInteractions.isEmpty()){
            List<PendingBlockInteraction> pending=new ArrayList<PendingBlockInteraction>(pendingBlockInteractions.values());
            pendingBlockInteractions.clear();
            for(PendingBlockInteraction click:pending)
                if(click!=null&&click.world!=null&&!click.world.isRemote&&click.player!=null&&!click.player.isDead)
                    SculkVibrationDispatcher.post(click.world,click.x+.5,click.y+.5,click.z+.5,SculkVibrationDispatcher.Event.BLOCK_ACTIVATE,click.player);
        }
    }

    private static boolean dampeningAt(World world,int x,int y,int z){
        if(world==null||y<0||y>=256||world.getChunkProvider()==null||!world.getChunkProvider().chunkExists(x>>4,z>>4))return false;
        return SculkVibrationDispatcher.isVibrationDampeningBlock(world.getBlock(x,y,z));
    }

    private static boolean playerStandingOnDampening(EntityPlayer p){
        int x=(int)Math.floor(p.posX),z=(int)Math.floor(p.posZ),feet=(int)Math.floor(p.posY);
        // Full wool sits one block below the player's feet; carpet occupies the feet block itself.
        return dampeningAt(p.worldObj,x,feet,z)||dampeningAt(p.worldObj,x,(int)Math.floor(p.posY-0.2D),z);
    }

    private static boolean arrowImpactIsDampened(EntityArrow arrow){
        try{
            int x=ReflectionHelper.getPrivateValue(EntityArrow.class,arrow,new String[]{"xTile","field_145791_d"});
            int y=ReflectionHelper.getPrivateValue(EntityArrow.class,arrow,new String[]{"yTile","field_145792_e"});
            int z=ReflectionHelper.getPrivateValue(EntityArrow.class,arrow,new String[]{"zTile","field_145789_f"});
            return dampeningAt(arrow.worldObj,x,y,z);
        }catch(RuntimeException ignored){
            return dampeningAt(arrow.worldObj,(int)Math.floor(arrow.posX),(int)Math.floor(arrow.posY),(int)Math.floor(arrow.posZ));
        }
    }

    @SubscribeEvent public void onPlayerTick(TickEvent.PlayerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||event.player.worldObj.isRemote)return;
        EntityPlayer p=event.player;double[] old=lastPlayerPos.get(p);if(old==null){lastPlayerPos.put(p,new double[]{p.posX,p.posY,p.posZ,0});return;}
        double dx=p.posX-old[0],dz=p.posZ-old[2],d2=dx*dx+dz*dz;old[0]=p.posX;old[1]=p.posY;old[2]=p.posZ;old[3]++;
        if(!p.isSneaking()&&p.onGround&&d2>0.0025D&&((long)old[3]%5L)==0L&&!playerStandingOnDampening(p))
            SculkVibrationDispatcher.post(p.worldObj,p.posX,p.posY,p.posZ,SculkVibrationDispatcher.Event.STEP,p);
    }

    @SubscribeEvent(priority=EventPriority.LOWEST) public void onPlace(BlockEvent.PlaceEvent e){
        if(e.player!=null) pendingBlockInteractions.remove(e.player);
        if(e.world!=null&&!e.world.isRemote&&!e.isCanceled()&&!SculkVibrationDispatcher.isVibrationDampeningBlock(e.world.getBlock(e.x,e.y,e.z)))
            SculkVibrationDispatcher.post(e.world,e.x+.5,e.y+.5,e.z+.5,SculkVibrationDispatcher.Event.BLOCK_PLACE,e.player);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public void onBreak(BlockEvent.BreakEvent e){
        if(e.world!=null&&!e.world.isRemote&&!e.isCanceled()&&!SculkVibrationDispatcher.isVibrationDampeningBlock(e.block))
            SculkVibrationDispatcher.post(e.world,e.x+.5,e.y+.5,e.z+.5,SculkVibrationDispatcher.Event.BLOCK_DESTROY,e.getPlayer());
    }
    @SubscribeEvent public void onInteract(PlayerInteractEvent e){
        if(e.entityPlayer==null||e.entityPlayer.worldObj.isRemote||e.action!=PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK||e.isCanceled())return;
        ItemStack held=e.entityPlayer.getHeldItem();
        if(held!=null&&held.getItem() instanceof ItemBlock){
            pendingBlockInteractions.put(e.entityPlayer,new PendingBlockInteraction(e.entityPlayer.worldObj,e.x,e.y,e.z,e.entityPlayer));
            return;
        }
        SculkVibrationDispatcher.post(e.entityPlayer.worldObj,e.x+.5,e.y+.5,e.z+.5,SculkVibrationDispatcher.Event.BLOCK_ACTIVATE,e.entityPlayer);
    }
    @SubscribeEvent public void onEntityInteract(EntityInteractEvent e){if(e.entityPlayer!=null&&!e.entityPlayer.worldObj.isRemote)SculkVibrationDispatcher.post(e.entityPlayer.worldObj,e.target.posX,e.target.posY,e.target.posZ,SculkVibrationDispatcher.Event.ENTITY_INTERACT,e.entityPlayer);}
    @SubscribeEvent public void onHurt(LivingHurtEvent e){if(e.entityLiving!=null&&!e.entityLiving.worldObj.isRemote)SculkVibrationDispatcher.post(e.entityLiving.worldObj,e.entityLiving.posX,e.entityLiving.posY,e.entityLiving.posZ,SculkVibrationDispatcher.Event.ENTITY_DAMAGE,e.source.getEntity(), e.source.getEntity() instanceof EntityPlayer?e.source.getEntity().getUniqueID():null);}
    @SubscribeEvent public void onArrowLoose(ArrowLooseEvent e){if(e.entityPlayer!=null&&!e.entityPlayer.worldObj.isRemote)SculkVibrationDispatcher.post(e.entityPlayer.worldObj,e.entityPlayer.posX,e.entityPlayer.posY,e.entityPlayer.posZ,SculkVibrationDispatcher.Event.PROJECTILE_SHOOT,e.entityPlayer);}
    @SubscribeEvent public void onUseFinish(PlayerUseItemEvent.Finish e){if(e.entityPlayer!=null&&!e.entityPlayer.worldObj.isRemote)SculkVibrationDispatcher.post(e.entityPlayer.worldObj,e.entityPlayer.posX,e.entityPlayer.posY,e.entityPlayer.posZ,SculkVibrationDispatcher.Event.EAT,e.entityPlayer);}

    @SubscribeEvent(priority=EventPriority.LOWEST) public void onDeath(LivingDeathEvent event) {
        if(event.entityLiving==null||event.entityLiving.worldObj.isRemote)return;
        Entity killer=event.source==null?null:event.source.getEntity(); UUID responsible=killer instanceof EntityPlayer?killer.getUniqueID():null;
        SculkVibrationDispatcher.post(event.entityLiving.worldObj,event.entityLiving.posX,event.entityLiving.posY,event.entityLiving.posZ,SculkVibrationDispatcher.Event.ENTITY_DIE,killer,responsible);
        if (!(event.entityLiving instanceof EntityLiving) || getXpMethod==null) return;
        TileEntitySculkCatalyst catalyst=findNearbyCatalyst(event.entityLiving);if(catalyst==null)return;
        FakePlayer player=FakePlayerFactory.getMinecraft((WorldServer)event.entityLiving.worldObj);player.setWorld(event.entityLiving.worldObj);
        int experience;try{experience=(Integer)getXpMethod.invoke(event.entityLiving,player);}catch(Exception ex){ex.printStackTrace();return;}
        if(experience<=0)return;
        catalyst.bloomWithExperience(experience,(int)Math.floor(event.entityLiving.posX),(int)Math.floor(event.entityLiving.posY),(int)Math.floor(event.entityLiving.posZ));
        synchronized(consumedXp){consumedXp.add(new ConsumedXp(event.entityLiving.worldObj,event.entityLiving.posX,event.entityLiving.posY,event.entityLiving.posZ));}
    }

    private static NBTTagCompound warningData(EntityPlayer player) {
        NBTTagCompound entityData=player.getEntityData();
        NBTTagCompound warning=entityData.hasKey(WARN_TAG)?entityData.getCompoundTag(WARN_TAG):new NBTTagCompound();
        entityData.setTag(WARN_TAG,warning);
        return warning;
    }

    private static int applyWarningDecay(NBTTagCompound warning,long now) {
        int level=Math.max(0,Math.min(4,warning.getInteger("Level")));
        long lastDecay=warning.getLong("LastDecay");
        if(lastDecay<=0L) lastDecay=warning.hasKey("LastWarning")?warning.getLong("LastWarning"):now;
        if(now>lastDecay && now-lastDecay>=12000L) {
            long intervals=(now-lastDecay)/12000L;
            level=Math.max(0,level-(int)Math.min(4L,intervals));
            lastDecay+=intervals*12000L;
        }
        warning.setInteger("Level",level);
        warning.setLong("LastDecay",lastDecay);
        return level;
    }

    /**
     * Mirrors the block-local part of modern WardenSpawnTracker.tryWarn. Returns -1 when any
     * nearby player is still inside the 200-tick warning cooldown; otherwise increments the
     * highest nearby warning level and synchronizes that state to every nearby player.
     */
    public int tryWarnPlayer(EntityPlayer source,int x,int y,int z){
        if(source==null||source.worldObj==null||source.worldObj.isRemote)return -1;
        long now=source.worldObj.getTotalWorldTime();
        @SuppressWarnings("unchecked") List<EntityPlayer> candidates=source.worldObj.getEntitiesWithinAABB(EntityPlayer.class,
                AxisAlignedBB.getBoundingBox(x-16,y-16,z-16,x+17,y+17,z+17));
        List<EntityPlayer> players=new ArrayList<EntityPlayer>();
        double cx=x+0.5D,cy=y+0.5D,cz=z+0.5D;
        for(EntityPlayer player:candidates) {
            if(player==null||player.isDead||!player.isEntityAlive())continue;
            if(player.getDistanceSq(cx,cy,cz)<=256.0D)players.add(player);
        }
        if(!players.contains(source))players.add(source);

        int maxLevel=0;
        for(EntityPlayer player:players) {
            NBTTagCompound warning=warningData(player);
            int level=applyWarningDecay(warning,now);
            maxLevel=Math.max(maxLevel,level);
            if(warning.hasKey("LastWarning") && now-warning.getLong("LastWarning")<200L)return -1;
        }

        int next=Math.min(4,maxLevel+1);
        for(EntityPlayer player:players) {
            NBTTagCompound warning=warningData(player);
            warning.setInteger("Level",next);
            warning.setLong("LastWarning",now);
            warning.setLong("LastDecay",now);
        }
        return next;
    }

    // Retained for source compatibility with the original Pass-38 call site/validators.
    public int warnPlayer(EntityPlayer source,int x,int y,int z){
        int result=tryWarnPlayer(source,x,y,z);
        return result<0?0:result;
    }

    @SubscribeEvent public void onWorldUnload(WorldEvent.Unload e){SculkVibrationDispatcher.clearWorld(e.world);synchronized(catalysts){catalysts.remove(e.world);} synchronized(consumedXp){Iterator<ConsumedXp>it=consumedXp.iterator();while(it.hasNext())if(it.next().world==e.world)it.remove();} Iterator<EntityArrow> arrows=trackedArrows.iterator();while(arrows.hasNext()){EntityArrow a=arrows.next();if(a==null||a.worldObj==e.world)arrows.remove();} Iterator<Map.Entry<EntityPlayer,PendingBlockInteraction>> clicks=pendingBlockInteractions.entrySet().iterator();while(clicks.hasNext()){Map.Entry<EntityPlayer,PendingBlockInteraction> entry=clicks.next();PendingBlockInteraction click=entry.getValue();if(click==null||click.world==e.world)clicks.remove();}}
}

package efrtest;

import com.mojang.authlib.GameProfile;
import ganymedes01.etfuturum.*;
import ganymedes01.etfuturum.api.GrindstoneRegistry;
import ganymedes01.etfuturum.api.mappings.ItemMetaPair;
import ganymedes01.etfuturum.api.tags.ItemTags;
import ganymedes01.etfuturum.inventory.*;
import ganymedes01.etfuturum.inventory.ContainerEnchantment;
import ganymedes01.etfuturum.recipes.*;
import ganymedes01.etfuturum.storage.EtFuturumPlayer;
import ganymedes01.etfuturum.tileentities.TileEntityLectern;
import net.minecraft.enchantment.*;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import java.io.File;
import java.util.*;
import static efrtest.EarthImportContracts.check;

public final class ScholarlyWorkstationsProbe {
    private static final int GX=-176,LX=-160,EX=-144,Y=128,Z=144;
    private static void clear(FakePlayer p) { for(int i=0;i<p.inventory.getSizeInventory();i++)p.inventory.setInventorySlotContents(i,null);p.inventory.setItemStack(null);p.capabilities.isCreativeMode=false;p.capabilities.allowEdit=true; }
    private static ItemStack book(int pages,boolean signed) {
        ItemStack s=new ItemStack(signed?Items.written_book:Items.writable_book);s.setStackDisplayName("Preserved book");
        s.getTagCompound().setString("author","Runtime probe");s.getTagCompound().setString("title","Runtime book");s.getTagCompound().setString("Custom","kept");
        NBTTagList list=new NBTTagList();for(int i=0;i<pages;i++)list.appendTag(new NBTTagString("Page "+(i+1)));
        s.getTagCompound().setTag("pages",list);return s;
    }
    private static int inventoryCount(FakePlayer p,net.minecraft.item.Item item) { int count=0;for(ItemStack s:p.inventory.mainInventory)if(s!=null && s.getItem()==item)count+=s.stackSize;return count; }
    private static int bookDrops(WorldServer world) {
        int total=0;for(Object entity:world.loadedEntityList)if(entity instanceof EntityItem) {
            EntityItem drop=(EntityItem)entity;
            if(!drop.isDead && Math.abs(drop.posX-LX)<2 && Math.abs(drop.posZ-Z)<2 && drop.getEntityItem().getItem()==Items.written_book)total+=drop.getEntityItem().stackSize;
        }return total;
    }
    private static int xp(WorldServer world) { int n=0;for(Object entity:world.loadedEntityList)if(entity instanceof EntityXPOrb) { EntityXPOrb orb=(EntityXPOrb)entity;if(!orb.isDead && Math.abs(orb.posX-GX)<2 && Math.abs(orb.posZ-Z)<2)n+=orb.xpValue; }return n; }
    private static ContainerGrindstone grind(FakePlayer p,WorldServer world) { p.setPosition(GX+.5,Y,Z+.5);return new ContainerGrindstone(p.inventory,world,GX,Y,Z); }
    private static ItemStack enchanted(ItemStack s) { s.addEnchantment(Enchantment.sharpness,3);s.setStackDisplayName("Retained name");s.getTagCompound().setString("Custom","kept");s.setRepairCost(31);return s; }
    private static void bookActions(WorldServer world,FakePlayer player,TileEntityLectern tile) {
        java.util.List<ganymedes01.etfuturum.core.utils.LecternBookText.Span> spans=ganymedes01.etfuturum.core.utils.LecternBookText.parse("{\"text\":\"Next\",\"bold\":true,\"click_event\":{\"action\":\"change_page\",\"page\":2},\"extra\":[{\"text\":\" page\"}]}",true);
        check(spans.size()==2 && spans.get(0).action.equals("change_page") && spans.get(0).value.equals("2") && spans.get(1).action.equals("change_page") && spans.get(1).format.contains("\u00a7l"),"signed component/style/link inheritance");
        check(ganymedes01.etfuturum.core.utils.LecternBookText.parse("{\"text\":\"literal\"}",false).get(0).text.equals("{\"text\":\"literal\"}"),"writable book interpreted as JSON");
        check(ganymedes01.etfuturum.core.utils.LecternBookText.parse("{broken",true).get(0).text.equals("{broken"),"invalid JSON unreadable");
        tile.removeBook();ItemStack signed=book(1,true);NBTTagList pages=new NBTTagList();pages.appendTag(new NBTTagString("{\"text\":\"Command\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/say lectern_probe\"}}"));signed.getTagCompound().setTag("pages",pages);tile.insert(signed);
        boolean old=ganymedes01.etfuturum.configuration.configs.ConfigFunctions.allowLecternBookClickActions;
        net.minecraft.network.NetHandlerPlayServer connection=player.playerNetServerHandler;Container previous=player.openContainer;
        final class CapturedConnection extends net.minecraft.network.NetHandlerPlayServer {
            int count;String text;
            CapturedConnection() { super(net.minecraft.server.MinecraftServer.getServer(),new net.minecraft.network.NetworkManager(false),player); }
            @Override public void processChatMessage(net.minecraft.network.play.client.C01PacketChatMessage packet) { count++;text=packet.func_149439_c(); }
            @Override public void sendPacket(net.minecraft.network.Packet packet) { }
        }
        CapturedConnection captured=new CapturedConnection();ContainerLectern container=new ContainerLectern(player,tile);container.windowId=42;player.openContainer=container;
        ganymedes01.etfuturum.network.LecternBookActionMessage message=new ganymedes01.etfuturum.network.LecternBookActionMessage(42,0,0,"/say lectern_probe");
        try {
            ganymedes01.etfuturum.configuration.configs.ConfigFunctions.allowLecternBookClickActions=false;
            check(!ganymedes01.etfuturum.network.LecternBookActionHandler.dispatch(player,message) && captured.count==0,"disabled lectern command dispatched");
            ganymedes01.etfuturum.configuration.configs.ConfigFunctions.allowLecternBookClickActions=true;container.detectAndSendChanges();check(container.allowClickActions,"server config not exposed to reader");
            io.netty.buffer.ByteBuf wire=io.netty.buffer.Unpooled.buffer();message.toBytes(wire);ganymedes01.etfuturum.network.LecternBookActionMessage decoded=new ganymedes01.etfuturum.network.LecternBookActionMessage();decoded.fromBytes(wire);wire.release();
            check(ganymedes01.etfuturum.network.LecternBookActionHandler.dispatch(player,decoded) && captured.count==1 && captured.text.equals("/say lectern_probe"),"signed-book command did not use normal chat dispatch");
            player.openContainer=container;
            check(!container.runBookCommand(player,1,0,message.expected) && !container.runBookCommand(player,0,999,message.expected) && !container.runBookCommand(player,0,0,"/op forged"),"stale/forged command request accepted");
            message.window=43;check(!ganymedes01.etfuturum.network.LecternBookActionHandler.dispatch(player,message),"wrong window command");message.window=42;
            tile.removeBook();ItemStack changed=signed.copy();NBTTagList changedPages=new NBTTagList();changedPages.appendTag(new NBTTagString("{\"text\":\"Other\",\"click_event\":{\"action\":\"run_command\",\"command\":\"/say other\"}}"));changed.getTagCompound().setTag("pages",changedPages);tile.insert(changed);
            check(!ganymedes01.etfuturum.network.LecternBookActionHandler.dispatch(player,message),"changed book executed an unseen command");
            tile.removeBook();ItemStack unsigned=book(1,false);unsigned.getTagCompound().setTag("pages",pages.copy());tile.insert(unsigned);check(!container.runBookCommand(player,0,0,message.expected),"unsigned book command accepted");
            io.netty.buffer.ByteBuf bad=io.netty.buffer.Unpooled.buffer();bad.writeInt(42);bad.writeInt(0);bad.writeInt(0);bad.writeShort(401);boolean refused=false;
            try { new ganymedes01.etfuturum.network.LecternBookActionMessage().fromBytes(bad); }catch(IllegalArgumentException expected) { refused=true; }finally { bad.release(); }
            check(refused && captured.count==1,"oversized packet not refused or command repeated");
        } finally { tile.removeBook();ganymedes01.etfuturum.configuration.configs.ConfigFunctions.allowLecternBookClickActions=old;player.playerNetServerHandler=connection;player.openContainer=previous; }
        System.out.println("LECTERN_BOOK_ACTIONS PASSED: default-off server gate, signed text, page/span/window/content validation, bounded wire and captured vanilla-chat dispatch; no real command/browser executed");
    }
    private static void lectern(WorldServer world,FakePlayer p) throws Exception {
        p.setPosition(LX+.5,Y,Z+.5);
        File marker=new File("scholarly-workstations-fixture-created");
        if(marker.exists()) {
            TileEntityLectern saved=(TileEntityLectern)world.getTileEntity(LX,Y,Z);
            check(saved!=null && saved.getPage()==3 && saved.getPageCount()==15 && "kept".equals(saved.getBook().getTagCompound().getString("Custom")),"lectern lost book/page after process restart");
            System.out.println("LECTERN_RESTART PASSED");
        }
        for(int quadrant=0;quadrant<4;quadrant++) {
            world.setBlock(LX,Y,Z,ModernMapParityBlocks.LECTERN.get(),0,3);p.rotationYaw=quadrant*90F;
            ModernMapParityBlocks.LECTERN.get().onBlockPlacedBy(world,LX,Y,Z,p,new ItemStack(ModernMapParityBlocks.LECTERN.get()));
            check((world.getBlockMetadata(LX,Y,Z)&3)==quadrant,"lectern facing must oppose player look in all four quadrants");
        }
        world.setBlock(LX,Y,Z,ModernMapParityBlocks.LECTERN.get(),2,3);
        TileEntityLectern tile=(TileEntityLectern)world.getTileEntity(LX,Y,Z);
        if(tile.hasBook())tile.removeBook();
        check(!tile.insert(new ItemStack(Items.book)) && tile.comparator()==0,"lectern accepted plain book");
        for(boolean signed:new boolean[]{false,true})for(int pages:new int[]{0,1,2,15,50,100}) {
            ItemStack input=book(pages,signed),before=input.copy();check(tile.insert(input),"lectern rejected readable book");
            check(ItemStack.areItemStacksEqual(input,before),"lectern owns caller's NBT");
            check(tile.comparator()==(pages>1?1:15),"lectern first page comparator");
            for(int i=0;i<Math.max(1,pages);i++) {
                tile.setPage(i);check(tile.getPage()==i,"lectern page mismatch");
                int expected=pages>1?1+(int)Math.floor(14F*i/(pages-1)):15;
                check(tile.comparator()==expected,"lectern comparator progression");
            }
            tile.setPage(Integer.MAX_VALUE);check(tile.getPage()==Math.max(0,pages-1),"lectern upper page clamp");
            tile.setPage(-10);check(tile.getPage()==0,"lectern lower page clamp");
            ItemStack copy=tile.getBook();copy.getTagCompound().setString("Custom","mutated");check("kept".equals(tile.getBook().getTagCompound().getString("Custom")),"lectern returns mutable live book");
            NBTTagCompound tag=new NBTTagCompound();tile.writeToNBT(tag);TileEntityLectern roundtrip=new TileEntityLectern();roundtrip.readFromNBT(tag);
            check(ItemStack.areItemStacksEqual(roundtrip.getBook(),input) && roundtrip.getPage()==tile.getPage(),"lectern book NBT roundtrip");
            check(ItemStack.areItemStacksEqual(tile.removeBook(),input) && tile.comparator()==0,"lectern remove changed book");
        }
        tile.insert(book(15,true));tile.setPage(3);
        check((world.getBlockMetadata(LX,Y,Z)&3)==2 && (world.getBlockMetadata(LX,Y,Z)&12)==12,"lectern flags destroyed facing");
        for(int side=0;side<6;side++) {
            check(ModernMapParityBlocks.LECTERN.get().isProvidingWeakPower(world,LX,Y,Z,side)==15,"lectern weak pulse");
            check(ModernMapParityBlocks.LECTERN.get().isProvidingStrongPower(world,LX,Y,Z,side)==(side==1?15:0),"lectern strong pulse side");
        }
        long tick=world.getTotalWorldTime();
        world.getWorldInfo().incrementTotalWorldTime(tick+1);world.tickUpdates(false);
        check((world.getBlockMetadata(LX,Y,Z)&4)!=0,"lectern pulse ended before two ticks");
        world.getWorldInfo().incrementTotalWorldTime(tick+2);world.tickUpdates(false);
        check((world.getBlockMetadata(LX,Y,Z)&4)==0 && (world.getBlockMetadata(LX,Y,Z)&8)!=0,"lectern pulse did not reset");
        check(tile.comparator()==4 && ModernMapParityBlocks.LECTERN.get().getComparatorInputOverride(world,LX,Y,Z,1)==4,"page comparator fell with temporary pulse");
        ContainerLectern first=new ContainerLectern(p,tile);
        FakePlayer other=FakePlayerFactory.get(world,new GameProfile(UUID.fromString("4bbbf5f0-d3d8-4b80-b1c3-42aad66bf491"),"lectern_reader"));other.setPosition(LX+.5,Y,Z+.5);clear(other);
        ContainerLectern second=new ContainerLectern(other,tile);
        check(!first.enchantItem(other,1),"lectern window owner bypass");
        check(first.enchantItem(p,1),"lectern next page");second.detectAndSendChanges();check(second.page==4,"lectern multiplayer page divergence");
        check(first.enchantItem(p,3) && tile.getPage()==0,"lectern page jump");
        check(!first.enchantItem(p,127) && !first.enchantItem(p,-1),"lectern invalid button");
        check(first.slotClick(0,0,0,p)==null && first.transferStackInSlot(p,0)==null,"lectern hidden slot extraction");
        p.capabilities.allowEdit=false;check(!first.enchantItem(p,2),"adventure removed book");p.capabilities.allowEdit=true;
        ItemStack original=tile.getBook();check(first.enchantItem(p,2) && !tile.hasBook() && inventoryCount(p,Items.written_book)==1,"lectern take book");
        check(!first.enchantItem(p,2),"lectern repeated take");check(!second.canInteractWith(other),"other reader retained removed book access");clear(p);
        tile.insert(original);p.setPosition(LX+12,Y,Z);check(!first.enchantItem(p,1),"lectern distance bypass");p.setPosition(LX+.5,Y,Z+.5);
        check(ModernMapParityBlocks.LECTERN.get().getBlockHardness(world,LX,Y,Z)==2.5F,"lectern hardness");
        check(ModernMapParityBlocks.LECTERN.get().getFlammability(world,LX,Y,Z,net.minecraftforge.common.util.ForgeDirection.UP)==20,"lectern flammability");
        List<AxisAlignedBB> boxes=new ArrayList<>();ModernMapParityBlocks.LECTERN.get().addCollisionBoxesToList(world,LX,Y,Z,null,boxes,p);
        check(boxes.size()==2 && boxes.get(0).maxY==Y+.125D && boxes.get(1).maxY==Y+.875D,"lectern collision shape");
        int dropsBefore=bookDrops(world);world.setBlockToAir(LX,Y,Z);int drops=0;
        for(Object entity:world.loadedEntityList)if(entity instanceof EntityItem) { EntityItem drop=(EntityItem)entity;
            if(!drop.isDead && Math.abs(drop.posX-LX)<2 && Math.abs(drop.posZ-Z)<2 && drop.getEntityItem().getItem()==Items.written_book)drops+=drop.getEntityItem().stackSize; }
        check(drops-dropsBefore==1 && !first.canInteractWith(p),"lectern break lost/duplicated book");
        world.setBlock(LX,Y,Z,ModernMapParityBlocks.LECTERN.get(),2,3);tile=(TileEntityLectern)world.getTileEntity(LX,Y,Z);bookActions(world,p,tile);tile.insert(book(15,true));tile.setPage(3);
        if(!marker.exists())check(marker.createNewFile(),"lectern fixture marker");
        System.out.println("LECTERN PASSED: writable/signed books, 0/1/2/15/50/100 pages, comparator, power, bounds, multiplayer, invalid/denied requests, take/drop, NBT and durable fixture");
    }
    private static void grindstone(WorldServer world,FakePlayer p) {
        world.setBlock(GX,Y,Z,ModernMapParityBlocks.GRINDSTONE.get());
        check(ModernMapParityBlocks.GRINDSTONE.get().getBlockHardness(world,GX,Y,Z)==2F,"grindstone hardness");
        check(!net.minecraftforge.common.ForgeHooks.canHarvestBlock(ModernMapParityBlocks.GRINDSTONE.get(),p,0),"grindstone drops without a tool");
        p.inventory.setInventorySlotContents(p.inventory.currentItem,new ItemStack(Items.wooden_pickaxe));
        check(net.minecraftforge.common.ForgeHooks.canHarvestBlock(ModernMapParityBlocks.GRINDSTONE.get(),p,0),"grindstone rejects wooden pickaxe");clear(p);
        for(int state=0;state<12;state++) {
            world.setBlockMetadataWithNotify(GX,Y,Z,state,2);List<AxisAlignedBB> pieces=new ArrayList<>();
            ModernMapParityBlocks.GRINDSTONE.get().addCollisionBoxesToList(world,GX,Y,Z,null,pieces,p);
            check(pieces.size()==5,"grindstone multipart collision state "+state);
            double volume=0;for(AxisAlignedBB box:pieces) { volume+=(box.maxX-box.minX)*(box.maxY-box.minY)*(box.maxZ-box.minZ);check(box.minX>=GX && box.maxX<=GX+1 && box.minY>=Y && box.maxY<=Y+1 && box.minZ>=Z && box.maxZ<=Z+1,"grindstone shape outside cell"); }
            check(Math.abs(volume-1440D/4096D)<.00001,"grindstone rotated shape volume");
            check(ModernMapParityBlocks.GRINDSTONE.get().damageDropped(state)==0,"grindstone drops facing metadata");
        }
        world.setBlockMetadataWithNotify(GX,Y,Z,4,2);
        check(ModernMapParityBlocks.GRINDSTONE.get().collisionRayTrace(world,GX,Y,Z,net.minecraft.util.Vec3.createVectorHelper(GX+.5,Y+.5,Z-1),net.minecraft.util.Vec3.createVectorHelper(GX+.5,Y+.5,Z+2))!=null,"grindstone wheel ray miss");
        check(ModernMapParityBlocks.GRINDSTONE.get().collisionRayTrace(world,GX,Y,Z,net.minecraft.util.Vec3.createVectorHelper(GX+.02,Y+.02,Z-1),net.minecraft.util.Vec3.createVectorHelper(GX+.02,Y+.02,Z+2))==null,"grindstone empty corner hit");
        int routes=0;
        for(net.minecraft.item.Item item:new net.minecraft.item.Item[]{Items.wooden_sword,Items.stone_pickaxe,Items.iron_axe,Items.diamond_sword,Items.bow,Items.shears,Items.iron_helmet,Items.fishing_rod}) {
            int max=item.getMaxDamage();
            for(int damageA:new int[]{0,max/2,max-1})for(int damageB:new int[]{0,max/2,max-1}) {
                clear(p);ContainerGrindstone c=grind(p,world);ItemStack a=enchanted(new ItemStack(item,1,damageA)),b=new ItemStack(item,1,damageB),before=a.copy();
                c.input.setInventorySlotContents(0,a);c.input.setInventorySlotContents(1,b);
                ItemStack expected=c.getSlot(2).getStack();int expectedDamage=Math.max(0,damageA+damageB-max-max*5/100);
                check(expected!=null && expected.getItemDamage()==expectedDamage && !expected.isItemEnchanted(),"grindstone durability/enchantment result");
                check(expected.getDisplayName().equals("Retained name") && expected.getTagCompound().getString("Custom").equals("kept") && expected.getRepairCost()==0,"grindstone lost name/NBT or retained prior work");
                int xpBefore=xp(world);c.slotClick(2,routes%2,0,p);
                check(p.inventory.getItemStack()!=null && c.input.getStackInSlot(0)==null && c.input.getStackInSlot(1)==null,"grindstone extraction consumption");
                check(ItemStack.areItemStacksEqual(a,before),"grindstone mutated caller input");int gain=xp(world)-xpBefore;
                check(gain>=12 && gain<=23,"grindstone Sharpness III refund range");c.onContainerClosed(p);routes++;
            }
        }
        clear(p);ContainerGrindstone c=grind(p,world);ItemStack book=new ItemStack(Items.enchanted_book);Items.enchanted_book.addEnchantment(book,new EnchantmentData(Enchantment.efficiency,4));book.setStackDisplayName("Named book");
        c.input.setInventorySlotContents(1,book);check(c.getSlot(2).getStack().getItem()==Items.book && c.getSlot(2).getStack().getDisplayName().equals("Named book"),"enchanted book conversion");
        c.slotClick(2,0,1,p);check(inventoryCount(p,Items.book)==1,"book shift extraction");clear(p);
        c=grind(p,world);c.input.setInventorySlotContents(0,new ItemStack(Items.diamond_sword,1,100));check(c.getSlot(2).getStack()==null,"single unenchanted damaged tool admitted");
        c.input.setInventorySlotContents(1,new ItemStack(Items.iron_sword,1,100));check(c.getSlot(2).getStack()==null,"mixed tool types admitted");
        c.input.setInventorySlotContents(0,new ItemStack(Items.stick));c.input.setInventorySlotContents(1,new ItemStack(Items.stick));
        // Two equal stackable items can be combined unchanged by modern computeResult; slot admission still rejects plain sticks.
        check(!c.getSlot(0).isItemValid(new ItemStack(Items.stick)),"grindstone input accepts plain item");
        c.input.setInventorySlotContents(0,enchanted(new ItemStack(Items.diamond_sword)));c.input.setInventorySlotContents(1,null);
        for(int i=0;i<36;i++)p.inventory.setInventorySlotContents(i,new ItemStack(Blocks.cobblestone,64));
        int xpBefore=xp(world);c.slotClick(2,0,1,p);check(c.input.getStackInSlot(0)!=null && xp(world)==xpBefore,"grindstone consumed/awarded XP for full inventory");clear(p);
        ItemStack stacked=enchanted(new ItemStack(Items.diamond_sword));stacked.stackSize=2;c.input.setInventorySlotContents(0,stacked);check(c.getSlot(2).getStack()==null,"oversized input stack admitted");
        int id=255;while(id>0 && Enchantment.enchantmentsList[id]!=null)id--;check(id>0,"no free test enchantment ID");
        Enchantment curse=new Enchantment(id,1,EnumEnchantmentType.breakable) {};GrindstoneRegistry.registerCurse(curse);
        try {
            ItemStack cursed=enchanted(new ItemStack(Items.diamond_sword,1,100));cursed.addEnchantment(curse,1);
            ItemStack result=GrindstoneRecipes.result(cursed,null);
            check(EnchantmentHelper.getEnchantmentLevel(curse.effectId,result)==1 && EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId,result)==0 && result.getRepairCost()==1,"curse retention/prior work");
            check(GrindstoneRecipes.experienceValue(cursed,null)==23,"curse paid XP");
        } finally { Enchantment.enchantmentsList[id]=null; }
        c.input.setInventorySlotContents(0,enchanted(new ItemStack(Items.diamond_sword)));c.onContainerClosed(p);int returned=inventoryCount(p,Items.diamond_sword);c.onContainerClosed(p);check(returned==1 && inventoryCount(p,Items.diamond_sword)==1,"grindstone close duplicated/lost input");clear(p);
        c=grind(p,world);c.input.setInventorySlotContents(0,enchanted(new ItemStack(Items.diamond_sword)));world.setBlockToAir(GX,Y,Z);check(c.slotClick(2,0,0,p)==null,"removed grindstone usable");c.onContainerClosed(p);clear(p);
        System.out.println("GRINDSTONE PASSED: "+routes+" repairs/extractions, exact durability, curses, names/NBT/prior-work, books, XP bounds, full inventory, invalid stacks, close and block removal");
    }
    private static ContainerEnchantment enchant(FakePlayer p,WorldServer world) { p.setPosition(EX+.5,Y,Z+.5);return new ContainerEnchantment(p.inventory,world,EX,Y,Z); }
    private static void enchanting(WorldServer world,FakePlayer p) {
        for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)for(int dy=0;dy<2;dy++)world.setBlockToAir(EX+dx,Y+dy,Z+dz);
        world.setBlock(EX,Y,Z,ModBlocks.ENCHANTMENT_TABLE.get());
        for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)if(Math.abs(dx)==2 || Math.abs(dz)==2)world.setBlock(EX+dx,Y,Z+dz,Blocks.bookshelf);
        int cases=0;
        for(boolean book:new boolean[]{false,true})for(int option=0;option<3;option++) {
            clear(p);p.experienceLevel=30;EtFuturumPlayer.get(p).setEnchantmentSeed(0x76543210+option);
            ContainerEnchantment c=enchant(p,world);ItemStack input=new ItemStack(book?Items.book:Items.diamond_sword);input.setStackDisplayName("Enchant name");input.getTagCompound().setString("Custom","kept");
            c.tableInventory.setInventorySlotContents(0,input);c.tableInventory.setInventorySlotContents(1,new ItemStack(Items.dye,10,4));
            check(c.enchantLevels[2]==30,"fifteen shelves did not give level 30");
            int[] offers=c.enchantmentIds.clone();c.onCraftMatrixChanged(c.tableInventory);check(Arrays.equals(offers,c.enchantmentIds),"offers rerolled on refresh");
            int hint=c.enchantmentIds[option],seed=c.enchantmentSeed,cost=option+1;
            check(!c.enchantItem(p,-1) && !c.enchantItem(p,3) && !c.enchantItem(p,Integer.MAX_VALUE),"invalid enchanting button accepted");
            c.tableInventory.setInventorySlotContents(1,new ItemStack(Items.stick,10));check(!c.enchantItem(p,option),"non-fuel accepted");
            c.tableInventory.setInventorySlotContents(1,new ItemStack(Items.dye,cost-1,4));check(!c.enchantItem(p,option),"insufficient fuel accepted");
            c.tableInventory.setInventorySlotContents(1,new ItemStack(Items.dye,10,4));p.experienceLevel=0;check(!c.enchantItem(p,option),"insufficient XP accepted");p.experienceLevel=30;
            check(c.enchantItem(p,option),"valid enchant rejected");
            check(p.experienceLevel==30-cost && c.tableInventory.getStackInSlot(1).stackSize==10-cost,"enchant level/lapis cost");
            ItemStack result=c.tableInventory.getStackInSlot(0);
            Map enchantments=EnchantmentHelper.getEnchantments(result);check(enchantments.containsKey(hint&255) && ((Integer)enchantments.get(hint&255))==(hint>>8),"enchant clue disagrees with purchased enchantment");
            check(result.getDisplayName().equals("Enchant name") && result.getTagCompound().getString("Custom").equals("kept"),"enchanting lost NBT");
            check(c.enchantmentSeed==EtFuturumPlayer.get(p).getEnchantmentSeed() && c.enchantmentSeed!=seed,"saved/live enchant seed divergence");
            int savedSeed=c.enchantmentSeed;NBTTagCompound data=new NBTTagCompound();EtFuturumPlayer.get(p).saveNBTData(data);EtFuturumPlayer.get(p).setEnchantmentSeed(1);EtFuturumPlayer.get(p).loadNBTData(data);check(EtFuturumPlayer.get(p).getEnchantmentSeed()==savedSeed,"enchant seed persistence");
            c.onContainerClosed(p);check(inventoryCount(p,result.getItem())==1,"enchant close lost item");cases++;
        }
        clear(p);ContainerEnchantment c=enchant(p,world);p.inventory.setInventorySlotContents(9,new ItemStack(Items.book,64));c.transferStackInSlot(p,2);
        check(c.tableInventory.getStackInSlot(0).stackSize==1 && p.inventory.getStackInSlot(9).stackSize==63,"enchant shift lost/duplicated stack");c.onContainerClosed(p);check(inventoryCount(p,Items.book)==64,"enchant close stack conservation");clear(p);
        p.capabilities.isCreativeMode=true;p.experienceLevel=0;c=enchant(p,world);c.tableInventory.setInventorySlotContents(0,new ItemStack(Items.diamond_sword));check(c.enchantItem(p,2) && p.experienceLevel==0,"creative enchant costs/fuel bypass");c.onContainerClosed(p);clear(p);
        List<ItemMetaPair> fuels=new ArrayList<>(ItemTags.getInTag(Tags.MOD_ID+":enchantment_fuel"));
        try {
            for(ItemMetaPair fuel:fuels)ItemTags.removeTags(fuel.get(),fuel.getMeta(),Tags.MOD_ID+":enchantment_fuel");
            c=enchant(p,world);check(c.noFuel && c.inventorySlots.size()==37,"fuel-free layout");
            p.experienceLevel=30;c.tableInventory.setInventorySlotContents(0,new ItemStack(Items.diamond_sword));check(c.enchantItem(p,2) && p.experienceLevel==27,"configured fuel-free enchant failed");c.onContainerClosed(p);clear(p);
            c=enchant(p,world);p.inventory.setInventorySlotContents(9,new ItemStack(Items.book,64));c.transferStackInSlot(p,1);check(c.tableInventory.getStackInSlot(0).stackSize==1 && p.inventory.getStackInSlot(9).stackSize==63,"fuel-free shift routing");c.onContainerClosed(p);clear(p);
        } finally { for(ItemMetaPair fuel:fuels)ItemTags.addTags(fuel.get(),fuel.getMeta(),Tags.MOD_ID+":enchantment_fuel"); }
        c=enchant(p,world);
        net.minecraft.item.Item unavailable=new net.minecraft.item.Item() {
            @Override public boolean isItemTool(ItemStack stack) { return true; }
            @Override public int getItemEnchantability(ItemStack stack) { return 10; }
        };
        c.tableInventory.setInventorySlotContents(0,new ItemStack(unavailable));
        check(c.enchantLevels[0]==0 && c.enchantLevels[1]==0 && c.enchantLevels[2]==0 && !c.enchantItem(p,0),"empty enchantment pool offered a purchase");
        c=enchant(p,world);c.updateProgressBar(3,(short)0xFED0);c.updateProgressBar(7,(short)0x9876);check(c.enchantmentSeed==0x9876FED0,"seed halves lost in short-valued window properties");
        for(int seed=0;seed<256;seed++) {
            List<EnchantmentData> list=EnchantingTableRecipes.build(new Random(seed),new ItemStack(Items.book),30);
            if(list!=null)for(EnchantmentData entry:list)check(EnchantingTableRecipes.allowed(entry.enchantmentobj),"treasure enchantment offered at table");
        }
        check(!EnchantingTableRecipes.allowed(ModEnchantments.mending) && !EnchantingTableRecipes.allowed(ModEnchantments.frostWalker) && !EnchantingTableRecipes.allowed(ModEnchantments.swiftSneak),"treasure filter");
        world.setBlockToAir(EX,Y,Z);check(!c.enchantItem(p,0),"removed table usable");
        System.out.println("ENCHANTING PASSED: "+cases+" deterministic purchases, clue/result, 1/2/3 level/fuel costs, creative and no-fuel modes, stack conservation, NBT, seed persistence/wire and treasure filtering");
    }
    public static void run(WorldServer world,boolean legacy) throws Exception {
        if(legacy) { check(!TileEntityLectern.enabled() && !GrindstoneRecipes.enabled(),"new workstations bypass legacy profile");System.out.println("SCHOLARLY_WORKSTATIONS_LEGACY PASSED");return; }
        FakePlayer p=FakePlayerFactory.get(world,new GameProfile(UUID.fromString("0cc28a7b-6b1b-4b74-8aa5-f9869fbde28f"),"scholarly_probe"));clear(p);
        lectern(world,p);clear(p);grindstone(world,p);clear(p);enchanting(world,p);clear(p);
        System.out.println("SCHOLARLY_WORKSTATIONS PASSED");
    }
}

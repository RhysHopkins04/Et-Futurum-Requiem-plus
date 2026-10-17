package ganymedes01.etfuturum.inventory;

import ganymedes01.etfuturum.tileentities.TileEntityLectern;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;

/** A read-only book slot synchronizes full NBT; page state is shared by all readers. */
public final class ContainerLectern extends Container {
    public final TileEntityLectern lectern;
    private final EntityPlayer owner;
    private final InventoryBasic display=new InventoryBasic("Lectern",false,1);
    public int page;
    public boolean allowClickActions;
    public ContainerLectern(EntityPlayer owner,TileEntityLectern lectern) {
        this.owner=owner;this.lectern=lectern;
        addSlotToContainer(new Slot(display,0,-1000,-1000) {
            @Override public boolean isItemValid(ItemStack stack) { return false; }
            @Override public boolean canTakeStack(EntityPlayer player) { return false; }
        });
        display.setInventorySlotContents(0,lectern.getBook());page=lectern.getPage();
    }
    public ItemStack getBook() { return display.getStackInSlot(0); }
    @Override public boolean canInteractWith(EntityPlayer player) { return player==owner && lectern.canUse(player) && lectern.hasBook(); }
    @Override public void detectAndSendChanges() {
        if(lectern.getWorldObj()!=null && !lectern.getWorldObj().isRemote) {
            display.setInventorySlotContents(0,lectern.getBook());page=lectern.getPage();
            allowClickActions=ganymedes01.etfuturum.configuration.configs.ConfigFunctions.allowLecternBookClickActions;
        }
        super.detectAndSendChanges();for(Object listener:crafters) { ((ICrafting)listener).sendProgressBarUpdate(this,0,page);((ICrafting)listener).sendProgressBarUpdate(this,1,allowClickActions?1:0); }
    }
    @Override public void addCraftingToCrafters(ICrafting listener) { super.addCraftingToCrafters(listener);listener.sendProgressBarUpdate(this,0,page);listener.sendProgressBarUpdate(this,1,ganymedes01.etfuturum.configuration.configs.ConfigFunctions.allowLecternBookClickActions?1:0); }
    @Override public void updateProgressBar(int id,int value) { if(id==0)page=value;else if(id==1)allowClickActions=value!=0; }
    @Override public boolean enchantItem(EntityPlayer player,int button) {
        if(!canInteractWith(player))return false;
        if(button<0 || button>102)return false;
        if(button>=3) {
            int target=button-3;if(target>=lectern.getPageCount())return false;
            if(!lectern.getWorldObj().isRemote) { lectern.setPage(target);detectAndSendChanges(); }
            return true;
        }
        if(button==2) {
            if(!player.capabilities.allowEdit || ganymedes01.etfuturum.api.spectator.SpectatorUtils.isSpectator(player))return false;
            if(!lectern.getWorldObj().isRemote) {
                ItemStack book=lectern.removeBook();
                if(book!=null && !player.inventory.addItemStackToInventory(book))player.dropPlayerItemWithRandomChoice(book,false);
                player.inventoryContainer.detectAndSendChanges();
                if(player instanceof net.minecraft.entity.player.EntityPlayerMP && ((net.minecraft.entity.player.EntityPlayerMP)player).playerNetServerHandler!=null) ((net.minecraft.entity.player.EntityPlayerMP)player).sendContainerToPlayer(player.inventoryContainer);
                detectAndSendChanges();
            }
            return true;
        }
        if(!lectern.getWorldObj().isRemote) { lectern.setPage(lectern.getPage()+(button==0?-1:1));detectAndSendChanges(); }
        return true;
    }
    public boolean runBookCommand(EntityPlayer player,int requestedPage,int spanIndex,String expected) {
        if(!ganymedes01.etfuturum.configuration.configs.ConfigFunctions.allowLecternBookClickActions || !canInteractWith(player)
            || lectern.getWorldObj().isRemote || !(player instanceof net.minecraft.entity.player.EntityPlayerMP) || requestedPage!=lectern.getPage())return false;
        ItemStack book=lectern.getBook();if(book==null || book.getItem()!=net.minecraft.init.Items.written_book || !book.hasTagCompound())return false;
        java.util.List<ganymedes01.etfuturum.core.utils.LecternBookText.Span> spans=ganymedes01.etfuturum.core.utils.LecternBookText.parse(book.getTagCompound().getTagList("pages",8).getStringTagAt(requestedPage),true);
        if(spanIndex<0 || spanIndex>=spans.size())return false;
        ganymedes01.etfuturum.core.utils.LecternBookText.Span span=spans.get(spanIndex);
        if(!"run_command".equals(span.action) || !span.value.equals(expected) || !span.value.startsWith("/") || span.value.length()>100)return false;
        for(int i=0;i<span.value.length();i++)if(!net.minecraft.util.ChatAllowedCharacters.isAllowedCharacter(span.value.charAt(i)))return false;
        net.minecraft.network.NetHandlerPlayServer connection=((net.minecraft.entity.player.EntityPlayerMP)player).playerNetServerHandler;
        if(connection==null)return false;
        connection.processChatMessage(new net.minecraft.network.play.client.C01PacketChatMessage(span.value));
        if(player.openContainer==this)((net.minecraft.entity.player.EntityPlayerMP)player).closeScreen();
        return true;
    }
    @Override public ItemStack slotClick(int slot,int button,int mode,EntityPlayer player) { return null; }
    @Override public ItemStack transferStackInSlot(EntityPlayer player,int index) { return null; }
    @Override public boolean canDragIntoSlot(Slot slot) { return false; }
}

package ganymedes01.etfuturum.tileentities;

import ganymedes01.etfuturum.ModernMapParityBlocks;
import ganymedes01.etfuturum.Tags;
import ganymedes01.etfuturum.configuration.configs.ConfigMapCompatibility;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

/** Persisted shared book/page. Deliberately not a world inventory: vanilla lecterns reject hoppers. */
public final class TileEntityLectern extends TileEntity {
    private ItemStack book;
    private int page;
    public static boolean enabled() { return ModernMapParityBlocks.LECTERN.get()!=null && !ConfigMapCompatibility.mapCompatibilityMode; }
    public static boolean accepts(ItemStack stack) { return stack!=null && stack.stackSize>0 && (stack.getItem()==Items.written_book || stack.getItem()==Items.writable_book); }
    public ItemStack getBook() { return book==null?null:book.copy(); }
    public boolean hasBook() { return accepts(book); }
    public int getPage() { return page; }
    public int getPageCount() { return book!=null && book.hasTagCompound()?book.getTagCompound().getTagList("pages",8).tagCount():0; }
    public int comparator() { return !hasBook()?0:1+(getPageCount()>1?(int)(14F*page/(getPageCount()-1)):14); }
    public boolean canUse(EntityPlayer player) { return player.isEntityAlive() && worldObj!=null && enabled() && worldObj.getTileEntity(xCoord,yCoord,zCoord)==this
        && worldObj.getBlock(xCoord,yCoord,zCoord)==ModernMapParityBlocks.LECTERN.get() && player.getDistanceSq(xCoord+.5D,yCoord+.5D,zCoord+.5D)<=64D; }
    public boolean insert(ItemStack stack) {
        if(!enabled() || hasBook() || !accepts(stack))return false;
        book=stack.copy();book.stackSize=1;page=0;syncBookState(false);return true;
    }
    public ItemStack removeBook() {
        ItemStack removed=book;book=null;page=0;syncBookState(false);return removed;
    }
    public boolean setPage(int requested) {
        if(!hasBook())return false;
        int next=Math.max(0,Math.min(requested,Math.max(0,getPageCount()-1)));
        if(next==page)return false;page=next;syncBookState(true);
        if(worldObj!=null && !worldObj.isRemote)worldObj.playSoundEffect(xCoord+.5D,yCoord+.5D,zCoord+.5D,Tags.MC_ASSET_VER+":item.book.page_turn",1F,1F);
        return true;
    }
    private void syncBookState(boolean pulse) {
        markDirty();
        if(worldObj!=null && !worldObj.isRemote && worldObj.getBlock(xCoord,yCoord,zCoord)==ModernMapParityBlocks.LECTERN.get()) {
            int meta=worldObj.getBlockMetadata(xCoord,yCoord,zCoord);
            worldObj.setBlockMetadataWithNotify(xCoord,yCoord,zCoord,(meta&3)|(hasBook()?8:0)|(pulse?4:0),3);
            worldObj.markBlockForUpdate(xCoord,yCoord,zCoord);
            worldObj.func_147453_f(xCoord,yCoord,zCoord,ModernMapParityBlocks.LECTERN.get());
            worldObj.notifyBlocksOfNeighborChange(xCoord,yCoord-1,zCoord,ModernMapParityBlocks.LECTERN.get());
            if(pulse)worldObj.scheduleBlockUpdate(xCoord,yCoord,zCoord,ModernMapParityBlocks.LECTERN.get(),2);
        }
    }
    public void dropBook() {
        ItemStack removed=removeBook();
        if(removed!=null && worldObj!=null && !worldObj.isRemote)worldObj.spawnEntityInWorld(new EntityItem(worldObj,xCoord+.5D,yCoord+.8D,zCoord+.5D,removed));
    }
    @Override public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);if(book!=null)tag.setTag("Book",book.writeToNBT(new NBTTagCompound()));else tag.removeTag("Book");tag.setInteger("Page",page);
    }
    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);book=tag.hasKey("Book",10)?ItemStack.loadItemStackFromNBT(tag.getCompoundTag("Book")):null;
        if(!accepts(book))book=null;else book.stackSize=1;
        page=Math.max(0,Math.min(tag.getInteger("Page"),Math.max(0,getPageCount()-1)));
    }
    @Override public Packet getDescriptionPacket() { NBTTagCompound tag=new NBTTagCompound();writeToNBT(tag);return new S35PacketUpdateTileEntity(xCoord,yCoord,zCoord,0,tag); }
    @Override public void onDataPacket(NetworkManager manager,S35PacketUpdateTileEntity packet) { readFromNBT(packet.func_148857_g());if(worldObj!=null)worldObj.markBlockForUpdate(xCoord,yCoord,zCoord); }
    @Override public void updateEntity() {
        // A restart must not strand a saved page-turn pulse in the powered state.
        if(worldObj!=null && !worldObj.isRemote) {
            int meta=worldObj.getBlockMetadata(xCoord,yCoord,zCoord);
            if(((meta&8)!=0)!=hasBook())syncBookState(false);
            else if((meta&4)!=0)worldObj.scheduleBlockUpdate(xCoord,yCoord,zCoord,ModernMapParityBlocks.LECTERN.get(),2);
        }
    }
}

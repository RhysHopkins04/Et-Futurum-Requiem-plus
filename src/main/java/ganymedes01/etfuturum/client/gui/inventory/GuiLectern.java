package ganymedes01.etfuturum.client.gui.inventory;

import ganymedes01.etfuturum.inventory.ContainerLectern;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/** Read-only book display; requests page turns from the server rather than modifying held books. */
public final class GuiLectern extends GuiContainer implements net.minecraft.client.gui.GuiYesNoCallback {
    private static final ResourceLocation BOOK=new ResourceLocation("textures/gui/book.png");
    private final ContainerLectern container;
    private GuiButton previous,next,take;
    public GuiLectern(ContainerLectern container) { super(container);this.container=container;xSize=192;ySize=192; }
    @Override public void initGui() {
        super.initGui();buttonList.clear();
        buttonList.add(previous=new GuiButton(0,guiLeft+20,guiTop+164,30,20,"<"));
        buttonList.add(next=new GuiButton(1,guiLeft+130,guiTop+164,30,20,">"));
        buttonList.add(take=new GuiButton(2,guiLeft+54,guiTop+164,72,20,I18n.format("lectern.take_book")));
        buttonList.add(new GuiButton(3,guiLeft+54,guiTop+188,72,20,I18n.format("gui.done")));
    }
    private int pages() { ItemStack book=container.getBook();return book!=null && book.hasTagCompound()?book.getTagCompound().getTagList("pages",8).tagCount():0; }
    @Override public void updateScreen() {
        super.updateScreen();previous.enabled=container.page>0;next.enabled=container.page<pages()-1;
        take.enabled=mc.thePlayer.capabilities.allowEdit && !ganymedes01.etfuturum.api.spectator.SpectatorUtils.isSpectator(mc.thePlayer);
        if(container.getBook()==null)mc.thePlayer.closeScreen();
    }
    @Override protected void actionPerformed(GuiButton button) {
        if(button.id==3)mc.thePlayer.closeScreen();
        else if(button.enabled)mc.playerController.sendEnchantPacket(inventorySlots.windowId,button.id);
    }
    @Override protected void drawGuiContainerBackgroundLayer(float ticks,int x,int y) {
        GL11.glColor4f(1,1,1,1);mc.getTextureManager().bindTexture(BOOK);drawTexturedModalRect(guiLeft,guiTop,0,0,192,192);
    }
    @Override protected void drawGuiContainerForegroundLayer(int x,int y) {
        String number=I18n.format("book.pageIndicator",container.page+1,Math.max(1,pages()));
        fontRendererObj.drawString(number,148-fontRendererObj.getStringWidth(number),18,0x404040);
        pieces.clear();
        ItemStack book=container.getBook();if(book==null || !book.hasTagCompound())return;
        NBTTagList list=book.getTagCompound().getTagList("pages",8);if(container.page<0 || container.page>=list.tagCount())return;
        String text=list.getStringTagAt(container.page);
        java.util.List<ganymedes01.etfuturum.core.utils.LecternBookText.Span> spans=ganymedes01.etfuturum.core.utils.LecternBookText.parse(text,book.getItem()==net.minecraft.init.Items.written_book);
        int column=0,line=0,spanIndex=0;
        for(ganymedes01.etfuturum.core.utils.LecternBookText.Span span:spans) {
            String remaining=span.text,prefix="\u00a7r"+span.format;
            while(!remaining.isEmpty() && line<14) {
                if(remaining.charAt(0)=='\n') { column=0;line++;remaining=remaining.substring(1);continue; }
                int newline=remaining.indexOf('\n');String part=newline<0?remaining:remaining.substring(0,newline);
                String fit=fontRendererObj.trimStringToWidth(prefix+part,116-column);
                int length=Math.max(0,fit.length()-prefix.length());
                if(length==0) { if(column>0) { column=0;line++;continue; }length=1; }
                if(length<part.length()) { int space=part.substring(0,length).lastIndexOf(' ');if(space>0)length=space+1; }
                String chunk=prefix+part.substring(0,length);int width=fontRendererObj.getStringWidth(chunk);
                int px=32+column,py=30+line*fontRendererObj.FONT_HEIGHT;
                fontRendererObj.drawString(chunk,px,py,0);pieces.add(new Piece(px,py,width,spanIndex,span));
                column+=width;remaining=remaining.substring(length);
                if(!remaining.isEmpty() && remaining.charAt(0)!='\n' && length<part.length()) { column=0;line++; }
            }
            spanIndex++;
        }
    }
    private static final class Piece {
        final int x,y,width,index;final ganymedes01.etfuturum.core.utils.LecternBookText.Span span;
        Piece(int x,int y,int width,int index,ganymedes01.etfuturum.core.utils.LecternBookText.Span span) { this.x=x;this.y=y;this.width=width;this.index=index;this.span=span; }
    }
    private final java.util.List<Piece> pieces=new java.util.ArrayList<>();
    private java.net.URI pendingLink;
    private Piece at(int mouseX,int mouseY) {
        int x=mouseX-guiLeft,y=mouseY-guiTop;
        for(Piece part:pieces)if(x>=part.x && x<part.x+part.width && y>=part.y && y<part.y+fontRendererObj.FONT_HEIGHT)return part;
        return null;
    }
    @Override public void drawScreen(int x,int y,float ticks) {
        super.drawScreen(x,y,ticks);Piece part=at(x,y);
        if(part!=null && !part.span.hover.isEmpty())func_146283_a(java.util.Arrays.asList(part.span.hover.split("\n")),x,y);
    }
    @Override protected void mouseClicked(int x,int y,int button) {
        Piece part=at(x,y);
        if(button==0 && part!=null && !part.span.action.isEmpty()) {
            String action=part.span.action,value=part.span.value;
            if("change_page".equals(action)) {
                try { int page=Integer.parseInt(value)-1;if(page>=0 && page<Math.min(100,pages()))mc.playerController.sendEnchantPacket(inventorySlots.windowId,page+3); }catch(NumberFormatException invalid) { }
            } else if(container.allowClickActions) {
                if("run_command".equals(action) && value.startsWith("/") && value.length()<=100) {
                    ganymedes01.etfuturum.EtFuturum.networkWrapper.sendToServer(new ganymedes01.etfuturum.network.LecternBookActionMessage(inventorySlots.windowId,container.page,part.index,value));
                } else if("suggest_command".equals(action)) { mc.thePlayer.closeScreen();mc.displayGuiScreen(new net.minecraft.client.gui.GuiChat(value)); }
                else if("copy_to_clipboard".equals(action))setClipboardString(value);
                else if("open_url".equals(action) && mc.gameSettings.chatLinks) {
                    try {
                        java.net.URI uri=new java.net.URI(value);
                        if("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) {
                            pendingLink=uri;
                            if(mc.gameSettings.chatLinksPrompt)mc.displayGuiScreen(new net.minecraft.client.gui.GuiConfirmOpenLink(this,value,10,false));
                            else browse(uri);
                        }
                    }catch(java.net.URISyntaxException invalid) { }
                }
            }
            return;
        }
        super.mouseClicked(x,y,button);
    }
    @Override protected void keyTyped(char character,int key) {
        if(key==org.lwjgl.input.Keyboard.KEY_RIGHT || key==org.lwjgl.input.Keyboard.KEY_LEFT) {
            mc.playerController.sendEnchantPacket(inventorySlots.windowId,key==org.lwjgl.input.Keyboard.KEY_LEFT?0:1);return;
        }
        super.keyTyped(character,key);
    }
    private void browse(java.net.URI uri) { try { java.awt.Desktop.getDesktop().browse(uri); }catch(Exception unavailable) { } }
    @Override public void confirmClicked(boolean accepted,int id) {
        if(id==10) { if(accepted && pendingLink!=null && container.allowClickActions)browse(pendingLink);pendingLink=null;mc.displayGuiScreen(this); }
    }
}

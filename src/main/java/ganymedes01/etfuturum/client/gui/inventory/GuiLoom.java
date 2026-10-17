package ganymedes01.etfuturum.client.gui.inventory;

import ganymedes01.etfuturum.inventory.ContainerLoom;
import ganymedes01.etfuturum.recipes.LoomRecipes;
import ganymedes01.etfuturum.tileentities.TileEntityBanner.EnumBannerPattern;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.GL12;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import java.util.List;

/** Modern four-column selection layout with wheel and draggable-scrollbar support. */
public final class GuiLoom extends GuiContainer {
    private static final ResourceLocation BACKGROUND=new ResourceLocation("textures/gui/container/loom.png");
    private final ContainerLoom container;
    private List<EnumBannerPattern> lastChoices;
    private int row;
    private boolean scrolling;
    public GuiLoom(ContainerLoom container) { super(container);this.container=container;xSize=176;ySize=166; }
    private int maxRow() { return Math.max(0,(container.getRecipes().size()+3)/4-4); }
    private void clamp() {
        if(lastChoices!=container.getRecipes()) { lastChoices=container.getRecipes();row=0; }
        row=Math.max(0,Math.min(row,maxRow()));
    }
    private int hovered(int mouseX,int mouseY) {
        int x=mouseX-guiLeft-60,y=mouseY-guiTop-13;
        return x>=0 && x<56 && y>=0 && y<56 ? row*4+(y/14)*4+x/14 : -1;
    }
    private void sprite(String name,int x,int y,int width,int height) {
        mc.getTextureManager().bindTexture(new ResourceLocation("textures/gui/sprites/container/loom/"+name+".png"));
        func_146110_a(x,y,0,0,width,height,width,height);
    }
    @Override protected void drawGuiContainerBackgroundLayer(float partialTicks,int mouseX,int mouseY) {
        clamp();if(!Mouse.isButtonDown(0))scrolling=false;
        GL11.glColor4f(1,1,1,1);mc.getTextureManager().bindTexture(BACKGROUND);
        drawTexturedModalRect(guiLeft,guiTop,0,0,xSize,ySize);
        List<EnumBannerPattern> choices=container.getRecipes();int hovered=hovered(mouseX,mouseY);
        for(int index=row*4;index<Math.min(choices.size(),row*4+16);index++) {
            int cell=index-row*4,x=guiLeft+60+cell%4*14,y=guiTop+13+cell/4*14;
            sprite(index==container.getSelected()?"pattern_selected":index==hovered?"pattern_highlighted":"pattern",x,y,14,14);
        }
        sprite(maxRow()>0?"scroller":"scroller_disabled",guiLeft+119,guiTop+13+(maxRow()==0?0:row*41/maxRow()),12,15);
        RenderHelper.enableGUIStandardItemLighting();
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,240F,240F);
        GL11.glColor4f(1,1,1,1);
        for(int index=row*4;index<Math.min(choices.size(),row*4+16);index++) {
            int cell=index-row*4,x=guiLeft+60+cell%4*14,y=guiTop+13+cell/4*14;
            ItemStack output=preview(index);
            GL11.glPushMatrix();GL11.glTranslatef(x,y,0);GL11.glScalef(.875F,.875F,1);
            itemRender.renderItemAndEffectIntoGUI(fontRendererObj,mc.getTextureManager(),output,0,0);
            GL11.glPopMatrix();
        }
        ItemStack selected=container.getSlot(3).getStack();
        if(selected!=null)itemRender.renderItemAndEffectIntoGUI(fontRendererObj,mc.getTextureManager(),selected,guiLeft+143,guiTop+21);
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.disableStandardItemLighting();GL11.glDisable(GL11.GL_LIGHTING);
    }
    @Override protected void drawGuiContainerForegroundLayer(int mouseX,int mouseY) {
        fontRendererObj.drawString(I18n.format("tile.etfuturum.loom.name"),8,6,0x404040);
        fontRendererObj.drawString(I18n.format("container.inventory"),8,72,0x404040);
    }
    private ItemStack preview(int index) {
        return LoomRecipes.apply(container.input.getStackInSlot(0),LoomRecipes.dye(container.input.getStackInSlot(1)),container.getRecipes().get(index));
    }
    @Override public void drawScreen(int mouseX,int mouseY,float partialTicks) {
        super.drawScreen(mouseX,mouseY,partialTicks);
        int index=hovered(mouseX,mouseY);
        if(index>=0 && index<container.getRecipes().size())
            func_146283_a(java.util.Collections.singletonList(I18n.format("item.banner."+container.getRecipes().get(index).getPatternName()+"."+ganymedes01.etfuturum.recipes.ModRecipes.dye_names[LoomRecipes.dye(container.input.getStackInSlot(1))])),mouseX,mouseY);
    }
    private void scrollTo(int mouseY) {
        row=Math.max(0,Math.min(maxRow(),Math.round((mouseY-guiTop-13-7.5F)/41F*maxRow())));
    }
    @Override protected void mouseClicked(int mouseX,int mouseY,int button) {
        clamp();
        if(button==0) {
            if(maxRow()>0 && mouseX>=guiLeft+119 && mouseX<guiLeft+131 && mouseY>=guiTop+13 && mouseY<guiTop+69) {
                scrolling=true;scrollTo(mouseY);return;
            }
            int index=hovered(mouseX,mouseY);
            if(index>=0 && index<container.getRecipes().size()) {
                if(container.enchantItem(mc.thePlayer,index))mc.playerController.sendEnchantPacket(inventorySlots.windowId,index);
                return;
            }
        }
        super.mouseClicked(mouseX,mouseY,button);
    }
    @Override protected void mouseClickMove(int mouseX,int mouseY,int button,long duration) {
        if(scrolling && button==0 && Mouse.isButtonDown(0))scrollTo(mouseY);
        else super.mouseClickMove(mouseX,mouseY,button,duration);
    }
    @Override public void handleMouseInput() {
        super.handleMouseInput();int wheel=Mouse.getEventDWheel();clamp();
        int x=Mouse.getEventX()*width/mc.displayWidth,y=height-Mouse.getEventY()*height/mc.displayHeight-1;
        if(wheel!=0 && x>=guiLeft+60 && x<guiLeft+131 && y>=guiTop+13 && y<guiTop+69)
            row=Math.max(0,Math.min(maxRow(),row+(wheel>0?-1:1)));
    }
}

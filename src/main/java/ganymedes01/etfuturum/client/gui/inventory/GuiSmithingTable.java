package ganymedes01.etfuturum.client.gui.inventory;

import ganymedes01.etfuturum.Tags;
import ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems;
import ganymedes01.etfuturum.entities.EntityArmourStand;
import ganymedes01.etfuturum.inventory.ContainerSmithingTable;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class GuiSmithingTable extends GuiContainer {
    public static final ResourceLocation texturePath = asset("textures/gui/container/smithing.png");
    private static final ResourceLocation ERROR = asset("textures/gui/sprites/container/smithing/error.png");
    private static final ResourceLocation TEMPLATE = asset("textures/gui/sprites/container/slot/smithing_template_netherite_upgrade.png");
    private static final ResourceLocation TRIM_TEMPLATE = asset("textures/gui/sprites/container/slot/smithing_template_armor_trim.png");
    public final ContainerSmithingTable container;
    private EntityArmourStand preview;

    private static ResourceLocation asset(String path) { return new ResourceLocation(Tags.MC_ASSET_VER, path); }

    public GuiSmithingTable(ContainerSmithingTable container) {
        super(container);
        this.container = container;
        xSize = 176;
        ySize = 166;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1, 1, 1, 1);
        mc.getTextureManager().bindTexture(texturePath);
        int left = (width - xSize) / 2;
        int top = (height - ySize) / 2;
        drawTexturedModalRect(left, top, 0, 0, xSize, ySize);
        if (!container.getSlot(0).getHasStack()) {
            mc.getTextureManager().bindTexture((mc.theWorld.getTotalWorldTime() / 30 & 1) == 0 ? TEMPLATE : TRIM_TEMPLATE);
            func_146110_a(left + 8, top + 48, 0, 0, 16, 16, 16, 16);
        }
        if (container.unable()) {
            mc.getTextureManager().bindTexture(ERROR);
            func_146110_a(left + 65, top + 46, 0, 0, 28, 21, 28, 21);
        }
        if (ConfigBlocksItems.enableArmourStand) {
            if (preview == null) {
                preview = new EntityArmourStand(mc.theWorld);
                NBTTagCompound state = new NBTTagCompound();
                preview.writeToNBT(state);
                state.setBoolean("ShowArms", true);
                state.setBoolean("NoBasePlate", true);
                preview.readFromNBT(state);
            }
            equipPreview(preview, container.getSlot(3).getStack());
            drawPreview(left + 141, top + 75);
        }
    }

    /** Keep the display isolated from input stacks and clear equipment when the recipe changes. */
    public static void equipPreview(EntityArmourStand stand, ItemStack output) {
        for (int slot = 0; slot < 5; slot++) stand.setCurrentItemOrArmor(slot, null);
        if (output != null && output.getItem() instanceof ItemArmor) {
            int type = ((ItemArmor) output.getItem()).armorType;
            if (type >= 0 && type < 4) stand.setCurrentItemOrArmor(4 - type, output.copy());
        }
    }

    private void drawPreview(int x, int y) {
        // Keep the fixed smithing view; the legacy renderer needs a 180-degree yaw
        // correction relative to the modern render state to show the armor's front.
        float oldView = RenderManager.instance.playerViewY;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        try {
            GL11.glEnable(GL11.GL_COLOR_MATERIAL);
            GL11.glTranslatef(x, y, 50);
            GL11.glScalef(-25, 25, 25);
            GL11.glRotatef(180, 0, 0, 1);
            GL11.glRotatef(135, 0, 1, 0);
            RenderHelper.enableStandardItemLighting();
            GL11.glRotatef(-135, 0, 1, 0);
            GL11.glRotatef(25, 1, 0, 0);
            preview.prevRotationYaw = preview.rotationYaw = 30;
            preview.prevRenderYawOffset = preview.renderYawOffset = 30;
            preview.prevRotationYawHead = preview.rotationYawHead = 30;
            RenderManager.instance.playerViewY = 180;
            RenderManager.instance.renderEntityWithPosYaw(preview, 0, 0, 0, 30, 1);
        } finally {
            RenderManager.instance.playerViewY = oldView;
            GL11.glPopMatrix();
            RenderHelper.disableStandardItemLighting();
            GL11.glPopAttrib();
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRendererObj.drawString(I18n.format("gui.smithing.upgrade"), 44, 15, 0x404040);
        fontRendererObj.drawString(I18n.format("container.inventory"), 8, 72, 0x404040);
    }
}

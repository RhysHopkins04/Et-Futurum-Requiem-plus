package ganymedes01.etfuturum.client.gui.inventory;

import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.ModernMapParityBlocks;
import ganymedes01.etfuturum.blocks.BlockWoodSign;
import ganymedes01.etfuturum.network.WoodSignUpdateMessage;
import ganymedes01.etfuturum.tileentities.TileEntityWoodSign;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.util.ChatAllowedCharacters;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

/** Vanilla-style four-line editor for either face of an EFR/modern parity sign. */
public class GuiEditWoodSign extends GuiScreen {
    private final TileEntityWoodSign tileSign;
    private final boolean editingBack;
    private int updateCounter;
    private int editLine;
    private GuiButton doneBtn;

    public GuiEditWoodSign(TileEntityWoodSign sign) {
        this(sign, false);
    }

    public GuiEditWoodSign(TileEntityWoodSign sign, boolean editingBack) {
        this.tileSign = sign;
        this.editingBack = editingBack;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        Keyboard.enableRepeatEvents(true);
        buttonList.add(doneBtn = new GuiButton(0, width / 2 - 100, height / 4 + 120, I18n.format("gui.done")));
        tileSign.setEditable(false);
        tileSign.setEditingBack(editingBack);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        tileSign.setEditingBack(false);
        EtFuturum.networkWrapper.sendToServer(new WoodSignUpdateMessage(tileSign.xCoord, tileSign.yCoord, tileSign.zCoord,
                editingBack, tileSign.getText(editingBack)));
        tileSign.setEditable(true);
    }

    @Override
    public void updateScreen() {
        ++updateCounter;
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.enabled && button.id == 0) {
            tileSign.markDirty();
            mc.displayGuiScreen(null);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        String[] text = tileSign.getText(editingBack);
        if (keyCode == 200) editLine = editLine - 1 & 3;
        if (keyCode == 208 || keyCode == 28 || keyCode == 156) editLine = editLine + 1 & 3;
        if (keyCode == 14 && text[editLine].length() > 0) text[editLine] = text[editLine].substring(0, text[editLine].length() - 1);

        if (ChatAllowedCharacters.isAllowedCharacter(typedChar)) {
            String current = text[editLine];
            String candidate = current + typedChar;
            ModernMapParityBlocks parity = ModernMapParityBlocks.fromBlock(tileSign.getBlockType());
            boolean hanging = parity != null && parity.getStyle() == ModernMapParityBlocks.Style.HANGING_SIGN;
            int maxCharacters = hanging ? 13 : 15;
            int maxPixelWidth = hanging ? 60 : 90;
            if (current.length() < maxCharacters && fontRendererObj.getStringWidth(candidate) <= maxPixelWidth) text[editLine] = candidate;
        }

        if (keyCode == 1) actionPerformed(doneBtn);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        BlockWoodSign legacy = tileSign.getBlockType() instanceof BlockWoodSign ? (BlockWoodSign) tileSign.getBlockType() : null;
        ModernMapParityBlocks parity = ModernMapParityBlocks.fromBlock(tileSign.getBlockType());
        boolean paritySign = parity != null && (parity.getStyle() == ModernMapParityBlocks.Style.SIGN
                || parity.getStyle() == ModernMapParityBlocks.Style.HANGING_SIGN);
        boolean vanillaSign = tileSign.getBlockType() == Blocks.standing_sign || tileSign.getBlockType() == Blocks.wall_sign;
        if (legacy == null && !paritySign && !vanillaSign) return;

        drawDefaultBackground();
        drawCenteredString(fontRendererObj, I18n.format("sign.edit"), width / 2, 40, 16777215);
        GL11.glPushMatrix();
        GL11.glTranslatef(width / 2, 0.0F, 50.0F);
        float scale = 93.75F;
        GL11.glScalef(-scale, -scale, -scale);
        GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);

        int meta = tileSign.getBlockMetadata();
        if ((legacy != null && legacy.standing) || tileSign.getBlockType() == Blocks.standing_sign) {
            GL11.glRotatef(meta * 360 / 16.0F, 0.0F, 1.0F, 0.0F);
        } else {
            float yaw = 0.0F;
            if (parity != null && parity.getStyle() == ModernMapParityBlocks.Style.HANGING_SIGN
                    && !parity.getRegistryName().endsWith("_wall_hanging_sign")) {
                if (meta >= 8 && meta <= 11) yaw = wallYaw(2 + meta - 8);
                else yaw = ((meta >> 1) & 3) * 90.0F;
            } else {
                yaw = wallYaw(meta);
            }
            GL11.glRotatef(yaw, 0.0F, 1.0F, 0.0F);
        }
        if (editingBack) GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
        GL11.glTranslatef(0.0F, -1.0625F, 0.0F);

        tileSign.setEditingBack(editingBack);
        if (updateCounter / 6 % 2 == 0) tileSign.lineBeingEdited = editLine;
        TileEntityRendererDispatcher.instance.renderTileEntityAt(tileSign, -0.5D, -0.75D, -0.5D, 0.0F);
        tileSign.lineBeingEdited = -1;
        GL11.glPopMatrix();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private static float wallYaw(int meta) {
        if (meta == 2) return 180.0F;
        if (meta == 4) return 90.0F;
        if (meta == 5) return -90.0F;
        return 0.0F;
    }
}

package ganymedes01.etfuturum.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import ganymedes01.etfuturum.ModernMapParityBlocks;
import ganymedes01.etfuturum.tileentities.TileEntityWoodSign;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatAllowedCharacters;

/** Applies a two-sided sign update after validating distance, wax state and legacy limits. */
public class WoodSignUpdateHandler implements IMessageHandler<WoodSignUpdateMessage, IMessage> {
    @Override
    public IMessage onMessage(WoodSignUpdateMessage message, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().playerEntity;
        if (player == null || player.worldObj == null || player.getDistanceSq(message.x + 0.5D, message.y + 0.5D, message.z + 0.5D) > 64.0D) return null;
        TileEntity tile = player.worldObj.getTileEntity(message.x, message.y, message.z);
        if (!(tile instanceof TileEntityWoodSign)) return null;
        TileEntityWoodSign sign = (TileEntityWoodSign) tile;
        if (sign.isWaxed() || sign.func_145911_b() != player) return null;

        ModernMapParityBlocks parity = ModernMapParityBlocks.fromBlock(sign.getBlockType());
        int maxCharacters = parity != null && parity.getStyle() == ModernMapParityBlocks.Style.HANGING_SIGN ? 13 : 15;
        String[] clean = new String[4];
        for (int i = 0; i < 4; i++) {
            String value = ChatAllowedCharacters.filerAllowedCharacters(message.lines[i] == null ? "" : message.lines[i]);
            clean[i] = value.length() > maxCharacters ? value.substring(0, maxCharacters) : value;
        }
        sign.setText(message.back, clean);
        sign.func_145912_a(null);
        return null;
    }
}

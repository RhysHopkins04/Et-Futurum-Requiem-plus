package ganymedes01.etfuturum.network;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.client.gui.inventory.GuiEditWoodSign;
import ganymedes01.etfuturum.tileentities.TileEntityWoodSign;
import net.minecraft.block.Block;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.tileentity.TileEntity;

public class WoodSignOpenHandler implements IMessageHandler<WoodSignOpenMessage, IMessage> {

	@SideOnly(Side.CLIENT)
	@Override
	public IMessage onMessage(WoodSignOpenMessage message, MessageContext ctx) {
		WorldClient world = FMLClientHandler.instance().getClient().theWorld;
		if (world.blockExists(message.tileX, message.tileY, message.tileZ)) {
			TileEntity tileEntity = FMLClientHandler.instance().getClient().theWorld.getTileEntity(message.tileX, message.tileY, message.tileZ);

			if (!(tileEntity instanceof TileEntityWoodSign)) {
				tileEntity = new TileEntityWoodSign();
				tileEntity.blockType = Block.getBlockById(message.id);
				tileEntity.setWorldObj(FMLClientHandler.instance().getClient().theWorld);
				tileEntity.xCoord = message.tileX;
				tileEntity.yCoord = message.tileY;
				tileEntity.zCoord = message.tileZ;
			}

			tileEntity.markDirty();

			// Use EFR's long-standing vanilla-compatible sign editor for every wood/sign family.
			// Pass 11 delegated parity signs to GuiEditSign directly, but that screen assumes its
			// tile belongs to Blocks.standing_sign / Blocks.wall_sign and the hanging-sign preview
			// could swallow visible keyboard edits. GuiEditWoodSign duplicates the vanilla typing,
			// cursor, Done-button and C12 packet flow while now also understanding parity models.
			FMLClientHandler.instance().getClient().displayGuiScreen(new GuiEditWoodSign((TileEntityWoodSign) tileEntity, message.back));
		}
		return null;
	}

}

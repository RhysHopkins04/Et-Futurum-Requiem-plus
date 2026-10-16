package ganymedes01.etfuturum.blocks.itemblocks;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.StatCollector;
import java.util.List;

/** Explains the drop/processing distinction without changing the material's name. */
public final class NtmOreItemBlock extends ItemBlock {
    public NtmOreItemBlock(Block block) {super(block);setHasSubtypes(false);}
    @Override
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public void addInformation(ItemStack stack,EntityPlayer player,List tooltip,boolean advanced) {
        tooltip.add(StatCollector.translateToLocal("tooltip.etfuturum.ntm_ore_processing"));
    }
}

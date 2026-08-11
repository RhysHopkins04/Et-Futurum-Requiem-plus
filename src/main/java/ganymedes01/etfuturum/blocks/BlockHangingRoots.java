package ganymedes01.etfuturum.blocks;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.client.sound.ModSounds;
import ganymedes01.etfuturum.core.utils.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.EnumPlantType;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * Ceiling-hanging roots. The 1.7 BlockBush base gives the block non-solid crossed-plane rendering
 * and normal neighbour survival checks; support is deliberately inverted to the block above.
 */
public class BlockHangingRoots extends BlockBush {

    public BlockHangingRoots() {
        setBlockName(Utils.getUnlocalisedName("hanging_roots"));
        setBlockTextureName("hanging_roots");
        Utils.setBlockSound(this, ModSounds.soundHangingRoots);
        setCreativeTab(EtFuturum.creativeTabBlocks);
        setBlockBounds(0.1F, 0.2F, 0.1F, 0.9F, 1.0F, 0.9F);
    }

    @Override
    public EnumPlantType getPlantType(IBlockAccess world, int x, int y, int z) {
        return EnumPlantType.Cave;
    }

    @Override
    public boolean canBlockStay(World world, int x, int y, int z) {
        if (y >= world.getHeight() - 1) {
            return false;
        }
        Block support = world.getBlock(x, y + 1, z);
        return support.isSideSolid(world, x, y + 1, z, ForgeDirection.DOWN);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg) {
        blockIcon = reg.registerIcon(getTextureName());
    }
}

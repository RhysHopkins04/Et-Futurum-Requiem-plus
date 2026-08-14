package ganymedes01.etfuturum.blocks;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.configuration.configs.ConfigSounds;
import ganymedes01.etfuturum.core.utils.Utils;
import ganymedes01.etfuturum.Tags;
import ganymedes01.etfuturum.lib.RenderIDs;
import net.minecraft.block.BlockCauldron;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import java.util.Random;

public class BlockLavaCauldron extends BlockCauldron {

	public BlockLavaCauldron() {
		super();
		this.setStepSound(Blocks.cauldron.stepSound);
		this.setHardness(2);
		this.setResistance(2);
		this.setLightLevel(1.0F);
		this.setBlockName(Utils.getUnlocalisedName("lava_cauldron"));
	}

	@Override
	public boolean onBlockActivated(World worldIn, int x, int y, int z, EntityPlayer player, int side, float subX, float subY, float subZ) {
		ItemStack held = player.getCurrentEquippedItem();
		if (held != null && held.getItem() == Items.bucket) {
			if (!worldIn.isRemote) {
				worldIn.setBlock(x, y, z, Blocks.cauldron, 0, 3);
				if (!player.capabilities.isCreativeMode) {
					player.inventory.setInventorySlotContents(player.inventory.currentItem, new ItemStack(Items.lava_bucket));
				}
				if (ConfigSounds.fluidInteract) {
					worldIn.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, Tags.MC_ASSET_VER + ":item.bucket.fill_lava", 1.0F, 1.0F);
				}
			}
			return true;
		}

		// Do not delegate to vanilla BlockCauldron: its water-bottle/leather logic would treat the
		// lava contents as ordinary water. Unsupported items simply do not consume the interaction.
		return false;
	}

	@Override
	public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
		if (!world.isRemote && !entity.isImmuneToFire()) {
			if (!(entity instanceof EntityLiving) && !entity.isBurning()) {
				entity.playSound("random.fizz", 0.4F, 2.0F + world.rand.nextFloat() * 0.4F);
				if (!world.isRaining() && !world.canBlockSeeTheSky(x, y + 1, z)) {
					entity.setFire(15);
				}
			}
			entity.attackEntityFrom(DamageSource.lava, 4.0F);
		}
	}

	@Override
	public int tickRate(World world) {
		return 0;
	}

	@Override
	public void randomDisplayTick(World world, int x, int y, int z, Random random) {
		float min = 0.125F;
		float max = 0.875F;
		double d0 = x + (min + random.nextFloat() * (max - min));
		double d1 = z + (min + random.nextFloat() * (max - min));

		if (world.canBlockSeeTheSky(x, y + 1, z) && world.isRaining()) {
			world.spawnParticle("smoke", d0, y + 0.9375F, d1, 0.0D, 0.0D, 0.0D);
		}
		if (random.nextInt(100) == 0) {
			world.spawnParticle("lava", d0, y + 0.9375F, d1, 0.0D, 0.0D, 0.0D);
			world.playSound(x + .5, y + 1, z + 0.5, "liquid.lavapop", 0.2F + random.nextFloat() * 0.2F, 0.9F + random.nextFloat() * 0.15F, false);
		}
		if (random.nextInt(200) == 0) {
			world.playSound(x + .5, y + 1, z + 0.5, "liquid.lava", 0.2F + random.nextFloat() * 0.2F, 0.9F + random.nextFloat() * 0.15F, false);
		}
	}

	@Override
	public IIcon getIcon(int side, int meta) {
		return Blocks.cauldron.getIcon(side, meta);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister reg) {
		// Taking Icons from original Cauldron
	}

	@Override
	public int getRenderType() {
		return RenderIDs.LAVA_CAULDRON;
	}

}

package ganymedes01.etfuturum.mixins.early.fencewallconnect;

import ganymedes01.etfuturum.blocks.ModernWallState;
import ganymedes01.etfuturum.lib.RenderIDs;
import net.minecraft.block.Block;
import net.minecraft.block.BlockWall;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

/**
 * Pass 31/31b bridge for the two vanilla 1.7.10 cobblestone-wall metadata variants.
 *
 * <p>The original connection mixin only fixed {@link BlockWall#canConnectWallTo}; vanilla
 * cobblestone and mossy cobblestone walls therefore kept the old 1.7 renderer and broad
 * bounds even while EFR walls used the new state resolver.  Keep the vanilla block identity
 * and metadata, but route its visual and physical wall shape through the same derived modern
 * state used by BaseWall.</p>
 */
@Mixin(value = BlockWall.class)
public abstract class MixinBlockWall extends Block {

	protected MixinBlockWall(Material material) {
		super(material);
	}

	/**
	 * @author Roadhog360 / Et Futurum Requiem Plus
	 * @reason Correct hardcoded 1.7 wall connections and make vanilla/EFR/parity wall links reciprocal.
	 */
	@Overwrite
	public boolean canConnectWallTo(IBlockAccess world, int x, int y, int z) {
		return ModernWallState.canConnectWallTo((BlockWall) (Object) this, world, x, y, z);
	}

	/** Route vanilla BlockWall through the Pass 31 custom modern wall renderer. */
	@Overwrite
	public int getRenderType() {
		return RenderIDs.MODERN_WALL;
	}

	/**
	 * Replace the old 1.7 straight-run 13/16-high bounds with the neighbour-derived
	 * modern NONE/LOW/TALL/UP envelope used by the EFR wall families.
	 */
	@Overwrite
	public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
		ModernWallState.State state = ModernWallState.derive((BlockWall) (Object) this, world, x, y, z);
		float minX = state.west.isConnected() ? 0.0F : 0.25F;
		float maxX = state.east.isConnected() ? 1.0F : 0.75F;
		float minZ = state.north.isConnected() ? 0.0F : 0.25F;
		float maxZ = state.south.isConnected() ? 1.0F : 0.75F;
		setBlockBounds(minX, 0.0F, minZ, maxX, state.getVisualMaxY(), maxZ);
	}

	/**
	 * BlockWall's 1.7 collision method exposes one broad rectangular box.  Merge an
	 * override of Block.addCollisionBoxesToList so vanilla walls use the same independent
	 * 6px arms, optional 8px post and retained 1.5-block anti-jump height as BaseWall.
	 */
	@Override
	public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask,
			List<AxisAlignedBB> list, Entity collider) {
		ModernWallState.State state = ModernWallState.derive((BlockWall) (Object) this, world, x, y, z);
		if (state.up) efr$addWallCollision(mask, list, x, y, z, 0.25, 0, 0.25, 0.75, 1.5, 0.75);
		if (state.north.isConnected()) efr$addWallCollision(mask, list, x, y, z, 5.0/16.0, 0, 0, 11.0/16.0, 1.5, 0.5);
		if (state.south.isConnected()) efr$addWallCollision(mask, list, x, y, z, 5.0/16.0, 0, 0.5, 11.0/16.0, 1.5, 1);
		if (state.west.isConnected()) efr$addWallCollision(mask, list, x, y, z, 0, 0, 5.0/16.0, 0.5, 1.5, 11.0/16.0);
		if (state.east.isConnected()) efr$addWallCollision(mask, list, x, y, z, 0.5, 0, 5.0/16.0, 1, 1.5, 11.0/16.0);
	}

	/**
	 * Preserve a sensible aggregate collision AABB for callers that query BlockWall directly;
	 * entity collision itself is handled by the independent prisms above.
	 */
	@Overwrite
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
		ModernWallState.State state = ModernWallState.derive((BlockWall) (Object) this, world, x, y, z);
		double minX = state.west.isConnected() ? 0.0 : 0.25;
		double maxX = state.east.isConnected() ? 1.0 : 0.75;
		double minZ = state.north.isConnected() ? 0.0 : 0.25;
		double maxZ = state.south.isConnected() ? 1.0 : 0.75;
		return AxisAlignedBB.getBoundingBox(x + minX, y, z + minZ, x + maxX, y + 1.5, z + maxZ);
	}

	@Unique
	private static void efr$addWallCollision(AxisAlignedBB mask, List<AxisAlignedBB> list,
			int x, int y, int z, double minX, double minY, double minZ,
			double maxX, double maxY, double maxZ) {
		AxisAlignedBB box = AxisAlignedBB.getBoundingBox(x + minX, y + minY, z + minZ,
				x + maxX, y + maxY, z + maxZ);
		if (mask == null || box.intersectsWith(mask)) list.add(box);
	}
}

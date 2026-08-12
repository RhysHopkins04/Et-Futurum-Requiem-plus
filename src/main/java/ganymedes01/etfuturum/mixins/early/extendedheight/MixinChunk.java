package ganymedes01.etfuturum.mixins.early.extendedheight;

import ganymedes01.etfuturum.core.utils.WorldHeightCompat;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/** Extends Chunk's vertical storage and entity sections from 16 to 24. */
@Mixin(Chunk.class)
public abstract class MixinChunk {

    @Shadow private ExtendedBlockStorage[] storageArrays;
    @Shadow public List[] entityLists;
    @Shadow public World worldObj;
    @Shadow @Final public int xPosition;
    @Shadow @Final public int zPosition;
    @Shadow private int queuedLightChecks;

    @Inject(method = "<init>(Lnet/minecraft/world/World;II)V", at = @At("RETURN"))
    private void etfu$extendVerticalArrays(World world, int chunkX, int chunkZ, CallbackInfo ci) {
        if (world.provider != null && world.provider.dimensionId == 0) {
            this.storageArrays = new ExtendedBlockStorage[WorldHeightCompat.EXTENDED_SECTION_COUNT];
            this.entityLists = new List[WorldHeightCompat.EXTENDED_SECTION_COUNT];
            for (int i = 0; i < this.entityLists.length; ++i) {
                this.entityLists[i] = new ArrayList();
            }
            this.queuedLightChecks = WorldHeightCompat.EXTENDED_SECTION_COUNT * 16 * 16;
        }
    }

    @ModifyConstant(method = "getAreLevelsEmpty", constant = @Constant(intValue = WorldHeightCompat.LEGACY_HEIGHT))
    private int etfu$extendEmptyCheckHeight(int original) {
        return this.worldObj.provider != null && this.worldObj.provider.dimensionId == 0 ? WorldHeightCompat.EXTENDED_HEIGHT : original;
    }

    @ModifyConstant(method = "getAreLevelsEmpty", constant = @Constant(intValue = WorldHeightCompat.LEGACY_MAX_Y))
    private int etfu$extendEmptyCheckMaxY(int original) {
        return this.worldObj.provider != null && this.worldObj.provider.dimensionId == 0 ? WorldHeightCompat.EXTENDED_MAX_Y : original;
    }

    /** Height-map values are ints; the vanilla unsigned-byte mask truncates values above 255. */
    @ModifyConstant(method = "relightBlock", constant = @Constant(intValue = 255, ordinal = 0))
    private int etfu$removeHeightMapByteMask(int original) {
        return -1;
    }

    /**
     * Vanilla encodes its round-robin index as 16 section slots x 16 x 16. Reproduce the same
     * traversal with 24 section slots so upper-section block lighting is eventually revisited too.
     *
     * @author Et Futurum Requiem Plus
     * @reason Extend the relight traversal across all 24 physical chunk sections.
     */
    @org.spongepowered.asm.mixin.Overwrite
    public void enqueueRelightChecks() {
        final int sectionCount = this.storageArrays.length;
        final int totalChecks = sectionCount * 16 * 16;
        for (int i = 0; i < 8; ++i) {
            if (this.queuedLightChecks >= totalChecks) {
                return;
            }

            int section = this.queuedLightChecks % sectionCount;
            int localX = this.queuedLightChecks / sectionCount % 16;
            int localZ = this.queuedLightChecks / (sectionCount * 16);
            ++this.queuedLightChecks;
            int worldX = (this.xPosition << 4) + localX;
            int worldZ = (this.zPosition << 4) + localZ;

            for (int localY = 0; localY < 16; ++localY) {
                int worldY = (section << 4) + localY;
                if ((this.storageArrays[section] == null
                        && (localY == 0 || localY == 15 || localX == 0 || localX == 15 || localZ == 0 || localZ == 15))
                        || (this.storageArrays[section] != null
                        && this.storageArrays[section].getBlockByExtId(localX, localY, localZ).getMaterial() == Material.air)) {
                    if (this.worldObj.getBlock(worldX, worldY - 1, worldZ).getLightValue() > 0) {
                        this.worldObj.func_147451_t(worldX, worldY - 1, worldZ);
                    }
                    if (this.worldObj.getBlock(worldX, worldY + 1, worldZ).getLightValue() > 0) {
                        this.worldObj.func_147451_t(worldX, worldY + 1, worldZ);
                    }
                    if (this.worldObj.getBlock(worldX - 1, worldY, worldZ).getLightValue() > 0) {
                        this.worldObj.func_147451_t(worldX - 1, worldY, worldZ);
                    }
                    if (this.worldObj.getBlock(worldX + 1, worldY, worldZ).getLightValue() > 0) {
                        this.worldObj.func_147451_t(worldX + 1, worldY, worldZ);
                    }
                    if (this.worldObj.getBlock(worldX, worldY, worldZ - 1).getLightValue() > 0) {
                        this.worldObj.func_147451_t(worldX, worldY, worldZ - 1);
                    }
                    if (this.worldObj.getBlock(worldX, worldY, worldZ + 1).getLightValue() > 0) {
                        this.worldObj.func_147451_t(worldX, worldY, worldZ + 1);
                    }
                    this.worldObj.func_147451_t(worldX, worldY, worldZ);
                }
            }
        }
    }
}

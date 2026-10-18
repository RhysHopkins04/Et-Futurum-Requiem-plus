package ganymedes01.etfuturum.mixins.early.imported;

import ganymedes01.etfuturum.world.imported.ImportedPopulationGuard;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public class MixinWorld {
    @Inject(method="setBlock(IIILnet/minecraft/block/Block;II)Z",at=@At("HEAD"),cancellable=true,require=1)
    private void etfu$protectImportedBlock(int x,int y,int z,Block block,int meta,int flags,CallbackInfoReturnable<Boolean> cir) {
        if(ImportedPopulationGuard.blocks((World)(Object)this,x,y,z))cir.setReturnValue(false);
    }
    @Inject(method="setBlockMetadataWithNotify",at=@At("HEAD"),cancellable=true,require=1)
    private void etfu$protectImportedMetadata(int x,int y,int z,int meta,int flags,CallbackInfoReturnable<Boolean> cir) {
        if(ImportedPopulationGuard.blocks((World)(Object)this,x,y,z))cir.setReturnValue(false);
    }
    @Inject(method="setTileEntity",at=@At("HEAD"),cancellable=true,require=1)
    private void etfu$protectImportedTile(int x,int y,int z,TileEntity tile,CallbackInfo ci) {
        if(ImportedPopulationGuard.blocks((World)(Object)this,x,y,z))ci.cancel();
    }
    @Inject(method="removeTileEntity",at=@At("HEAD"),cancellable=true,require=1)
    private void etfu$protectImportedTileRemoval(int x,int y,int z,CallbackInfo ci) {
        if(ImportedPopulationGuard.blocks((World)(Object)this,x,y,z))ci.cancel();
    }
    @Inject(method="getTileEntity",at=@At("HEAD"),cancellable=true,require=1)
    private void etfu$hideImportedTileDuringPopulation(int x,int y,int z,CallbackInfoReturnable<TileEntity> cir) {
        // Some generators ignore a failed setBlock and then fill a chest or tank.
        if(ImportedPopulationGuard.blocks((World)(Object)this,x,y,z))cir.setReturnValue(null);
    }
}

# Crucible dev-280 inventory compatibility

This is an Et Futurum mod-only fix for the supplied, unchanged
`Crucible-1.7.10-staging-667a28e-dev-280-server.jar`.
Its SHA-256 is `bb44fe4ef0898716a614ce42be58debc4c9d93b04b674933572a6db86e3f5fd4`.
No server archive replacement, world edit, inventory deletion or additional dependency is required.

## Cause

The reports from 22:42 and 22:59 show the same failure: the hopper at
(9795,384,-2181) has no Bukkit inventory holder when Crucible creates an
`InventoryMoveItemEvent`. Its original `TileEntity.getOwner()` asks Bukkit for
the block state at the correct coordinates, but `CraftBlock` masks the block
reader Y coordinate with `& 255`. The lookup therefore reads Y=128 instead
of Y=384 and can return a non-inventory block state.

X5 attempted to transform the Bukkit readers. The real server still crashed,
and its startup log lacked the transformer installation message: isolated
transformer tests had not established that Bukkit would use the FML loading
path. That approach and the server archive patching tooling have been removed.

## Current implementation

`CrucibleInventoryOwnerTransformer` wraps the original Minecraft
`TileEntity.getOwner()` method in a narrow, exception-safe scope. The original
Bukkit holder creation, transfer event and cancellation handling are retained.
The one original `CraftWorld.getBlockAt` call is redirected through a native
bridge only for the scoped owner position. That bridge constructs the same
`CraftBlock` with full X/Y/Z, avoiding the Y masks in both `CraftWorld.getBlockAt`
and `CraftChunk.getBlock` before the state captures its coordinates. Other
positions use the original lookup unchanged. Only inventory tile entities in the enabled extended Overworld
above Y=255 activate the scope.

`MixinChunkInventoryOwner` restores the original tile Y for block, metadata and
light reads only when world, chunk, local X/Z and the masked Y all match that
owner. The corrected call cannot redirect again. Nested lookups restore their
previous scope; exceptions also clean up. Ordinary chunk reads keep their
original coordinates. There are no Bukkit class imports in these helpers,
no new chunk loads and no changes to inventory contents. Ordinary Forge
has no Bukkit `getOwner` method, so the transformer leaves it alone.

The startup log should include:

```
Installed full-coordinate inventory-owner lookup with exception-safe cleanup
```

## Verification and remaining live check

The regression test loads the transformer from the packaged production mod.
It uses the original dev-280 `TileEntity` bytecode reconstructed from the
server's binpatch and the checksum-verified vanilla base class. The extracted
class SHA-256 is `e10536bd99951b630a014ad712d3d6ce628bc0d6cbf93914dc5620406d37e355`.
The first fixture missed the masks in `CraftWorld.getBlockAt` and
`CraftChunk.getBlock`. X7 therefore produced a holder with a wrapped coordinate;
the real server then crashed in `CraftChest.getInventory`, as shown by both
23:21 reports. X7 is insufficient for the full hopper/chest path.

The revised fixture executes the unchanged dev-280 bytecode for
`CraftWorld.getBlockAt`, `CraftWorld.getBlockTypeIdAt`, `CraftChunk.getBlock`,
`CraftBlock.getTypeId` and `CraftChest.getInventory`, remapping their dependency
types into detached fixtures. Block-state selection and native storage remain
fixtures, explicitly, rather than a complete running server. It reproduces
the X7 chest failure at Y=129 before verifying full-coordinate holder creation
and inventory access after scope cleanup for hopper, chest and trapped chest.
It also tests Y=128,255,256,384,385,511,769,1023, success/exception cleanup,
nested scopes, world/chunk/coordinate isolation, plain Forge and idempotence.
Only the scoped block-lookup call changes in the original owner method.
Bukkit classes themselves remain outside the transformer.

With that extracted native class available, run:

```sh
./gradlew -I scripts/crucible_inventory_owner_tests.gradle \
  -PcrucibleNativeTileClass=/path/to/extracted/aor.class \
  -PcrucibleHeightJar=/path/to/unchanged/Crucible-dev-280-server.jar \
  crucibleInventoryOwnerContracts reobfJar --offline
```

These are class-level regression tests, not a complete Crucible/modpack boot.
Install the new production EFR mod jar in place of the older EFR jar while
leaving the supplied Crucible server jar unchanged. Live verification is a
restart into the affected chunk, followed by the hopper/chest/NTM launch-pad
transfer above Y=255, launch, refill and another restart. No hopper removal
or world editing is part of this fix.

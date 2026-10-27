# Forge multiplayer badge transparency

The supplied screenshot shows Forge's green compatibility tick on a white square.
Forge's original icon atlas contains transparent pixels whose hidden RGB is white.
FMLClientHandler.enhanceServerListEntry binds that atlas and draws the badge without
establishing alpha/blending state. This fits inherited opaque GUI state; the source
review does not establish which renderer last changed that state.

A client-only mixin wraps only the call from ServerListEntryNormal.drawEntry to
FMLClientHandler.enhanceServerListEntry. It pushes enable/color-buffer attributes,
enables alpha testing (GREATER 0.1) and ordinary alpha blending, then calls the
original method and returns its original tooltip. A finally block restores the
prior attributes. Both the badge and any blocked overlay are inside this scope.
It does not replace assets, determine compatibility, intercept connections or
change general GUI rendering. On Angelica the existing GL call rewriting routes
the calls through its tracked state stack; no direct Angelica dependency is added.

Validation: offline check/build and existing Earth import contracts passed.
The release refmap contains drawEntry -> func_148279_a with its full descriptor.
Archive comparison against the accepted water-skylight jar shows changes only to
the early client registration, new icon mixin and refmap (plus directory entries).
The water implementation is byte-for-byte unchanged. No jar was auto-installed,
no networking configuration changed. The user subsequently authorised the EFR
checkpoint commit on 27 October. Pushing was not requested.

Visual acceptance remains pending: open Multiplayer, inspect the tick's background
and hover tooltip, then inspect again after returning from a world. An existing
incompatible-server entry can also verify the cross; no server/mod mismatch needs
to be manufactured for this check. Keep the prior jar available for rollback.

# Fidelity Pass 31 — Modern Wall States

Pass 31 gives vanilla `BlockWall` (cobblestone and mossy cobblestone), mature `BaseWall` families
and the parity `resin_brick_wall` one shared neighbour-derived modern wall state contract.

## Runtime state

The four horizontal sides are derived as `none`, `low` or `tall`, and the center post is derived as
`up=true|false`. No additional metadata or tile entity is allocated: existing `BaseWall` subtype
metadata remains untouched.

A connected side is `tall` when the block above provides a solid downward support face, or when a
wall directly above has the matching horizontal connection. Otherwise the connected side is `low`.
The center post follows modern wall rules: isolated/corner/T shapes raise it, straight low runs and
low four-way crosses can omit it, and a matching opposite pair of `tall` sides explicitly suppresses
the post. A raised wall directly above still propagates its post. Outside that straight-tall exception,
full support above and the 1.21.11 wall-post override families (torches, tripwire, signs, banners,
pressure plates and cactus flower) raise it.

Rendering and selection use 14/16-high low arms and full-height tall arms/post geometry. Collision
keeps the existing 1.5-block anti-jump height but uses independent 6px wall-arm prisms and only adds
the 8px center prism when `up=true`.

## Vanilla 1.7 wall bridge (Pass 31b)

The original 1.7.10 `BlockWall` renderer only understands boolean connections and its special
13/16-high straight-run shape. Pass 31b extends the existing wall mixin so the two vanilla wall
metadata variants route through `RenderIDs.MODERN_WALL`, use the shared NONE/LOW/TALL/UP resolver,
and use the same independent 6px collision arms plus optional 8px center post as `BaseWall`. Their
registry identity and metadata (`0=cobblestone`, `1=mossy_cobblestone`) remain unchanged.

Pass 31c corrects the stacked-straight post rule. In a two-high straight wall row, the wall above
causes the lower row's matching side arms to become `tall`, but an opposite `tall` pair explicitly
forces `up=false`. Therefore the interior lower wall in a 3-wide x 2-high run stays smooth/narrow,
and both interior lower walls in a 4-wide x 2-high run stay smooth/narrow. End/corner/T pieces and
other non-straight states continue to raise the center post normally.

## Backporter/import contract

Modern wall `north`, `east`, `south`, `west` and `up` properties are **derived state**. They do not
consume 1.7 metadata and are intentionally discarded during structure palette conversion. Once the
wall is present in the imported world, rendering, selection and collision recompute those properties
from the imported neighbours.

This means Pass 31 does **not** preserve impossible/debug-stick wall overrides that disagree with the
surrounding world. A later importer pass may add an explicit override format only if there is a real
map-fidelity requirement for those non-natural states.

Waterlogging remains out of scope.

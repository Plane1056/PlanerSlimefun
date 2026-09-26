# Integration notes

Background on what the 2.1 / 2.2 integration changed, why, and which contracts
downstream code depends on. Useful when reading the source for the first time;
not a changelog.

## What was removed

- **The standalone Howitzer weapon** and everything only it used: the
  fire-control GUI, recon map, observer, lanyard, reload, ballistics, and
  Howitzer-only configuration. Artillery of that kind now comes from the separate
  HowitzerArtillery plugin when it is installed.
- **The entire conveyor package**, along with every item, scheduler, listener,
  GUI, machine, renderer, and shutdown registration that referenced it.
- **End Stone sandbag block/slab/stair items**, replaced by Sandbags 2.0 (below).
- **WWSF's old standalone contact-damage barbed-wire listener**, replaced by the
  Howitzer bridge (below).

## What was unified

- **Gas.** Grenades and artillery gas shells share one `GasEffectService`,
  `GasDetonationEvent`, configurable `GasCloud`, immunity handling, and gas-alarm
  detection path. The separate chlorine grenade and mustard bomb merged into a
  single Gas Bomb; both retired IDs stay registered as hidden, recipe-less items
  so copies already in player inventories keep working.
- **Barbed wire.** The Slimefun recipe output is created by HowitzerArtillery's
  `ArtilleryItemFactory` rather than imitated from its display name and material,
  so the crafted item keeps the real `arty:item_id=barbed_wire_coil` tag and
  custom model data 1010, and feeds Howitzer's own placement flow. The recipe is
  eight Steel Ingots around one Stick. Without HowitzerArtillery installed, WWSF2
  skips only this recipe with one warning instead of disabling itself.
- **Steel.** Added `WWSF_STEEL_BLOCK` on End Stone Bricks with a nine-Steel-Ingot
  recipe, as the base of a future wall/stair/slab family.

## Sandbags 2.0 resource-pack contract

This is the contract the resource pack must satisfy. The pack itself is not in
this source tree.

The physical carrier is a **reserved Piglin Note Block state**, not a visible
Chorus Flower or End Stone block. Chorus Flower is only the placement item.

- HowitzerArtillery owns Piglin Note Block notes `0..9` in both powered states.
- Sandbags reserve **unpowered notes `10..24`** for connection masks `0..14`, and
  **powered note `10`** for mask `15`.
- Mask bits: north `1`, east `2`, south `4`, west `8`.

Six connection topologies are computed from the mask — isolated, end, straight,
corner, T-junction, and cross — each with its rotations. The pack must supply
canonical and rotated models without globally overriding vanilla Chorus Flowers
or ordinary Note Blocks.

The plugin recalculates state after placement, breaking, explosions, chunk loads,
and note events. Legacy Chorus Flower, End Stone, and display-entity sandbags
migrate automatically; removed slab/stair records are cleared without deleting
the underlying vanilla blocks.

## Item model data

Item model data was migrated to the Minecraft 1.21.8 component API, and every
item ID that uses model data is registered with Slimefun.
`CustomModelDataRegistryTest` guards against two items sharing a model-data value
on the same vanilla carrier — duplicates across *different* carriers are legal
only when the pack selectors agree.

## Optional plugin bridges

`Movecraft`, `Vault`, and `HowitzerArtillery` are all `softdepend`. Each bridge
must degrade to a single log line rather than an exception when its plugin is
absent:

- No Movecraft → static artillery only.
- No Vault (with `survey-cost` above zero) → survey warns and is unavailable.
- No HowitzerArtillery → local fallback gas cloud, no barbed-wire recipe.

## Slimefun fork dependency

The build compiles against a specific Slimefun-United build rather than upstream
Slimefun4 RC-37. The two use different block-storage APIs and are not
interchangeable at runtime. See [`lib/README.md`](lib/README.md).

## Version note

These notes were written during the 2.1 integration; the source now builds as
2.2.7-oil-stability. Where a document and the code disagree, the code is
authoritative.

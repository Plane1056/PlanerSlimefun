# WWSF2

A Slimefun addon for Paper 1.21.8 combining WW1-themed artillery and fortifications
with a geographic oil-drilling economy.

Current version: **2.2.7-oil-stability**

---

## Requirements

| Component | Version | Notes |
|---|---|---|
| Paper | 1.21.8 | `paper-api` 1.21.8-R0.1-SNAPSHOT, `provided` scope |
| Java | 17 to compile | The server itself may need Java 21 if HowitzerArtillery is also installed |
| Maven | 3.8+ | |
| Slimefun-United | 4.10-alpha-19+050decd | **Not included — see [`lib/README.md`](lib/README.md)** |
| Vault / VaultAPI | 1.7.1 | Optional at runtime, `softdepend` |
| Movecraft | any current build | Optional at runtime, `softdepend` |
| HowitzerArtillery | schema 79 or newer | Optional at runtime, `softdepend` |

## Build

Put the Slimefun-United jar in `lib/` first (see [`lib/README.md`](lib/README.md)),
then:

```bash
mvn clean package
```

The output is `target/WWSF2-2.2.7-oil-stability.jar`.

Run the tests alone with:

```bash
mvn test
```

35 tests across 9 classes under `src/test/java` cover oil-field geometry and
tier maths, parachute physics, spent-casing state, blast resistance, gas-bomb
recipes, the Howitzer recipe bridge, and custom-model-data uniqueness. They are
plain JUnit 5 unit tests — no server or Slimefun runtime is needed.

### About the Slimefun dependency

The `pom.xml` declares Slimefun with `system` scope pointing at
`lib/Slimefun-United-4.10-alpha-19+050decd.jar`. This is deliberate: that fork's
database-backed `BlockStorage` API is **not** binary-compatible with upstream
Slimefun4 RC-37, and swapping the two produces runtime `NoSuchMethodError`s
rather than compile failures. If you intend to target upstream Slimefun4
instead, expect to rework `com.wwsf.integration.SlimefunStorageAccess` and every
direct `BlockStorage` call site.

## Source layout

```
src/main/java/com/wwsf/
  WWSFPlugin.java        plugin entry point; registration order matters (see below)
  ammo/                  shell and machine-gun ammunition items
  artillery/             ballistics, aiming, shell templates, blast profiles, MG heat
  aviation/              parachute item, listener, physics
  commands/              /wwsf command tree
  config/                config.yml helpers and the WWSFItem base class
  fortifications/        barbed wire, sandbags, steel block family, Gas Alarm Bell
  gas/                   shared gas engine: clouds, immunity, detonation event
  integration/           HowitzerArtillery bridge, Slimefun storage access
  items/                 misc items, multiblock interaction, TEMPLATE_* starters
  multiblock/            cannon/MG pattern definitions, loaders, validators, markers
  setup/                 Slimefun item groups, item registration, research, model data
  support/               bandage, morphine, signal flare
  tools/                 entrenching tool
  util/                  model data and message helpers
  vehicle/               optional Movecraft artillery bridge

src/main/java/com/oildrills/
                         drill machines, tiers, GUI, fuel, refining, persistence,
                         dowsing rod, and the geographic oil-field model

src/main/resources/
  config.yml             all balance tuning
  plugin.yml             Bukkit descriptor
  oil_fields.json        the 79-field geographic database (bundled default)
  *_cannon.yml           multiblock patterns
  maxim_machine_gun.yml  multiblock pattern
```

`TEMPLATE_NewItem.java` and `TEMPLATE_NewBlock.java` in `items/` are intentional
copy-paste starting points for new content, not dead code.

### Startup order

`WWSFPlugin.initialize()` runs synchronously in `onEnable`, not on a delay. That
matters: registering Slimefun items late let Slimefun finish loading block
storage before it knew WWSF2's item IDs, which made already-placed machines
render as vanilla blocks after a restart. Within `initialize()` the ordering
constraint is that oil items publish **before** `GasGrenadeRegistry` and
`HowitzerRecipeRegistry`, because both consume the Gas Bottle the oil chain
defines.

## Runtime data

Written under `plugins/WWSF2/`:

| File | Contents |
|---|---|
| `config.yml` | Copied from resources on first start; server-owned afterwards |
| `oil_fields.json` | Copied from resources on first start; server-owned afterwards |
| `oil_field_state.yml` | Per-field remaining reserves and survey flags |
| `active_drills.yml` | In-progress drill state |
| `markers.yml` | Multiblock marker positions, autosaved every 5 minutes |
| `chunk_oil.yml` | Legacy pre-2.1 file; read once for survey migration, then ignored |

## Further reading

- [`OIL-FIELDS.md`](OIL-FIELDS.md) — how the geographic oil economy resolves,
  stores, and migrates field state
- [`INTEGRATION-NOTES.md`](INTEGRATION-NOTES.md) — what the 2.1/2.2 integration
  changed and the Sandbags 2.0 resource-pack contract
- [`RELEASE-CHECKLIST.md`](RELEASE-CHECKLIST.md) — the pre-release test script,
  including the open blockers

## Known open work

Carried over from the release checklist, unresolved at handoff:

1. **Two gas engines.** `GasEffectService`/`GasCloud` in `gas/` and
   HowitzerArtillery's concentration-cell simulation both exist. The Gas Bomb
   prefers Howitzer's when it is installed and falls back to the local cloud
   otherwise. Whether to keep the fallback or delete it is undecided.
2. **Internal artillery scope.** `WWSFPlugin` still registers
   `ArtilleryCombatListener`, `ArtilleryRegistry`, aiming, shell templates, and
   MG heat, which overlaps with what HowitzerArtillery provides. Deciding
   whether WWSF2 owns artillery or defers to Howitzer is the biggest outstanding
   architectural call.
3. **Resource pack.** Sandbags 2.0 binds 16 connection masks to reserved Piglin
   Note Block states; the models live in a separate resource pack that is not
   part of this source tree. See the contract in `INTEGRATION-NOTES.md`.
4. Live-server acceptance in `RELEASE-CHECKLIST.md` has not been run against a
   current build.

## Licensing

No licence has been chosen for this source. Pick one before publishing or
redistributing. Note that Slimefun4 and its forks are GPL-3.0 licensed, which
affects what you may do with a plugin that links against them.

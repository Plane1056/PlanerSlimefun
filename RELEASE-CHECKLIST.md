# Release checklist

A ticked source item is not a substitute for its live Paper test. Ticks below
record what was verified in source or by unit test at the time of handoff;
everything unticked still needs doing.

## Open blockers

- [ ] **Decide the internal artillery subsystem's scope.** `WWSFPlugin` still
  registers `ArtilleryCombatListener`, `ArtilleryRegistry`, aiming, shell
  templates, machine-gun heat, and vehicle-artillery state, which overlaps with
  HowitzerArtillery. Either keep WWSF2's artillery deliberately or remove it —
  the current state is undecided, not intended.
- [ ] **Resolve the duplicate gas engines.** The gas alarm detects Howitzer gas,
  but WWSF2 still registers its own `GasGrenadeRegistry`, `GasGrenadeListener`,
  `GasCloud`, gas config, and detonation event as a fallback. Decide whether the
  fallback stays.
- [ ] Pass every live test below on the target Paper/Slimefun/HowitzerArtillery
  stack.
- [ ] Resolve every new `ERROR`, `SEVERE`, stack trace, or WWSF/Howitzer warning
  in `logs/latest.log`.

Do not publish a release while any blocker above remains open.

## Build and deployment preflight

- [ ] Stop the server fully; never install or migrate with `/reload`.
- [ ] Back up `plugins/WWSF2/`, `plugins/HowitzerArtillery/`, Slimefun data, and
  the world folders.
- [ ] Confirm the runtime is **Paper 1.21.8**. WWSF2 targets Java 17; the
  standalone HowitzerArtillery plugin requires Java 21, so a combined install
  needs Java 21.
- [ ] Install exactly one WWSF2 jar; remove older duplicates.
- [ ] Install a schema-79-or-newer HowitzerArtillery jar.
- [ ] Install the matching Howitzer resource pack.
- [ ] Install the approved Slimefun-United build (see `lib/README.md`).
- [ ] If `survey-cost` is above zero, install both Vault and a Vault economy
  provider. Vault alone does not provide balances.
- [ ] If vessel artillery is wanted, install and test a Movecraft build.
  Otherwise confirm the optional bridge disables cleanly.
- [ ] Confirm plugin enable order in the log: `Slimefun` and `HowitzerArtillery`
  before WWSF2 initialization.
- [ ] Confirm WWSF2 logs that it registered the Slimefun barbed-wire recipe using
  Howitzer's real coil.
- [ ] Confirm the client has both the Howitzer and WWSF2 sandbag packs enabled,
  with an intentional load order and no model/texture errors.

## Automated verification

- [x] Compiles on Java 17.
- [x] `mvn test` passes.
- [x] `mvn clean package` passes and includes the clean test run.
- [ ] Record the candidate's SHA-256 hashes in the release notes: the WWSF2 jar,
  the WWSF2 sandbag pack, and the Howitzer pack.
- [ ] Run the HowitzerArtillery build's own test suite and `assemble`; record its
  test count, jar hash, and resource-pack hash alongside.
- [ ] Open every final pack zip and confirm `pack.mcmeta` and `assets/` sit at
  its root rather than inside another directory.

## Removals and registrations

- [x] No conveyor implementation or registration remains.
- [x] End-Stone sandbag block/slab/stair items are removed; `WWSF_SANDBAG` uses
  Chorus Flower only as its placement item and reserved Note Block states once
  placed.
- [x] Steel Block is registered on End Stone Bricks.
- [x] The old standalone contact-damage barbed-wire listener is removed.
- [ ] Complete the artillery-scope blocker above.
- [ ] Complete the gas-engine blocker above.
- [ ] Start a copy of a pre-release world and confirm removed Slimefun item IDs
  do not disable WWSF2 or corrupt BlockStorage.
- [ ] Find any already-placed legacy WWSF barbed-wire Iron Bars and deliberately
  remove or migrate them; they are not Howitzer-tracked routes.

## Barbed-wire bridge

- [x] The Slimefun recipe output is created by Howitzer's `ArtilleryItemFactory`,
  not imitated from its display name and material.
- [x] `WWSF_BARBED_WIRE` preserves Howitzer custom model data **1010** through
  Slimefun's texture registry.
- [ ] Open the Slimefun guide and verify the displayed result is the Artillery
  Barbed Wire Coil model.
- [ ] Craft it with eight Slimefun Steel Ingots around one Stick.
- [ ] Inspect the crafted item and verify it retains `arty:item_id=barbed_wire_coil`.
- [ ] Use it to complete Howitzer's first-anchor, preview, and second-anchor
  placement flow.
- [ ] Verify the coil is consumed exactly once on success and not consumed on
  cancellation or failure.
- [ ] Verify placed wire uses Howitzer's post/wire models, persistence, cutting,
  0.5x slowdown, and distance-based damage.
- [ ] Restart with a placed route and verify it remains authoritative.
- [ ] Repeat the barbed-wire manual-test script from the HowitzerArtillery
  project (`docs/manual-tests/f4-barbed-wire.md` there).
- [ ] Start WWSF2 once without HowitzerArtillery and confirm it skips only the
  wire recipe with one clear warning rather than disabling the whole addon.

## Gas Alarm Bell

- [x] The bell monitor reads Howitzer simulation cells directly.
- [x] It filters through `harmfulConcentrationAt`, so smoke is not an alarm.
- [x] It uses a per-world index and does not load arbitrary chunks.
- [ ] Craft and place a Gas Alarm Bell.
- [ ] Fire a Howitzer gas shell within 24 blocks with a clear route; confirm the
  bell reacts within the configured scan interval.
- [ ] Confirm the warning identifies Howitzer artillery poison gas.
- [ ] Fire a smoke shell in the same place; confirm no alarm.
- [ ] Test gas at 23.9, 24.0, and beyond 24 blocks.
- [ ] Test direct sky access.
- [ ] Test a roofed bell with an open horizontal route.
- [ ] Test a roofed bell with the horizontal route blocked; confirm no alarm.
- [ ] Test multiple bells and multiple clouds without repeated task errors.
- [ ] Let gas linger past one alarm cycle and confirm the repeat-warning
  behaviour is what you want.
- [ ] Break a bell during an active alarm and confirm no orphan task, sound, or
  stuck alarm state remains.
- [ ] Restart with bells placed and verify monitoring resumes.
- [ ] Stress-test near Howitzer's 8,000-cell global cap with the expected maximum
  bell count; inspect tick time with Spark.
- [ ] Confirm the legacy chlorine/mustard event still triggers the bell for as
  long as the fallback gas engine remains enabled.

## Parachute

- [x] Default descent is 5 blocks/second, equal to `-0.25` vertical blocks/tick.
- [x] Invalid zero, negative, or non-finite tuning falls back to 5 blocks/s.
- [x] Deployment and stop use the same `PLAYERS` sound category.
- [x] A predicted collision and every deployment stop call `stopSound` for the
  Elytra-flight sound.
- [x] Landing has a two-tick protection window so listener ordering cannot
  re-enable fall damage.
- [ ] Deploy while falling faster than 5 blocks/s and verify Motion Y settles at
  approximately `-0.25`.
- [ ] Verify horizontal velocity is preserved.
- [ ] Hit the ground and verify the flight sound stops immediately.
- [ ] Hit a wall while descending and verify the flight sound stops.
- [ ] Verify the landing causes no fall damage.
- [ ] Verify expiry after 600 ticks restores normal falling.
- [ ] Verify logout, death, plugin disable, and rapid redeployment leave no
  orphan task or stale expiry able to stop a newer deployment.
- [ ] Test under high latency and low TPS.

## Sandbags 2.0

- [x] The carrier is a reserved Note Block state rather than a visible Chorus
  Flower or End Stone block.
- [x] Six connection topologies are computed: isolated, end, straight, corner,
  T-junction, cross.
- [x] The display-entity rendering path is removed; all 16 masks bind directly to
  reserved pack-owned block states.
- [ ] Verify the held item uses `wwsf2:sandbag`.
- [ ] Place every topology and every directional rotation.
- [ ] Add and remove neighbours and verify every affected model updates.
- [ ] Verify models join without holes, z-fighting, or wrong pivots.
- [ ] Verify ordinary Chorus Flowers and Note Blocks remain vanilla.
- [ ] Break sandbags and verify legacy display entities are removed.
- [ ] Restart, unload/reload chunks, move sandbags with allowed mechanics, and
  verify each reserved model state is restored.
- [ ] Load a world with old End-Stone sandbags and verify the migration does not
  destroy unrelated vanilla blocks.
- [ ] Test explosions and Slimefun protection plugins against the carrier and the
  display cleanup.

## Oil fields and economy

- [x] Automated tests cover ellipse boundaries, world allowlists, overlap
  priority, and invalid definitions.
- [ ] Back up live `plugins/WWSF2/oil_fields.json` and `oil_field_state.yml`.
- [ ] Confirm `oil-fields.worlds` names the intended Earth world, or is
  deliberately empty for every normal-environment world.
- [ ] Stand at the exact centre of a known field and survey with the Dowsing Rod.
- [ ] Test inside both ellipse axes and immediately outside each boundary.
- [ ] Test the same X/Z in a disallowed world, the Nether, and the End.
- [ ] Test overlapping fields and confirm the closest normalized centre wins.
- [ ] Place two drills in different chunks of one field and verify they share one
  reserve.
- [ ] Place drills in different fields and verify reserves stay independent.
- [ ] Restart and confirm remaining reserves and surveyed state persist.
- [ ] Move, resize, or rename a field while keeping its ID and verify state is
  retained.
- [ ] Add a new unique ID and verify it starts as a new unsurveyed full field.
- [ ] Reduce a field's score and verify saved remaining oil clamps to the new
  capacity.
- [ ] Increase a field's score and confirm it does not silently refill a depleted
  field.
- [ ] Test paid survey with Vault and a real economy provider.
- [ ] Test `survey-cost: 0` without Vault.
- [ ] Confirm the fee is withdrawn once and that a failed payment does not mark a
  field surveyed.
- [ ] Confirm a rebuilt jar does not overwrite the server-owned `oil_fields.json`.

## Model data and resource packs

- [x] Entrenching Tool and Gas Alarm Bell no longer share model data.
- [x] The Howitzer coil recipe retains custom model data 1010.
- [ ] Inspect every Slimefun guide item for a missing or purple-black model.
- [ ] Check duplicate custom-model-data values that share a vanilla carrier;
  duplicates on different carriers are acceptable only when the pack selectors
  agree.
- [ ] Verify WWSF2, Howitzer, and any global Slimefun pack compose in the chosen
  client load order.
- [ ] Verify the server resource-pack URL, SHA-1, prompt, download, and cache
  refresh on a clean client profile.
- [ ] Test Java Edition with all packs, and test the no-pack fallback.
- [ ] Test Geyser/Bedrock fallback or ship the required Bedrock mappings.

## Final acceptance

- [ ] Start a staging copy of the target server and confirm WWSF2, Slimefun,
  HowitzerArtillery, Vault/economy, and any other installed plugins all enable
  without errors.
- [ ] Run `/plugins` and verify required plugins are green.
- [ ] Run `/sf guide`, craft every changed recipe, place every changed block, and
  verify permissions as a non-op player.
- [ ] Run the HowitzerArtillery project's own release-check scripts in its
  documented order.
- [ ] Profile a representative battle with artillery, gas, bells, oil drills, and
  sandbag connections all active.
- [ ] Stop the server normally and verify both plugins flush state without
  shutdown exceptions.
- [ ] Restart and repeat spot checks for gas bell, wire, parachute, sandbags, and
  oil persistence.
- [ ] Record maintainer sign-off, exact artifact hashes, known limitations,
  migration and rollback instructions, and the tested server/plugin versions.
- [ ] Keep the pre-release backup until the release has run cleanly for the
  agreed rollback window.

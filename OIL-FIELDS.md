# Geographic oil-field storage

WWSF2 resolves oil at the centre of the exact Minecraft block against the circles
and ellipses in `src/main/resources/oil_fields.json`. It does not roll or store
random oil per chunk. That random per-chunk generation was the pre-2.1 behaviour
and is disabled.

## How it resolves

- At first start the plugin copies the bundled database to
  `plugins/WWSF2/oil_fields.json`. Server operators may edit that copy and
  restart; a rebuilt jar does not overwrite it.
- Each entry's `oil_amount_score` is multiplied by `oil-fields.barrels-per-score`
  from `config.yml` to get its initial shared reserve. The default multiplier is
  60, so a score-100 field starts with 6,000 barrels and every other score scales
  linearly.
- Runtime balances and per-field survey flags live in
  `plugins/WWSF2/oil_field_state.yml`. **All drills inside the same field drain
  the same balance.**
- When ellipses overlap, the field with the smallest normalized distance to its
  centre wins. Locations outside every field have zero oil.
- By default the database applies to all NORMAL-environment worlds. Set
  `oil-fields.worlds` to a list of world names to restrict it to the Earth map —
  necessary on any server with more than one normal world.

## Discovery

Players craft a single-use Dowsing Rod from three Slimefun Steel Ingots and
right-click the ground. The rod checks a ±50-block area, displays the grade and
shared reserves when a field footprint is detected, and marks that field ready
for drilling. The older `/oilsurvey`, `/survey`, and `/oilcheck` commands are no
longer registered.

If `survey-cost` is above zero, Vault **and** a Vault economy provider must both
be installed. Vault alone does not provide balances.

## Migration

On the first upgrade from a pre-2.1 install, survey flags in the old
`chunk_oil.yml` are mapped to their containing fields. The random legacy chunk
reserve values are deliberately ignored. The old file may be left in place as a
backup; nothing reads it after the one-time survey migration.

State keys off the field **ID**, not its geometry, so a field can be moved,
resized, or renamed while keeping its remaining oil. Reducing a score clamps
saved remaining oil to the new capacity; increasing it does not refill a depleted
field.

## The database itself

`oil_fields.json` holds 79 fields generated from a WW1 oil-field dataset
projected onto an Earth map. The projection is equirectangular:

```text
x = longitude / 360 * 49150
z = -latitude / 180 * 24575
```

North is negative Z, and X spans a world border of -24,575 to +24,575. Each entry
carries an ID, name, real-world location, latitude/longitude, the derived
Minecraft `x`/`z`, an ellipse or circle geometry with radii in blocks, and an
`oil_amount_score` from 1 to 100.

The geometry is an inspection symbol sized for gameplay, not a claim about the
real geological field boundary.

## Deploying a build

1. Stop the Paper server fully. Slimefun does not support plugin reloads safely.
2. Back up `plugins/WWSF2/` — at minimum `oil_fields.json` and
   `oil_field_state.yml`.
3. Remove the old WWSF2 jar from `plugins` and copy in the new one. Do not leave
   two WWSF2 jars installed.
4. Start the server and confirm the log reports 79 geographic oil fields.
5. If the Earth map is not the only NORMAL world, set `oil-fields.worlds` in
   `plugins/WWSF2/config.yml` to that world's exact name and restart once more.

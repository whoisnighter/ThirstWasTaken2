# Hexalia integration plan

What ThirstWasTaken2 does with [Hexalia](https://modrinth.com/mod/hexalia) (mod id `hexalia`, package
`net.astralya.hexalia`), a witchcraft mod: herbs, a Small Cauldron that brews, brews drunk from a
rustic bottle, salt, accessories. Asked for in issue #66, which reported a crash at load with both
mods on Fabric 1.20.1.

Written on 2026-11-19 from:

- the repository [AstralyaStudios/Hexalia](https://github.com/AstralyaStudios/Hexalia), branches
  `hexalia-1.21.1` (`28f5117c`) and `hexalia-1.20.1` (`5989185e`), both `1.3.8.2`;
- the Modrinth project `hexalia`, whose newest release is `1.3.8.2` (2026-10-28) on every loader.

## Which build for which node

| Node | Build | Modrinth id | Requires |
|---|---|---|---|
| `1.21.1`, `1.21.1-neoforge` | `1.3.8.2-1.21.1` | `pFStkNW4`, `yzAefsls` | Architectury API, GeckoLib |
| `1.20.1`, `1.20.1-forge` | `1.3.8.2-1.20.1` | `4R0X15iG`, `W6UySSJ7` | Architectury API, GeckoLib |
| every other node | none | — | — |

Nothing here compiles against Hexalia, so no node has a deps key for it.

## The crash (issue #66)

Hexalia's Brew of Bloodlust stops natural healing. On 1.20.1 it does that in its own `FoodDataMixin`,
on `FoodData#tick`'s calls to `Player#heal`, the same two calls our `FoodDataMixin` takes over for
dehydration. Up to `1.3.8.1` both mods used `@Redirect`; a call can be redirected once, so the second
one failed its `require = 1` and the game stopped at load. Hexalia `1.3.8.2` moved to MixinExtras'
`@WrapWithCondition` ("Fix startup crash with thirst mods"). Its 1.21.1 builds have no such mixin:
Bloodlust there only steals health on a hit.

**Step 1, done.** Our `FoodDataMixin` is a `@WrapOperation` now. A wrap chains with any other wrap and
with someone else's redirect, so the game loads with every Hexalia build, and with any other mod that
redirects the same call. Behaviour is unchanged: a blocked heal still refunds the food cost, an allowed
one calls through, and with Hexalia's wrap around ours Bloodlust stops both.

Checked on 2026-11-19 in a driven `runClient` with a world open, since `FoodData` is only transformed
once a player exists: on `1.20.1` with Hexalia `1.3.8.1` and `1.3.8.2` (plus Architectury, GeckoLib and
Cloth Config) dropped in `run/1.20.1/mods`, and on `1.20.1-forge` with the same two builds added to
`runClient` for the run. In all four, eight damage healed back from 12 to 20 health on a full bar and
saturation, stayed at 12 under Bloodlust, and stayed at 12 at thirst 4. With the old `@Redirect` and
Hexalia `1.3.8.1`, the world crashed on join with Mixin's "@Redirect conflict" and "Critical injection
failure", the reporter's crash. `runServer` does not show it: no player, so `FoodData` never loads, and
on Fabric Hexalia's nested Terraform Wood API asks for a client-only Fabric API module there.

## The brews

**Step 2, done.** Nine brews, by id in `ThirstConfig` (`OtherModItemValues.hexaliaDrinks`): homestead,
bloodlust, slimewalker, spikeskin, siphon, daybloom, gravebloom (1.21.1 only), arachnid grace, hollow
silence. Herbs steeped in water and drunk from a bottle, so a tea's worth, 6 and 4, whatever the
brew does. They are tagged `hexalia:brews`, not `c:drinks`, so until now they restored nothing. The two
salves are rubbed on, not drunk, and are left out.

## Salt

Nothing to do. Hexalia's salt is in `c:salt` on both versions (and `forge:salt` on 1.20.1), which the
distiller's salt tag already takes.

## The Small Cauldron (not done)

**Step 3, open.** The Small Cauldron (`SmallCauldronContents.isWaterContainer`,
`tryFillWithWaterPublic`) takes a water bucket (1000 mB) or water bottles (250 mB each) and brews
from them, sea water included. Every other mod's brewing or cooking vessel we cover refuses sea water
(see `docs/dev/mechanics/SALT-WATER-REFUSALS.md`). Doing so here means a mixin against Hexalia, so a
deps key and an integration row on the four nodes above. Left until a player asks; a brew made from
sea water is the only effect today.

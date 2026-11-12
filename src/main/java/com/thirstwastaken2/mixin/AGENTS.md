# mixin/

Vanilla hooks. Everything the mod cannot do through a Fabric API event lands here, and nothing else.

## Rules

- Package-private, `abstract`, one target class per file, named `<Target>Mixin`.
- Every injected member is prefixed `thirst$` — methods, `@Unique` fields, constants excepted.
- **A new mixin must be added to `src/main/resources/thirstwastaken2.mixins.json`** or it silently
  does nothing. `injectors.defaultRequire` is 1, so a stale target throws at load instead of failing
  quietly — a `runServer` that starts is already proof every injection point still resolves.
- There are no client-only mixins in this package. Client mixins live in the client source set, in
  two configs of their own. `MinecraftMixin` and `LocalPlayerMixin` name no loader API and are in
  `src/client/java` under `com.thirstwastaken2.client.mixin`, listed in
  `src/client/resources/thirstwastaken2.client.mixins.json`, which both loaders load. `GuiMixin` stands
  in for Fabric API's HUD registry on 1.21.1 and so is loader code: `src/client/fabric/java` under
  `com.thirstwastaken2.fabric.mixin`, listed in `thirstwastaken2.fabric.client.mixins.json`.
- A mixin that only one version needs still exists on every version, with an empty body elsewhere:
  the mixin config is shared, and a listed class that is missing is a crash. `BlocksMixin` and
  `GuiMixin` are the examples. No block comments inside their `//?` blocks; see the root `AGENTS.md`.
- Keep the mixin thin: capture or redirect, then call into `com.thirstwastaken2.*`. Game logic does
  not belong in this package. One line of body is the target.
- A mixin is the one place outside `platform/` allowed to carry a Stonecutter `//?` branch, because
  an `@Inject` signature tracks its target method across Minecraft versions.

## What is hooked

| Mixin | Target | Purpose |
|---|---|---|
| `PlayerMixin` | `causeFoodExhaustion` (HEAD), `hasEnoughFoodToDoExhaustiveManoeuvres` (`@ModifyReturnValue`, after 1.21.1); implements `ExhaustionTracker.Holder` | buffer hunger exhaustion for the thirst tick; block sprinting at thirst ≤ 6 through `ThirstManager.allowsSprinting`. Not `Player#canSprint`: that only asks whether a rider may sprint the player, and hooking it blocked nothing. On 1.21.1 the check lives on `LocalPlayer`, so `LocalPlayerMixin` in `src/client/java` gates it there, on both loaders |
| `FoodDataMixin` | `FoodData#tick`, both `ServerPlayer#heal` call sites (`Player#heal` on 1.21.1) | dehydration and Upset Stomach halt natural regen and refund the food cost vanilla would have charged |
| `ItemStackMixin` | `use` (HEAD), `finishUsingItem` (HEAD), `addDetailsToTooltip` (TAIL); on 1.21.1 a `@WrapOperation` round the `appendHoverText` call in `getTooltipLines` | block plain water at full thirst; restore thirst on consume; append waterskin, purity and droplet lines |
| `BottleItemMixin` | `BottleItem#use` | let the bottle's ray stop at flowing water too (`ClipContext.Fluid.ANY`, not vanilla's `SOURCE_ONLY`); stamp sampled quality onto a bottle filled from a water block |
| `BucketItemMixin` | `BucketItem#use` | stamp sampled quality onto a bucket filled from a water block |
| `LayeredCauldronBlockMixin` | `createBlockStateDefinition`, `handlePrecipitation`, `receiveStalactiteDrip` | add the quality property; grade water that rain or a dripstone added |
| `CauldronBlockMixin` | `handlePrecipitation`, `receiveStalactiteDrip` | the same two fills, on the empty cauldron they turn into a water cauldron |
| `BlocksMixin` | 1.21.1 only: the two `new LayeredCauldronBlock` in `Blocks`' static init | mark which one is the water cauldron, for `Vanilla.isWaterCauldron` |

A canteen or flask keeps its servings through a furnace by `CookingRecipeMixin`, on the recipe's
`assemble`. Vanilla's furnace before 1.21.2 never calls it: it copies the recipe's own result
(`getResultItem`), so on Fabric 1.20.1 and 1.21.1 the vessel came out empty (issue #55). Forge and
NeoForge patch their furnace to assemble. The Fabric-only `FurnaceMixin` in `src/main/fabric/java`
under `com.thirstwastaken2.fabric.mixin`, listed in `thirstwastaken2.fabric.mixins.json`, gives that
result the input's servings in `burn`; it is empty from 1.21.2. Anything else that cooks from
`getResultItem` (another mod's furnace) needs the same fix of its own.

`BlocksMixin` exists because 1.21.1 computes a block's description id from the registry and caches
it. Asking a cauldron for it inside its own constructor, which is where the quality property has to
be added, would find it unregistered and name it air for good. The water cauldron is told apart from
the powder snow one by the precipitation it is built with.

## The fragile ones

`BottleItemMixin` and `BucketItemMixin` share a two-step shape, implemented in
`com.thirstwastaken2.purity.FillCapture`: a server-only `@Inject` at HEAD re-raycasts the player's
view (`ClipContext.Fluid.SOURCE_ONLY`) and stores the sampled quality, then a `@ModifyArg` stamps the
resulting stack. They depend on an exact target descriptor, and the bucket one also on the first
`ItemUtils#createFilledResult` after `pickupBlock`, whose descriptor forks on 1.21.1. Both break on a
vanilla refactor rather than misbehaving, which is the intent.

The two cauldron fill hooks inject at `RETURN`, not `TAIL`: vanilla returns early when the roll
fails, and only `RETURN` covers every exit. Both call into `WaterInteractions`, which decides from
the before and after blockstates whether any water was actually added.

`FoodDataMixin` uses `@Redirect` with `ordinal = 0` (saturation-driven regen) and `ordinal = 1`
(hunger-driven regen). Redirecting means vanilla's `heal` is *not* called unless the mixin calls it, so
every branch must either heal or refund exhaustion — dropping both would let hunger drain for free.

`@Unique` fields on an item mixin live on the shared item singleton, not per stack. `FillCapture`
therefore uses `ThreadLocal`: integrated-client prediction and the server may call the same item
singleton from different threads. Values are removed at HEAD and immediately after stamping; do not
let interaction state outlive one `use` call.

`PlayerMixin`'s `@Unique` field is the opposite case: it lives on each player entity, so per-player
state is safe there. It holds a lazily created `com.thirstwastaken2.data.ExhaustionTracker`, reached
through the `ExhaustionTracker.Holder` duck interface so the logic itself stays in `data/`. Only
`ThirstManager` reads or writes it, and only on the server thread.

# src/main/extradelight — water grades in Extra Delight's kitchen

[Extra Delight](https://modrinth.com/mod/extradelight) (mod id `extradelight`, package
`com.lance5057.extradelight`) is a Farmer's Delight addon with a Tap, Sinks, Jars, Kegs, a Vat, a Mixing
Bowl, a Chiller, a Melting Pot, an Evaporator and a Funnel. Its tanks keep fluid components, but every
bottle and bucket crossed into them as plain water, so a Dirty or salty bottle came back Clean; the Tap and
the Sink poured Clean water from nothing; sea water pickled, mixed lemonade and dried into salt as well as
fresh water did. This directory fixes that. The plan and its decisions are in
[docs/dev/integration/cooking/EXTRA-DELIGHT-INTEGRATION.md](../../../docs/dev/integration/cooking/EXTRA-DELIGHT-INTEGRATION.md).

**Built on `1.21.1-neoforge` only**, the mod's build 2.7, its only official one. What it does:

- **a tank keeps the grade** poured into it, by bottle or by bucket: the Jar, the Keg, and the fluid
  slots of the Vat, the Mixing Bowl and the Chiller. Two grades never share a tank: the tanks compare
  components, so a bottle of another grade is refused;
- **the Tap and the Sink give Murky water**, the grade every other mod's endless sink gives, by bottle,
  by bucket and through pipes. They stay endless;
- **a Funnel taking up a water source** grades it where it lay;
- **meltwater** (the Chiller's drip tray, ice in the Melting Pot) is rainwater, Clean by default;
- **sea water makes nothing** in the Vat or the Mixing Bowl, and crafts no Vinegar Pot or Yeast Pot;
- **the Evaporator dries only sea water**, into salt, over about eight minutes; fresh water stands in it untouched.

The drink and food values are not here. They are ids in `config/itemvalues/FarmersDelightAddonItemValues`, common code that names no class
of the mod.

```
extradelight/java/com/thirstwastaken2/extradelight/
  ExtraDelightPresence            the gate: each target class and method read off its class file
  ExtraDelightMixinPlugin         applies each mixin only while the mod is there and its target declares every method it needs
  ExtraDelightWater               TAP (Murky), meltwater(), stamped(), the sea water tests, and the graded() transfer scope
  mixin/BottleFluidRegistryMixin  getFluidFromBottle / getBottleFromFluid: the grade crosses both ways
  mixin/FluidUtilMixin            NeoForge's FluidUtil.getFluidHandler(ItemStack): graded handlers, inside graded() only
  mixin/JarBlockEntityMixin       use: inside graded()
  mixin/KegBlockEntityMixin       use: inside graded()
  mixin/EvaporatorBlockMixin      useItemOn: inside graded()
  mixin/TapBlockMixin             useItemOn: inside graded(), and the bottle it builds itself is Murky
  mixin/SinkCabinetBlockMixin     the same for the Sink
  mixin/WellFluidCapabilityMixin  the Tap's and the Sink's endless water is Murky
  mixin/IFancyTankHandlerMixin    fillInternal / drainInternal: slot containers graded, and the bucket it builds stamped
  mixin/ChillerBlockEntityMixin   the drip tray's meltwater, and the bucket or bottle drawn from it
  mixin/MeltingPotBlockEntityMixin ice melted into meltwater
  mixin/FunnelBlockEntityMixin    pullFluidIn: a source block taken up is sampled where it lay
  mixin/VatRecipeMixin            matches: false while the tank holds sea water
  mixin/MixingBowlRecipeMixin     the same
  mixin/EvaporatorRecipeMixin     matches: false for fresh water
  mixin/ShapelessRecipeMixin      a sea water container crafts no item of the mod's
extradelight/resources/
  thirstwastaken2.extradelight.mixins.json
```

## How it works

- **Bottles.** Every bottle goes through `util/BottleFluidRegistry`. `getFluidFromBottle` hands out the
  first stack of a cached `SizedFluidIngredient`, so the mixin stamps a **copy**; stamping in place would
  stamp every later bottle. `getBottleFromFluid` stamps the bottle it returns with the fluid's grade,
  `defaultPurity` for water with none.
- **Buckets and other containers.** NeoForge's bucket wrapper takes and gives plain water. Rather than
  change it for every mod, `ExtraDelightWater.graded` marks the server thread as inside one of this mod's
  transfers (a `@WrapMethod` on each block's click handler), and only there `FluidUtilMixin` wraps the
  handler `FluidUtil.getFluidHandler(ItemStack)` returns in core's `WaterQualityFluidHandler`
  (`src/main/neoforge-fluidhandler`, shared with Sophisticated): water leaving a container carries its
  grade, and the container it turns into gets the grade of the water entering it. Every `FluidUtil`
  helper finds the container's handler through that method.
- **The fluid slots.** `IFancyTankHandler.fillInternal` and `drainInternal` are interface default
  methods, shared by the Vat, the Mixing Bowl and the Chiller, and ask the stack for its capability
  directly instead of through `FluidUtil`; the interface mixin wraps that call. The bucket branch of
  `drainInternal` builds a bucket from the fluid's type, so it is stamped with the grade the tank held at
  the start of the call.
- **The Tap and the Sink.** Their endless water is `WellFluidCapability(WATER)`, whose only users they
  are; `getFluid` and `drain(int)` hand it out Murky, and `drain(FluidStack)` goes through `drain(int)`.
  The glass bottle path builds `new FluidStack(WATER)` itself, so that argument is stamped Murky too.
- **Recipes.** The Vat, Mixing Bowl and Evaporator match water by fluid alone. Their `matches` mixins read
  the tank: sea water stops the first two, fresh water stops the Evaporator. The tank keeps what it holds.
- **Compiled against the mod.** The mixins name its classes, so `build.neoforge.gradle.kts` has it as
  `compileOnly`. Every injection point was checked against the 2.6.6 jar's bytecode, and again against 2.7's.

## Not covered

- **`TapBlockEntity.fill` / `drain` and the Sink's**, two helpers nothing in 2.6.6 calls. A future caller
  would hand out Murky water (the capability) in a bucket with no grade.
- **The Juicer and the Mortar** fill only their own juices and pastes, never water.
- **Water saved in a tank before this integration** is unstamped and refuses stamped water until it is
  used up or drained, as in every tank; the Chiller's drip tray stops collecting meltwater until then.
- **The Forge 1.20.1 backport** (`extra-delight-unofficial-port`), by someone else, is not built against.

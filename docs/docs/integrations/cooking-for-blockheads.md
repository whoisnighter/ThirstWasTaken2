# ![](https://cdn.modrinth.com/data/vJnhuDde/811d0c55230d84d9e182df1b0bd631ddebecc0ad_96.webp){.mod-icon} Cooking for Blockheads

With ![](https://cdn.modrinth.com/data/vJnhuDde/811d0c55230d84d9e182df1b0bd631ddebecc0ad_96.webp){.mod-icon} [Cooking for Blockheads](https://modrinth.com/mod/cooking-for-blockheads) installed, its Sink gives water that
needs boiling, and its Oven can boil it.

::: warning Supported versions
Every version and loader this mod supports, with Cooking for Blockheads 16.0.15 on 1.20.1, 21.1.24 on
1.21.1, 21.11.6 on 1.21.11, 26.1.2.6 on 26.1, 26.2.0.5 on 26.2 and 26.3.0.2 on 26.3. It needs Balm.
:::

## Sink

The Sink is an endless tap, and it stays one. But its water is Murky, so boil it before drinking.
Bottles and buckets filled at the Sink come out Murky. Up to Minecraft 26.2 the Sink also fills a
waterskin, canteen or flask, and the water it adds is Murky too, mixed with what was already in it. That
is the same water other mods' kitchen sinks give.

The Sink won't take sea water, by hand or by pipe. A bucket of sea water stays in your hand.

### Sink as a tank

With Cooking for Blockheads' `sinkRequiresWater` setting on, the Sink no longer makes its own water. It
holds what is piped or poured into it, and keeps its grade. Pipe Pure water in from a
[Copper Distiller](/docs/features/water-purity#copper-distiller) and the Sink becomes a refill station
for bottles, buckets and canteens.

Water of two grades mixed in the Sink takes the worse one, so a little Dirty water spoils a tank of Pure
water. Once the Sink is empty, the next water in sets its grade again. A Sink filled before this update
gives Murky water until it runs dry.

Water piped back out of the Sink loses its grade. Fill containers at the Sink by hand.

## Oven

The Oven boils water like a Smoker: a Dirty or Murky bottle, bowl or bucket comes out Clean. Sea water
won't boil in it. Clean is as far as heat goes; for Pure water, use a
[Copper Distiller](/docs/features/water-purity#copper-distiller).

## Kitchen

The cooking table uses water from the Sink for recipes as it always has. Recipes that need water use
it up, so it never reaches a drink.

How grades are worked out is on the [Water Purity](/docs/features/water-purity#the-four-grades) page.

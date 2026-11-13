// Other mods this one works with, and their icons, for the sidebar (config.mts) and the home page's
// "Works with" grid (theme/HomeIntegrations.vue). Icons are linked from each mod's own page, never
// copied into the repo.

export interface Mod {
  name: string
  icon: string
  link: string
  // The wide crate icons of Farmer's Delight and its addons, shown a little bigger.
  large?: boolean
}

// Farmer's Delight and its addons draw their icons inside a wide transparent margin; wsrv.nl crops
// it and pads the icon square again.
const trimmed = (url: string) =>
  `https://wsrv.nl/?url=${encodeURIComponent(url)}&trim=1&w=96&h=96&fit=contain&cbg=00000000&output=png`

// AppleSkin and Jade only show what is already there, and Terralith only adds two swamps to the
// water grade table, so none of them has a page of its own.
export const SHOWS_ONLY: Mod[] = [
  {
    name: 'AppleSkin',
    icon: 'https://cdn.modrinth.com/data/EsAfCjCV/icon.png',
    link: '/docs/features/thirst-and-quenched'
  },
  {
    name: 'Jade',
    icon: 'https://cdn.modrinth.com/data/nvQzSEkH/b04217bc2b7dc524c4d12f81ff42cc1cefb9b0fc_96.webp',
    link: '/docs/features/water-purity#checking-water-with-jade'
  },
  {
    name: 'Terralith',
    icon: 'https://cdn.modrinth.com/data/8oi3bsk5/1959d924a1088944bbf07a06ba523726112d7e7a_96.webp',
    link: '/docs/features/water-purity#the-four-grades'
  }
]

export const INTEGRATIONS: Mod[] = [
  {
    name: 'Create',
    icon: 'https://cdn.modrinth.com/data/LNytGWDc/61d716699bcf1ec42ed4926a9e1c7311be6087e2_96.webp',
    link: '/docs/integrations/create'
  },
  {
    name: 'Sophisticated Backpacks',
    icon: 'https://cdn.modrinth.com/data/TyCTlI4b/e31c7e2f8769d317339e25b2a8d1b40fbf312729_96.webp',
    link: '/docs/integrations/sophisticated-backpacks'
  },
  {
    name: 'Supplementaries',
    icon: 'https://cdn.modrinth.com/data/fFEIiSDQ/e9f5f66fa3b67e54acb91258a1428d68311c58bc_96.webp',
    link: '/docs/integrations/supplementaries'
  },
  {
    name: 'Cold Sweat',
    icon: 'https://cdn.modrinth.com/data/uXhSmPjd/bf55420556c30d44d2f5cf7b8915705b9214b4ef.png',
    link: '/docs/integrations/cold-sweat'
  },
  {
    name: 'Serene Seasons',
    icon: 'https://cdn.modrinth.com/data/e0bNACJD/f8b292ea53e0a0ea908570defddc48673d16d7d6.png',
    link: '/docs/integrations/serene-seasons'
  },
  {
    name: 'Spelunkery',
    icon: 'https://cdn.modrinth.com/data/krskFMfA/465cfcd453c22ee5a09884ede98a0442e97658c5.png',
    link: '/docs/integrations/spelunkery'
  },
  {
    name: "No Man's Land",
    icon: 'https://cdn.modrinth.com/data/kjZCvAn6/958489a1729e9e17a6a5a0728ef249236c07f7b3_96.webp',
    link: '/docs/integrations/no-mans-land'
  },
  {
    name: 'Bountiful Fares',
    icon: 'https://cdn.modrinth.com/data/YgrSDjlb/f4e10611dcc1791138225224fa444f5a8ef4348f_96.webp',
    link: '/docs/integrations/bountiful-fares'
  },
  {
    name: 'Croptopia',
    icon: 'https://media.forgecdn.net/avatars/thumbnails/308/636/64/64/637392485303151332.png',
    link: '/docs/integrations/croptopia'
  },
  {
    name: 'Mekanism',
    icon: 'https://cdn.modrinth.com/data/Ce6I4WUE/ea185eb1300a64867f89101d4798e71a54ef6bed_96.webp',
    link: '/docs/integrations/mekanism'
  },
  {
    name: 'Immersive Engineering',
    icon: 'https://cdn.modrinth.com/data/tIm2nV03/icon.png',
    link: '/docs/integrations/immersive-engineering'
  },
  {
    name: 'Little Logistics',
    icon: 'https://cdn.modrinth.com/data/1zZJrh9c/fdee529567ce17536fcc53177731e28304ece5af_96.webp',
    link: '/docs/integrations/little-logistics'
  },
  {
    name: 'Cooking for Blockheads',
    icon: 'https://cdn.modrinth.com/data/vJnhuDde/811d0c55230d84d9e182df1b0bd631ddebecc0ad_96.webp',
    link: '/docs/integrations/cooking-for-blockheads'
  }
]

export const FARMERS_DELIGHT: Mod = {
  name: "Farmer's Delight",
  icon: trimmed('https://cdn.modrinth.com/data/R2OftAxM/8e7aa38ab94d94bb0a2894a218b69beb49002b34.png'),
  link: '/docs/integrations/farmers-delight/',
  large: true
}

export const DELIGHT_ADDONS: Mod[] = [
  ["Brewin' and Chewin'", 'hIu9KJTT/f7c591a80046859d3d45c04ecbbc54d264483d5e.png', 'brewin-and-chewin'],
  ['Cultural Delights', 'YttyNOFA/d857243f0e7dedd3d7f552c4371326773629e42e.png', 'cultural-delights'],
  ['Fruits Delight', 'g6sbyCTu/4ecc5d554f260b876d21c427aa6c2bdf4457fd5c.png', 'fruits-delight'],
  ["Youkais' Feasts", 'Ohy0TgO2/4e25b7b5de37ad03845970ec8ef72f5c0b8e4617_96.webp', 'youkais-feasts'],
  ["Miner's Delight", 'qMxbM4BQ/0d6f967d3ad184dd296c62a9891e2b2b7d45f61d.png', 'miners-delight'],
  ['Hearth and Harvest', '8EEEXOzj/e5d9aa8bd6bf5dcbd674f08b92957d4b001229e3.png', 'hearth-and-harvest'],
  ["Ocean's Delight", 'DGiq4ZSW/949ba66d6fffb5a984fbb70e3ef4a51f15be3191.png', 'oceans-delight'],
  ['Expanded Delight', 'e9V6wFcR/4cbbace573b20628290929948a77c74d95ed7a70.png', 'expanded-delight'],
  ['Rustic Delight', 'foa4fGIH/eecc99e281522f2291081c48176f0faa84c107bc.png', 'rustic-delight'],
  ['Extra Delight', 'yRrY3XII/cec2396cf9f6f74a5b0cff196301a4b8b8124e1e.png', 'extra-delight']
].map(([name, file, page]) => ({
  name,
  icon: trimmed(`https://cdn.modrinth.com/data/${file}`),
  link: `/docs/integrations/farmers-delight/${page}`,
  large: true
}))

// The Let's Do mods, grouped under Farm & Charm the same way as Farmer's Delight's addons: its addon
// Candlelight, then HerbalBrews and Beachparty, which keep their own pages outside farm-and-charm/.
export const FARM_AND_CHARM: Mod = {
  name: "Let's Do: Farm & Charm",
  icon: 'https://cdn.modrinth.com/data/HJetCzWo/7c6c372629b3efa41409621631d60df12963f005_96.webp',
  link: '/docs/integrations/farm-and-charm/'
}

export const LETS_DO: Mod[] = [
  {
    name: "Let's Do: Candlelight",
    icon: 'https://cdn.modrinth.com/data/qwbArkQk/5e0770c8da0fab82a70bc9c3913c8d3996c53345_96.webp',
    link: '/docs/integrations/farm-and-charm/candlelight'
  },
  {
    name: "Let's Do: HerbalBrews",
    icon: 'https://cdn.modrinth.com/data/Eh11TaTm/cea48ad39e9323e9e0f5354ee1d4c160f46b50be_96.webp',
    link: '/docs/integrations/herbalbrews'
  },
  {
    name: "Let's Do: Beachparty",
    icon: 'https://cdn.modrinth.com/data/GyKzAh3l/41b9b45c365ecd55aced04bcd22af93878f766a0_96.webp',
    link: '/docs/integrations/beachparty'
  },
  {
    name: "Let's Do: Vinery",
    icon: 'https://cdn.modrinth.com/data/1DWmBJVA/029aec55be4d860ba0aede4939dd93332b6dafad_96.webp',
    link: '/docs/integrations/vinery'
  },
  {
    name: "Let's Do: Meadow",
    icon: 'https://cdn.modrinth.com/data/ra7o6Sl7/d7e6f6bcaa82b9af4a670e6dd6cb980c970aa0f2_96.webp',
    link: '/docs/integrations/meadow'
  }
]

// The Kaleidoscope mods, grouped under Kaleidoscope Cookery the same way: its addons Flora and Chinese
// Food in kaleidoscope-cookery/, then Kaleidoscope Tavern, a mod of its own that keeps its page outside.
export const KALEIDOSCOPE_COOKERY: Mod = {
  name: 'Kaleidoscope Cookery',
  icon: 'https://media.forgecdn.net/avatars/thumbnails/1361/462/64/64/638884307253099520.png',
  link: '/docs/integrations/kaleidoscope-cookery/'
}

export const KALEIDOSCOPE_ADDONS: Mod[] = [
  {
    name: 'Kaleidoscope Flora',
    icon: 'https://media.forgecdn.net/avatars/thumbnails/2040/206/64/64/639247202160912813.png',
    link: '/docs/integrations/kaleidoscope-cookery/kaleidoscope-flora'
  },
  {
    name: 'Kaleidoscope Chinese Food',
    icon: 'https://cdn.modrinth.com/data/cuIIkdlx/748a556a3658f0a5f67068f4d8d5cf0041b794a7.png',
    link: '/docs/integrations/kaleidoscope-cookery/kaleidoscope-chinese-food'
  },
  {
    name: 'Kaleidoscope Tavern',
    icon: 'https://cdn.modrinth.com/data/r9RZvhiJ/b96860512d16e72dcaa30b15cb0b9b25c18a38e8.png',
    link: '/docs/integrations/kaleidoscope-tavern'
  }
]

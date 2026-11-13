# tools/agent — agent client scripts

Request files for the agent queue: one JSON request per line, `//` lines are comments, and `expect` /
`checks` beside a request are what `drive.py --verify` asserts afterwards. The protocol, the queue
directories and every command are in
[src/dev/java/com/thirstwastaken2/dev/agent/AGENTS.md](../../src/dev/java/com/thirstwastaken2/dev/agent/AGENTS.md).

```bash
./gradlew ":26.3.x:runClient" -Pagent=tools/agent/gameplay/parched.jsonl -Pquickplay=ParchedAgent
```

```bash
python tools/agent/drive.py run/26.3.x/agent/client tools/agent/gameplay/parched.jsonl --verify
```

## Waiting for a `-Pagent` run

A `-Pagent` run ends by itself: the game closes once the last request is answered, and a run stuck for
300 seconds closes too (`DONE stalled`). So **the Gradle task finishing is the signal**. Run it in the
background, wait for that one process, and read the result afterwards:

```bash
./gradlew ":26.2.x:runClient" -Pagent=tools/agent/shots/recipes.jsonl -Pquickplay=RecipeShots --offline -q > build/agent-run.log 2>&1; echo "exit=$?"; grep -a "ThirstAgent\] DONE" run/26.2.x/logs/latest.log | tail -1
```

```bash
python tools/agent/drive.py run/26.2.x/agent/client tools/agent/shots/recipes.jsonl --verify
```

`DONE script answered` is a finished run; `DONE stalled` or `DONE refused` names what went wrong.
Do not start a second loop that polls `out.jsonl` while the game runs. It has no reliable end: the
previous run's `out.jsonl` already holds the same ids until the queue rotates, and comparing it against
a log that is still being written never comes true. A loop that waits on a file needs both a condition
that this run alone can meet and a deadline.

Every path is relative to the repository root. Each script's header says which nodes it runs on, the
world it needs and how to read the result; read it before running one.

## Layout

| Folder | What is in it |
|---|---|
| `drive.py` | Sends a file or standard input to a queue, waits for the answers, and `--verify`s a finished run |
| `make_gif.py` | Turns a `client.record` folder into a GIF or WebP, drawing the virtual pointer as a cursor. Options in its docstring |
| `new_world.py`, `nbt.py` | Makes the throwaway world a script runs in. See [A world for a script](#a-world-for-a-script) |
| `clean.py` | Deletes the worlds and captures a run left behind and the next run makes again. See [Cleaning up](#cleaning-up) |
| `smoke/` | Checks with no gameplay: `boot.jsonl` (a client comes up and stays up, with `-PwithoutOptional`) and `server-probe.jsonl` (a dedicated server's queue answers with nobody online) |
| `ui/` | The client's own drawing and screens: `hud-layout.jsonl` (thirst against food and air), `hud-hidden.jsonl` (F1), `hud-death-screen.jsonl`, `config-screen.jsonl`, and `config-showcase.jsonl`, which records the config screen for a GIF rather than checking it |
| `gameplay/` | The mod's own mechanics in a real client: `client-sync.jsonl`, `parched.jsonl`, `loot-and-boil.jsonl`, `waterskin-stack.jsonl`, `hanging-pot.jsonl`, `canteen.jsonl`, `distiller.jsonl`, `finite-water.jsonl` (and `-1.20.1`) |
| `integrations/` | One script per optional mod: `create-water.jsonl` (and `-1.20.1` for Forge), `pipesnphysics-water.jsonl` (Create with Create Pipes n Physics, both `runClientMod` lines uncommented), `createfly-waterskin.jsonl`, `farmers-delight.jsonl` (and `-1.20.1`, in NBT), `kaleidoscope-cookery.jsonl`, `kaleidoscope-chinese-food.jsonl` (the addon's jar dropped into the run's `mods` by hand) and `kaleidoscope-chinese-food-tap.jsonl` (its tap into the stockpot and teapot, with its `runClientMod` line uncommented), `kaleidoscope-tavern.jsonl` (and `-1.20.1`), `supplementaries.jsonl`, `brewin-and-chewin.jsonl`, `cold-sweat.jsonl`, `cultural-delights.jsonl`, `fruits-delight.jsonl`, `expanded-delight.jsonl`, `serene-seasons.jsonl`, `farm-and-charm.jsonl` (Candlelight too), `herbalbrews.jsonl`, `beachparty.jsonl`, `meadow.jsonl`, `vinery.jsonl`, `croptopia.jsonl`, `hearth-and-harvest.jsonl` (and `-1.20.1` for Forge), `miners-delight.jsonl` (and `-1.20.1` for Forge, and `miners-delight-create-1.20.1.jsonl` for its Create recipes), `no-mans-land.jsonl`, `mekanism-water.jsonl` (and `-1.20.1` for Forge), `immersive-engineering.jsonl` (and `-1.20.1` for Forge), `cooking-for-blockheads.jsonl` |
| `shots/` | Scenes for the docs images rather than checks: `hanging-pot.jsonl`, `distiller.jsonl`, `supplementaries-faucet.jsonl`, and `recipes.jsonl` with `recipe_images.py`, which turns its captures into the recipe images. The capture-screenshots skill says which image each one makes |
| `integrations/sophisticated/` | Sophisticated Backpacks and Storage, one script per upgrade, and `sophisticated-pack/`, the data pack of backpack templates those scripts give out |

## A world for a script

A client script runs in a throwaway world, made fresh for each run:

```bash
python tools/agent/new_world.py 1.21.1 BrewinAgent
```

By default the world is a **sky floor**: a superflat overworld with one layer of stone at y 199 and
nothing below it, one biome (plains, or `--biome <id>`), no structures. The player arrives standing on
it at y 200, at noon, in clear weather, with no mobs spawning. Most scripts build at y 199 to 206, so
their scene sits on the floor. Nothing is under it to fall into, no powder snow or cave is near, and
chunks generate in a fraction of the time and disk real terrain takes. `fillbiome` still sets a biome
where a script needs one.

Pass **`--terrain`** only when the script needs real ground: docs screenshots (`shots/`, the
config showcase), natural biomes, heights or caves. That world keeps the generator, seed and spawn point
of another world of the node, and the player arrives on that spawn point, which may be powder snow: a
terrain script teleports first.

Either way it leaves out what a copied `level.dat` used to bring along: the old player, who arrived
mid air or under water and died during the opening wait. Difficulty is left alone, since Peaceful
refills thirst. `--datapack <folder>` adds a data pack; the tool's docstring has the rest. `nbt.py` is
the NBT reader and writer it uses, with no dependency.

So a script needs no `kill` and `client.respawn` at the start; `client.state` expecting
`result.alive` is enough to show the world came up.

Use the world name the script's header gives, every time. A new name for each attempt (`PotAgentTest`,
`PotAgentTest2`) leaves one more 20 MB world per attempt that nothing ever opens again. A world made by
hand, or copied, instead of through `new_world.py` has no `.agent-world` file, so `clean.py` will not
know it is throwaway.

## Cleaning up

`run/` grew to 6 GB once, nearly all of it worlds and frames nobody would open again. A world is
throwaway when a run makes it again; delete those, keep the rest.

```bash
python tools/agent/clean.py 26.3.x --world ParchedAgent
```

once a script has run and its result has been read: `--verify` passed, or the captures you needed were
copied out. The world goes; the queue's `out.jsonl` stays as the record.

```bash
python tools/agent/clean.py --dry-run
```

for a sweep of every node, or name the nodes. `--screenshots` adds the queues' `screenshots/` folders.
The script's docstring says what it touches.

| Delete | Why it is safe |
|---|---|
| A world in `saves/` holding `.agent-world` | `new_world.py` made it and makes it again from nothing on the next run |
| `run/<node>/gametest/world` | `runGametest` deletes it and makes it again on every run; one left from a run is only disk |
| `agent/<queue>/screenshots/`, `client.record` frames | Once the keepers are copied to `docs/public/screenshots/` or a GIF is made, the next capture writes them again. A recording is hundreds of PNGs |

| Keep | Why |
|---|---|
| `New World`, and any world without `.agent-world` | `new_world.py` copies its seed and spawn point, so deleting the last one of a node stops every script there |
| A world staged by hand for a docs screenshot (`Sand Filter Test`, `RecipeShot`) | Building the scene again is the expensive part. Name it for what it shows, not `…Agent`, so it is not taken for a throwaway. A scene a script builds with commands in a `new_world.py` world (`PotShots`) is throwaway like any other |
| The queue's `in.jsonl`, `out.jsonl`, `previous-*` | Small, and the only record of what the last run did |
| A world a person is testing in | Ask before deleting a world you did not make in this session |

`clean.py` lists the unmarked worlds it kept, with their size. Some are throwaway worlds from before
`new_world.py` marked them (`ParchedAgent` on several nodes): delete one only after checking that the
script that uses it calls `new_world.py`, or ask.

## Adding a script

- Put it in the folder its subject belongs to. A new optional mod gets its script in
  `integrations/`, or a folder of its own there once it has more than one.
- Start with a header: what it proves, the nodes it needs, the world (`-Pquickplay=<world>` and the
  `new_world.py` line that makes it), and what to read afterwards when the file cannot say it with `expect` and `checks`.
- Reference it from the `AGENTS.md` or `docs/dev` page of the area it checks, by its full
  `tools/agent/<folder>/<name>.jsonl` path, so a move shows up in a search.
- Prefer a gametest when the check needs no client and no real time. A script is for what
  [src/gametest](../../src/gametest/java/AGENTS.md) cannot reach: rendering, screens, the real right
  click, other mods, and the client's view of synced values.
- Delete a script once a gametest covers the same thing, and remove its references with it.

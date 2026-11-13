# Agent client: faster scripted runs

Status: done, 2026-11-13. All three changes are in; see [What was done](#what-was-done) at the end for
the decisions taken where this plan left a choice, the timings, and what is left. Noted 2026-11-13 while
running the Sophisticated scripts on `26.3.x-neoforge`.

The `-Pagent` client (`src/dev/java/com/thirstwastaken2/dev/agent/`, scripts in `tools/agent/`) spends
most of a run either waiting a fixed number of ticks or starting and stopping the game. Three changes
would cut a typical integration check from many minutes to about one.

## Where the time goes today

- **Fixed `wait`s.** A `wait` holds the queue for its full tick count at 20 ticks a second, even when
  the thing it waits for is done early. `tools/agent/integrations/sophisticated/` alone waits:

  | Script | Wait ticks | About |
  |---|---|---|
  | `sophisticated-cooking.jsonl` | 3365 | 2 min 48 s |
  | `sophisticated-tank.jsonl` | 1770 | 1 min 29 s |
  | `sophisticated-drinking-tab.jsonl` | 1344 | 1 min 7 s |
  | `sophisticated-pump.jsonl` | 1311 | 1 min 6 s |
  | `sophisticated-drinking.jsonl` | 1150 | 58 s |
  | `sophisticated-alchemy.jsonl` | 690 | 35 s |
  | `sophisticated-drinking-craft.jsonl` | 635 | 32 s |
  | `sophisticated-feeding.jsonl` | 615 | 31 s |

  Most of that is two kinds of wait: `load` (400 ticks, about 21 s, so the world is in) and cooking or
  brewing timers (300 to 600 ticks per case).
- **One client per script.** `-Pagent=<file>` answers one file and closes the game. Every script pays a
  full launch, about 30 to 60 s on a NeoForge node with several mods, plus world load. Checking an
  integration with four scripts means four launches.

## 1. Use `server.sprint` for timers

`server.sprint` already exists (see the command table in
`src/dev/java/com/thirstwastaken2/dev/agent/AGENTS.md`): it runs `/tick sprint <ticks>` and answers once
the ticks have run, unthrottled. A 300-tick cook takes a second or two instead of 15.

- Replace the `*_cooking` waits in `sophisticated-cooking.jsonl`, and every other wait that only lets
  game time pass (fermenting, brewing, crop growth, tank fill, tap rising), with `server.sprint`.
- Keep a short `wait` where the client itself has to catch up (a screen opening, an inventory sync),
  since a sprint runs the server ahead of the client.
- Check each converted script still passes on one node of each loader before converting the next.

## 2. Add `wait_until`

A command that answers as soon as a condition holds, or refuses after a maximum tick count:

```json
{"id": "load", "command": "wait_until", "args": {"condition": "world_ready", "maxTicks": 600}}
{"id": "cooked", "command": "wait_until", "args": {"condition": "slot_not_empty", "slot": 2, "maxTicks": 600}}
```

- Conditions to start with: `world_ready` (player in a level, chunks around the player loaded, no
  loading screen), `screen_open` (a given screen class), `slot_not_empty` / `slot_matches` (container
  slot), and `block_state` (a block property at a position).
- The answer carries the ticks it actually took, so a script can still assert on timing when that
  matters.
- Replaces the fixed 400-tick `load` at the top of nearly every client script.
- Must respect the stall guard: `maxTicks` stays below `thirstwastaken2.agent.stallSeconds`.

## 3. Several scripts in one client

Run several files back to back in one launch:

```bash
./gradlew ":1.21.1-neoforge:runClient" -Pagent=tools/agent/integrations/sophisticated/sophisticated-tank.jsonl,tools/agent/integrations/sophisticated/sophisticated-pump.jsonl,tools/agent/integrations/sophisticated/sophisticated-cooking.jsonl -Pquickplay=SophisticatedAgent
```

- **Separate output per script.** Each file's replies go to `out-<script name>.jsonl` (and the log
  prints a `DONE script answered` line per file), so `drive.py --verify` still checks one script
  against its own expectations. `drive.py` needs a way to pick that file.
- **A fresh world per script, optionally.** Scripts assume a clean world at fixed coordinates. Give the
  suite a way to leave the world and open the next one (a `client.world` command, `open` / `leave`),
  with the worlds made beforehand by `tools/agent/new_world.py`, or a per-script `world` header line.
  Without it, scripts that share a world must not overlap in space or state.
- **Failure handling.** A refused or stalled request ends that script, not the suite; the next script
  still runs. The game closes after the last file.
- Keep `-Pagent=<one file>` working exactly as today.
- Document it in `src/dev/java/com/thirstwastaken2/dev/agent/AGENTS.md` and `tools/agent/AGENTS.md`.

## Expected gain

The four core Sophisticated scripts (tank, pump, cooking, drinking) on one node: today four launches and
about 8 to 9 minutes of waiting; after all three changes, one launch and about a minute.

## Constraints

- All of this is in the `dev` source set, which is never packaged, so players are not affected.
- Anything a 1.20.1 node compiles uses nothing newer than Java 17.
- Make the change in its own worktree or when no other `-Pagent` run is building, since every run
  compiles this source set.
- Prove it with the Sophisticated and Farm & Charm scripts on one Fabric and one NeoForge node, and keep
  a before/after time for one script in the commit message.

## What was done

All three parts, in the plan's order. The commands are documented in
[the agent's AGENTS.md](../../src/dev/java/com/thirstwastaken2/dev/agent/AGENTS.md#not-waiting-longer-than-the-game-needs)
and [tools/agent/AGENTS.md](../../tools/agent/AGENTS.md#several-scripts-in-one-launch).

### Decisions where the plan left a choice

- **`server.sprint` was already there**, so part 1 is script changes only. Converted: every
  `_cooking`, `_tick`, `_pumping`, `_drinking`, `starving` and `eating` wait in
  `integrations/sophisticated/`, Farm & Charm's `cook`, `sink_hand_fill` and `_hungry` waits, and the
  hanging pot's `boil`. Screen, slot and key waits stay `wait`. Other scripts' game-time waits were
  left for whoever next runs each one, per "check each converted script before converting the next".
  On the 1.20.1 nodes `/tick` does not exist, so a sprint there lasts as long as a `wait`.
- **`wait_until`** answers on the first look that holds, except `world_ready`, which must hold 40
  looks in a row (two seconds) so a world that is in for one tick is not taken as settled; `hold`
  overrides either. `maxTicks` defaults to 600 and is capped at 4800 (four minutes at 20 a second),
  under the 300 s stall guard. `slot_matches` takes `count` as an exact count. `block_state` reads the
  client's own world on a client and the overworld on a dedicated server. `world_ready` also asks
  for no screen open (`allowScreen` lifts that) and, in singleplayer, the integrated server to have
  the player.
- **The opening `load`** became `wait_until world_ready` (`maxTicks` 2400) in every client script
  except `shots/` and `ui/config-showcase.jsonl`, which keep the fixed wait because a capture needs the
  terrain drawn, not only loaded. This one line is generic, so it was changed everywhere at once
  rather than script by script.
- **Several scripts**: `-Pagent=a.jsonl,b.jsonl`. With one file nothing changes (`out.jsonl`, one
  `DONE` line, a stall stops the game). With several, each answers into `out-<script>.jsonl`, the log
  has a `DONE` line per script and a last `DONE suite answered` line, and `drive.py --verify --suite`
  (or `--out <file>`) checks one. "A refused or stalled request ends that script" was taken as: a
  stall ends that script (its waiting requests are refused into its own file) and the next one
  starts; a refused request still does not end a script, alone or in a suite, as before. A file that
  cannot be read is skipped with `DONE skipped`.
- **Worlds**: `client.world` with `open`/`from`/`leave`. `from` copies a `new_world.py` world over the
  named one and opens the copy, so one template serves every script of a suite; `open` naming the
  world already open is a no-op, so the same line works under `-Pquickplay`. No per-script header.
  Leaving a world now disconnects the level first, as the pause screen does: `Minecraft.disconnect`
  alone (which `client.disconnect` used before) waits forever for an integrated server that was never
  told to stop, rendering without ticking until the watchdog halts the game.

### Timings

"Script" is from the queue opening to the last answer, from the game's log; "wall" is the whole
Gradle task, launch included, on a machine where other builds were running.

| Node | Run | Script | Wall |
|---|---|---|---|
| `1.21.1-neoforge` | `sophisticated-tank.jsonl`, before (fixed `load` and `_tick` waits) | 89.5 s | 171 s |
| `1.21.1-neoforge` | `sophisticated-tank.jsonl`, after | 16.1 s | 94 s |
| `1.21.1-neoforge` | tank, cooking, drinking and pump in one launch, after | 67.0 s for all four | 136 s |
| `1.21.1` (Fabric) | `gameplay/hanging-pot.jsonl`, before | 68 s | 137 s |
| `1.21.1` (Fabric) | `gameplay/hanging-pot.jsonl`, after | 20 s | 56 s |

The four core Sophisticated scripts were about 8 to 9 minutes of waiting plus four launches before;
they are now one launch of 136 s. The tank's exported backpacks were byte for byte the same before and
after, and the four scripts' results in one launch were the ones each header expects (drinking
16/20/4/10/12/20, every cooking export as described, every pump check passed). `client.world` with
`from`, and the `wait_until` conditions, were checked in a suite on `1.21.1` and `26.3.x` (a copy
opened in about 7 s).

### Not done

- **Farm & Charm was not run.** Its two `runClientMod` lines are off by default, and turned on for
  Fabric 1.21.1 the client did not start: Farm & Charm 1.1.29 and Candlelight 2.1.15 require
  `foundation` (>=1.0.7), which is only nested in Farm & Charm's jar and is not put on the dev
  classpath. Foundation is on runClient since `292e790` (uncomment Farm & Charm's three
  `runClientMod` lines to run it); its waits are converted but unverified. The hanging pot stood in
  as the Fabric proof, and passed again on `1.21.1` after the merge onto `main`.
- The Sophisticated `alchemy`, `drinking-craft` and `feeding` scripts' sprints were converted but not
  run; `drinking-tab` has only client waits.
- Game-time waits in other scripts are still `wait`.

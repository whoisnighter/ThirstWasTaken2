# Agent client: faster scripted runs

Status: planned, not started. Noted 2026-11-13 while running the Sophisticated scripts on
`26.3.x-neoforge`.

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

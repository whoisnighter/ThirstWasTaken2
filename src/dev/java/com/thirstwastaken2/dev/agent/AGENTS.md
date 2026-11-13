# The agent client

A way for an agent to drive a real Minecraft client and read real numbers out of it, in place of
counting droplets in a screenshot. It exists for the checks in
[docs/dev/MANUAL-TESTING.md](../../../../../../../docs/dev/MANUAL-TESTING.md) that a gametest cannot reach because
they are client side: the HUD, what a client is told, the config screen. What used to be read off a
screenshot is read as a value the client already holds: `/thirst set` reaching the client is the
number the client reports, the sprint gate is `LocalPlayer.isSprinting()` after the key has been
held, the bar's place above the hunger bar is the rectangles the mod drew, and the quenched outline's
colour is a framebuffer sample at a point those rectangles name. A screenshot stays useful as
evidence for a person; it is never the assertion.

It is on every node, Fabric and NeoForge, on every run task except `runGametest` and `runDatagen`.
The two extra clients, `runManualA` and `runManualB`, are on the NeoForge nodes only.

`core/` is the queue, the reply envelope and the dispatch loop, in plain Java and Gson; `thirst/` is
everything that names Minecraft, a loader or the mod. [src/dev/java/AGENTS.md](../../../../AGENTS.md)
is the source set this belongs to, and holds the rules every tool here follows and the harness this
one shares with `/thirst benchmark`.

## The queue

Two text files in a directory, one line of JSON each way. No port, no firewall prompt, and the whole
exchange is still on disk to read afterwards.

    run/<node>/agent/<name>/
        ready.json    written once the game will answer; holds startedAt, pid and the command list
        in.jsonl      the agent appends one request a line
        out.jsonl     the game appends one reply a line
        out-<script>.jsonl   each script's replies, when one launch ran several (see Several scripts in one launch)
        previous-*    the run before this one, kept rather than overwritten
        screenshots/  what client.capture wrote, and a folder per client.record recording

`<name>` is `server` or `client` by default and `A` or `B` on the two extra clients, so a server and
a client of the same node never share a file. One queue per process: a client with an integrated
server is one process and has one queue, polled on the client tick, and `server.*` reaches its
integrated server from there.

A request is `{"id": "...", "command": "...", "args": {...}}`; `id` is echoed back and may be left
out, in which case the position in the queue stands in for it. A reply is

```json
{"id":"set","sequence":3,"command":"server.thirst.set","ok":true,"result":{...},"tookMs":1.1}
```

with `error` instead of `result` when `ok` is false. **Requests are answered in order, one at a
time.** A command that takes ticks to finish — `wait`, `wait_until`, `server.sprint`, `client.hold`,
`client.capture`, `client.slot`, `client.respawn`, `client.world` — holds the next request up until it has
answered, so a file of requests is a sequence rather than a batch of things that all happen in one tick.

`drive.py` also accepts an `expect` object keyed by dotted reply paths, plus relational `checks`.
Expectations use exact JSON equality; a mismatch is printed as `assertionErrors` and exits 1. An
expected `null` passes when the path is absent as well as when it is null, because Gson leaves a null
property out of the reply: that is how "no screen is open" is written. A check has `left`, `op`, and
either another reply path in `right` or a literal in `value`. The operations are `eq`, `ne`, `lt`,
`le`, `gt`, `ge`, `contains`, `not_contains`, and `within` (with `tolerance`); `contains` on a list of
messages is membership, not a substring, so a whole message goes in `value`.

`client.hud` includes numeric rectangles from the real thirst, food and air draw calls.
`client.hud.toggle` changes the same vanilla hidden-HUD state as F1, so a script can assert both the
flag and that the thirst row's last-draw age stops refreshing, then toggle it back.

A dedicated server's queue is polled on the server tick, and a dedicated server with nobody online
stops ticking after `pause-when-empty-seconds`, which would kill the queue about a minute after
startup — before an agent has had time to bring a client up. `ServerAwake`, in the source set's
[shared harness](../../../../AGENTS.md#the-shared-harness), holds that pause off for as long as the
queue is open. A client's integrated server is left to pause on the player's own rules.

**Wait for this run's `ready.json`, not for the file.** The previous run leaves one behind, and a
game takes most of a minute to come up; a request written into `in.jsonl` before the game opens the
queue is rotated into `previous-in.jsonl` and never answered. `startedAt` is there to tell the two
apart.

## Driving it

Appending a line and reading `out.jsonl` is the whole protocol, so an agent with only file tools
needs nothing else. [tools/agent/drive.py](../../../../../../../tools/agent/drive.py) does the matching up.
The request files beside it are sorted into folders; [tools/agent/AGENTS.md](../../../../../../../tools/agent/AGENTS.md)
says which holds what:

```bash
python tools/agent/drive.py run/1.21.1-neoforge/agent/server tools/agent/smoke/server-probe.jsonl
```

```bash
echo '{"command": "client.state"}' | python tools/agent/drive.py run/manual-1.21.1-neoforge-A/agent/A -
```

It waits for each answer, prints them in the order asked, and exits 1 if any was refused. Pass
`--ready <seconds>` when the game is still starting: it then waits for a `ready.json` newer than the
moment the command began. Requests can be read from a file or from standard input, and a line
starting with `//` is a comment.

Unattended, without an agent at all:

```bash
./gradlew ":1.21.1-neoforge:runServer" -Pagent=tools/agent/smoke/server-probe.jsonl
```

`-Pagent=<file>` answers that file once the game is up and then stops the game. The path is relative
to the repository root. It applies to every run task of the node, clients included. Nothing in the
game reads the `expect` and `checks` lines beside the requests, so a run like this is checked
afterwards against what it recorded:

```bash
python tools/agent/drive.py run/1.21.1-neoforge/agent/server tools/agent/smoke/server-probe.jsonl --verify
```

A run that crashed before it opened the queue leaves the previous run's files in place, and `--verify`
would pass on them. `--max-age <minutes>` refuses a `ready.json` older than that; use it whenever the
question is whether the run started at all.

A client script that needs a world gets one with `-Pquickplay=<world>`, which opens that singleplayer
world of `run/<node>/saves` straight from launch; server commands then reach its integrated server.
[tools/agent/gameplay/hanging-pot.jsonl](../../../../../../../tools/agent/gameplay/hanging-pot.jsonl) runs that way, in a
throwaway world made by `tools/agent/new_world.py` (see [tools/agent/AGENTS.md](../../../../../../../tools/agent/AGENTS.md#a-world-for-a-script)).

### Not waiting longer than the game needs

A `wait` holds the queue for its whole count at 20 ticks a second, whatever it waits for. Two commands
take its place wherever the thing waited for is not the client catching up
([docs/plans/AGENT-CLIENT-SPEED.md](../../../../../../../docs/plans/AGENT-CLIENT-SPEED.md) has the measurements):

- **`wait_until`** answers as soon as a condition holds, with the `ticks` it took, and refuses after
  `maxTicks` (600 by default, at most 4800) with what it last saw. A client script opens with
  `{"id": "load", "command": "wait_until", "args": {"condition": "world_ready", "maxTicks": 2400}}`
  rather than a 400-tick `wait`: the world is in when a player stands in a level with no screen open,
  the chunks within `radius` (2) of them are loaded and, in singleplayer, the integrated server has the
  player. `world_ready` has to hold for 40 looks in a row (`hold`), two seconds, so the first tick of a
  world is not taken for a settled one; the other conditions answer on the first look that holds.
- **`server.sprint`** runs game time unthrottled: a 300-tick cook takes a second or two instead of 15.
  Use it for what only time does on the server (cooking, brewing, fermenting, crops, a tank or a pump
  filling, an upgrade ticking in the player's inventory), never for the client catching up. A sprint
  runs the server ahead of a client that still ticks at 20 a second, so a screen opening, an inventory
  syncing or a key held keeps a short `wait`. `/tick` came in 1.20.3, so on the 1.20.1 nodes the command
  goes unanswered by the server and `server.sprint` lasts as long as a `wait` of the same ticks would.

### Several scripts in one launch

Every launch costs 30 to 60 seconds before the first request, more on a NeoForge node with several
mods. `-Pagent` takes several files, comma separated, and answers them one after another in one game:

```bash
./gradlew ":1.21.1-neoforge:runClient" -Pagent=tools/agent/integrations/sophisticated/sophisticated-tank.jsonl,tools/agent/integrations/sophisticated/sophisticated-pump.jsonl -Pquickplay=SophisticatedAgent
```

- **One answer file per script.** Each script answers into `out-<script>.jsonl`, named after the file
  without `.jsonl` (a second file of the same name gets `-2`), so two scripts may share ids such as
  `load`. `out.jsonl` keeps whatever is written to `in.jsonl` meanwhile. Check one with
  `drive.py <queue> <script> --verify --suite`, or name the file with `--out`.
- **The log.** Each script ends with `[ThirstAgent] DONE script answered (2/4), script=…, out=…` and the
  run with `[ThirstAgent] DONE suite answered: 4 scripts, 0 stalled or skipped`, so the last `DONE` line
  is still the run's result. A file that cannot be read is skipped with `DONE skipped (3/4)`. Pass
  absolute paths as `C:/...` from Git Bash with `MSYS2_ARG_CONV_EXCL='*'`, or MSYS rewrites a
  comma-separated list of them into nonsense.
- **A stalled script ends that script, not the suite.** After `stallSeconds` without an answer the
  request it was stuck on is refused, the rest of it is refused into its own file, `DONE stalled (2/4)`
  is logged and the next script starts. A refused request does not end a script, alone or in a suite,
  exactly as before. A frozen game is still halted by the watchdog, which ends the run.
- **Worlds.** Scripts that run in one world share it, so they must not build over each other or leave
  state the next one trips on (an inventory, a block entity, a biome). One that needs a world of its own
  opens one with `client.world`: `{"open": "TankAgent", "from": "SophisticatedAgent"}` leaves the
  world, copies `saves/SophisticatedAgent` over `saves/TankAgent` and opens the copy, answering once it
  is `world_ready`. The template, made by `new_world.py` and never opened, stays fresh for the next
  one. `{"open": "<world>"}` without `from` naming the world already open changes nothing, so a script
  carrying that line runs on its own under `-Pquickplay=<world>` exactly as before.
- **`-Pagent=<one file>` is unchanged**: `out.jsonl`, one `DONE` line, and a stall stops the game.

### One game per queue, and an unattended run always ends

- **A queue belongs to one game.** The game that opens `run/<node>/agent/<name>/` holds an operating
  system lock on `queue.lock` in it for as long as it lives. A second game of the same node started
  meanwhile leaves the files alone, logs who owns them (the pid from their `ready.json`), and with
  `-Pagent` logs `DONE refused` and closes. Without it, the second game rotated the first one's
  `out.jsonl` away and both appended to the new one, so every answer of both runs was mixed. The usual
  way to get there is killing a `runClient` Gradle task: that does not always close the game window,
  which then runs its script to the end. The system drops the lock when the process ends, however it
  ends. Two games of one node at once need `-Dthirstwastaken2.agent=<name>` on one of them.
- **`stop` jumps a running script.** Written to `in.jsonl` while a `-Pagent` script runs, it is answered
  on the poll that reads it rather than after the script's last line. Otherwise it keeps its place:
  `drive.py` writes a whole file at once, and one ending in `stop` means after the rest.
- **A stalled run stops.** With `-Pagent`, when requests are waiting and none has been answered for 300
  seconds (`-Dthirstwastaken2.agent.stallSeconds`), the rest is refused as stalled, the log says
  `DONE stalled` with the request it was stuck on, and the game closes. The longest request a script
  can make is a one minute `wait` or a four minute `wait_until`; a long `server.sprint` tells the
  check it is moving as it goes. In a launch of several scripts a stall ends that script, not the run:
  see [Several scripts in one launch](#several-scripts-in-one-launch).
- **A frozen game is halted.** A game thread that stops ticking cannot run that check, so a watchdog
  thread halts the process after 300 seconds without a tick (`-Dthirstwastaken2.agent.frozenSeconds`),
  logging where the game thread was stuck and writing a `watchdog` line to `out.jsonl`. Generating a
  world renders without ticking for a while, which the limit leaves plenty of room for.
- **The window title says what the script is doing** on a driven client, once a second: the request
  it is answering, the ticks it still waits, and how far through the script it is, such as
  `ThirstWasTaken2 agent: brewing3 (wait), 1140 ticks left, 152/187`. A long `wait` looks exactly like a
  hung game to anyone glancing at the window; the title is how to tell. It never reaches a capture.

## Driving a client while the machine is in use

A client that is being driven is not being played, and four things a played client does get in the way
of using the desktop it opened on, or of the run going anywhere at all. `-Pdriven` turns them around:

```bash
./gradlew ":26.1.x-neoforge:runManualA" -Pdriven
```

- **It never takes the mouse pointer.** Minecraft grabs the cursor as soon as a window with a world
  open is focused, and holds it inside the frame until a screen opens, so clicking the window to
  glance at the bar costs the pointer everywhere else on the desktop. A driven client refuses the
  grab outright, which costs nothing: every key the agent holds goes through the game's own key state
  and every command through the player's connection, so nothing here was ever steered by a physical
  mouse. Refusing it rather than releasing it a tick later is what keeps the cursor from being hidden
  and warped to the middle of the window on every click.
- **It opens maximised**, rather than at the small size the run tasks ask for, which is what makes the
  HUD worth looking at while a script drives it. Maximised and not full screen, on purpose: exclusive
  full screen takes over the display the person is working on.
- **It passes vanilla's experimental settings prompt.** "Worlds using Experimental Settings are not
  supported" stands between `-Pquickplay` and the world whenever a mod on the classpath turns a feature
  flag on, which the Supplementaries and Moonlight Lib clients do, so an unattended run waits on a
  button nobody is there to press. A driven client presses "I know what I'm doing!" for itself and
  writes a line to the log; a played one still gets to read it. `ClientWindow.passWorldPrompt`, on the
  client tick while no world is loaded. It finds the button by its message rather than by naming the
  screen, so a version that moves that screen leaves the prompt standing rather than failing to build.
- **On NeoForge it passes the "warnings while loading mods" screen** when no warning on it is this
  mod's, as if the button had been pressed, and logs each warning it passes over. Other mods' warnings
  (a deprecated `logoFile` in Cloth Config, AppleSkin's old translation key) otherwise stop a
  `-Pquickplay` or `-Pagent` client before the world opens. It stays on the screen for an error, for a
  warning about this mod, and for a warning that names no mod. `LoadingWarnings` in
  `src/dev/neoforge`; the screen keeps its issues private, so it reads them by reflection.
- **An unattended NeoForge run that fails to load stops**, non-zero, rather than sitting on the error
  screen with the Gradle task open. That is what a client crashing without an optional mod looks like
  (`tools/agent/smoke/boot.jsonl` exists for it). NeoForge posts no events until loading has finished, so
  this one is a mixin, `LoadingErrorScreenMixin`, in a NeoForge dev config of its own; a Fabric client
  exits on a loading crash by itself.

`-Pagent=<file>` implies `-Pdriven`, because an unattended run has nobody at the keyboard. It is off
by default otherwise, because it is the opposite of what a manual pass needs: with no grab there is
no mouse look and no click reaches the world, so a person — or computer use standing in for one —
cannot play the client at all. `client.info` answers `driven` and `mouseGrabbed`, so a script can
tell which kind of client it is talking to rather than assuming.

It is client side only. `-Pdriven` on `runServer` does nothing, and passing it to every task of a
node is harmless.

## The commands

`ready.json` lists what the process it belongs to answers; a server answers the first two groups and
a client answers all three.

| Command | Arguments | Answers |
|---|---|---|
| `probe` | | side, loader, Minecraft version, run directory, queue, whether a server is running, the command list |
| `wait` | `ticks` | after that many game ticks have run. The way to let the game catch up |
| `wait_until` | `condition`, `maxTicks`, `hold`, and the condition's own | as soon as the condition has held for `hold` looks in a row, with the `ticks` it took and what it `seen`; refused after `maxTicks`. Both sides: `block_state` (`x`, `y`, `z`, `block`, `properties`; the client's world on a client, the overworld on a dedicated server). Client only: `world_ready` (`radius`, `allowScreen`), `screen_open` (`screen`: a class name, simple or full, `any` or `none`), `slot_not_empty` and `slot_matches` (`slot` or `inventory` as `client.slot` takes them, `item`, `count`) of the open menu, or the player's inventory with none open |
| `stop` | | after stopping this process: halting a server, closing a client's window. Answered ahead of a running `-Pagent` script |
| `server.info` | | dedicated, tick count, players, difficulty, levels |
| `server.players` | | every online player's thirst, position, health, food and flags |
| `server.thirst.get` | `player` | the same, for one player |
| `server.thirst.set` | `player`, `thirst`, `quenched`, `exhaustion`, `enabled` | what it was and what it is now. Writes the state directly, not through `/thirst set` |
| `server.thirst.modifier` | `player`, which may be left out when one player is online | the same, plus `modifier`: what the drain multiplies exhaustion by for them now (climate, Fire Resistance, Fire Protection). Reused for a second, so `wait` 25 ticks after a move |
| `server.command` | `command`, `as` | what the command returned and what it said, collected rather than logged |
| `server.sprint` | `ticks` | after `/tick sprint <ticks>` has run every tick: `ran`, the wall-clock `seconds` it took. For what only time does, a keg fermenting or a crop growing; the same ticks run the same code, without the sleep. Use it rather than a sprint and a fixed `wait`, which waits the full time anyway since the client ticks at 20 a second |
| `client.info` | | window and GUI size, GUI scale, fps, screen, server, player, key names, `toggleCrouch`, `toggleSprint`, and whether this client is `driven` and `mouseGrabbed` |
| `client.state` | | what this client holds: thirst, sprinting, sneaking, health, food, dimension, position, whether the bar should render |
| `client.hud` | | the rectangle the mod drew the bar in, the values it drew, the ten droplet rectangles, and the config preview's |
| `client.hud.toggle` | | the new hidden state after toggling the same vanilla state as F1 |
| `client.capture` | `name` | a PNG beside the queue, once it is on disk |
| `client.pixels` | `points`, `space`, `capture`, `name` | the framebuffer colour at each point, in GUI pixels by default |
| `client.command` | `command` | after sending it through the player's own connection |
| `client.chat` | `message` | after sending it |
| `client.hold` | `keys`, `ticks` | the movement state before and after holding those keys for that long |
| `client.key` | `key`, `down` | after setting one key's state and leaving it there |
| `client.screen` | `open` (`none`, `config`, `inventory`) | which screen is open now |
| `client.tooltip` | `item`, `count`, `slot`, `advanced` | the tooltip lines that item produced, as text, with their colours. `slot` reads the stack the player holds (`mainhand`, `offhand` or an inventory index) instead of building one from an id, which is the only way to ask about a stack's components |
| `client.click` | `x`, `y`, `button`, `from` (`centre`, `corner`, `top`, `bottom`) | after pressing and releasing a mouse button on the open screen: which child was under the point and whether the press was taken. Without `x` and `y`, where the virtual pointer is |
| `client.scroll` | `x`, `y`, `from` as `client.click`, `amount` (wheel notches, positive down the page) | after turning the wheel over the open screen: whether the screen took it. The config screen's pages scroll one row a notch |
| `client.mouse` | `x`, `y`, `from` as `client.click`, `ticks`, `drag`, `hide` | after showing the virtual pointer and gliding it there over `ticks`, dragging with the left button held when `drag` is true: where it is and which child is under it. See [Recording](#recording-a-screen) |
| `client.record` | `action` (`start`, `stop`), `name`, `every` (ticks a frame, 2), `downscale` (2) | `start` at once; `stop` once every frame is on disk, with the frame count and how many `failed` |
| `client.slots` | | the open menu's slots that hold something, with their class and player inventory index, and what the cursor carries |
| `client.slot` | `slot` or `inventory`, `button`, `action` (`pickup`, `quick_move`, …) | the same, a few ticks after clicking that slot through the game mode |
| `client.language` | `code` | the language now selected, after loading its translations again |
| `client.textWidth` | `keys`, `texts` | how wide the game's font draws each, in GUI pixels, keys translated in the current language |
| `client.respawn` | | after pressing the death screen's button through the connection |
| `client.disconnect` | | after leaving to the title screen, as the pause screen's quit button does (the level is disconnected first, so an integrated server saves and stops) |
| `client.connect` | `address` | after starting a connection; `wait` for it to finish |
| `client.world` | `open` with `from` and `maxTicks`, or `leave` | `open`: once that world of `saves/` is `world_ready`, after leaving the one open and, with `from`, copying that world over it first; at once with `alreadyOpen` when it is the world open and there is no `from`. `leave`: after leaving to the title screen. See [Several scripts in one launch](#several-scripts-in-one-launch) |

`/thirst agent probe` in game says the same thing `probe` does, for the moment before an agent is
wired up at all. It needs permission level 4.

Two things about input. It goes through the game's own key state, never the operating system, so a
key the agent holds stays held for as many ticks as it asks — which is what makes the sprint gate
readable. But vanilla's Sneak and Sprint accessibility settings turn those two keys into toggles, and
the dev clients here have `toggleCrouch:true`, so holding sneak for thirty ticks crouches the player
and leaves them crouching. `client.info` answers both settings; read the state back rather than
assuming. The dev client also turns `pauseOnLostFocus` off when it opens the agent: an unattended
window is necessarily unfocused, and otherwise vanilla repeatedly opens `PauseScreen` and suppresses
the movement keys the agent is deliberately holding.

Clicks are for controls nothing else reaches, another mod's settings buttons for instance; a screen the
game can open, and a slot, have commands of their own. `client.click` measures from the centre of the
screen by default because a container screen is centred, so a control on one keeps its offset from
the centre at any window size. A screen made of a header, a list and a footer anchors its rows to an
edge rather than to the centre, so `from: "top"` and `from: "bottom"` keep x measured from the centre
and measure y from that edge, which is what makes a script for the config screen portable between
window sizes. `button` is 0 left, 1 right and 2 middle everywhere: 26.3 moved the client from GLFW to
SDL, which numbers the buttons from one, and `AgentClientVanilla.click` turns the number back so that
a file written on one node presses the same button on another.

## Recording a screen

`client.record` and `client.mouse` turn a script into a GIF: a showcase of a screen, with a cursor that
moves, hovers, drags and clicks. [tools/agent/ui/config-showcase.jsonl](../../../../../../../tools/agent/ui/config-showcase.jsonl)
is the config screen's, and its header has the three commands that make the GIF.

- **The virtual pointer.** `client.click` presses a control without the mouse ever being over it, which
  is right for a check and looks like nothing happened in a video. `client.mouse` shows a pointer the
  game believes is the mouse (`Pointer`, written into `MouseHandler` through `MouseHandlerAccessor`),
  so a screen draws its hover states and tooltips under it, and glides it from where it is over
  `ticks`. Once it is shown, `client.click` and `client.scroll` without a point land where it is, and a
  click with one moves it there first. While it is shown the real mouse is ignored (`MouseMoveMixin`),
  so a hand passing over the window changes nothing. `{"hide": true}` gives the real mouse back.
- **Frames.** `client.record` writes `screenshots/<name>/frame-00001.png` onward, `every` ticks apart,
  and `frames.jsonl` beside them: the pointer's place in frame pixels, and whether a button is down,
  for each. The framebuffer never holds a cursor, since the system draws the real one, so
  [tools/agent/make_gif.py](../../../../../../../tools/agent/make_gif.py) draws one there. A frame is taken at the
  start of a pass of the game loop (`FrameStartMixin`), when the target still holds the last frame
  drawn and the pointer has not moved since, so the cursor and the screen under it always agree.
- **Lockstep.** Reading a maximised window back costs vanilla over a hundred milliseconds, pixel by
  pixel on the render thread, so a recording holds the game to a few frames a second. Rather than drop
  frames, the queue waits while a frame is due: a `wait` or a glide in the script spans exactly the
  frames its ticks call for, and the GIF plays at the speed the script was written for however long
  the recording took. The config showcase's 270 frames take about three minutes to record.
- **The clock.** Whatever reads the clock rather than counting ticks (the config preview's rising and
  falling droplets, a tooltip's fade) would still run at real time, three times too fast once each
  frame plays in 50 ms. So while a recording runs, `RecordingClock` holds the game's clock still
  (`Util.timeSource`, which vanilla keeps swappable for tests) and moves it on one tick's worth a pass
  until the next frame is due: the game sees 50 ms pass a frame, and runs one tick and draws once
  between two frames. On stop the real clock comes back less the time spent frozen, so it never jumps.
  The frame limiter and the agent's stall checks read `System.nanoTime` and are left alone.
- **Size.** Frames are kept at every `downscale`th pixel, half the window each way by default, and
  written on a thread of the recorder's own. A GUI drawn at a scale `downscale` divides loses
  nothing. Vanilla's own downscale is not used: it refuses a window whose height it does not divide,
  and a maximised one here is 1191 high.

A recording is evidence for a person, like `client.capture`; nothing is asserted on its pixels.

`client.slot` takes a player inventory index as `inventory`, the `container.N` of `/item replace`,
and finds that slot in whatever menu is open. `client.language` loads only the translations again: a full resource reload in a world freed a font atlas that Jade's
overlay drew from a frame later and crashed the client, and the fonts already hold every script.

## What a check looks like

[tools/agent/gameplay/client-sync.jsonl](../../../../../../../tools/agent/gameplay/client-sync.jsonl) is MANUAL-TESTING.md's "Sync
to the client" section, the four items one client can answer, with the expected answer written above
each one. Start `runServer` and `runManualA`, put the player somewhere flat with `fall_damage` off,
and:

```bash
python tools/agent/drive.py run/manual-1.21.1-neoforge-A/agent/A tools/agent/gameplay/client-sync.jsonl
```

The fifth item, that each player sees only their own bar, needs three queues and so is not one file.
Start `runServer`, `runManualA` and `runManualB`, stand the two testers together, then write
different values from the server's queue and read both clients' `client.hud` back:

```bash
python tools/agent/drive.py run/1.21.1-neoforge/agent/server - <<'EOF'
{"command": "server.command", "args": {"command": "tp TesterB 21 103 20"}}
{"command": "server.thirst.set", "args": {"player": "TesterA", "thirst": 6, "quenched": 0}}
{"command": "server.thirst.set", "args": {"player": "TesterB", "thirst": 14, "quenched": 2}}
{"command": "wait", "args": {"ticks": 20}}
EOF
```

Then `client.hud` on each of `run/manual-<node>-A/agent/A` and `run/manual-<node>-B/agent/B`, and
swap the two values and read again, so that "each client kept what it already had" cannot pass for
"each client was told its own".

## Many players

Three different things get called "many players", and only one of them is what a given check needs.

| Way | Cost each | Sees the HUD | Gets synced | Practical ceiling |
|---|---|---|---|---|
| Server-side simulated player (`BenchmarkPlayer`, a Fabric `FakePlayer`) | almost nothing | no | no, the fake connection drops every packet | 2000, already measured |
| Real modded client (`runClient`, `runManualA`, `runManualB`) | a whole Minecraft client | yes | yes | 2 to 4 on a desktop |
| Vanilla protocol bot | small | no | no, it never negotiates the mod's channel | many, and useless here |
| Simulated player with a captured connection (`SyncPlayer`, in `src/gametest`) | almost nothing | no | yes, into a buffer the test reads | as many as the test wants |

So the answer depends on the check. Anything about cost, drain, loot or damage at scale belongs to
the benchmark's simulated players. Anything about the HUD needs real clients, and two is enough for
every item in MANUAL-TESTING.md — which is why `runManualA` and `runManualB` exist and why there is
no third. Anything about what a client is *told*, for many players at once, is
`PlayerSyncGameTest`; its ceiling is that it still cannot see a pixel.

The agent client deliberately does not scale by simulating players. A fake connection drops every
packet, so a simulated player can never answer a question about sync or about the HUD, which is the
only kind of question this tool exists for.

## Known issues

**The Fabric 26.2.x client crashes on its way out, after the script has been answered.** The run
prints `[ThirstAgent] DONE`, then about 15 seconds later `java.lang.Error: Watchdog (Client shutdown
from post-main)`, writes a crash report to `run/26.2.x/crash-reports/`, and Gradle reports exit value
-8 and `BUILD FAILED`. The replies in `out.jsonl` are complete; nothing the script asked failed. Until
this is gone, a 26.2.x run's result is its `DONE` line, not the Gradle task's exit.

It is Create Fly's, not the agent's. Create Fly (`26.2-rc-2-6.0.9-1`, the newest for 26.2 on
2026-09-19) starts Flywheel's `Flywheel Task Executor #N` threads as non-daemon threads and never
stops them: nothing calls `ParallelTaskExecutor.stopWorkers()`. Up to 26.1, `Minecraft.destroy()`
ended in `System.exit(0)`, which took them down with everything else, so 26.1.x has the same threads
and still exits cleanly. 26.2 dropped that exit: `Main` returns, leaves the JVM to end when its last
non-daemon thread does, and starts `ClientShutdownWatchdog`, which dumps the threads after 15 seconds
and calls `System.exit(-8)`. The thread dump holds nothing else that is not a daemon. `stop`, and the
end of a `-Pagent` script, call `Minecraft.stop()`, the same as the title screen's Quit button, so a
person quitting a 26.2 client that has Create Fly gets the same crash report. The other nodes do not
load Create Fly.

Setting Flywheel's `workerThreads` to `0` in `run/26.2.x/config/flywheel-client.json`
(`"workerThreads": {"value": 0}`) avoids it: Flywheel then does its work on the render thread and
starts no workers, and the run ends with `BUILD SUCCESSFUL`. The file is under `run/`, which is not
committed, so each checkout sets it once. Delete this entry once a Create Fly build for 26.2 stops its
workers or makes them daemon threads. To check, run any client script on 26.2.x with `workerThreads`
back at `-1`.

## Rules

- **Every probe answers a number or a string, never a picture.** `client.capture` exists so a person
  can look at the frame afterwards. The moment an assertion depends on a screenshot, the item is not
  automated.
- **No change to the mod for the agent's sake.** Where a probe cannot reach something, read it from
  this source set instead — that is what `dev/mixin/ThirstHudMixin` and `HudRecord` are, rather than
  the mod recording its own rectangles.
- **`dev/agent/core` knows nothing of Minecraft, of a loader or of the mod.** `checkAgentCore`, in
  `gradle/shared.gradle.kts`, fails the build when a class there imports one. Everything that names
  them lives in `dev/agent/thirst`.
- **A check that becomes a number leaves MANUAL-TESTING.md**, the same rule that file already states
  for gametests.

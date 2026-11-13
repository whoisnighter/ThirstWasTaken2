"""Publishes one release of ThirstWasTaken2 to Modrinth, and holds what `publish_curseforge.py` shares.

A release is one file per node, each Minecraft version on each of its loaders (Fabric, NeoForge and, on
1.20.1, Forge), each with its own jar, its own list of Minecraft releases and its own list of optional
mods. Done by hand in a web form that is a dozen chances to attach the wrong jar or forget a game version,
and nothing afterwards would say so. Everything a
release needs is already written down in the repository, so it is read rather than retyped:

- the version number and every node from `stonecutter.properties.toml`, the file the build and CI read
  too, so a node added there is released without touching this script;
- the Minecraft releases an upload claims, from that node's `mod.mc_releases`;
- the mods to list, from the `deps.*` keys the node resolves, so a dependency dropped from a node stops
  being listed for it;
- the release notes, from the matching section of `CHANGELOG.md`;
- the project page's description, from `docs/MODRINTH.md` (and `docs/CURSEFORGE.md` on CurseForge).

    python tools/release/publish.py --dry-run               # print every upload, send nothing
    python tools/release/publish.py                         # build, then upload to Modrinth
    python tools/release/publish.py --no-build              # publish the jars already in build/libs
    python tools/release/publish.py --only 26.2.x-neoforge  # one node only
    python tools/release/publish.py --description-only      # update the project page, upload nothing
    python tools/release/publish.py --update-dependencies   # fix the mods published files list

`publish_curseforge.py` takes the same flags and uploads the same jars to CurseForge; run it with
`--no-build` after this one, since this one has just built them.

Before running it: bump `mod.version` in `stonecutter.properties.toml` (the one place the version is
written), write the `## [<version>]` section of `CHANGELOG.md` in the style of the `write-docs` skill,
run `python .github/scripts/update_mc_deps.py --check`, work through `docs/dev/MANUAL-TESTING.md`, and
commit. Modrinth and CurseForge are the only places a release is published: there is no tag and no
GitHub release.

The token comes from `MODRINTH_TOKEN` in the environment or in `.env`, which is git-ignored; it needs
Modrinth's create-version scope, and the write-projects scope to update the description.

After the uploads the project page's description is compared with `docs/MODRINTH.md` and replaced when
they differ, so the page follows the file the way the files follow `CHANGELOG.md`. `--dry-run` only
says whether it would. A token without the write-projects scope leaves the description as it is and
says so; the uploads before it still count. CurseForge has no API for the description at all, so
`publish_curseforge.py` can only say where to paste `docs/CURSEFORGE.md` by hand.

Re-running is safe, and is how a half-finished release is finished: a version already on Modrinth is
skipped by its version number. Nothing already published is overwritten: a bad upload is deleted on
Modrinth by hand, then re-run here.

Standard library only, like the rest of the Python in this repository.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
import tomllib
import urllib.error
import urllib.request
import uuid
from dataclasses import dataclass, field
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

# tools/node_names.py: what a node's name says, shared with the dependency and benchmark scripts.
sys.path.insert(0, str(ROOT / "tools"))
from node_names import LOADERS, node_name, suffix  # noqa: E402
PROPERTIES = ROOT / "stonecutter.properties.toml"
SETTINGS = ROOT / "settings.gradle.kts"
CHANGELOG = ROOT / "CHANGELOG.md"
MODRINTH_DESCRIPTION = ROOT / "docs" / "MODRINTH.md"
LIBS = ROOT / "build" / "libs"
ENV_FILE = ROOT / ".env"

MODRINTH = "https://api.modrinth.com/v2"
MODRINTH_PROJECT = "thirst-was-taken-2"
# Modrinth asks every client for a User-Agent that identifies the project.
USER_AGENT = "whoisnighter/ThirstWasTaken2 release publisher (github.com/whoisnighter/ThirstWasTaken2)"


@dataclass(frozen=True)
class Dependency:
    """One mod a player can install, as each site names it. Kept here rather than per script so the
    two sites cannot drift apart on which mods a file lists."""

    modrinth_slug: str
    modrinth_id: str
    """Modrinth's version endpoint stores the project id, and an id outlives a rename."""
    curseforge_slug: str | None
    """CurseForge's upload API names a relation by slug. None when that API refuses the project, so the
    relation is added by hand on the file's page; `publish_curseforge.py` says which."""
    required: bool = False


# Every `deps.*` key that names a mod a player can install. A key left out is left out deliberately:
# `fabric_loader`, `neoforge`, `forge` and `loomx.loom_version` are the platform rather than a mod,
# `mixinextras` is nested in the Forge jar, and
# `cloth_config` is only on the dev client's runtime classpath -- no code in the mod names it -- so an
# upload must not tell players to install it. `architectury`, `glitchcore`, `forge_config_api_port` and
# `cooks_collection` are libraries or runtime companions of the mods below, which pull them in themselves,
# and so are `trinkets` and `curios`, which Beachparty needs.
DEPENDENCIES = {
    "fabric_api": Dependency("fabric-api", "P7dR8mSH", "fabric-api", required=True),
    "modmenu": Dependency("modmenu", "mOgUt4GM", "modmenu"),
    "appleskin": Dependency("appleskin", "EsAfCjCV", "appleskin"),
    "jade": Dependency("jade", "nvQzSEkH", "jade"),
    "farmersdelight": Dependency("farmers-delight-refabricated", "7vxePowz", "farmers-delight-refabricated"),
    "create_fly": Dependency("create-fly", "dKvj0eNn", "create-fly"),
    "create": Dependency("create", "LNytGWDc", "create"),
    # Sophisticated Core is left out: both mods below require it, so a player gets it through them.
    "sophisticated_backpacks": Dependency("sophisticated-backpacks", "TyCTlI4b", "sophisticated-backpacks"),
    "sophisticated_storage": Dependency("sophisticated-storage", "hMlaZH8f", "sophisticated-storage"),
    # Moonlight Lib is left out for the same reason: Supplementaries requires it.
    "supplementaries": Dependency("supplementaries", "fFEIiSDQ", "supplementaries"),
    # The Fabric nodes pin Refabricated, the maintained Fabric port. CurseForge's upload API answers 500
    # to a relation naming it, though the slug is right (1.2.0, 2026-09-23), so it is listed there by hand.
    "kaleidoscope_cookery": Dependency("kaleidoscope-cookery-refabricated", "Ct11Kuii", None),
    # One project for both loaders on both sites.
    "brewin_and_chewin": Dependency("brewin-and-chewin", "hIu9KJTT", "brewin-and-chewin"),
    # NeoForge 1.21.1 only (and Cold Sweat and Fruits Delight on Forge 1.20.1), so only those nodes' deps name them.
    "cold_sweat": Dependency("cold-sweat", "uXhSmPjd", "cold-sweat"),
    "cultural_delights": Dependency("cultural-delights", "YttyNOFA", "cultural-delights"),
    "fruits_delight": Dependency("fruits-delight", "g6sbyCTu", "fruits-delight"),
    # Youkais' Feasts, NeoForge 1.21.1 and Forge 1.20.1. Its CurseForge slug is unverified, so it is
    # listed there by hand.
    "youkais_feasts": Dependency("gensokyo-delight-youkais-feasts", "Ohy0TgO2", None),
    "expanded_delight": Dependency("expanded-delight", "e9V6wFcR", None),
    "serene_seasons": Dependency("serene-seasons", "e0bNACJD", "serene-seasons"),
    # Farm & Charm and Candlelight: their CurseForge slugs are unverified, so they are listed there by hand.
    "farm_and_charm": Dependency("lets-do-farm-charm", "HJetCzWo", None),
    "candlelight": Dependency("lets-do-candlelight-farmcharm-compat", "qwbArkQk", None),
    # HerbalBrews, Beachparty and Meadow, likewise listed by hand on CurseForge.
    "herbalbrews": Dependency("lets-do-herbalbrews", "Eh11TaTm", None),
    "beachparty": Dependency("lets-do-beachparty", "GyKzAh3l", None),
    "meadow": Dependency("lets-do-meadow", "ra7o6Sl7", None),
    # Little Logistics and Cooking for Blockheads: CurseForge slugs unverified, listed there by hand.
    "little_logistics": Dependency("little-logistics", "1zZJrh9c", None),
    "cooking_for_blockheads": Dependency("cooking-for-blockheads", "vJnhuDde", None),
}
# The same keys where a NeoForge or Forge node's dependency is a different project: Farmer's Delight
# Refabricated and Kaleidoscope Cookery Refabricated are Fabric ports, and the other loaders use the originals.
NEOFORGE_DEPENDENCIES = {
    "farmersdelight": Dependency("farmers-delight", "R2OftAxM", "farmers-delight"),
    "kaleidoscope_cookery": Dependency("kaleidoscope-cookery", "v17FatAc", "kaleidoscope-cookery"),
}


@dataclass
class Node:
    """One Stonecutter node, which is one file on each site."""

    name: str
    """The node as settings.gradle.kts names it: `26.2.x`, or `26.2.x-neoforge`."""
    loader: str
    minecraft: str
    """The Minecraft version the node compiles against; the jar name carries it."""
    game_versions: list[str]
    """Every Minecraft release the upload claims, from `mod.mc_releases`."""
    dependencies: list[Dependency] = field(default_factory=list)
    jar: Path = ROOT

    def dependency_names(self) -> str:
        """The dependencies as slugs, only so the printed plan is readable."""
        names = [dep.modrinth_slug + (" (required)" if dep.required else "") for dep in self.dependencies]
        return ", ".join(names) or "none"


def fail(message: str):
    sys.exit(f"error: {message}")


def run(command: list[str], **kwargs) -> subprocess.CompletedProcess:
    return subprocess.run(command, cwd=ROOT, text=True, **kwargs)


def env_value(key: str) -> str | None:
    """`key` from the environment, or else from `.env`, which is git-ignored."""
    value = os.environ.get(key)
    if value:
        return value.strip()
    if not ENV_FILE.exists():
        return None
    for line in ENV_FILE.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line.startswith("#") or "=" not in line:
            continue
        name, value = line.split("=", 1)
        if name.strip() == key:
            return value.strip().strip('"').strip("'") or None
    return None


def node_minecraft_versions() -> dict[str, str]:
    """Maps each node to the Minecraft version it compiles against, from settings.gradle.kts.

    The same reading .github/scripts/update_mc_deps.py does: `versions("1.21.1")` names a node after its
    Minecraft version, and `version("26.1.x", "26.1.2")` gives a node a name of its own plus the version
    behind it.
    """
    text = SETTINGS.read_text(encoding="utf-8")
    nodes: dict[str, str] = {}
    for group in re.findall(r"\bversions\(([^)]*)\)", text):
        for name in re.findall(r'"([^"]+)"', group):
            nodes[name] = name
    for name, minecraft in re.findall(r'\bversion\(\s*"([^"]+)"\s*,\s*"([^"]+)"\s*\)', text):
        nodes[name] = minecraft
    if not nodes:
        fail(f"no Stonecutter versions found in {SETTINGS.name}")
    return nodes


def version_number(node: Node, mod_version: str) -> str:
    """What the buildscripts set as the project version, so what the jar and the upload are called."""
    return f"{mod_version}+{node.minecraft}{suffix(node.loader)}"


def read_nodes(props: dict, mod_version: str) -> list[Node]:
    """Every node in the properties file, newest Minecraft first, Fabric before NeoForge before Forge.

    A loader table is what makes a node -- the rule CI's matrix and the dependency updater go by -- so
    `[neoforge."26.2.x"]` is the node `26.2.x-neoforge`. Values are read from the loader table first and
    then from the shared `["26.2.x"]` table, the layering stonecutter.gradle.kts tags each node with.
    """
    minecraft_of = node_minecraft_versions()
    nodes: list[Node] = []
    for loader in LOADERS:
        for version, table in props.get(loader, {}).items():
            name = node_name(version, loader)
            shared = props.get(version, {})

            def value(key: str, subkey: str, table=table, shared=shared):
                return table.get(key, {}).get(subkey, shared.get(key, {}).get(subkey))

            if name not in minecraft_of:
                fail(f"`{name}` has a table in {PROPERTIES.name} but no entry in {SETTINGS.name}")
            releases = value("mod", "mc_releases")
            if not releases:
                fail(f"`{name}` has no mod.mc_releases; both sites need the Minecraft releases to list")

            dependencies = []
            for key, dependency in DEPENDENCIES.items():
                if loader != "fabric":
                    dependency = NEOFORGE_DEPENDENCIES.get(key, dependency)
                if value("deps", key) is not None:
                    dependencies.append(dependency)

            node = Node(name, loader, minecraft_of[name], list(releases), dependencies)
            node.jar = LIBS / f"{props['mod']['name']}-{version_number(node, mod_version)}.jar"
            nodes.append(node)

    # Newest Minecraft first, Fabric before the others: the order the sites list them in afterwards, and
    # the order a player reads down the file list.
    def order(node: Node) -> tuple:
        return (LOADERS.index(node.loader), [-int(part) for part in node.minecraft.split(".")])

    return sorted(nodes, key=order)


def changelog_section(mod_version: str) -> str:
    """The body of `## [1.0.6] - <date>`, up to the next release heading.

    Taken whole rather than summarised: the file is already written for players to read.
    """
    text = CHANGELOG.read_text(encoding="utf-8")
    pattern = re.compile(r"^## \[" + re.escape(mod_version) + r"\][^\n]*\n(.*?)(?=^## \[|\Z)", re.S | re.M)
    match = pattern.search(text)
    if not match:
        fail(f"{CHANGELOG.name} has no `## [{mod_version}]` section; write the entry before releasing")
    body = match.group(1).strip()
    if not body:
        fail(f"the `## [{mod_version}]` section of {CHANGELOG.name} is empty")
    return body


def http(url: str, headers: dict[str, str], method: str = "GET", body: bytes | None = None):
    request = urllib.request.Request(url, data=body, headers={"User-Agent": USER_AGENT, **headers},
                                     method=method)
    with urllib.request.urlopen(request, timeout=300) as response:
        return json.load(response)


def multipart(field_name: str, metadata: dict, jar: Path) -> tuple[bytes, str]:
    """Both upload endpoints take the metadata as one JSON field beside the file itself; only the name
    of that field differs, `data` on Modrinth and `metadata` on CurseForge."""
    boundary = uuid.uuid4().hex
    body = bytearray()
    body += f"--{boundary}\r\n".encode()
    body += f'Content-Disposition: form-data; name="{field_name}"\r\n'.encode()
    body += b"Content-Type: application/json\r\n\r\n"
    body += json.dumps(metadata).encode("utf-8") + b"\r\n"
    body += f"--{boundary}\r\n".encode()
    body += f'Content-Disposition: form-data; name="file"; filename="{jar.name}"\r\n'.encode()
    body += b"Content-Type: application/java-archive\r\n\r\n"
    body += jar.read_bytes() + b"\r\n"
    body += f"--{boundary}--\r\n".encode()
    return bytes(body), f"multipart/form-data; boundary={boundary}"


class Modrinth:
    name = "Modrinth"

    def __init__(self):
        self.token = env_value("MODRINTH_TOKEN")
        if not self.token:
            fail(f"no MODRINTH_TOKEN in the environment or in {ENV_FILE.name}")
        self.project = self.request(f"/project/{MODRINTH_PROJECT}")
        self.version_ids = {version["version_number"]: version["id"]
                            for version in self.request(f"/project/{MODRINTH_PROJECT}/version")}
        self.published = set(self.version_ids)

    def request(self, path: str, method: str = "GET", body: bytes | None = None,
                content_type: str | None = None):
        headers = {"Authorization": self.token}
        if content_type:
            headers["Content-Type"] = content_type
        return http(f"{MODRINTH}{path}", headers, method, body)

    def is_published(self, node: Node, number: str) -> bool:
        return number in self.published

    def describe(self, node: Node) -> list[str]:
        return [f"loader    {node.loader}", f"minecraft {', '.join(node.game_versions)}"]

    @staticmethod
    def dependencies(node: Node) -> list[dict]:
        return [{"project_id": dep.modrinth_id,
                 "dependency_type": "required" if dep.required else "optional"}
                for dep in node.dependencies]

    def update_dependencies(self, node: Node, number: str) -> None:
        """Replaces the mods an already published version lists with the ones the node resolves now."""
        self.patch(f"/version/{self.version_ids[number]}", {"dependencies": self.dependencies(node)})

    def patch(self, path: str, fields: dict) -> None:
        # 204 No Content on success, so the answer is not read as JSON.
        request = urllib.request.Request(
            f"{MODRINTH}{path}", data=json.dumps(fields).encode("utf-8"), method="PATCH",
            headers={"User-Agent": USER_AGENT, "Authorization": self.token, "Content-Type": "application/json"})
        urllib.request.urlopen(request, timeout=60).close()

    def upload(self, node: Node, number: str, changelog: str) -> str:
        data = {
            "project_id": self.project["id"],
            # What the version is called on the page. Past releases read "Thirst Was Taken 2 1.0.5+26.2",
            # the project's own title and the version number: `mod.name` is the jar's name,
            # ThirstWasTaken2, not the name on Modrinth.
            "name": f"{self.project['title']} {number}",
            "version_number": number,
            "changelog": changelog,
            "dependencies": self.dependencies(node),
            "game_versions": node.game_versions,
            "version_type": "release",
            "loaders": [node.loader],
            # Modrinth features the newest of each loader by itself; featuring eight files by hand would
            # bury the rest of the page.
            "featured": False,
            "status": "listed",
            "file_parts": ["file"],
            "primary_file": "file",
        }
        body, content_type = multipart("data", data, node.jar)
        created = self.request("/version", "POST", body, content_type)
        return created["id"]

    def sync_description(self, dry_run: bool) -> None:
        """Replaces the project page's description with `docs/MODRINTH.md` when the two differ."""
        wanted = MODRINTH_DESCRIPTION.read_text(encoding="utf-8").strip()
        if (self.project.get("body") or "").strip() == wanted:
            print(f"Description: already matches {MODRINTH_DESCRIPTION.name}")
            return
        if dry_run:
            print(f"Description: would replace the project page's with {MODRINTH_DESCRIPTION.name}")
            return
        try:
            self.patch(f"/project/{self.project['id']}", {"body": wanted})
        except urllib.error.HTTPError as error:
            detail = error.read().decode("utf-8", "replace")
            print(f"warning: Modrinth refused the description ({error.code}): {detail}")
            if error.code in (401, 403):
                print("         the token needs the write-projects scope; the files above are published")
            return
        print(f"Description: replaced with {MODRINTH_DESCRIPTION.name}")

    def done_url(self) -> str:
        return f"https://modrinth.com/mod/{MODRINTH_PROJECT}/versions"

    @staticmethod
    def order_nodes(nodes: list[Node]) -> list[Node]:
        """Keep Modrinth's existing newest-first upload order."""
        return nodes


def git(*args: str) -> str:
    result = run(["git", *args], capture_output=True)
    if result.returncode != 0:
        fail(f"git {' '.join(args)} failed: {result.stderr.strip()}")
    return result.stdout.strip()


def check_worktree(allow_dirty: bool) -> None:
    """A release has to be a commit someone can check out again, so a dirty tree stops it."""
    if git("status", "--porcelain"):
        if not allow_dirty:
            fail("the working tree has uncommitted changes; commit them or pass --allow-dirty")
        print("warning: releasing with uncommitted changes; no commit matches what is published")
    branch = git("rev-parse", "--abbrev-ref", "HEAD")
    if branch != "main":
        print(f"warning: on branch `{branch}`, not `main`")


def gradle_build() -> None:
    """`buildAndCollect` builds every node and copies the jars into build/libs, the same as CI."""
    wrapper = str(ROOT / "gradlew.bat") if os.name == "nt" else "./gradlew"
    print(f"$ {wrapper} buildAndCollect\n")
    if run([wrapper, "buildAndCollect"]).returncode != 0:
        fail("the build failed; nothing was published")


def release(publisher_class, description: str) -> None:
    """The whole run both scripts share: read the nodes, check the tree, build, then upload each jar
    the site does not have yet. `publisher_class` is `Modrinth` or `publish_curseforge.CurseForge`."""
    parser = argparse.ArgumentParser(description=description)
    parser.add_argument("--dry-run", action="store_true", help="print every upload and send nothing")
    parser.add_argument("--no-build", action="store_true", help="publish the jars already in build/libs")
    parser.add_argument("--only", action="append", metavar="NODE", help="publish this node only")
    parser.add_argument("--version", help="fail unless stonecutter.properties.toml says this version")
    parser.add_argument("--allow-dirty", action="store_true", help="release with uncommitted changes")
    parser.add_argument("--update-dependencies", action="store_true",
                        help="on files already published, replace the mods they list; upload nothing")
    parser.add_argument("--description-only", action="store_true",
                        help="only bring the project page's description up to date; upload no files")
    args = parser.parse_args()

    if args.description_only:
        publisher_class().sync_description(args.dry_run)
        return

    props = tomllib.loads(PROPERTIES.read_text(encoding="utf-8"))
    mod_version = props["mod"]["version"]
    if args.version and args.version != mod_version:
        fail(f"{PROPERTIES.name} says `{mod_version}`, not `{args.version}`")
    changelog = changelog_section(mod_version)
    nodes = read_nodes(props, mod_version)
    if args.only:
        unknown = set(args.only) - {node.name for node in nodes}
        if unknown:
            fail(f"no such node: {', '.join(sorted(unknown))}")
        nodes = [node for node in nodes if node.name in args.only]

    if args.update_dependencies:
        publisher = publisher_class()
        for node in nodes:
            number = version_number(node, mod_version)
            print(f"  {number}  mods {node.dependency_names()}")
            if not publisher.is_published(node, number):
                print(f"    -> not on {publisher.name}, skipped")
            elif args.dry_run:
                print("    -> would update")
            else:
                try:
                    publisher.update_dependencies(node, number)
                except urllib.error.HTTPError as error:
                    detail = error.read().decode("utf-8", "replace")
                    fail(f"{publisher.name} refused `{number}` ({error.code}): {detail}")
                print("    -> updated")
        return

    print(f"ThirstWasTaken2 {mod_version}, {len(nodes)} files to {publisher_class.name}\n")
    check_worktree(args.allow_dirty)

    if not args.no_build and not args.dry_run:
        gradle_build()
    missing = [node.jar.name for node in nodes if not node.jar.exists()]
    if missing:
        fail("not in build/libs: " + ", ".join(missing) + "\n       build first, or drop --no-build")

    # The site is asked for what it already has before anything is sent, so a missing token or an
    # unknown game version stops the release before the first upload rather than halfway through it.
    publisher = publisher_class()
    nodes = publisher.order_nodes(nodes)
    for node in nodes:
        number = version_number(node, mod_version)
        print(f"  {number}  ({node.name})")
        print(f"    file      {node.jar.name}, {node.jar.stat().st_size // 1024} KiB")
        for line in publisher.describe(node):
            print(f"    {line}")
        print(f"    mods      {node.dependency_names()}")
        if publisher.is_published(node, number):
            print(f"    -> already on {publisher.name}, skipped")
        elif args.dry_run:
            print("    -> would upload")
        else:
            try:
                created = publisher.upload(node, number, changelog)
            except urllib.error.HTTPError as error:
                detail = error.read().decode("utf-8", "replace")
                fail(f"{publisher.name} refused `{number}` ({error.code}): {detail}")
            print(f"    -> uploaded, {created}")
        print()

    publisher.sync_description(args.dry_run)
    print()

    if args.dry_run:
        print("Dry run: nothing was published.")
    else:
        print(f"Done. {publisher.done_url()}")


if __name__ == "__main__":
    release(Modrinth, "Publish a ThirstWasTaken2 release to Modrinth.")

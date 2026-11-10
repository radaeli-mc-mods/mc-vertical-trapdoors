#!/usr/bin/env python3
"""Validate the production mod JAR against one target-matrix entry."""

from __future__ import annotations

import argparse
import json
import re
import sys
import zipfile
from pathlib import Path


def fail(message: str) -> None:
    raise SystemExit(f"JAR check failed: {message}")


def read_text(jar: zipfile.ZipFile, path: str) -> str:
    try:
        return jar.read(path).decode("utf-8")
    except KeyError:
        fail(f"missing {path}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", required=True, type=Path)
    parser.add_argument("--minecraft-version", required=True)
    parser.add_argument("--loader", required=True)
    parser.add_argument("--java", required=True, type=int)
    parser.add_argument("--mod-id", required=True)
    parser.add_argument("--mod-version", required=True)
    args = parser.parse_args()

    if not args.jar.is_file():
        fail(f"JAR does not exist: {args.jar}")
    expected_name = (f"{args.mod_id}-{args.mod_version}+{args.minecraft_version}-"
                     f"{args.loader}.jar")
    if args.jar.name != expected_name:
        fail(f"expected artifact name {expected_name}, found {args.jar.name}")

    with zipfile.ZipFile(args.jar) as jar:
        names = set(jar.namelist())
        if args.loader == "neoforge":
            toml = read_text(jar, "META-INF/neoforge.mods.toml")
            expected_mod_metadata = {
                "mod id": (r'modId="([^"]+)"', args.mod_id),
                "mod version": (r'version="([^"]+)"', f"{args.mod_version}+{args.minecraft_version}"),
                "Minecraft dependency": (r'modId="minecraft"\s*\ntype="required"\s*\nversionRange="\[([^\]]+)\]"', args.minecraft_version),
            }
            for label, (pattern, expected) in expected_mod_metadata.items():
                match = re.search(pattern, toml)
                if not match or match.group(1) != expected:
                    fail(f"{label} metadata should be {expected!r}")
            if re.search(r"\$\{[^}]+}", toml):
                fail("NeoForge metadata contains an unresolved template placeholder")
            if re.search(r"\$\{[^}]+}", read_text(jar, "pack.mcmeta")):
                fail("pack metadata contains an unresolved template placeholder")
            main_class = "com/radaeli/verticaltrapdoors/neoforge/VerticalTrapdoors.class"
            config_paths = re.findall(r'config="([^"]+\.mixins\.json)"', toml)
            forbidden_loader_paths = ("/fabric/", "/forge/")
        elif args.loader == "fabric":
            metadata_text = read_text(jar, "fabric.mod.json")
            metadata = json.loads(metadata_text)
            if metadata.get("id") != args.mod_id:
                fail(f"Fabric mod id should be {args.mod_id!r}")
            if metadata.get("version") != args.mod_version:
                fail(f"Fabric mod version should be {args.mod_version!r}")
            if metadata.get("depends", {}).get("minecraft") != f"={args.minecraft_version}":
                fail(f"Fabric Minecraft dependency should be ={args.minecraft_version!r}")
            if not metadata.get("depends", {}).get("fabricloader"):
                fail("Fabric metadata declares no Fabric Loader dependency")
            if not metadata.get("depends", {}).get("fabric-api"):
                fail("Fabric metadata declares no Fabric API dependency")
            if re.search(r"\$\{[^}]+}", metadata_text):
                fail("Fabric metadata contains an unresolved template placeholder")
            main_class = "com/radaeli/verticaltrapdoors/fabric/VerticalTrapdoorsFabric.class"
            client_class = "com/radaeli/verticaltrapdoors/fabric/client/VerticalTrapdoorsFabricClient.class"
            if client_class not in names:
                fail(f"missing Fabric client entrypoint class {client_class}")
            config_paths = [entry if isinstance(entry, str) else entry.get("config")
                            for entry in metadata.get("mixins", [])]
            forbidden_loader_paths = ("/neoforge/", "/forge/")
        elif args.loader == "forge":
            toml = read_text(jar, "META-INF/mods.toml")
            expected_mod_metadata = {
                "mod id": (r'modId="([^\"]+)"', args.mod_id),
                "mod version": (r'version="([^\"]+)"', f"{args.mod_version}+{args.minecraft_version}"),
                "Minecraft dependency": (
                    r'modId="minecraft"\s*\nmandatory=true\s*\nversionRange="\[([^\]]+)\]"',
                    args.minecraft_version,
                ),
            }
            for label, (pattern, expected) in expected_mod_metadata.items():
                match = re.search(pattern, toml)
                if not match or match.group(1) != expected:
                    fail(f"{label} metadata should be {expected!r}")
            forge_dependency = re.search(
                r'modId="forge"\s*\nmandatory=true\s*\nversionRange="\[([^,\]]+),\)"', toml
            )
            if not forge_dependency or not forge_dependency.group(1).startswith("47."):
                fail("Forge dependency metadata should require Forge 47.x or newer")
            if re.search(r"\$\{[^}]+}", toml):
                fail("Forge metadata contains an unresolved template placeholder")
            if re.search(r"\$\{[^}]+}", read_text(jar, "pack.mcmeta")):
                fail("pack metadata contains an unresolved template placeholder")
            main_class = "com/radaeli/verticaltrapdoors/forge/VerticalTrapdoorsForge.class"
            manifest_lines = re.sub(r"\r?\n ", "", read_text(jar, "META-INF/MANIFEST.MF"))
            manifest = dict(
                line.split(": ", 1)
                for line in manifest_lines.splitlines()
                if ": " in line
            )
            config_paths = [path.strip() for path in manifest.get("MixinConfigs", "").split(",") if path.strip()]
            expected_configs = {
                f"{args.mod_id}.forge.mixins.json",
                f"{args.mod_id}.forge.client.mixins.json",
            }
            if len(config_paths) != len(expected_configs) or set(config_paths) != expected_configs:
                fail("Forge manifest must list the common and client Mixin configurations")
            forbidden_loader_paths = ("/neoforge/", "/fabric/")
        else:
            fail(f"unsupported loader: {args.loader}")

        if main_class not in names:
            fail(f"missing {args.loader} entrypoint class {main_class}")
        if not config_paths:
            fail(f"{args.loader} metadata declares no Mixin configuration")
        for config_path in config_paths:
            config = json.loads(read_text(jar, config_path))
            refmap = config.get("refmap")
            if refmap and refmap not in names:
                fail(f"Mixin refmap declared by {config_path} is missing: {refmap}")
            if args.loader == "forge":
                if not refmap:
                    fail(f"Forge Mixin configuration declares no refmap: {config_path}")
                mappings = json.loads(read_text(jar, refmap)).get("mappings", {})
            package = config.get("package", "")
            for category in ("mixins", "client", "server"):
                for mixin in config.get(category, []):
                    class_path = "/".join(filter(None, (package.replace(".", "/"), mixin.replace(".", "/")))) + ".class"
                    if class_path not in names:
                        fail(f"declared {category} Mixin class missing: {class_path}")
                    if args.loader == "forge" and not mappings.get(class_path.removesuffix(".class")):
                        fail(f"Forge Mixin has no refmap mappings: {class_path}")

        if any(path in f"/{name.lower()}" for path in forbidden_loader_paths for name in names):
            fail("artifact contains classes or metadata for another loader")
        forbidden_prefixes = (
            "net/minecraft/", "net/neoforged/", "net/fabricmc/", "net/minecraftforge/",
            "org/spongepowered/", "org/junit/", "org/gradle/",
        )
        bundled = [name for name in names if name.endswith(".class") and name.startswith(forbidden_prefixes)]
        if bundled:
            fail(f"artifact bundles a game, loader, test, or build dependency class: {bundled[0]}")
        if any(name.startswith("META-INF/jarjar/") for name in names):
            fail("artifact bundles nested dependency metadata")

        class_entries = [name for name in names if name.endswith(".class")]
        expected_major = args.java + 44
        majors = set()
        for name in class_entries:
            data = jar.read(name)
            if len(data) < 8 or data[:4] != b"\xca\xfe\xba\xbe":
                fail(f"invalid class file: {name}")
            major = int.from_bytes(data[6:8], "big")
            majors.add(major)
            if major != expected_major:
                fail(f"{name} targets class-file major {major}; expected Java {args.java} ({expected_major})")

    print(f"Validated {args.jar}: {args.loader} {args.minecraft_version}, Java {args.java}, "
          f"mod {args.mod_version}, {len(class_entries)} classes (major {expected_major}).")


if __name__ == "__main__":
    main()

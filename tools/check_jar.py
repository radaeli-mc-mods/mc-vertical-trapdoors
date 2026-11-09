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

        main_class = f"com/radaeli/verticaltrapdoors/neoforge/VerticalTrapdoors.class"
        if main_class not in names:
            fail(f"missing NeoForge entrypoint class {main_class}")

        config_paths = re.findall(r'config="([^"]+\.mixins\.json)"', toml)
        if not config_paths:
            fail("NeoForge metadata declares no Mixin configuration")
        for config_path in config_paths:
            config = json.loads(read_text(jar, config_path))
            package = config.get("package", "")
            for category in ("mixins", "client", "server"):
                for mixin in config.get(category, []):
                    class_path = "/".join(filter(None, (package.replace(".", "/"), mixin.replace(".", "/")))) + ".class"
                    if class_path not in names:
                        fail(f"declared {category} Mixin class missing: {class_path}")

        if any("/fabric/" in f"/{name.lower()}" or "/forge/" in f"/{name.lower()}" for name in names):
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

    print(f"Validated {args.jar}: NeoForge {args.minecraft_version}, Java {args.java}, "
          f"mod {args.mod_version}, {len(class_entries)} classes (major {expected_major}).")


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Run isolated Forge checks and fail on probe failures even if FML returns exit code zero."""
import pathlib
import shutil
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
OUT = ROOT / "build/modern-state-tests"
SERVER = OUT / "server"


def run(name, tasks, marker):
    log = OUT / (name + ".log")
    with log.open("w") as stream:
        result = subprocess.run([str(ROOT / "gradlew"), "--offline", "-I", "scripts/modern_state_tests.gradle", *tasks], cwd=ROOT, stdout=stream, stderr=subprocess.STDOUT)
    text = log.read_text(errors="replace")
    if result.returncode or marker not in text or "MODERN_STATE_PROBE FAILED" in text:
        raise RuntimeError("Check failed; inspect " + str(log))
    print(name + " passed: " + str(log), flush=True)
    return text


def main():
    SERVER.mkdir(parents=True, exist_ok=True)
    if not (SERVER / "eula.txt").exists():
        source = ROOT / "run/server/eula.txt"
        if not source.exists() or "eula=true" not in source.read_text():
            raise RuntimeError("Use an existing accepted Minecraft EULA in " + str(SERVER / "eula.txt"))
        shutil.copyfile(source, SERVER / "eula.txt")
    if not (SERVER / "config").exists():
        shutil.copytree(ROOT / "run/server/config", SERVER / "config")
    (SERVER / "server.properties").write_text("server-ip=127.0.0.1\nserver-port=25576\nonline-mode=false\nlevel-type=FLAT\ngenerator-settings=2;7,2x3,2;1;\nspawn-monsters=false\nspawn-animals=false\ngenerate-structures=false\nlevel-name=world\nview-distance=3\n")
    run("contracts-and-build", ["modernStateContracts", "build"], "Modern state contracts PASSED")
    run("runtime-save", ["runServer"], "MODERN_STATE_PROBE PASSED")
    text = run("runtime-restart", ["runServer"], "MODERN_STATE_PROBE PASSED")
    if "entity freeze state=true" not in text:
        raise RuntimeError("Missing restart evidence for persisted entity freezing")
    print("All modern-state automated contracts, Forge gameplay and restart checks passed.")


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print(str(exc), file=sys.stderr)
        sys.exit(1)

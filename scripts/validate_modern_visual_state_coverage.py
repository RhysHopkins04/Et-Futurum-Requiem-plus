#!/usr/bin/env python3
"""Audit visible 1.21.11 blockstate coverage against EFR's deterministic target state.

The validator reads Mojang blockstate JSON either from an extracted client asset root or directly
from a client JAR. Any property that participates in model selection is conservatively treated as
visible unless every observed value resolves to the same apply/model signature. The result for each
source identity/property is exactly one of STORED_EXACTLY, DERIVED_EXACTLY, VISUALLY_IRRELEVANT,
or UNSUPPORTED.

Strict mode (the default) fails for model-affecting UNSUPPORTED properties, except the explicit
project-wide waterlogging deferral. --allow-unsupported is intended while Passes 34-36 are still
closing known visual-state debt; it reports the debt but exits successfully.
"""
from __future__ import print_function
import argparse, json, os, pathlib, sys, zipfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
CLASSES = ("STORED_EXACTLY", "DERIVED_EXACTLY", "VISUALLY_IRRELEVANT", "UNSUPPORTED")
WATERLOGGING_EXCEPTION = "waterlogged"

EXTRA_PROPERTIES = {
    "decorated_pot": {"cracked": "VISUALLY_IRRELEVANT"},
}

def values_from_key(key):
    out = {}
    if not key:
        return out
    for part in key.split(','):
        if '=' not in part:
            continue
        k, v = part.split('=', 1)
        out[k.strip()] = v.strip()
    return out

def canonical(obj):
    return json.dumps(obj, sort_keys=True, separators=(',', ':'))

def multipart_properties(when):
    props = set()
    if not isinstance(when, dict):
        return props
    for k, v in when.items():
        if k in ("OR", "AND") and isinstance(v, list):
            for child in v:
                props.update(multipart_properties(child))
        else:
            props.add(k)
    return props

def inspect_blockstate(data):
    """Return {property: model_affecting_bool} from one Mojang blockstate JSON object."""
    result = {}
    variants = data.get("variants", {}) if isinstance(data, dict) else {}
    rows = []
    if isinstance(variants, dict):
        for key, apply in variants.items():
            rows.append((values_from_key(key), canonical(apply)))
        properties = sorted({p for state, _ in rows for p in state})
        for prop in properties:
            visible = False
            # If two otherwise-compatible variants differ in this property and resolve differently,
            # the property materially participates in model selection.
            for i, (a, sig_a) in enumerate(rows):
                for b, sig_b in rows[i + 1:]:
                    if a.get(prop) == b.get(prop):
                        continue
                    keys = (set(a) | set(b)) - {prop}
                    if all(a.get(k) == b.get(k) for k in keys) and sig_a != sig_b:
                        visible = True
                        break
                if visible:
                    break
            result[prop] = visible
    multipart = data.get("multipart", []) if isinstance(data, dict) else []
    if isinstance(multipart, list):
        for part in multipart:
            if not isinstance(part, dict) or "when" not in part:
                continue
            for prop in multipart_properties(part.get("when")):
                # Conditional multipart presence changes geometry/model composition.
                result[prop] = True
    return result

def classify(name, prop, visible):
    if name in EXTRA_PROPERTIES and prop in EXTRA_PROPERTIES[name]:
        return EXTRA_PROPERTIES[name][prop]
    if not visible:
        return "VISUALLY_IRRELEVANT"
    if name == "sea_pickle" and prop == "waterlogged":
        # Bounded Pass 34 live/dead visual bit only; not a general water/fluid state.
        return "STORED_EXACTLY"
    if prop == WATERLOGGING_EXCEPTION:
        return "UNSUPPORTED"
    if name == "pale_moss_carpet" and prop in {"bottom", "north", "east", "south", "west"}:
        return "STORED_EXACTLY"
    if name == "pale_hanging_moss" and prop == "tip":
        return "STORED_EXACTLY"
    if name == "creaking_heart" and prop in {"axis", "creaking_heart_state"}:
        return "STORED_EXACTLY"
    if name == "dried_ghast" and prop in {"facing", "hydration"}:
        return "STORED_EXACTLY"
    if name == "torchflower_crop" and prop == "age":
        return "STORED_EXACTLY"
    if name == "pitcher_crop" and prop in {"age", "half"}:
        return "STORED_EXACTLY"
    if name == "pitcher_plant" and prop == "half":
        return "STORED_EXACTLY"
    if name == "sniffer_egg" and prop == "hatch":
        return "STORED_EXACTLY"
    if name == "mangrove_propagule" and prop in {"hanging", "age"}:
        return "STORED_EXACTLY"
    if name == "sea_pickle" and prop == "pickles":
        return "STORED_EXACTLY"
    if name == "tall_seagrass" and prop == "half":
        return "STORED_EXACTLY"
    if name == "pale_oak_button" and prop in {"face", "facing", "powered"}:
        return "STORED_EXACTLY"
    if name == "pale_oak_pressure_plate" and prop == "powered":
        return "STORED_EXACTLY"
    if name == "copper_wall_torch" and prop == "facing":
        return "STORED_EXACTLY"
    if name == "copper_torch":
        return "VISUALLY_IRRELEVANT"
    if name.endswith("candle_cake") and prop == "lit":
        return "STORED_EXACTLY"
    if name == "decorated_pot" and prop == "facing":
        return "STORED_EXACTLY"
    if name in {"sculk_vein", "resin_clump"} and prop in {"down","up","north","south","west","east"}:
        return "STORED_EXACTLY"
    if name.endswith("_froglight") and prop == "axis":
        return "STORED_EXACTLY"
    if name.endswith("copper_chain") and prop == "axis":
        return "STORED_EXACTLY"
    if name.endswith("copper_lantern") and prop == "hanging":
        return "STORED_EXACTLY"
    if name in {"pale_oak_log", "stripped_pale_oak_log"} and prop == "axis":
        return "STORED_EXACTLY"
    if name.endswith("_wall") and prop in {"north","east","south","west","up"}:
        return "DERIVED_EXACTLY"
    if name.endswith("_fence") and prop in {"north","east","south","west"}:
        return "DERIVED_EXACTLY"
    if name.endswith("_pane") and prop in {"north","east","south","west"}:
        return "DERIVED_EXACTLY"
    if name.endswith("_fence_gate"):
        if prop in {"facing","open"}: return "STORED_EXACTLY"
        if prop == "in_wall": return "DERIVED_EXACTLY"
        if prop == "powered": return "VISUALLY_IRRELEVANT"
    if name.endswith("_candle") and not name.endswith("_candle_cake") and prop in {"candles","lit"}:
        return "STORED_EXACTLY"
    if name in {"campfire","soul_campfire"} and prop in {"facing","lit"}:
        return "STORED_EXACTLY"
    if name == "turtle_egg" and prop in {"eggs","hatch"}:
        return "STORED_EXACTLY"
    if name in {"leaf_litter","wildflowers"} and prop in {"facing","segment_amount","flower_amount"}:
        return "STORED_EXACTLY"
    if name == "scaffolding" and prop == "bottom":
        return "STORED_EXACTLY"
    if name.endswith("_wall_sign") and prop == "facing":
        return "STORED_EXACTLY"
    if name.endswith("_wall_hanging_sign") and prop == "facing":
        return "STORED_EXACTLY"
    if name.endswith("_sign") and prop == "rotation":
        return "STORED_EXACTLY"
    return "UNSUPPORTED"

class AssetReader(object):
    def __init__(self, path):
        self.path = pathlib.Path(path)
        self.jar = zipfile.ZipFile(str(self.path)) if self.path.is_file() else None
    def read_blockstate(self, name):
        rels = ["assets/minecraft/blockstates/%s.json" % name, "blockstates/%s.json" % name]
        if self.jar:
            for rel in rels:
                try:
                    return json.loads(self.jar.read(rel).decode('utf-8'))
                except KeyError:
                    pass
            return None
        for rel in rels:
            p = self.path / rel
            if p.is_file():
                return json.loads(p.read_text(encoding='utf-8'))
        return None
    def close(self):
        if self.jar: self.jar.close()

def run_self_test():
    sample = {"variants": {
        "face=wall,facing=north,powered=false": {"model":"a"},
        "face=wall,facing=north,powered=true": {"model":"b"},
        "face=floor,facing=north,powered=false": {"model":"c"},
        "face=floor,facing=east,powered=false": {"model":"d"},
    }}
    props = inspect_blockstate(sample)
    expected = {"face":True,"facing":True,"powered":True}
    if props != expected:
        raise AssertionError("property/model detector mismatch: %r" % props)
    for prop in expected:
        if classify("pale_oak_button", prop, True) != "STORED_EXACTLY":
            raise AssertionError("button classification mismatch for " + prop)
    if classify("decorated_pot", "cracked", True) != "VISUALLY_IRRELEVANT":
        raise AssertionError("decorated pot cracked classification mismatch")
    if classify("pale_moss_carpet", "north", True) != "STORED_EXACTLY":
        raise AssertionError("Pass 34 pale moss classification mismatch")
    if classify("sea_pickle", "waterlogged", True) != "STORED_EXACTLY":
        raise AssertionError("Pass 34 bounded sea-pickle live/dead classification mismatch")
    if classify("tall_seagrass", "half", True) != "STORED_EXACTLY":
        raise AssertionError("Pass 34 tall seagrass classification mismatch")
    if classify("pitcher_plant", "half", True) != "STORED_EXACTLY":
        raise AssertionError("Pass 34d mature pitcher plant classification mismatch")
    if classify("some_future_block", "pose", True) != "UNSUPPORTED":
        raise AssertionError("unknown visible property must be UNSUPPORTED")
    print("Modern visual-state coverage validator self-test PASSED")

def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--assets", help="extracted 1.21.11 client root or client JAR; defaults to EFR_MODERN_ASSETS")
    ap.add_argument("--allow-unsupported", action="store_true", help="report current Pass 34-36 debt without failing")
    ap.add_argument("--json", action="store_true", help="emit machine-readable results")
    ap.add_argument("--self-test", action="store_true")
    args = ap.parse_args()
    if args.self_test:
        run_self_test(); return 0
    asset_path = args.assets or os.environ.get("EFR_MODERN_ASSETS")
    if not asset_path:
        print("ERROR: provide --assets <1.21.11 client.jar/extracted root> or EFR_MODERN_ASSETS", file=sys.stderr)
        return 2
    manifest = json.loads((ROOT / "scripts/modern_map_parity_blocks.json").read_text(encoding='utf-8'))
    reader = AssetReader(asset_path)
    rows=[]; missing=[]; failures=[]
    try:
        for block in manifest.get("blocks", []):
            name=block["name"]
            data=reader.read_blockstate(name)
            if data is None:
                missing.append(name); continue
            props=inspect_blockstate(data)
            for prop, visible in sorted(props.items()):
                cls=classify(name, prop, visible)
                rows.append({"block":name,"property":prop,"affects_model":bool(visible),"classification":cls})
                if visible and cls=="UNSUPPORTED" and prop != WATERLOGGING_EXCEPTION:
                    failures.append("%s.%s" % (name, prop))
            for prop, cls in sorted(EXTRA_PROPERTIES.get(name, {}).items()):
                if prop not in props:
                    rows.append({"block":name,"property":prop,"affects_model":False,"classification":cls,"source":"explicit persistent-state audit"})
    finally:
        reader.close()
    if args.json:
        print(json.dumps({"classification_values":list(CLASSES),"rows":rows,"missing_blockstates":missing,"unsupported_visible":failures,"known_global_exception":WATERLOGGING_EXCEPTION},indent=2))
    else:
        counts={k:0 for k in CLASSES}
        for row in rows: counts[row["classification"]]+=1
        print("Modern visual-state coverage audit: %d properties across %d/%d identities" % (len(rows), manifest.get('count',0)-len(missing), manifest.get('count',0)))
        for k in CLASSES: print(" - %s: %d" % (k, counts[k]))
        if missing: print(" - missing blockstate JSON: %d" % len(missing))
        if failures:
            print("Visible unsupported state debt (%d): %s" % (len(failures), ", ".join(failures)))
        print("Known exception reported separately: general waterlogging")
    if failures and not args.allow_unsupported:
        return 1
    return 0

if __name__ == '__main__':
    sys.exit(main())

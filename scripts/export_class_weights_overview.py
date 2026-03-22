import argparse
import csv
import json
import math
import random
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "src" / "main" / "resources" / "data" / "domesticationinnovation" / "tameslevel" / "class_weights.json"
OUTPUT_DIR = ROOT / "docs" / "class_weights_overview"
SIMULATION_TRIALS = 6000

BASE_STAT_KEYS = [
    "hp",
    "damage",
    "speed",
    "armor",
    "armor_toughness",
    "knockback",
    "knockback_resist",
]

ATTRIBUTE_KEYS = sorted([
    "speed",
    "resistance",
    "strength",
    "fire_resistance",
    "poison_resistance",
    "ability_power",
    "lifesteal",
    "regeneration",
    "rejuvenation",
    "comfort",
    "firefang",
    "poison_fang",
    "witherfang",
    "lightningfang",
    "emergency_cooldown_reduction",
    "killer",
    "pacifist",
    "bosskiller",
    "killexploder",
    "totem",
    "jump_boost",
    "feather_falling",
    "explosion_resistance",
    "smite",
    "bane_of_arthropods",
    "positive_effect_steal",
    "negative_effect_transfer",
    "sweeping_edge",
    "chain_lightning",
    "frost_fang",
    "magnetic",
    "linked_inventory",
    "health_siphon",
    "victim_siphon",
    "pierce",
    "bubbling",
    "herding",
    "amphibious",
    "wall_climber",
    "void_cloud",
    "charisma",
    "disc_jockey",
    "warping_bite",
    "ore_scenting",
    "gluttonous",
    "tethered_teleport",
    "muffled",
    "blazing_protection",
])

ABILITY_KEYS = sorted([
    "creeper_explosion",
    "arrow_shot",
    "ghast_fireball",
    "battle_strength",
    "defensive_aura",
    "snowball_shot",
    "ender_pearl_jump",
    "lightning_strike",
    "warden_scream",
    "wither_skull",
    "blaze_attack",
    "guardian_beam",
    "elder_guardian_beam",
    "berserker",
    "bloodlust",
    "trident",
    "crossbow",
    "evoker_fangs",
    "shulker_bullet",
    "dragon_fireball",
    "llama_spit",
    "fishing",
    "dash",
    "retaliation_slow",
    "immunity_frame",
    "deflection",
    "defusal",
    "shadow_hands",
    "psychic_wall",
    "healing_aura",
    "healing_bottle",
    "guardian_repulse",
    "last_stand_fury",
    "shield_block",
    "sky_launch",
    "guardian_intercept",
    "emergency_shield",
    "body_block",
    "battlefield_medic",
    "triage_pulse",
    "revitalizing_presence",
    "cleanse_touch",
    "pack_guard",
    "life_gift",
])

ATTRIBUTE_MAX_LEVELS = {
    "totem": 5,
    "linked_inventory": 1,
    "amphibious": 1,
    "wall_climber": 1,
    "void_cloud": 1,
    "disc_jockey": 1,
    "ore_scenting": 1,
    "gluttonous": 1,
    "tethered_teleport": 1,
    "muffled": 1,
}

ABILITY_MAX_LEVELS = {
    "deflection": 1,
}

ABILITY_UPGRADABLE = {
    "deflection": False,
}


def read_source():
    with SOURCE.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def write_source(data):
    with SOURCE.open("w", encoding="utf-8", newline="\n") as handle:
        json.dump(data, handle, indent=2)
        handle.write("\n")


def write_csv(path, header, rows):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle)
        writer.writerow(header)
        writer.writerows(rows)


def read_csv(path):
    encodings = ("utf-8-sig", "utf-16", "cp1252")
    last_error = None
    for encoding in encodings:
        try:
            with path.open("r", encoding=encoding, newline="") as handle:
                sample = handle.read(2048)
                handle.seek(0)
                try:
                    dialect = csv.Sniffer().sniff(sample, delimiters=",;\t")
                except csv.Error:
                    dialect = csv.excel
                reader = csv.DictReader(handle, dialect=dialect)
                return list(reader)
        except UnicodeDecodeError as exc:
            last_error = exc
            continue
    if last_error is not None:
        raise last_error
    return []


def collect_keys(classes, section):
    keys = set()
    for payload in classes.values():
        keys.update(payload.get(section, {}).keys())
    return sorted(keys)


def attribute_max_level(attribute_id):
    return ATTRIBUTE_MAX_LEVELS.get(attribute_id, math.inf)


def ability_max_level(ability_id):
    return ABILITY_MAX_LEVELS.get(ability_id, math.inf)


def ability_upgradable(ability_id):
    return ABILITY_UPGRADABLE.get(ability_id, True)


def owned_ability_roll_multiplier(current_level):
    if current_level <= 0:
        return 1.0
    if current_level < 5:
        return 9.0
    return 4.0


def owned_attribute_roll_multiplier(current_level):
    if current_level <= 0:
        return 1.0
    if current_level < 5:
        return 3.0
    return 1.5


def weighted_choice(rng, weighted_options):
    total = sum(max(0.0, weight) for _, weight in weighted_options)
    if total <= 0.0:
        return None
    roll = rng.random() * total
    cursor = 0.0
    for value, weight in weighted_options:
        safe_weight = max(0.0, weight)
        cursor += safe_weight
        if roll < cursor:
            return value
    return weighted_options[-1][0]


def class_category_weights(data, tame_class):
    payload = data.get("classes", {}).get(tame_class, {})
    category = payload.get("category", {})
    defaults = data.get("defaultCategoryWeights", {})
    return (
        float(category.get("base", defaults.get("base", 0.0))),
        float(category.get("attribute", defaults.get("attribute", 0.0))),
        float(category.get("ability", defaults.get("ability", 0.0))),
    )


def class_preferred_attribute_multiplier(data, tame_class):
    payload = data.get("classes", {}).get(tame_class, {})
    return float(payload.get("preferredAttributeWeightMultiplier", data.get("preferredAttributeWeightMultiplier", 1.0)))


def class_preferred_ability_multiplier(data, tame_class):
    payload = data.get("classes", {}).get(tame_class, {})
    return float(payload.get("preferredAbilityWeightMultiplier", data.get("preferredAbilityWeightMultiplier", 1.0)))


def raw_attribute_weight(data, tame_class, attribute_id):
    return float(data.get("classes", {}).get(tame_class, {}).get("attributes", {}).get(attribute_id, 1.0))


def raw_ability_weight(data, tame_class, ability_id):
    return float(data.get("classes", {}).get(tame_class, {}).get("abilities", {}).get(ability_id, 1.0))


def effective_attribute_weight(data, tame_class, attribute_id):
    raw = raw_attribute_weight(data, tame_class, attribute_id)
    if raw <= 1.0:
        return raw
    return raw * class_preferred_attribute_multiplier(data, tame_class)


def effective_ability_weight(data, tame_class, ability_id):
    raw = raw_ability_weight(data, tame_class, ability_id)
    if raw <= 1.0:
        return raw
    return raw * class_preferred_ability_multiplier(data, tame_class)


def is_preferred_attribute(data, tame_class, attribute_id):
    return raw_attribute_weight(data, tame_class, attribute_id) > 1.0


def is_preferred_ability(data, tame_class, ability_id):
    return raw_ability_weight(data, tame_class, ability_id) > 1.0


def roll_attribute_reward(data, tame_class, attribute_levels, ability_levels, rng, allow_ability_fallback=True):
    upgradeable = []
    for attribute_id in ATTRIBUTE_KEYS:
        current = attribute_levels.get(attribute_id, 0)
        if 0 < current < attribute_max_level(attribute_id):
            upgradeable.append((attribute_id, owned_attribute_roll_multiplier(current)))
    upgraded = None
    if upgradeable and rng.random() <= 0.50:
        upgraded = weighted_choice(rng, upgradeable)
    if upgraded is not None:
        attribute_levels[upgraded] = attribute_levels.get(upgraded, 0) + 1
        return ("attribute", upgraded)

    options = []
    for attribute_id in ATTRIBUTE_KEYS:
        current = attribute_levels.get(attribute_id, 0)
        if current >= attribute_max_level(attribute_id):
            continue
        weight = effective_attribute_weight(data, tame_class, attribute_id) * owned_attribute_roll_multiplier(current)
        options.append((attribute_id, weight))
    rolled = weighted_choice(rng, options)
    if rolled is not None:
        attribute_levels[rolled] = attribute_levels.get(rolled, 0) + 1
        return ("attribute", rolled)

    if allow_ability_fallback:
        return roll_ability_reward(data, tame_class, attribute_levels, ability_levels, rng, allow_attribute_fallback=False)
    return ("base", None)


def roll_ability_reward(data, tame_class, attribute_levels, ability_levels, rng, allow_attribute_fallback=True):
    unowned = [ability_id for ability_id in ABILITY_KEYS if ability_levels.get(ability_id, 0) <= 0]
    new_unlock_weight = 1.0 if unowned else 0.0
    upgrade_weight = 0.0
    upgradeable = []
    for ability_id in ABILITY_KEYS:
        current = ability_levels.get(ability_id, 0)
        if current > 0 and ability_upgradable(ability_id) and current < ability_max_level(ability_id):
            weight = owned_ability_roll_multiplier(current)
            upgrade_weight += weight
            upgradeable.append((ability_id, weight))

    choice_options = []
    if new_unlock_weight > 0.0:
        choice_options.append(("new", new_unlock_weight))
    if upgrade_weight > 0.0:
        choice_options.append(("upgrade", upgrade_weight))
    choice = weighted_choice(rng, choice_options) if choice_options else "new"

    if choice == "new":
        options = [(ability_id, effective_ability_weight(data, tame_class, ability_id)) for ability_id in unowned]
        rolled = weighted_choice(rng, options)
        if rolled is not None:
            ability_levels[rolled] = 1
            return ("ability", rolled)
    elif choice == "upgrade":
        upgraded = weighted_choice(rng, upgradeable)
        if upgraded is not None:
            ability_levels[upgraded] = ability_levels.get(upgraded, 0) + 1
            return ("ability", upgraded)

    if unowned:
        options = [(ability_id, effective_ability_weight(data, tame_class, ability_id)) for ability_id in unowned]
        rolled = weighted_choice(rng, options)
        if rolled is not None:
            ability_levels[rolled] = 1
            return ("ability", rolled)
    if upgradeable:
        upgraded = weighted_choice(rng, upgradeable)
        if upgraded is not None:
            ability_levels[upgraded] = ability_levels.get(upgraded, 0) + 1
            return ("ability", upgraded)

    if allow_attribute_fallback:
        return roll_attribute_reward(data, tame_class, attribute_levels, ability_levels, rng, allow_ability_fallback=False)
    return ("base", None)


def simulate_nonpreferred_risk(data, tame_class, target_level, trials, seed):
    base_weight, attribute_weight, ability_weight = class_category_weights(data, tame_class)
    rng = random.Random(seed)
    attribute_hits = 0
    ability_hits = 0

    for _ in range(trials):
        attribute_levels = {}
        ability_levels = {}
        saw_nonpreferred_attribute = False
        saw_nonpreferred_ability = False

        for _level_up in range(max(0, target_level - 1)):
            category = weighted_choice(
                rng,
                [("base", base_weight), ("attribute", attribute_weight), ("ability", ability_weight)],
            )
            if category == "attribute":
                reward_category, reward_id = roll_attribute_reward(data, tame_class, attribute_levels, ability_levels, rng, allow_ability_fallback=True)
            elif category == "ability":
                reward_category, reward_id = roll_ability_reward(data, tame_class, attribute_levels, ability_levels, rng, allow_attribute_fallback=True)
            else:
                reward_category, reward_id = ("base", None)

            if reward_category == "attribute" and reward_id is not None and not is_preferred_attribute(data, tame_class, reward_id):
                saw_nonpreferred_attribute = True
            elif reward_category == "ability" and reward_id is not None and not is_preferred_ability(data, tame_class, reward_id):
                saw_nonpreferred_ability = True

        if saw_nonpreferred_attribute:
            attribute_hits += 1
        if saw_nonpreferred_ability:
            ability_hits += 1

    return {
        "attribute": attribute_hits / trials if trials > 0 else 0.0,
        "ability": ability_hits / trials if trials > 0 else 0.0,
    }


def build_meta_rows(data):
    defaults = data.get("defaultCategoryWeights", {})
    base_weights = data.get("defaultBaseStatWeights", {})
    return [
        ["preferredAttributeWeightMultiplier", data.get("preferredAttributeWeightMultiplier", 1.0)],
        ["preferredAbilityWeightMultiplier", data.get("preferredAbilityWeightMultiplier", 1.0)],
        ["defaultCategory.base", defaults.get("base", 0.0)],
        ["defaultCategory.attribute", defaults.get("attribute", 0.0)],
        ["defaultCategory.ability", defaults.get("ability", 0.0)],
        *[[f"defaultBaseStat.{key}", value] for key, value in sorted(base_weights.items())],
    ]


def build_category_rows(classes):
    rows = []
    for tame_class, payload in sorted(classes.items()):
        category = payload.get("category", {})
        rows.append([
            tame_class,
            category.get("base", ""),
            category.get("attribute", ""),
            category.get("ability", ""),
            payload.get("preferredAttributeWeightMultiplier", ""),
            payload.get("preferredAbilityWeightMultiplier", ""),
            payload.get("autoPreferredWeightBalance", ""),
        ])
    return rows


def build_weight_rows(classes, section, keys):
    rows = []
    for tame_class, payload in sorted(classes.items()):
        weights = payload.get(section, {})
        rows.append([tame_class, *[weights.get(key, 1.0) for key in keys]])
    return rows


def build_risk_rows(data, classes):
    rows = []
    for index, tame_class in enumerate(sorted(classes)):
        level_100 = simulate_nonpreferred_risk(data, tame_class, 100, SIMULATION_TRIALS, 1000 + index)
        level_200 = simulate_nonpreferred_risk(data, tame_class, 200, SIMULATION_TRIALS, 2000 + index)
        rows.append([
            tame_class,
            level_100["ability"],
            level_200["ability"],
            level_100["attribute"],
            level_200["attribute"],
        ])
    return rows


def parse_number(raw, field_name):
    if raw is None:
        raise ValueError(f"Missing numeric value for {field_name}.")
    text = str(raw).strip()
    if not text:
        raise ValueError(f"Blank numeric value for {field_name}.")
    return float(text.replace(",", "."))


def parse_optional_weight(raw):
    if raw is None:
        return None
    text = str(raw).strip()
    if not text:
        return None
    return float(text.replace(",", "."))


def parse_optional_bool(raw):
    if raw is None:
        return None
    text = str(raw).strip().lower()
    if not text:
        return None
    if text in ("true", "1", "-1", "yes", "y"):
        return True
    if text in ("false", "0", "no", "n"):
        return False
    raise ValueError(f"Invalid boolean value '{raw}'.")


def load_weight_map(path, section_name):
    rows = read_csv(path)
    loaded = {}
    for row in rows:
        tame_class = (row.get("class") or "").strip().lower()
        if not tame_class:
            continue
        weights = {}
        for key, raw in row.items():
            if key == "class":
                continue
            value = parse_optional_weight(raw)
            if value is None:
                continue
            if abs(value - 1.0) > 1.0e-9:
                weights[key] = value
        loaded[tame_class] = weights
    return loaded


def load_overview_data():
    data = {
        "preferredAttributeWeightMultiplier": 1.0,
        "preferredAbilityWeightMultiplier": 1.0,
        "defaultCategoryWeights": {},
        "defaultBaseStatWeights": {},
        "classes": {},
    }
    classes = data["classes"]

    meta_rows = read_csv(OUTPUT_DIR / "meta.csv")
    meta = {}
    for row in meta_rows:
        key = (row.get("key") or "").strip()
        if not key:
            continue
        meta[key] = parse_number(row.get("value"), key)

    if "preferredAttributeWeightMultiplier" in meta:
        data["preferredAttributeWeightMultiplier"] = meta["preferredAttributeWeightMultiplier"]
    if "preferredAbilityWeightMultiplier" in meta:
        data["preferredAbilityWeightMultiplier"] = meta["preferredAbilityWeightMultiplier"]

    default_category = data.setdefault("defaultCategoryWeights", {})
    for key in ("base", "attribute", "ability"):
        meta_key = f"defaultCategory.{key}"
        if meta_key in meta:
            default_category[key] = meta[meta_key]

    default_base_stats = {}
    for key, value in meta.items():
        if key.startswith("defaultBaseStat."):
            default_base_stats[key.removeprefix("defaultBaseStat.")] = value
    if default_base_stats:
        data["defaultBaseStatWeights"] = dict(sorted(default_base_stats.items()))

    for row in read_csv(OUTPUT_DIR / "categories.csv"):
        tame_class = (row.get("class") or "").strip().lower()
        if not tame_class:
            continue
        class_entry = classes.setdefault(tame_class, {})
        class_entry["category"] = {
            "base": parse_number(row.get("base"), f"{tame_class}.category.base"),
            "attribute": parse_number(row.get("attribute"), f"{tame_class}.category.attribute"),
            "ability": parse_number(row.get("ability"), f"{tame_class}.category.ability"),
        }
        preferred_attribute_multiplier = parse_optional_weight(row.get("preferredAttributeWeightMultiplier"))
        preferred_ability_multiplier = parse_optional_weight(row.get("preferredAbilityWeightMultiplier"))
        auto_balance = parse_optional_bool(row.get("autoPreferredWeightBalance"))
        if preferred_attribute_multiplier is not None:
            class_entry["preferredAttributeWeightMultiplier"] = preferred_attribute_multiplier
        if preferred_ability_multiplier is not None:
            class_entry["preferredAbilityWeightMultiplier"] = preferred_ability_multiplier
        if auto_balance is not None:
            class_entry["autoPreferredWeightBalance"] = auto_balance

    for section, file_name in (
        ("baseStats", "base_stats.csv"),
        ("attributes", "attributes.csv"),
        ("abilities", "abilities.csv"),
    ):
        loaded = load_weight_map(OUTPUT_DIR / file_name, section)
        for tame_class, weights in loaded.items():
            class_entry = classes.setdefault(tame_class, {})
            if weights:
                class_entry[section] = dict(sorted(weights.items()))

    data["classes"] = dict(sorted(classes.items()))
    return data


def export_overview(include_risk=True):
    data = read_source()
    classes = data.get("classes", {})

    base_stat_keys = BASE_STAT_KEYS
    attribute_keys = ATTRIBUTE_KEYS
    ability_keys = ABILITY_KEYS

    write_csv(OUTPUT_DIR / "meta.csv", ["key", "value"], build_meta_rows(data))
    write_csv(
        OUTPUT_DIR / "categories.csv",
        ["class", "base", "attribute", "ability", "preferredAttributeWeightMultiplier", "preferredAbilityWeightMultiplier", "autoPreferredWeightBalance"],
        build_category_rows(classes)
    )
    write_csv(OUTPUT_DIR / "base_stats.csv", ["class", *base_stat_keys], build_weight_rows(classes, "baseStats", base_stat_keys))
    write_csv(OUTPUT_DIR / "attributes.csv", ["class", *attribute_keys], build_weight_rows(classes, "attributes", attribute_keys))
    write_csv(OUTPUT_DIR / "abilities.csv", ["class", *ability_keys], build_weight_rows(classes, "abilities", ability_keys))
    if include_risk:
        write_csv(
            OUTPUT_DIR / "class_risk.csv",
            ["class", "ability_nonpreferred_100", "ability_nonpreferred_200", "attribute_nonpreferred_100", "attribute_nonpreferred_200"],
            build_risk_rows(data, classes),
        )


def import_overview():
    data = load_overview_data()
    write_source(data)


def main():
    parser = argparse.ArgumentParser(description="Export/import spreadsheet-friendly class weight CSVs.")
    parser.add_argument(
        "mode",
        nargs="?",
        choices=("export", "import"),
        default="export",
        help="export CSV overview files or import edited CSV files back into class_weights.json",
    )
    parser.add_argument(
        "--skip-risk",
        action="store_true",
        help="skip regenerating class_risk.csv during export",
    )
    args = parser.parse_args()

    if args.mode == "import":
        import_overview()
        return

    export_overview(include_risk=not args.skip_risk)


if __name__ == "__main__":
    main()

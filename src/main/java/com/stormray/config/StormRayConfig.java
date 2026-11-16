package com.stormray.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Server/common configuration, written to config/stormray-common.toml.
 */
public final class StormRayConfig {
    public static final ForgeConfigSpec SPEC;

    // Boss
    public static final ForgeConfigSpec.DoubleValue BOSS_HEALTH;
    public static final ForgeConfigSpec.DoubleValue BOSS_ARMOR;
    public static final ForgeConfigSpec.DoubleValue TENTACLE_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue TENTACLE_REACH;
    public static final ForgeConfigSpec.IntValue TENTACLE_COOLDOWN;
    public static final ForgeConfigSpec.DoubleValue DETECTION_RADIUS;
    public static final ForgeConfigSpec.DoubleValue HOVER_HEIGHT;
    public static final ForgeConfigSpec.DoubleValue LEASH_RADIUS;
    public static final ForgeConfigSpec.EnumValue<DifficultyPreset> DIFFICULTY;

    // Summoning
    public static final ForgeConfigSpec.BooleanValue ENABLE_RITUAL;
    public static final ForgeConfigSpec.BooleanValue REQUIRE_STEVE_HEAD;
    public static final ForgeConfigSpec.IntValue SUMMON_DURATION;
    public static final ForgeConfigSpec.BooleanValue ALLOW_MULTIPLE;
    public static final ForgeConfigSpec.DoubleValue MIN_BOSS_DISTANCE;

    // Environment
    public static final ForgeConfigSpec.DoubleValue ENVIRONMENT_RADIUS;
    public static final ForgeConfigSpec.BooleanValue LIGHTNING_ENABLED;
    public static final ForgeConfigSpec.IntValue LIGHTNING_INTERVAL;

    // Petrification
    public static final ForgeConfigSpec.DoubleValue PETRIFY_RADIUS;
    public static final ForgeConfigSpec.IntValue CHARGE_TICKS;
    public static final ForgeConfigSpec.IntValue PETRIFY_TICKS;
    public static final ForgeConfigSpec.IntValue COOLDOWN_TICKS;
    public static final ForgeConfigSpec.BooleanValue INSTANT_KILL;
    public static final ForgeConfigSpec.DoubleValue PETRIFY_DAMAGE;
    public static final ForgeConfigSpec.BooleanValue AFFECT_PLAYERS;
    public static final ForgeConfigSpec.BooleanValue AFFECT_MOBS;
    public static final ForgeConfigSpec.BooleanValue AFFECT_CREATIVE;

    // Ashen Reflection dimension
    public static final ForgeConfigSpec.BooleanValue PORTAL_ENABLED;
    public static final ForgeConfigSpec.BooleanValue COPY_BUILDS;
    public static final ForgeConfigSpec.BooleanValue HOSTILE_ONLY;
    public static final ForgeConfigSpec.IntValue SYNC_CHUNKS_PER_TICK;
    public static final ForgeConfigSpec.IntValue SYNC_MILLIS_PER_TICK;
    public static final ForgeConfigSpec.IntValue MIRROR_LIGHTNING_INTERVAL;

    // Phantom X
    public static final ForgeConfigSpec.DoubleValue PHANTOM_HEALTH;
    public static final ForgeConfigSpec.DoubleValue PHANTOM_HIT_DAMAGE;
    public static final ForgeConfigSpec.IntValue PHANTOM_COMBO_HITS;
    public static final ForgeConfigSpec.IntValue PHANTOM_COMBO_COOLDOWN;
    public static final ForgeConfigSpec.IntValue PHANTOM_SPAWN_WEIGHT;

    // Phantom Sea
    public static final ForgeConfigSpec.DoubleValue SEA_HEALTH;
    public static final ForgeConfigSpec.DoubleValue SEA_BITE_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue SEA_PULL_STRENGTH;
    public static final ForgeConfigSpec.IntValue SEA_GRAB_TICKS;
    public static final ForgeConfigSpec.DoubleValue SEA_BREAK_FREE_DAMAGE;
    public static final ForgeConfigSpec.IntValue SEA_SPAWN_WEIGHT;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Storm Ray boss stats and behaviour").push("boss");
        BOSS_HEALTH = b.comment("Base health before the difficulty multiplier. Vanilla caps max health at 1024;",
                        "anything above that is emulated by reducing the damage the boss takes.")
                .defineInRange("health", 900.0D, 1.0D, 100000.0D);
        BOSS_ARMOR = b.comment("Armor points (vanilla cap is 30).")
                .defineInRange("armor", 14.0D, 0.0D, 30.0D);
        TENTACLE_DAMAGE = b.comment("Damage of one tentacle lash before the difficulty multiplier.")
                .defineInRange("tentacleDamage", 14.0D, 0.0D, 10000.0D);
        TENTACLE_REACH = b.comment("Radius in blocks hit by a tentacle lash.")
                .defineInRange("tentacleReach", 9.0D, 2.0D, 64.0D);
        TENTACLE_COOLDOWN = b.comment("Ticks between tentacle swoops (20 ticks = 1 second).")
                .defineInRange("tentacleCooldownTicks", 90, 10, 24000);
        DETECTION_RADIUS = b.comment("How far away the boss notices players.")
                .defineInRange("detectionRadius", 128.0D, 16.0D, 1024.0D);
        HOVER_HEIGHT = b.comment("How high above its target the boss hovers.")
                .defineInRange("hoverHeight", 14.0D, 4.0D, 64.0D);
        LEASH_RADIUS = b.comment("When idle, the boss drifts back if it is farther than this from where it was summoned.")
                .defineInRange("leashRadius", 96.0D, 16.0D, 4096.0D);
        DIFFICULTY = b.comment("Preset multiplier for health, damage and cooldowns: CASUAL, NORMAL, HARD, NIGHTMARE.")
                .defineEnum("difficulty", DifficultyPreset.NORMAL);
        b.pop();

        b.comment("Redstone ritual").push("summoning");
        ENABLE_RITUAL = b.comment("Allow summoning with the redstone T + Steve head ritual.")
                .define("enableRitual", true);
        REQUIRE_STEVE_HEAD = b.comment("Only accept a Steve head (a player head without owner, or owned by 'Steve').",
                        "Set to false to accept any player head.")
                .define("requireSteveHead", true);
        SUMMON_DURATION = b.comment("Length of the emergence sequence in ticks.")
                .defineInRange("summonDurationTicks", 140, 20, 2400);
        ALLOW_MULTIPLE = b.comment("Allow more than one Storm Ray near each other.")
                .define("allowMultipleBosses", false);
        MIN_BOSS_DISTANCE = b.comment("When multiple bosses are not allowed, a new ritual fails within this distance of a living Storm Ray.")
                .defineInRange("minDistanceBetweenBosses", 256.0D, 0.0D, 100000.0D);
        b.pop();

        b.comment("Red sky event").push("environment");
        ENVIRONMENT_RADIUS = b.comment("Players within this horizontal distance of the boss see the red sky, fog and boss bar.")
                .defineInRange("radius", 160.0D, 16.0D, 2048.0D);
        LIGHTNING_ENABLED = b.comment("Spawn harmless visual lightning around the boss.")
                .define("lightning", true);
        LIGHTNING_INTERVAL = b.comment("Average ticks between distant lightning strikes.")
                .defineInRange("lightningAverageIntervalTicks", 60, 5, 24000);
        b.pop();

        b.comment("Petrification pulse").push("petrification");
        PETRIFY_RADIUS = b.comment("Horizontal radius of the petrification pulse.")
                .defineInRange("radius", 22.0D, 2.0D, 256.0D);
        CHARGE_TICKS = b.comment("Ticks the boss charges before releasing the pulse.")
                .defineInRange("chargeTicks", 80, 10, 2400);
        PETRIFY_TICKS = b.comment("Ticks an entity spends turning to stone before the effect resolves.")
                .defineInRange("petrifyTicks", 60, 5, 2400);
        COOLDOWN_TICKS = b.comment("Ticks between petrification pulses (before the difficulty multiplier).")
                .defineInRange("cooldownTicks", 600, 40, 72000);
        INSTANT_KILL = b.comment("If true, petrified entities die when the effect resolves (a Totem of Undying still saves players).",
                        "If false, they take 'damage' and are released.")
                .define("instantKill", true);
        PETRIFY_DAMAGE = b.comment("Damage dealt when instantKill is false (before the difficulty multiplier).")
                .defineInRange("damage", 30.0D, 0.0D, 100000.0D);
        AFFECT_PLAYERS = b.comment("Whether players can be petrified.")
                .define("affectPlayers", true);
        AFFECT_MOBS = b.comment("Whether mobs and animals can be petrified.")
                .define("affectMobs", true);
        AFFECT_CREATIVE = b.comment("Whether creative-mode players can be petrified.")
                .define("affectCreativePlayers", false);
        b.pop();

        b.comment("Ashen Reflection: the decayed mirror of the overworld").push("dimension");
        PORTAL_ENABLED = b.comment("Allow opening portals: a 4x4 hole ringed with redstone blocks, then throw a Steve head inside.")
                .define("enablePortal", true);
        COPY_BUILDS = b.comment("Copy player-made changes from the overworld the first time a mirror chunk is loaded.",
                        "Container contents are never copied, so items cannot be duplicated.")
                .define("copyBuilds", true);
        HOSTILE_ONLY = b.comment("Only hostile monsters may exist in the mirror (no animals, villagers, fish or bats).")
                .define("hostileOnly", true);
        SYNC_CHUNKS_PER_TICK = b.comment("Maximum mirror chunks synchronised with the overworld per server tick.")
                .defineInRange("syncChunksPerTick", 4, 1, 64);
        SYNC_MILLIS_PER_TICK = b.comment("Time budget in milliseconds for chunk synchronisation per server tick.")
                .defineInRange("syncMillisPerTick", 8, 1, 45);
        MIRROR_LIGHTNING_INTERVAL = b.comment("Average ticks between harmless red lightning strikes near each player in the mirror.")
                .defineInRange("lightningAverageIntervalTicks", 90, 5, 24000);
        b.pop();

        b.comment("Phantom X: the soot wraith of the Ashen Reflection").push("phantom_x");
        PHANTOM_HEALTH = b.comment("Max health (vanilla Enderman has 40).")
                .defineInRange("health", 40.0D, 1.0D, 1024.0D);
        PHANTOM_HIT_DAMAGE = b.comment("Damage of each hit in the combo (2 = one heart).")
                .defineInRange("hitDamage", 3.0D, 0.0D, 1000.0D);
        PHANTOM_COMBO_HITS = b.comment("Hits per combo.")
                .defineInRange("comboHits", 4, 1, 8);
        PHANTOM_COMBO_COOLDOWN = b.comment("Ticks between combos.")
                .defineInRange("comboCooldownTicks", 50, 10, 1200);
        PHANTOM_SPAWN_WEIGHT = b.comment("Natural spawn weight in the Ashen Reflection (zombie = 95, skeleton = 100). 0 disables.")
                .defineInRange("spawnWeight", 100, 0, 1000);
        b.pop();

        b.comment("Phantom Sea: drowned wraith that drags swimmers to the sea floor").push("phantom_sea");
        SEA_HEALTH = b.comment("Max health.")
                .defineInRange("health", 30.0D, 1.0D, 1024.0D);
        SEA_BITE_DAMAGE = b.comment("Damage of each bite while holding its prey (once per second).")
                .defineInRange("biteDamage", 3.0D, 0.0D, 1000.0D);
        SEA_PULL_STRENGTH = b.comment("Downward speed it drags its prey with (blocks per tick).")
                .defineInRange("pullStrength", 0.2D, 0.01D, 2.0D);
        SEA_GRAB_TICKS = b.comment("Maximum ticks it holds on before letting go.")
                .defineInRange("grabDurationTicks", 120, 10, 2400);
        SEA_BREAK_FREE_DAMAGE = b.comment("Damage the prey must deal to it to break free early.")
                .defineInRange("breakFreeDamage", 6.0D, 0.0D, 1000.0D);
        SEA_SPAWN_WEIGHT = b.comment("Spawn weight in the Ashen Reflection's water (only counted for underwater positions). 0 disables.")
                .defineInRange("spawnWeight", 150, 0, 1000);
        b.pop();

        SPEC = b.build();
    }

    private StormRayConfig() {
    }
}

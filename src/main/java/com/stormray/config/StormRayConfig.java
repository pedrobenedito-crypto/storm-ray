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
    public static final ForgeConfigSpec.DoubleValue BOSS_BAR_RADIUS;
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
    public static final ForgeConfigSpec.BooleanValue LIVE_SYNC;
    public static final ForgeConfigSpec.IntValue SYNC_COLUMNS_PER_TICK;
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

    public static final ForgeConfigSpec.BooleanValue ISLANDS_IN_OVERWORLD;
    public static final ForgeConfigSpec.DoubleValue ISLANDS_OVERWORLD_CHANCE;

    // Phantom gear
    public static final ForgeConfigSpec.BooleanValue GAZE_ENABLED;
    public static final ForgeConfigSpec.DoubleValue GAZE_RANGE;
    public static final ForgeConfigSpec.IntValue GAZE_COOLDOWN;
    public static final ForgeConfigSpec.IntValue GAZE_PETRIFY_TICKS;
    public static final ForgeConfigSpec.DoubleValue GAZE_DAMAGE;
    public static final ForgeConfigSpec.IntValue SWORD_FIRE_SECONDS;
    public static final ForgeConfigSpec.DoubleValue SWORD_DASH_POWER;
    public static final ForgeConfigSpec.IntValue SWORD_DASH_TICKS;
    public static final ForgeConfigSpec.IntValue SWORD_DASH_COOLDOWN;

    // Phantom Bat
    public static final ForgeConfigSpec.DoubleValue BAT_HEALTH;
    public static final ForgeConfigSpec.DoubleValue BAT_BITE_DAMAGE;
    public static final ForgeConfigSpec.IntValue BAT_BLEED_TICKS;
    public static final ForgeConfigSpec.IntValue BAT_SPAWN_WEIGHT;
    public static final ForgeConfigSpec.IntValue BAT_OVERWORLD_SPAWN_WEIGHT;

    // Ruined Colosseum
    public static final ForgeConfigSpec.DoubleValue COLOSSEUM_BOSS_HEALTH;
    public static final ForgeConfigSpec.IntValue COLOSSEUM_BAT_WAVE;
    public static final ForgeConfigSpec.IntValue COLOSSEUM_MAX_BATS;
    public static final ForgeConfigSpec.BooleanValue COLOSSEUM_BOSS;
    public static final ForgeConfigSpec.IntValue COLOSSEUM_RESPAWN_TICKS;
    public static final ForgeConfigSpec.IntValue COLOSSEUM_GUARDS;
    public static final ForgeConfigSpec.IntValue COLOSSEUM_WAVE_INTERVAL;
    public static final ForgeConfigSpec.IntValue COLOSSEUM_MAX_PHANTOMS;
    public static final ForgeConfigSpec.IntValue COLOSSEUM_PETRIFY_WAVE;
    public static final ForgeConfigSpec.IntValue COLOSSEUM_MAX_WAVES;

    // Eye lasers
    public static final ForgeConfigSpec.DoubleValue LASER_CHANCE;
    public static final ForgeConfigSpec.DoubleValue LASER_DAMAGE;
    public static final ForgeConfigSpec.BooleanValue LASER_FIRE;

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

        b.comment("Eye lasers: twin beams that sweep after the target and set the ground on fire").push("laser");
        LASER_CHANCE = b.comment("Chance that an attack is the eye lasers instead of the tentacle swoop (0.2 = 20%). 0 disables.")
                .defineInRange("chance", 0.2D, 0.0D, 1.0D);
        LASER_DAMAGE = b.comment("Damage per hit while standing where the beams land (about two hits per second), before the difficulty multiplier.")
                .defineInRange("damage", 4.0D, 0.0D, 1000.0D);
        LASER_FIRE = b.comment("The beams set the ground on fire (also needs the mobGriefing game rule).")
                .define("setFire", true);
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
        BOSS_BAR_RADIUS = b.comment("Players within this horizontal distance of the boss see its health bar.",
                        "Independent of the red sky, so the bar stays up while the boss rises and while it dies.")
                .defineInRange("bossBarRadius", 192.0D, 16.0D, 2048.0D);
        ENVIRONMENT_RADIUS = b.comment("Players within this horizontal distance of the boss see the red sky and fog.")
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
        ISLANDS_IN_OVERWORLD = b.comment("Sky Islands also float in the normal world (same places as in the Ashen Reflection).",
                        "The Ruined Colosseum never generates in the normal world.")
                .define("skyIslandsInOverworld", true);
        ISLANDS_OVERWORLD_CHANCE = b.comment("Share of the island spots that are actually built in the normal world",
                        "(0.15 = one in about seven, so they are far rarer there than in the Ashen Reflection).")
                .defineInRange("skyIslandsOverworldChance", 0.15D, 0.0D, 1.0D);
        LIVE_SYNC = b.comment("Keep copying: every later block change in the overworld is also applied to the mirror",
                        "(needs copyBuilds). The overworld wins: a synced column replaces mirror-side changes in that column.")
                .define("liveSync", true);
        SYNC_COLUMNS_PER_TICK = b.comment("Maximum changed block columns copied to the mirror per server tick.")
                .defineInRange("syncColumnsPerTick", 64, 1, 4096);
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
                .defineInRange("naturalSpawnWeight", 160, 0, 1000);
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
                .defineInRange("naturalSpawnWeight", 250, 0, 1000);
        b.pop();

        b.comment("Phantom gear: armour and sword forged from the scales of a slain Storm Ray").push("gear");
        GAZE_ENABLED = b.comment("A full set of phantom armour lets the wearer petrify what they look at.")
                .define("petrifyingGaze", true);
        GAZE_RANGE = b.comment("How far the gaze reaches, in blocks.")
                .defineInRange("gazeRange", 24.0D, 2.0D, 128.0D);
        GAZE_COOLDOWN = b.comment("Ticks before the gaze can be used again (600 = 30 seconds).",
                        "Shown as a cooldown on the helmet.")
                .defineInRange("gazeCooldownTicks", 600, 0, 72000);
        GAZE_PETRIFY_TICKS = b.comment("Ticks the victim spends turning to stone.")
                .defineInRange("gazePetrifyTicks", 60, 5, 2400);
        GAZE_DAMAGE = b.comment("Damage dealt when the petrification completes. The boss's instantKill never applies here.")
                .defineInRange("gazeDamage", 20.0D, 0.0D, 1000.0D);
        SWORD_FIRE_SECONDS = b.comment("Seconds the phantom sword sets its target on fire (Fire Aspect II burns for 8).")
                .defineInRange("swordFireSeconds", 8, 0, 120);
        SWORD_DASH_POWER = b.comment("Strength of the right-click dash.")
                .defineInRange("swordDashPower", 1.9D, 0.1D, 10.0D);
        SWORD_DASH_TICKS = b.comment("Ticks the dasher stays invisible (30 = 1.5 seconds).")
                .defineInRange("swordDashInvisibleTicks", 30, 1, 1200);
        SWORD_DASH_COOLDOWN = b.comment("Ticks before the dash can be used again (120 = 6 seconds).")
                .defineInRange("swordDashCooldownTicks", 120, 0, 72000);
        b.pop();

        b.comment("Phantom Bat: soot bat whose bite causes bleeding").push("phantom_bat");
        BAT_HEALTH = b.comment("Max health.")
                .defineInRange("health", 8.0D, 1.0D, 1024.0D);
        BAT_BITE_DAMAGE = b.comment("Damage of the bite itself (reduced by armor).")
                .defineInRange("biteDamage", 2.0D, 0.0D, 1000.0D);
        BAT_BLEED_TICKS = b.comment("Length of the bleeding left by a bite (100 = 5 seconds = 2 hearts, ignoring armor). 0 disables.")
                .defineInRange("bleedTicks", 100, 0, 6000);
        BAT_SPAWN_WEIGHT = b.comment("Natural spawn weight in the Ashen Reflection (zombie = 95). 0 disables.")
                .defineInRange("naturalSpawnWeight", 130, 0, 1000);
        BAT_OVERWORLD_SPAWN_WEIGHT = b.comment("Natural spawn weight in the normal world, in the dark like other monsters (zombie = 95). 0 disables.")
                .defineInRange("overworldSpawnWeight", 20, 0, 1000);
        b.pop();

        b.comment("Ruined Colosseum: the single great arena of each world").push("colosseum");
        COLOSSEUM_BOSS = b.comment("A Storm Ray rises from the arena when a survival player steps onto its floor.")
                .define("awakenBoss", true);
        COLOSSEUM_RESPAWN_TICKS = b.comment("Ticks the arena rests after its Storm Ray dies before it can wake again",
                        "(72000 = 3 Minecraft days). 0 = the fight happens only once per world.")
                .defineInRange("respawnTicks", 72000, 0, Integer.MAX_VALUE);
        COLOSSEUM_BOSS_HEALTH = b.comment("Health multiplier for the colosseum's Storm Ray (on top of boss.health and the difficulty preset).",
                        "Below 1 because the guardian does not fight alone.")
                .defineInRange("bossHealthMultiplier", 0.75D, 0.05D, 10.0D);
        COLOSSEUM_GUARDS = b.comment("Phantom X in the first wave, which appears together with the Storm Ray.",
                        "Every second wave after that brings one more.")
                .defineInRange("firstWavePhantoms", 2, 0, 16);
        COLOSSEUM_WAVE_INTERVAL = b.comment("Ticks between waves while the colosseum's Storm Ray is alive and players are near",
                        "(900 = 45 seconds). 0 disables the extra waves.")
                .defineInRange("ticksBetweenWaves", 900, 0, 72000);
        COLOSSEUM_MAX_PHANTOMS = b.comment("Waves never raise the number of living Phantom X around the arena above this.")
                .defineInRange("maxPhantomX", 6, 1, 64);
        COLOSSEUM_BAT_WAVE = b.comment("Phantom Bats join the waves from this wave on (2 bats, one more every second wave).")
                .defineInRange("batsFromWave", 3, 1, 1000);
        COLOSSEUM_MAX_BATS = b.comment("Waves never raise the number of living Phantom Bats around the arena above this. 0 = no bats.")
                .defineInRange("maxBats", 6, 0, 64);
        COLOSSEUM_MAX_WAVES = b.comment("The fight has at most this many waves; after the last one no more mobs are sent",
                        "and only the Storm Ray itself is left to beat.")
                .defineInRange("maxWaves", 9, 1, 1000);
        COLOSSEUM_PETRIFY_WAVE = b.comment("The colosseum's Storm Ray only starts using its petrification pulse from this wave on",
                        "(1 = from the start). Storm Rays summoned by the ritual are not affected.")
                .defineInRange("petrifyFromWave", 4, 1, 1000);
        b.pop();

        SPEC = b.build();
    }

    private StormRayConfig() {
    }
}

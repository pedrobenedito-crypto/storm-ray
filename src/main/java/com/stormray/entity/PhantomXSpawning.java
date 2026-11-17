package com.stormray.entity;

import com.stormray.StormRayMod;
import com.stormray.config.StormRayConfig;
import com.stormray.dimension.ModDimensions;
import com.stormray.registry.ModEntities;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Natural spawning of the Phantom X and Phantom Bat (on land) and the Phantom Sea (in water).
 * They are added to the monster spawn list only for positions inside the Ashen Reflection (plus
 * a few bats in the normal world), so the overworld, which shares the same biomes, stays as it is.
 *
 * <p>The spawn entries are kept and reused. The game asks for the list twice per spawn attempt
 * (once to pick a mob, once to confirm the pick is still in the list) and compares entries by
 * identity, so handing out a fresh entry each time would make every attempt fail.</p>
 */
public final class PhantomXSpawning {
    private static MobSpawnSettings.SpawnerData phantomEntry;
    private static MobSpawnSettings.SpawnerData seaEntry;
    private static MobSpawnSettings.SpawnerData batEntry;
    private static MobSpawnSettings.SpawnerData overworldBatEntry;

    private PhantomXSpawning() {
    }

    /** Returns the existing entry unless the configured weight changed. */
    private static MobSpawnSettings.SpawnerData entry(MobSpawnSettings.SpawnerData current, EntityType<?> type, int weight,
                                                      int minCount, int maxCount) {
        if (current != null && current.getWeight().asInt() == weight) {
            return current;
        }
        return new MobSpawnSettings.SpawnerData(type, weight, minCount, maxCount);
    }

    @Mod.EventBusSubscriber(modid = StormRayMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ForgeEvents {
        private ForgeEvents() {
        }

        @SubscribeEvent
        public static void onPotentialSpawns(LevelEvent.PotentialSpawns event) {
            if (event.getMobCategory() != MobCategory.MONSTER) {
                return;
            }
            boolean water = event.getLevel().getFluidState(event.getPos()).is(FluidTags.WATER);
            if (!ModDimensions.isMirror(event.getLevel())) {
                // The normal world only gets the bats, and far fewer of them.
                int overworldBats = StormRayConfig.BAT_OVERWORLD_SPAWN_WEIGHT.get();
                if (overworldBats > 0 && !water && event.getLevel() instanceof Level level && level.dimension() == Level.OVERWORLD) {
                    overworldBatEntry = entry(overworldBatEntry, ModEntities.PHANTOM_BAT.get(), overworldBats, 2, 3);
                    event.addSpawnerData(overworldBatEntry);
                }
                return;
            }
            if (water) {
                // underwater spawn attempts: the Phantom Sea
                int seaWeight = StormRayConfig.SEA_SPAWN_WEIGHT.get();
                if (seaWeight > 0) {
                    seaEntry = entry(seaEntry, ModEntities.PHANTOM_SEA.get(), seaWeight, 1, 3);
                    event.addSpawnerData(seaEntry);
                }
                return;
            }
            int weight = StormRayConfig.PHANTOM_SPAWN_WEIGHT.get();
            if (weight > 0) {
                phantomEntry = entry(phantomEntry, ModEntities.PHANTOM_X.get(), weight, 1, 3);
                event.addSpawnerData(phantomEntry);
            }
            int batWeight = StormRayConfig.BAT_SPAWN_WEIGHT.get();
            if (batWeight > 0) {
                batEntry = entry(batEntry, ModEntities.PHANTOM_BAT.get(), batWeight, 2, 4);
                event.addSpawnerData(batEntry);
            }
        }
    }

    @Mod.EventBusSubscriber(modid = StormRayMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        private ModEvents() {
        }

        @SubscribeEvent
        public static void registerPlacements(SpawnPlacementRegisterEvent event) {
            event.register(ModEntities.PHANTOM_X.get(), SpawnPlacements.Type.ON_GROUND,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
                    SpawnPlacementRegisterEvent.Operation.REPLACE);
            event.register(ModEntities.PHANTOM_BAT.get(), SpawnPlacements.Type.ON_GROUND,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
                    SpawnPlacementRegisterEvent.Operation.REPLACE);
            event.register(ModEntities.PHANTOM_SEA.get(), SpawnPlacements.Type.IN_WATER,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, PhantomSeaEntity::checkSpawnRules,
                    SpawnPlacementRegisterEvent.Operation.REPLACE);
        }
    }
}

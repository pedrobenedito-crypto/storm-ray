package com.stormray.entity;

import com.stormray.StormRayMod;
import com.stormray.config.StormRayConfig;
import com.stormray.dimension.ModDimensions;
import com.stormray.registry.ModEntities;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Natural spawning of the Phantom X (on land) and the Phantom Sea (in water). It is added to the monster spawn list only for positions
 * inside the Ashen Reflection, so the overworld (which shares the same biomes) is untouched.
 */
public final class PhantomXSpawning {
    private PhantomXSpawning() {
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
            if (!ModDimensions.isMirror(event.getLevel())) {
                // The normal world only gets the bats, and far fewer of them.
                int overworldBats = StormRayConfig.BAT_OVERWORLD_SPAWN_WEIGHT.get();
                if (overworldBats > 0 && event.getLevel() instanceof net.minecraft.world.level.Level level
                        && level.dimension() == net.minecraft.world.level.Level.OVERWORLD
                        && !level.getFluidState(event.getPos()).is(FluidTags.WATER)) {
                    event.addSpawnerData(new MobSpawnSettings.SpawnerData(ModEntities.PHANTOM_BAT.get(), overworldBats, 2, 3));
                }
                return;
            }
            if (event.getLevel().getFluidState(event.getPos()).is(FluidTags.WATER)) {
                // underwater spawn attempts: the Phantom Sea
                int seaWeight = StormRayConfig.SEA_SPAWN_WEIGHT.get();
                if (seaWeight > 0) {
                    event.addSpawnerData(new MobSpawnSettings.SpawnerData(ModEntities.PHANTOM_SEA.get(), seaWeight, 1, 3));
                }
                return;
            }
            int weight = StormRayConfig.PHANTOM_SPAWN_WEIGHT.get();
            if (weight > 0) {
                event.addSpawnerData(new MobSpawnSettings.SpawnerData(ModEntities.PHANTOM_X.get(), weight, 1, 2));
            }
            int batWeight = StormRayConfig.BAT_SPAWN_WEIGHT.get();
            if (batWeight > 0) {
                event.addSpawnerData(new MobSpawnSettings.SpawnerData(ModEntities.PHANTOM_BAT.get(), batWeight, 2, 4));
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

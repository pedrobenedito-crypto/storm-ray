package com.stormray.registry;

import com.stormray.StormRayMod;
import com.stormray.entity.PhantomSeaEntity;
import com.stormray.entity.PhantomXEntity;
import com.stormray.entity.StormRayEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, StormRayMod.MODID);

    /** Hitbox covers the body; wings and tail are purely visual. */
    public static final RegistryObject<EntityType<StormRayEntity>> STORM_RAY = ENTITY_TYPES.register("storm_ray",
            () -> EntityType.Builder.<StormRayEntity>of(StormRayEntity::new, MobCategory.MONSTER)
                    .sized(12.0F, 11.0F)
                    .fireImmune()
                    .clientTrackingRange(20)
                    .updateInterval(1)
                    .build(StormRayMod.id("storm_ray").toString()));

    /** Soot wraith of the Ashen Reflection; spawns naturally only there (see PhantomXSpawning). */
    public static final RegistryObject<EntityType<PhantomXEntity>> PHANTOM_X = ENTITY_TYPES.register("phantom_x",
            () -> EntityType.Builder.<PhantomXEntity>of(PhantomXEntity::new, MobCategory.MONSTER)
                    .sized(0.7F, 2.6F)
                    .fireImmune()
                    .clientTrackingRange(8)
                    .build(StormRayMod.id("phantom_x").toString()));

    /** Drowned wraith of the mirror's oceans; drags swimmers to the sea floor. */
    public static final RegistryObject<EntityType<PhantomSeaEntity>> PHANTOM_SEA = ENTITY_TYPES.register("phantom_sea",
            () -> EntityType.Builder.<PhantomSeaEntity>of(PhantomSeaEntity::new, MobCategory.MONSTER)
                    .sized(1.1F, 0.9F)
                    .clientTrackingRange(8)
                    .build(StormRayMod.id("phantom_sea").toString()));

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(PHANTOM_SEA.get(), PhantomSeaEntity.createAttributes().build());
        event.put(STORM_RAY.get(), StormRayEntity.createAttributes().build());
        event.put(PHANTOM_X.get(), PhantomXEntity.createAttributes().build());
    }

    private ModEntities() {
    }
}

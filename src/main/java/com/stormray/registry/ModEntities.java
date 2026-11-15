package com.stormray.registry;

import com.stormray.StormRayMod;
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

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(STORM_RAY.get(), StormRayEntity.createAttributes().build());
    }

    private ModEntities() {
    }
}

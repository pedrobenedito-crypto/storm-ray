package com.stormray.registry;

import com.stormray.StormRayMod;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, StormRayMod.MODID);

    public static final RegistryObject<SoundEvent> SUMMON = register("entity.storm_ray.summon");
    public static final RegistryObject<SoundEvent> AMBIENT = register("entity.storm_ray.ambient");
    public static final RegistryObject<SoundEvent> HURT = register("entity.storm_ray.hurt");
    public static final RegistryObject<SoundEvent> DEATH = register("entity.storm_ray.death");
    public static final RegistryObject<SoundEvent> TENTACLE = register("entity.storm_ray.tentacle");
    public static final RegistryObject<SoundEvent> CHARGE = register("entity.storm_ray.charge");
    public static final RegistryObject<SoundEvent> PULSE = register("entity.storm_ray.pulse");
    public static final RegistryObject<SoundEvent> PETRIFY = register("entity.storm_ray.petrify");
    public static final RegistryObject<SoundEvent> LIGHTNING = register("environment.storm.lightning");
    public static final RegistryObject<SoundEvent> STORM_LOOP = register("environment.storm.loop");
    public static final RegistryObject<SoundEvent> RITUAL_IGNITE = register("ritual.ignite");
    public static final RegistryObject<SoundEvent> PORTAL_OPEN = register("block.ashen_portal.open");
    public static final RegistryObject<SoundEvent> PORTAL_TRAVEL = register("block.ashen_portal.travel");
    public static final RegistryObject<SoundEvent> PORTAL_AMBIENT = register("block.ashen_portal.ambient");

    public static final RegistryObject<SoundEvent> PHANTOM_AMBIENT = register("entity.phantom_x.ambient");
    public static final RegistryObject<SoundEvent> PHANTOM_HURT = register("entity.phantom_x.hurt");
    public static final RegistryObject<SoundEvent> PHANTOM_DEATH = register("entity.phantom_x.death");
    public static final RegistryObject<SoundEvent> PHANTOM_SLASH = register("entity.phantom_x.slash");
    public static final RegistryObject<SoundEvent> PHANTOM_CLOAK = register("entity.phantom_x.cloak");
    public static final RegistryObject<SoundEvent> PHANTOM_REVEAL = register("entity.phantom_x.reveal");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(StormRayMod.id(name)));
    }

    private ModSounds() {
    }
}

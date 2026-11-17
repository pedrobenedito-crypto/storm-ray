package com.stormray.registry;

import com.stormray.StormRayMod;
import com.stormray.effect.StoneBreakerEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, StormRayMod.MODID);
    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(ForgeRegistries.POTIONS, StormRayMod.MODID);

    /** Eight minutes of immunity to petrification. */
    public static final int STONE_BREAKER_TICKS = 8 * 60 * 20;

    public static final RegistryObject<MobEffect> STONE_BREAKER =
            MOB_EFFECTS.register("stone_breaker", StoneBreakerEffect::new);

    /** A vanilla-style potion (also splash, lingering and tipped arrow) with black liquid. */
    public static final RegistryObject<Potion> STONE_BREAKER_POTION = POTIONS.register("stone_breaker",
            () -> new Potion(new MobEffectInstance(STONE_BREAKER.get(), STONE_BREAKER_TICKS)));

    private ModEffects() {
    }
}

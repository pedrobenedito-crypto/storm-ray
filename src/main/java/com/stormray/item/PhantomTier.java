package com.stormray.item;

import com.stormray.registry.ModItems;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

/** Tool tier of the phantom scales: diamond-grade mining, netherite-grade bite. */
public enum PhantomTier implements Tier {
    PHANTOM;

    @Override
    public int getUses() {
        return 1800;
    }

    @Override
    public float getSpeed() {
        return 8.0F;
    }

    @Override
    public float getAttackDamageBonus() {
        return 4.0F;
    }

    @Override
    public int getLevel() {
        return 3;
    }

    @Override
    public int getEnchantmentValue() {
        return 15;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(ModItems.REINFORCED_PHANTOM_SCALE.get());
    }

}

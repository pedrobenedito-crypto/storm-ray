package com.stormray.item;

import com.stormray.StormRayMod;
import com.stormray.registry.ModItems;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Armour forged from the scales of a Storm Ray: a little tougher than diamond, a little
 * short of netherite, and repaired with more scales. The name carries the mod id, which is
 * how Forge finds textures/models/armor/phantom_layer_N.png inside this mod's assets.
 */
public enum PhantomArmorMaterial implements ArmorMaterial {
    PHANTOM;

    private static final Map<ArmorItem.Type, Integer> DEFENCE = new EnumMap<>(Map.of(
            ArmorItem.Type.BOOTS, 3,
            ArmorItem.Type.LEGGINGS, 6,
            ArmorItem.Type.CHESTPLATE, 8,
            ArmorItem.Type.HELMET, 3));
    private static final Map<ArmorItem.Type, Integer> DURABILITY = new EnumMap<>(Map.of(
            ArmorItem.Type.BOOTS, 13,
            ArmorItem.Type.LEGGINGS, 15,
            ArmorItem.Type.CHESTPLATE, 16,
            ArmorItem.Type.HELMET, 11));
    private static final int DURABILITY_FACTOR = 37;

    private final Supplier<Ingredient> repair = () -> Ingredient.of(ModItems.PHANTOM_SCALE.get());

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return DURABILITY.get(type) * DURABILITY_FACTOR;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return DEFENCE.get(type);
    }

    @Override
    public int getEnchantmentValue() {
        return 15;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_NETHERITE;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return this.repair.get();
    }

    @Override
    public String getName() {
        return StormRayMod.MODID + ":phantom";
    }

    @Override
    public float getToughness() {
        return 2.5F;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.1F;
    }
}

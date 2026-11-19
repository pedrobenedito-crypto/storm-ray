package com.stormray.item;

import com.stormray.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A piece of phantom armour: black plating overgrown with vine, carrying the enchanted sheen
 * of the ray it was cut from. Wearing the whole set grants the petrifying gaze
 * (see {@link com.stormray.ability.PetrifyGaze}).
 */
public class PhantomArmorItem extends ArmorItem {
    public PhantomArmorItem(ArmorItem.Type type, Properties properties) {
        super(PhantomArmorMaterial.PHANTOM, type, properties);
    }

    /** True when all four pieces are worn. */
    public static boolean hasFullSet(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.PHANTOM_HELMET.get())
                && entity.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.PHANTOM_CHESTPLATE.get())
                && entity.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.PHANTOM_LEGGINGS.get())
                && entity.getItemBySlot(EquipmentSlot.FEET).is(ModItems.PHANTOM_BOOTS.get());
    }

    /** The sheen is part of the material, so the armour always looks enchanted. */
    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.stormray.phantom_armor.tooltip").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.stormray.phantom_armor.tooltip.ability").withStyle(ChatFormatting.DARK_PURPLE));
    }
}

package com.stormray.portal;

import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.stormray.util.SteveNames;

import javax.annotation.Nullable;

/** A player head without an owner renders as Steve; heads owned by "Steve"/"MHF_Steve" also count. */
public final class SteveHeads {
    private SteveHeads() {
    }

    public static boolean isSteveName(@Nullable String name, boolean hasId) {
        return SteveNames.isSteve(name, hasId);
    }

    public static boolean isSteve(@Nullable GameProfile owner) {
        return owner == null || isSteveName(owner.getName(), owner.getId() != null);
    }

    public static boolean isSteve(ItemStack stack) {
        if (!stack.is(Items.PLAYER_HEAD)) {
            return false;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("SkullOwner")) {
            return true;
        }
        if (tag.contains("SkullOwner", Tag.TAG_STRING)) {
            return isSteveName(tag.getString("SkullOwner"), false);
        }
        CompoundTag owner = tag.getCompound("SkullOwner");
        return isSteveName(owner.getString("Name"), owner.hasUUID("Id"));
    }
}

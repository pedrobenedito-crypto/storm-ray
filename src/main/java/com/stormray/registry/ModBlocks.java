package com.stormray.registry;

import com.stormray.StormRayMod;
import com.stormray.portal.AshenPortalBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, StormRayMod.MODID);

    /** The open gateway. No item: it only exists inside a lit redstone frame. */
    public static final RegistryObject<Block> ASHEN_PORTAL = BLOCKS.register("ashen_portal",
            () -> new AshenPortalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .noCollission()
                    .strength(-1.0F, 3600000.0F)
                    .lightLevel(state -> 11)
                    .noLootTable()
                    .pushReaction(PushReaction.BLOCK)
                    .sound(SoundType.GLASS)));

    private ModBlocks() {
    }
}

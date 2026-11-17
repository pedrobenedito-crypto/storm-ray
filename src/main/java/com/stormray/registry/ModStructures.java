package com.stormray.registry;

import com.mojang.serialization.Codec;
import com.stormray.StormRayMod;
import com.stormray.dimension.ModDimensions;
import com.stormray.structure.ColosseumPiece;
import com.stormray.structure.ColosseumStructure;
import com.stormray.structure.SkyIslandPiece;
import com.stormray.structure.SkyIslandStructure;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * World structures: the Ruined Colosseum (exactly one per world, placed like the stronghold
 * ring, in the overworld and mirrored in the Ashen Reflection) and the Sky Islands of the
 * Ashen Reflection, whose chest holds the Colosseum Locator.
 * Placement lives in data/stormray/worldgen/structure and structure_set.
 */
public final class ModStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, StormRayMod.MODID);
    public static final DeferredRegister<StructurePieceType> PIECE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, StormRayMod.MODID);

    public static final RegistryObject<StructureType<ColosseumStructure>> COLOSSEUM =
            STRUCTURE_TYPES.register("ruined_colosseum", () -> typeOf(ColosseumStructure.CODEC));
    public static final RegistryObject<StructureType<SkyIslandStructure>> SKY_ISLAND =
            STRUCTURE_TYPES.register("sky_island", () -> typeOf(SkyIslandStructure.CODEC));

    public static final RegistryObject<StructurePieceType> COLOSSEUM_PIECE =
            PIECE_TYPES.register("ruined_colosseum", () -> contextless(ColosseumPiece::new));
    public static final RegistryObject<StructurePieceType> SKY_ISLAND_PIECE =
            PIECE_TYPES.register("sky_island", () -> contextless(SkyIslandPiece::new));

    public static final ResourceKey<Structure> COLOSSEUM_KEY =
            ResourceKey.create(Registries.STRUCTURE, StormRayMod.id("ruined_colosseum"));
    public static final ResourceKey<Structure> SKY_ISLAND_KEY =
            ResourceKey.create(Registries.STRUCTURE, StormRayMod.id("sky_island"));
    /** Structure tag used by the locator item (data/stormray/tags/worldgen/structure). */
    public static final TagKey<Structure> COLOSSEUM_TAG =
            TagKey.create(Registries.STRUCTURE, StormRayMod.id("ruined_colosseum"));
    public static final ResourceLocation SKY_ISLAND_LOOT = StormRayMod.id("chests/sky_island");

    private ModStructures() {
    }

    private static <S extends Structure> StructureType<S> typeOf(Codec<S> codec) {
        return () -> codec;
    }

    private static StructurePieceType contextless(StructurePieceType.ContextlessType type) {
        return type;
    }

    /**
     * True if the generator belongs to the Ashen Reflection. Structures only see their chunk
     * generator while choosing a position, and the mirror shares biomes and noise with the
     * overworld, so the generator instance is the one thing that tells the two worlds apart.
     */
    public static boolean isMirrorGenerator(ChunkGenerator generator) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return false;
        }
        ServerLevel mirror = server.getLevel(ModDimensions.ASHEN_LEVEL);
        return mirror != null && mirror.getChunkSource().getGenerator() == generator;
    }

    /** True if a Colosseum or Sky Island overlaps this chunk. */
    public static boolean touchesModStructure(ServerLevel level, ChunkAccess chunk) {
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Structure colosseum = registry.get(COLOSSEUM_KEY);
        if (colosseum != null && !chunk.getReferencesForStructure(colosseum).isEmpty()) {
            return true;
        }
        Structure island = registry.get(SKY_ISLAND_KEY);
        return island != null && !chunk.getReferencesForStructure(island).isEmpty();
    }
}

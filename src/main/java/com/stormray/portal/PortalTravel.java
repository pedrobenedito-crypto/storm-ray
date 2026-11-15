package com.stormray.portal;

import com.stormray.StormRayMod;
import com.stormray.dimension.ModDimensions;
import com.stormray.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;

import java.util.function.Function;

/** Two-way travel between the overworld and the Ashen Reflection, at identical coordinates. */
public final class PortalTravel {
    private PortalTravel() {
    }

    public static void travel(ServerPlayer player, ServerLevel from, BlockPos touched) {
        ResourceKey<Level> destinationKey = ModDimensions.isMirror(from) ? Level.OVERWORLD : ModDimensions.ASHEN_LEVEL;
        ServerLevel destination = from.getServer().getLevel(destinationKey);
        player.setPortalCooldown();
        if (destination == null) {
            player.displayClientMessage(Component.translatable("message.stormray.portal.missing").withStyle(ChatFormatting.RED), true);
            StormRayMod.LOGGER.warn("Ashen Reflection dimension is not loaded; is the mod's data pack disabled?");
            return;
        }

        BlockPos sourceMin = PortalLogic.findPortalMin(from, touched);
        int y = Mth.clamp(sourceMin.getY(), destination.getMinBuildHeight() + 2, destination.getMaxBuildHeight() - 4);
        BlockPos targetMin = new BlockPos(sourceMin.getX(), y, sourceMin.getZ());
        if (!PortalLogic.isCompletePortal(destination, targetMin)) {
            PortalLogic.buildPortal(destination, targetMin);
        }
        Vec3 landing = PortalLogic.prepareLanding(destination, targetMin);

        from.playSound(null, touched, ModSounds.PORTAL_TRAVEL.get(), SoundSource.PLAYERS, 1.5F, 1.0F);
        player.changeDimension(destination, new Teleporter(landing));
        player.setPortalCooldown();
        destination.playSound(null, landing.x, landing.y, landing.z, ModSounds.PORTAL_TRAVEL.get(), SoundSource.PLAYERS, 1.5F, 0.8F);
    }

    /** Places the traveller at an exact spot, facing the portal (+Z). Never creates vanilla portals. */
    private record Teleporter(Vec3 target) implements ITeleporter {
        @Override
        public PortalInfo getPortalInfo(Entity entity, ServerLevel destWorld, Function<ServerLevel, PortalInfo> defaultPortalInfo) {
            return new PortalInfo(this.target, Vec3.ZERO, 0.0F, entity.getXRot());
        }

        @Override
        public Entity placeEntity(Entity entity, ServerLevel currentWorld, ServerLevel destWorld, float yaw,
                                  Function<Boolean, Entity> repositionEntity) {
            return repositionEntity.apply(false);
        }

        @Override
        public boolean playTeleportSound(ServerPlayer player, ServerLevel sourceWorld, ServerLevel destWorld) {
            return false;
        }
    }
}

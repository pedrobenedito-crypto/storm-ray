package com.stormray.portal;

import com.stormray.StormRayMod;
import com.stormray.config.StormRayConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Watches thrown player heads. When one comes to rest inside an open 4x4 ring of redstone
 * blocks, it is consumed, lightning strikes and the portal opens.
 */
@Mod.EventBusSubscriber(modid = StormRayMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PortalIgnition {
    private static final int MAX_WATCH_TICKS = 1200;
    private static final int CHECK_INTERVAL = 4;

    private static final class Watch {
        final ResourceKey<Level> dimension;
        int age;

        Watch(ResourceKey<Level> dimension) {
            this.dimension = dimension;
        }
    }

    private static final Map<UUID, Watch> WATCHED = new HashMap<>();

    private PortalIgnition() {
    }

    @SubscribeEvent
    public static void onItemJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.loadedFromDisk()) {
            return;
        }
        if (event.getEntity() instanceof ItemEntity item && item.getItem().is(Items.PLAYER_HEAD)) {
            WATCHED.put(item.getUUID(), new Watch(event.getLevel().dimension()));
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || WATCHED.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % CHECK_INTERVAL != 0) {
            return;
        }
        Iterator<Map.Entry<UUID, Watch>> it = WATCHED.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Watch> entry = it.next();
            Watch watch = entry.getValue();
            watch.age += CHECK_INTERVAL;
            ServerLevel level = server.getLevel(watch.dimension);
            Entity entity = level == null ? null : level.getEntity(entry.getKey());
            if (!(entity instanceof ItemEntity item) || !item.isAlive() || watch.age > MAX_WATCH_TICKS) {
                it.remove();
                continue;
            }
            if (!StormRayConfig.PORTAL_ENABLED.get() || !item.onGround()) {
                continue;
            }
            BlockPos min = PortalLogic.findIgnitableFrame(level, item.blockPosition());
            if (min == null) {
                continue;
            }
            it.remove();
            if (StormRayConfig.REQUIRE_STEVE_HEAD.get() && !SteveHeads.isSteve(item.getItem())) {
                tellThrower(level, item, Component.translatable("message.stormray.ritual.wrong_head").withStyle(ChatFormatting.GRAY));
                continue;
            }
            item.getItem().shrink(1);
            if (item.getItem().isEmpty()) {
                item.discard();
            }
            PortalLogic.ignite(level, min);
            tellThrower(level, item, Component.translatable("message.stormray.portal.opened").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        }
    }

    /** Redstone bound into an open portal crumbles when broken: the portal closes and nothing drops. */
    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!event.getLevel().isClientSide() && PortalLogic.isBoundFrame(event.getLevel(), event.getPos(), event.getState())) {
            event.setCanceled(true);
            event.getLevel().levelEvent(2001, event.getPos(), Block.getId(event.getState()));
            event.getLevel().setBlock(event.getPos(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        WATCHED.clear();
    }

    private static void tellThrower(ServerLevel level, ItemEntity item, Component message) {
        Entity owner = item.getOwner();
        if (owner instanceof Player player) {
            player.displayClientMessage(message, true);
        }
    }
}

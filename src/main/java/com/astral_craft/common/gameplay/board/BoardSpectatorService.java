package com.astral_craft.common.gameplay.board;

import com.astral_craft.common.network.s2c.CloseBoardPresentationPayload;
import com.astral_craft.common.registry.AstralDataComponents;
import com.astral_craft.common.registry.AstralItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Keeps the temporary one-board presentation subscription created by the spectator tool. */
public class BoardSpectatorService {

    private static final double PUBLIC_PRESENTATION_MARGIN = 32.0D;
    private static final Map<UUID, UUID> WATCHED_BOARDS = new LinkedHashMap<>();

    public static Optional<UUID> cycle(ServerPlayer player, List<BoardSession> sessions, ItemStack tool) {
        if (player == null || tool == null || !tool.is(AstralItems.BOARD_SPECTATOR.get())) return Optional.empty();
        List<BoardSession> playing = sessions.stream().filter(session -> session.phase() == BoardPhase.PLAYING).toList();
        if (playing.isEmpty()) return Optional.empty();
        if (player.isShiftKeyDown()) {
            stopWatching(player);
            return Optional.empty();
        }

        UUID previous = WATCHED_BOARDS.get(player.getUUID());
        int previousIndex = -1;
        for (int index = 0; index < playing.size(); index++) {
            if (playing.get(index).id().equals(previous)) {
                previousIndex = index;
                break;
            }
        }
        BoardSession target = playing.get((previousIndex + 1) % playing.size());
        for (BoardSession candidate : playing) {
            if (!candidate.id().equals(target.id())) closePresentation(player, candidate.id());
        }
        clearToolBindings(player);
        tool.set(AstralDataComponents.BOARD_SPECTATOR_BINDING.get(), target.id());
        WATCHED_BOARDS.put(player.getUUID(), target.id());
        BoardSessionManager.syncBoardSnapshot(player.level(), target);
        return Optional.of(target.id());
    }

    public static void focusParticipant(ServerPlayer player, BoardSession session) {
        if (player == null || session == null) return;
        UUID watched = WATCHED_BOARDS.get(player.getUUID());
        if (watched != null && !watched.equals(session.id())) removeBinding(player, watched);
        for (BoardSession candidate : BoardSessionManager.venueSessions(player.level(), session)) {
            if (!candidate.id().equals(session.id())) closePresentation(player, candidate.id());
        }
    }

    public static void stopWatching(ServerPlayer player) {
        if (player == null) return;
        UUID previous = WATCHED_BOARDS.get(player.getUUID());
        if (previous != null) removeBinding(player, previous);
    }

    public static Optional<UUID> watchedBoard(ServerPlayer player) {
        return player == null ? Optional.empty() : Optional.ofNullable(WATCHED_BOARDS.get(player.getUUID()));
    }

    public static List<ServerPlayer> presentationViewers(ServerLevel level, BoardSession session) {
        Set<ServerPlayer> viewers = new LinkedHashSet<>(BoardSessionManager.humanPlayers(level, session));
        viewers.addAll(spectators(level, session));
        if (BoardSessionManager.publicSessionForVenue(level, session).filter(value -> value.id().equals(session.id())).isPresent()) {
            double radius = Math.max(session.protectedArea().width(), session.protectedArea().depth()) * 0.5D + PUBLIC_PRESENTATION_MARGIN;
            double radiusSqr = radius * radius;
            BlockPos center = session.protectedArea().center();
            for (ServerPlayer player : level.players()) {
                if (WATCHED_BOARDS.containsKey(player.getUUID())) continue;
                BoardSession controlled = BoardSessionManager.findByController(player).orElse(null);
                if (controlled != null && BoardSessionManager.sameVenue(controlled, session) && !controlled.id().equals(session.id())) continue;
                if (player.distanceToSqr(center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D) <= radiusSqr) viewers.add(player);
            }
        }
        return List.copyOf(viewers);
    }

    public static List<ServerPlayer> spectators(ServerLevel level, BoardSession session) {
        List<ServerPlayer> result = new ArrayList<>();
        for (Map.Entry<UUID, UUID> entry : WATCHED_BOARDS.entrySet()) {
            if (!session.id().equals(entry.getValue())) continue;
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player != null && player.level() == level && hasSpectatorTool(player)) result.add(player);
        }

        return List.copyOf(result);
    }

    public static void clearBoard(ServerLevel level, UUID boardId) {
        if (level == null || boardId == null) return;
        Iterator<Map.Entry<UUID, UUID>> iterator = WATCHED_BOARDS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, UUID> entry = iterator.next();
            if (!boardId.equals(entry.getValue())) continue;
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                clearToolBindings(player);
                closePresentation(player, boardId);
            }
            iterator.remove();
        }
    }

    public static void serverTick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, UUID>> iterator = WATCHED_BOARDS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, UUID> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            boolean remove = player == null || !hasSpectatorTool(player);
            if (!remove) {
                remove = true;
                for (ServerLevel level : server.getAllLevels()) {
                    BoardSession session = BoardSessionManager.session(level, entry.getValue()).orElse(null);
                    if (session != null) {
                        remove = session.phase() != BoardPhase.PLAYING;
                        break;
                    }
                }
            }

            if (!remove) continue;
            if (player != null) {
                clearToolBindings(player);
                closePresentation(player, entry.getValue());
            }
            iterator.remove();
        }
    }

    private static void removeBinding(ServerPlayer player, UUID boardId) {
        WATCHED_BOARDS.remove(player.getUUID());
        clearToolBindings(player);
        closePresentation(player, boardId);
    }

    private static void closePresentation(ServerPlayer player, UUID boardId) {
        boolean participant = false;
        for (ServerLevel level : player.server.getAllLevels()) {
            BoardSession session = BoardSessionManager.session(level, boardId).orElse(null);
            if (session != null) {
                participant = session.participantByController(player.getUUID()).isPresent();
                break;
            }
        }

        if (!participant) PacketDistributor.sendToPlayer(player, new CloseBoardPresentationPayload(boardId));
    }

    private static boolean hasSpectatorTool(ServerPlayer player) {
        UUID boardId = WATCHED_BOARDS.get(player.getUUID());
        if (boardId == null) return false;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(AstralItems.BOARD_SPECTATOR.get())
                    && boardId.equals(stack.get(AstralDataComponents.BOARD_SPECTATOR_BINDING.get()))) return true;
        }
        return false;
    }

    private static void clearToolBindings(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(AstralItems.BOARD_SPECTATOR.get())) {
                stack.remove(AstralDataComponents.BOARD_SPECTATOR_BINDING.get());
            }
        }
    }

}
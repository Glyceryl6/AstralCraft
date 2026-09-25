package com.astral_craft.common.items;

import com.astral_craft.common.gameplay.board.BoardPhase;
import com.astral_craft.common.gameplay.board.BoardSession;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.gameplay.board.BoardSpectatorService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class BoardSpectatorItem extends Item {

    public BoardSpectatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        if (!(context.getPlayer() instanceof ServerPlayer player)) return InteractionResult.PASS;
        List<BoardSession> sessions = BoardSessionManager.sessionsAt(player.level(), context.getClickedPos());
        if (sessions.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.board.not_registered"), true);
            return InteractionResult.FAIL;
        }
        List<BoardSession> playing = sessions.stream().filter(session -> session.phase() == BoardPhase.PLAYING).toList();
        if (playing.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.board.spectator.not_playing"), true);
            return InteractionResult.FAIL;
        }

        Optional<UUID> watched = BoardSpectatorService.cycle(player, playing, context.getItemInHand());
        if (watched.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.board.spectator.unbound"), true);
        } else {
            int index = 0;
            for (int current = 0; current < playing.size(); current++) {
                if (playing.get(current).id().equals(watched.get())) {
                    index = current;
                    break;
                }
            }
            player.sendSystemMessage(Component.translatable("message.astral_craft.board.spectator.bound_index",
                    index + 1, playing.size()), true);
        }
        return InteractionResult.SUCCESS;
    }

}
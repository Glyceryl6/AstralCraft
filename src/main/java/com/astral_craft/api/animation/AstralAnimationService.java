package com.astral_craft.api.animation;

import com.astral_craft.common.network.s2c.CharacterAnimationCommandPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server-side entry point for manually controlling client animation controllers. */
public class AstralAnimationService {

    public static void send(Entity entity, AstralAnimationCommand command) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity,
                new CharacterAnimationCommandPayload(entity.getId(), level.getGameTime(), command));
    }

    public static void play(Entity entity, String animation) {
        send(entity, AstralAnimationCommand.play(animation));
    }

    public static void play(Entity entity, String controller, String animation, int transitionTicks, float speed, int priority) {
        send(entity, AstralAnimationCommand.play(controller, animation, transitionTicks, speed, priority));
    }

    public static void queue(Entity entity, String controller, String animation, int transitionTicks, float speed, int priority) {
        send(entity, AstralAnimationCommand.queue(controller, animation, transitionTicks, speed, priority));
    }

    public static void stop(Entity entity, String controller, int transitionTicks) {
        send(entity, AstralAnimationCommand.stop(controller, transitionTicks));
    }

    public static void pause(Entity entity, String controller) {
        send(entity, AstralAnimationCommand.pause(controller));
    }

    public static void resume(Entity entity, String controller) {
        send(entity, AstralAnimationCommand.resume(controller));
    }

    public static void seek(Entity entity, String controller, float seconds) {
        send(entity, AstralAnimationCommand.seek(controller, seconds));
    }

}
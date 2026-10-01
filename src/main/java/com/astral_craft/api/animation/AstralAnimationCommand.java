package com.astral_craft.api.animation;

import java.util.List;

/**
 * Loader-independent animation command description. Minecraft-specific transport lives outside this type.
 */
public record AstralAnimationCommand(
        Operation operation,
        String controller,
        String animation,
        int transitionTicks,
        float speed,
        int priority,
        BlendMode blendMode,
        LoopMode loopMode,
        List<String> boneMask,
        float seekSeconds) {

    public static final String BASE_CONTROLLER = "base";
    public static final String ACTION_CONTROLLER = "action";

    public AstralAnimationCommand {
        controller = controller.isBlank() ? ACTION_CONTROLLER : controller;
        transitionTicks = Math.clamp(transitionTicks, 0, 200);
        speed = Math.clamp(speed, 0.01F, 16.0F);
        priority = Math.clamp(priority, -1000, 1000);
        boneMask = List.copyOf(boneMask);
        seekSeconds = Math.max(0.0F, seekSeconds);
    }

    public static AstralAnimationCommand play(String animation) {
        return play(ACTION_CONTROLLER, animation, 4, 1.0F, 100);
    }

    public static AstralAnimationCommand play(String controller, String animation, int transitionTicks, float speed, int priority) {
        return new AstralAnimationCommand(Operation.PLAY, controller, animation, transitionTicks, speed, priority,
                BlendMode.OVERRIDE, LoopMode.INHERIT, List.of(), 0.0F);
    }

    public static AstralAnimationCommand queue(String controller, String animation, int transitionTicks, float speed, int priority) {
        return new AstralAnimationCommand(Operation.QUEUE, controller, animation, transitionTicks, speed, priority,
                BlendMode.OVERRIDE, LoopMode.INHERIT, List.of(), 0.0F);
    }

    public static AstralAnimationCommand stop(String controller, int transitionTicks) {
        return new AstralAnimationCommand(Operation.STOP, controller, "", transitionTicks, 1.0F, 0,
                BlendMode.OVERRIDE, LoopMode.INHERIT, List.of(), 0.0F);
    }

    public static AstralAnimationCommand pause(String controller) {
        return new AstralAnimationCommand(Operation.PAUSE, controller, "", 0, 1.0F, 0,
                BlendMode.OVERRIDE, LoopMode.INHERIT, List.of(), 0.0F);
    }

    public static AstralAnimationCommand resume(String controller) {
        return new AstralAnimationCommand(Operation.RESUME, controller, "", 0, 1.0F, 0,
                BlendMode.OVERRIDE, LoopMode.INHERIT, List.of(), 0.0F);
    }

    public static AstralAnimationCommand seek(String controller, float seconds) {
        return new AstralAnimationCommand(Operation.SEEK, controller, "", 0, 1.0F, 0,
                BlendMode.OVERRIDE, LoopMode.INHERIT, List.of(), seconds);
    }

    public AstralAnimationCommand withBlend(BlendMode blendMode) {
        return new AstralAnimationCommand(this.operation, this.controller, this.animation, this.transitionTicks, this.speed,
                this.priority, blendMode, this.loopMode, this.boneMask, this.seekSeconds);
    }

    public AstralAnimationCommand withLoop(LoopMode loopMode) {
        return new AstralAnimationCommand(this.operation, this.controller, this.animation, this.transitionTicks, this.speed,
                this.priority, this.blendMode, loopMode, this.boneMask, this.seekSeconds);
    }

    public AstralAnimationCommand withBoneMask(List<String> boneMask) {
        return new AstralAnimationCommand(this.operation, this.controller, this.animation, this.transitionTicks, this.speed,
                this.priority, this.blendMode, this.loopMode, boneMask, this.seekSeconds);
    }

    public enum Operation { PLAY, QUEUE, STOP, PAUSE, RESUME, SEEK }
    public enum BlendMode { OVERRIDE, ADDITIVE }
    public enum LoopMode { INHERIT, ONCE, LOOP, HOLD_LAST }
}

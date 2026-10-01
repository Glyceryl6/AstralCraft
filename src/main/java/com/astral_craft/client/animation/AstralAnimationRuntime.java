package com.astral_craft.client.animation;

import com.astral_craft.client.model.character.*;
import com.astral_craft.api.animation.AstralAnimationCommand;
import net.minecraft.resources.Identifier;

import java.util.*;

/**
 * Stateful animation runtime for one animatable instance.
 * The class deliberately knows nothing about entities, networking or rendering APIs.
 */
public class AstralAnimationRuntime {

    private final Map<String, ControllerState> controllers = new LinkedHashMap<>();
    private final List<AstralGeoAnimationEvent> pendingEvents = new ArrayList<>();
    private float lastUpdateSeconds = Float.NaN;

    public void apply(AstralAnimationCommand command, float startSeconds) {
        ControllerState controller = this.controllers.computeIfAbsent(command.controller(), ControllerState::new);
        switch (command.operation()) {
            case PLAY -> controller.play(command, startSeconds);
            case QUEUE -> controller.queue(command, startSeconds);
            case STOP -> controller.stop(command.transitionTicks() / 20.0F, startSeconds);
            case PAUSE -> controller.pause(startSeconds);
            case RESUME -> controller.resume(startSeconds);
            case SEEK -> controller.seek(command.seekSeconds(), startSeconds);
        }
    }

    public AstralGeoPose sample(Identifier animationSet, String fallbackAnimation, float fallbackTime,
                                String boneName, float nowSeconds) {
        this.update(animationSet, nowSeconds);
        AstralGeoAnimationClip baseClip = AstralGeoAnimationManager.INSTANCE.clip(animationSet, fallbackAnimation);
        AstralGeoPose pose = baseClip == null ? AstralGeoPose.IDENTITY : baseClip.sample(boneName, fallbackTime);
        List<ControllerState> active = this.controllers.values().stream()
                .filter(ControllerState::active)
                .sorted(Comparator.comparingInt(ControllerState::priority))
                .toList();
        for (ControllerState controller : active) {
            if (!controller.appliesTo(boneName)) continue;
            AstralGeoAnimationClip clip = AstralGeoAnimationManager.INSTANCE.exactClip(animationSet, controller.animation());
            if (clip == null) continue;
            AstralGeoPose layer = clip.sample(boneName, controller.sampleTime(clip));
            float weight = controller.weight(nowSeconds);
            pose = controller.blendMode() == AstralAnimationCommand.BlendMode.ADDITIVE
                    ? additive(pose, layer, weight)
                    : blend(pose, layer, weight);
        }
        return pose;
    }

    public List<AstralGeoAnimationEvent> drainEvents() {
        if (this.pendingEvents.isEmpty()) return List.of();
        List<AstralGeoAnimationEvent> events = List.copyOf(this.pendingEvents);
        this.pendingEvents.clear();
        return events;
    }

    public boolean isPlaying(String controller) {
        ControllerState state = this.controllers.get(controller);
        return state != null && state.active();
    }

    public String currentAnimation(String controller) {
        ControllerState state = this.controllers.get(controller);
        return state == null ? "" : state.animation();
    }

    public void clear() {
        this.controllers.clear();
        this.pendingEvents.clear();
        this.lastUpdateSeconds = Float.NaN;
    }

    private void update(Identifier animationSet, float nowSeconds) {
        if (Float.isNaN(this.lastUpdateSeconds)) {
            this.lastUpdateSeconds = nowSeconds;
            return;
        }
        float delta = Math.clamp(nowSeconds - this.lastUpdateSeconds, 0.0F, 1.0F);
        this.lastUpdateSeconds = nowSeconds;
        for (ControllerState controller : this.controllers.values()) {
            AstralGeoAnimationClip clip = AstralGeoAnimationManager.INSTANCE.exactClip(animationSet, controller.animation());
            if (clip != null) controller.advance(clip, delta, nowSeconds, this.pendingEvents);
        }
        this.controllers.values().removeIf(ControllerState::removable);
    }

    private static AstralGeoPose blend(AstralGeoPose base, AstralGeoPose layer, float weight) {
        return new AstralGeoPose(
                base.rotation().lerp(layer.rotation(), weight),
                base.position().lerp(layer.position(), weight),
                base.scale().lerp(layer.scale(), weight));
    }

    private static AstralGeoPose additive(AstralGeoPose base, AstralGeoPose layer, float weight) {
        AstralGeoTransform rotation = base.rotation().add(layer.rotation().scale(weight));
        AstralGeoTransform position = base.position().add(layer.position().scale(weight));
        AstralGeoTransform scaleDelta = layer.scale().add(AstralGeoTransform.ONE.scale(-1.0F)).scale(weight);
        return new AstralGeoPose(rotation, position, base.scale().add(scaleDelta));
    }

    private static class ControllerState {
        private final String id;
        private final ArrayDeque<AstralAnimationCommand> queue = new ArrayDeque<>();
        private AstralAnimationCommand command;
        private float playhead;
        private float startedAt;
        private float pausedAt;
        private boolean paused;
        private boolean stopping;
        private float stopStartedAt;
        private float stopDuration;

        private ControllerState(String id) {
            this.id = id;
        }

        private void play(AstralAnimationCommand command, float nowSeconds) {
            this.command = command;
            this.playhead = 0.0F;
            this.startedAt = nowSeconds;
            this.pausedAt = 0.0F;
            this.paused = false;
            this.stopping = false;
            this.stopDuration = 0.0F;
        }

        private void queue(AstralAnimationCommand command, float nowSeconds) {
            if (this.command == null) this.play(command, nowSeconds);
            else this.queue.addLast(command);
        }

        private void stop(float duration, float nowSeconds) {
            if (this.command == null) return;
            if (duration <= 0.0F) {
                this.command = null;
                this.queue.clear();
                return;
            }
            this.stopping = true;
            this.stopStartedAt = nowSeconds;
            this.stopDuration = duration;
            this.queue.clear();
        }

        private void pause(float nowSeconds) {
            if (this.command == null || this.paused) return;
            this.paused = true;
            this.pausedAt = nowSeconds;
        }

        private void resume(float nowSeconds) {
            if (this.command == null || !this.paused) return;
            this.startedAt += Math.max(0.0F, nowSeconds - this.pausedAt);
            this.paused = false;
        }

        private void seek(float seconds, float nowSeconds) {
            if (this.command == null) return;
            this.playhead = seconds;
            this.startedAt = nowSeconds - seconds / this.command.speed();
        }

        private void advance(AstralGeoAnimationClip clip, float delta, float nowSeconds, List<AstralGeoAnimationEvent> events) {
            if (this.command == null || this.paused) return;
            float previous = this.playhead;
            this.playhead += delta * this.command.speed();
            collectEvents(clip, previous, this.playhead, this.looping(clip), events);
            if (this.stopping && nowSeconds - this.stopStartedAt >= this.stopDuration) {
                this.command = null;
                return;
            }
            if (this.looping(clip) || this.command.loopMode() == AstralAnimationCommand.LoopMode.HOLD_LAST) return;
            if (clip.lengthSeconds() <= 0.0F || this.playhead < clip.lengthSeconds()) return;
            AstralAnimationCommand next = this.queue.pollFirst();
            if (next == null) {
                this.command = null;
                return;
            }
            float overflow = Math.max(0.0F, this.playhead - clip.lengthSeconds());
            this.play(next, nowSeconds - overflow / next.speed());
            this.playhead = overflow;
        }

        private float sampleTime(AstralGeoAnimationClip clip) {
            if (this.command == null) return 0.0F;
            if (this.looping(clip)) return clip.normalizeTime(this.playhead);
            return Math.min(this.playhead, clip.lengthSeconds());
        }

        private float weight(float nowSeconds) {
            if (this.command == null) return 0.0F;
            float transition = this.command.transitionTicks() / 20.0F;
            float in = transition <= 0.0F ? 1.0F : Math.clamp((nowSeconds - this.startedAt) / transition, 0.0F, 1.0F);
            if (!this.stopping || this.stopDuration <= 0.0F) return in;
            float out = 1.0F - Math.clamp((nowSeconds - this.stopStartedAt) / this.stopDuration, 0.0F, 1.0F);
            return Math.min(in, out);
        }

        private boolean looping(AstralGeoAnimationClip clip) {
            return this.command != null && switch (this.command.loopMode()) {
                case LOOP -> true;
                case ONCE, HOLD_LAST -> false;
                case INHERIT -> clip.loop();
            };
        }

        private boolean appliesTo(String boneName) {
            return this.command != null && (this.command.boneMask().isEmpty() || this.command.boneMask().contains(boneName));
        }

        private boolean active() {
            return this.command != null;
        }

        private boolean removable() {
            return this.command == null && this.queue.isEmpty();
        }

        private String animation() {
            return this.command == null ? "" : this.command.animation();
        }

        private int priority() {
            return this.command == null ? Integer.MIN_VALUE : this.command.priority();
        }

        private AstralAnimationCommand.BlendMode blendMode() {
            return this.command == null ? AstralAnimationCommand.BlendMode.OVERRIDE : this.command.blendMode();
        }
    }

    private static void collectEvents(AstralGeoAnimationClip clip, float previous, float current, boolean looping,
                                      List<AstralGeoAnimationEvent> output) {
        if (clip.events().isEmpty() || current <= previous) return;
        if (!looping || clip.lengthSeconds() <= 0.0F) {
            for (AstralGeoAnimationEvent event : clip.events()) {
                if (event.time() > previous && event.time() <= current) output.add(event);
            }
            return;
        }
        float length = clip.lengthSeconds();
        int firstLoop = (int) Math.floor(previous / length);
        int lastLoop = (int) Math.floor(current / length);
        for (int loop = firstLoop; loop <= lastLoop; loop++) {
            float offset = loop * length;
            for (AstralGeoAnimationEvent event : clip.events()) {
                float absolute = offset + event.time();
                if (absolute > previous && absolute <= current) output.add(event);
            }
        }
    }
}

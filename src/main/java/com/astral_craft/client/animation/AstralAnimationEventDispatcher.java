package com.astral_craft.client.animation;

import com.astral_craft.client.model.character.AstralGeoAnimationEvent;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/** Client-side bridge for sound, particle and custom timeline keyframes. */
public class AstralAnimationEventDispatcher {

    private static final Map<AstralGeoAnimationEvent.Type, List<BiConsumer<Entity, AstralGeoAnimationEvent>>> HANDLERS =
            new EnumMap<>(AstralGeoAnimationEvent.Type.class);

    public static void register(AstralGeoAnimationEvent.Type type, BiConsumer<Entity, AstralGeoAnimationEvent> handler) {
        HANDLERS.computeIfAbsent(type, ignored -> new ArrayList<>()).add(handler);
    }

    public static void dispatch(Entity entity, List<AstralGeoAnimationEvent> events) {
        for (AstralGeoAnimationEvent event : events) {
            for (BiConsumer<Entity, AstralGeoAnimationEvent> handler : HANDLERS.getOrDefault(event.type(), List.of())) {
                handler.accept(entity, event);
            }
        }
    }

    public static void clearHandlers() {
        HANDLERS.clear();
    }

}
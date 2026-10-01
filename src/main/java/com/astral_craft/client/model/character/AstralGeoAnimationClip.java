package com.astral_craft.client.model.character;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record AstralGeoAnimationClip(String name, float lengthSeconds, boolean loop,
                                     Map<String, AstralGeoBoneAnimation> bones, List<AstralGeoAnimationEvent> events) {

    public AstralGeoPose sample(String boneName, float timeSeconds) {
        AstralGeoBoneAnimation animation = this.bones.get(boneName);
        if (animation == null) return AstralGeoPose.IDENTITY;
        return animation.sample(this.normalizeTime(timeSeconds));
    }

    public float normalizeTime(float timeSeconds) {
        if (this.loop && this.lengthSeconds > 0.0F) return Math.floorMod((long) (timeSeconds * 1000.0F), Math.max(1L, (long) (this.lengthSeconds * 1000.0F))) / 1000.0F;
        return Math.clamp(timeSeconds, 0.0F, Math.max(0.0F, this.lengthSeconds));
    }

    public static AstralGeoAnimationClip read(String name, JsonObject object) {
        float length = object.has("animation_length") ? object.get("animation_length").getAsFloat() : 1.0F;
        boolean loop = object.has("loop") && object.get("loop").isJsonPrimitive()
                && (object.get("loop").getAsJsonPrimitive().isBoolean() ? object.get("loop").getAsBoolean() : !"false".equalsIgnoreCase(object.get("loop").getAsString()));
        Map<String, AstralGeoBoneAnimation> bones = new LinkedHashMap<>();
        if (object.has("bones") && object.get("bones").isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : object.getAsJsonObject("bones").entrySet()) {
                if (entry.getValue().isJsonObject()) bones.put(entry.getKey(), AstralGeoBoneAnimation.read(entry.getKey(), entry.getValue().getAsJsonObject()));
            }
        }
        List<AstralGeoAnimationEvent> events = new ArrayList<>();
        readEvents(object, "sound_effects", AstralGeoAnimationEvent.Type.SOUND, events);
        readEvents(object, "particle_effects", AstralGeoAnimationEvent.Type.PARTICLE, events);
        readTimeline(object, events);
        events.sort(Comparator.comparingDouble(AstralGeoAnimationEvent::time));
        return new AstralGeoAnimationClip(name, length, loop, Map.copyOf(bones), List.copyOf(events));
    }

    private static void readEvents(JsonObject object, String key, AstralGeoAnimationEvent.Type type, List<AstralGeoAnimationEvent> events) {
        if (!object.has(key) || !object.get(key).isJsonObject()) return;
        for (Map.Entry<String, JsonElement> entry : object.getAsJsonObject(key).entrySet()) {
            try {
                float time = Float.parseFloat(entry.getKey());
                if (entry.getValue().isJsonObject()) {
                    JsonObject value = entry.getValue().getAsJsonObject();
                    String effect = string(value, type == AstralGeoAnimationEvent.Type.PARTICLE ? "effect" : "effect", "");
                    if (effect.isBlank()) effect = string(value, "sound", "");
                    String locator = string(value, "locator", "");
                    String script = string(value, "pre_effect_script", "");
                    events.add(new AstralGeoAnimationEvent(time, type, effect, locator, script));
                } else if (entry.getValue().isJsonPrimitive()) {
                    events.add(new AstralGeoAnimationEvent(time, type, entry.getValue().getAsString(), "", ""));
                }
            } catch (NumberFormatException ignored) {}
        }
    }

    private static void readTimeline(JsonObject object, List<AstralGeoAnimationEvent> events) {
        if (!object.has("timeline") || !object.get("timeline").isJsonObject()) return;
        for (Map.Entry<String, JsonElement> entry : object.getAsJsonObject("timeline").entrySet()) {
            try {
                float time = Float.parseFloat(entry.getKey());
                String script = entry.getValue().isJsonArray() ? entry.getValue().toString() : entry.getValue().getAsString();
                events.add(new AstralGeoAnimationEvent(time, AstralGeoAnimationEvent.Type.TIMELINE, "", "", script));
            } catch (RuntimeException ignored) {}
        }
    }

    private static String string(JsonObject object, String key, String fallback) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : fallback;
    }
}

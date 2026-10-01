package com.astral_craft.client.model.character;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public record AstralGeoBoneAnimation(String boneName, List<AstralGeoKeyframe> rotations,
                                     List<AstralGeoKeyframe> positions, List<AstralGeoKeyframe> scales) {

    public AstralGeoPose sample(float time) {
        return new AstralGeoPose(
                AstralGeoKeyframe.sample(this.rotations, time, AstralGeoTransform.ZERO),
                AstralGeoKeyframe.sample(this.positions, time, AstralGeoTransform.ZERO),
                AstralGeoKeyframe.sample(this.scales, time, AstralGeoTransform.ONE));
    }

    public static AstralGeoBoneAnimation read(String boneName, JsonObject object) {
        return new AstralGeoBoneAnimation(boneName,
                readChannel(object.get("rotation"), AstralGeoTransform.ZERO),
                readChannel(object.get("position"), AstralGeoTransform.ZERO),
                readChannel(object.get("scale"), AstralGeoTransform.ONE));
    }

    private static List<AstralGeoKeyframe> readChannel(JsonElement element, AstralGeoTransform fallback) {
        List<AstralGeoKeyframe> frames = new ArrayList<>();
        if (element == null || element.isJsonNull()) return frames;
        if (element.isJsonArray() || element.isJsonPrimitive()) {
            AstralGeoTransform value = AstralGeoTransform.read(element, fallback);
            frames.add(new AstralGeoKeyframe(0.0F, value, value, "linear", "linear", List.of()));
            return frames;
        }
        if (!element.isJsonObject()) return frames;
        JsonObject object = element.getAsJsonObject();
        if (object.has("vector")) {
            AstralGeoTransform value = AstralGeoTransform.read(object.get("vector"), fallback);
            frames.add(new AstralGeoKeyframe(0.0F, value, value, interpolation(object), easing(object), easingArgs(object)));
            return frames;
        }
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            try {
                float time = Float.parseFloat(entry.getKey());
                JsonElement value = entry.getValue();
                AstralGeoTransform pre;
                AstralGeoTransform post;
                String interpolation = "linear";
                String easing = "linear";
                List<Float> easingArgs = List.of();
                if (value != null && value.isJsonObject()) {
                    JsonObject frame = value.getAsJsonObject();
                    AstralGeoTransform vector = AstralGeoTransform.read(frame.get("vector"), fallback);
                    pre = AstralGeoTransform.read(frame.get("pre"), vector);
                    post = AstralGeoTransform.read(frame.get("post"), vector);
                    interpolation = interpolation(frame);
                    easing = easing(frame);
                    easingArgs = easingArgs(frame);
                } else {
                    pre = AstralGeoTransform.read(value, fallback);
                    post = pre;
                }
                frames.add(new AstralGeoKeyframe(time, pre, post, interpolation, easing, easingArgs));
            } catch (NumberFormatException ignored) {}
        }
        frames.sort(Comparator.comparingDouble(AstralGeoKeyframe::time));
        return List.copyOf(frames);
    }

    private static String interpolation(JsonObject object) {
        return object.has("lerp_mode") ? object.get("lerp_mode").getAsString() : object.has("interpolation") ? object.get("interpolation").getAsString() : "linear";
    }

    private static String easing(JsonObject object) {
        return object.has("easing") ? object.get("easing").getAsString() : "linear";
    }

    private static List<Float> easingArgs(JsonObject object) {
        if (!object.has("easingArgs") || !object.get("easingArgs").isJsonArray()) return List.of();
        List<Float> values = new ArrayList<>();
        JsonArray array = object.getAsJsonArray("easingArgs");
        for (JsonElement element : array) if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) values.add(element.getAsFloat());
        return List.copyOf(values);
    }
}

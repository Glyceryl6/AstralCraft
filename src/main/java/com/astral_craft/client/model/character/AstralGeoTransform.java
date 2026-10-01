package com.astral_craft.client.model.character;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public record AstralGeoTransform(float x, float y, float z) {

    public static final AstralGeoTransform ZERO = new AstralGeoTransform(0.0F, 0.0F, 0.0F);
    public static final AstralGeoTransform ONE = new AstralGeoTransform(1.0F, 1.0F, 1.0F);

    public static AstralGeoTransform read(JsonElement element, AstralGeoTransform fallback) {
        if (element == null || element.isJsonNull()) return fallback;
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("vector")) return read(object.get("vector"), fallback);
            return fallback;
        }
        if (element instanceof JsonPrimitive primitive) {
            if (primitive.isNumber()) {
                float value = primitive.getAsFloat();
                return new AstralGeoTransform(value, value, value);
            }
            try {
                float value = Float.parseFloat(primitive.getAsString());
                return new AstralGeoTransform(value, value, value);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        if (!element.isJsonArray()) return fallback;
        JsonArray array = element.getAsJsonArray();
        return new AstralGeoTransform(component(array, 0, fallback.x()), component(array, 1, fallback.y()), component(array, 2, fallback.z()));
    }

    private static float component(JsonArray array, int index, float fallback) {
        if (array.size() <= index || !array.get(index).isJsonPrimitive()) return fallback;
        try {
            return Float.parseFloat(array.get(index).getAsString());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public AstralGeoTransform add(AstralGeoTransform other) {
        return new AstralGeoTransform(this.x + other.x, this.y + other.y, this.z + other.z);
    }

    public AstralGeoTransform scale(float factor) {
        return new AstralGeoTransform(this.x * factor, this.y * factor, this.z * factor);
    }

    public AstralGeoTransform lerp(AstralGeoTransform other, float t) {
        float value = Math.clamp(t, 0.0F, 1.0F);
        return new AstralGeoTransform(this.x + (other.x - this.x) * value, this.y + (other.y - this.y) * value, this.z + (other.z - this.z) * value);
    }

    public static AstralGeoTransform catmullRom(AstralGeoTransform p0, AstralGeoTransform p1, AstralGeoTransform p2, AstralGeoTransform p3, float t) {
        return new AstralGeoTransform(catmull(p0.x, p1.x, p2.x, p3.x, t), catmull(p0.y, p1.y, p2.y, p3.y, t), catmull(p0.z, p1.z, p2.z, p3.z, t));
    }

    private static float catmull(float p0, float p1, float p2, float p3, float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        return 0.5F * ((2.0F * p1) + (-p0 + p2) * t + (2.0F * p0 - 5.0F * p1 + 4.0F * p2 - p3) * t2 + (-p0 + 3.0F * p1 - 3.0F * p2 + p3) * t3);
    }
}

package com.astral_craft.client.model.character;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record AstralGeoModelDefinition(
        Identifier id, String modelIdentifier, int textureWidth, int textureHeight,
        Map<String, Bone> bones, JsonObject source) {

    private static final int MAX_BONES = 512;
    private static final int MAX_CUBES_PER_BONE = 2048;
    private static final int MAX_LOCATORS_PER_BONE = 256;

    public static final Codec<AstralGeoModelDefinition> CODEC = Codec.PASSTHROUGH.xmap(
            dynamic -> AstralGeoModelDefinition.read(null, dynamic.convert(JsonOps.INSTANCE).getValue()),
            value -> new Dynamic<>(JsonOps.INSTANCE, value.source()));

    public static AstralGeoModelDefinition read(Identifier id, JsonElement element) {
        JsonObject object = element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
        JsonObject geometry = firstGeometry(object);
        String identifier = id == null ? "astral_craft:unknown" : id.toString();
        int textureWidth = 64;
        int textureHeight = 64;
        Map<String, Bone> bones = new LinkedHashMap<>();
        if (geometry != null) {
            if (geometry.has("description") && geometry.get("description").isJsonObject()) {
                JsonObject description = geometry.getAsJsonObject("description");
                if (description.has("identifier")) identifier = description.get("identifier").getAsString();
                if (description.has("texture_width")) textureWidth = Math.max(1, description.get("texture_width").getAsInt());
                if (description.has("texture_height")) textureHeight = Math.max(1, description.get("texture_height").getAsInt());
            }
            if (geometry.has("bones") && geometry.get("bones").isJsonArray()) {
                for (JsonElement value : geometry.getAsJsonArray("bones")) {
                    if (bones.size() >= MAX_BONES) break;
                    if (!value.isJsonObject()) continue;
                    Bone bone = Bone.read(value.getAsJsonObject());
                    if (!bone.name().isBlank()) bones.put(bone.name(), bone);
                }
            }
        }
        return new AstralGeoModelDefinition(id, identifier, textureWidth, textureHeight, Map.copyOf(bones), object.deepCopy());
    }

    public boolean hasRenderableGeometry() {
        return this.bones.values().stream().anyMatch(bone -> !bone.cubes().isEmpty());
    }

    public List<Bone> rootBones() {
        List<Bone> roots = new ArrayList<>();
        for (Bone bone : this.bones.values()) {
            if (bone.parent().isBlank() || !this.bones.containsKey(bone.parent())) roots.add(bone);
        }
        return List.copyOf(roots);
    }

    public List<Bone> children(String name) {
        List<Bone> children = new ArrayList<>();
        for (Bone bone : this.bones.values()) if (name.equals(bone.parent())) children.add(bone);
        return List.copyOf(children);
    }

    private static JsonObject firstGeometry(JsonObject object) {
        if (object.has("minecraft:geometry") && object.get("minecraft:geometry").isJsonArray()) {
            JsonArray array = object.getAsJsonArray("minecraft:geometry");
            if (!array.isEmpty() && array.get(0).isJsonObject()) return array.get(0).getAsJsonObject();
        }
        for (String key : object.keySet()) if (key.startsWith("geometry.") && object.get(key).isJsonObject()) return object.getAsJsonObject(key);
        return null;
    }

    public record Bone(String name, String parent, AstralGeoTransform pivot, AstralGeoTransform rotation,
                       boolean mirror, boolean neverRender, List<Cube> cubes, Map<String, Locator> locators) {
        private static Bone read(JsonObject object) {
            String name = string(object, "name", "");
            String parent = string(object, "parent", "");
            AstralGeoTransform pivot = AstralGeoTransform.read(object.get("pivot"), AstralGeoTransform.ZERO);
            AstralGeoTransform rotation = AstralGeoTransform.read(object.get("rotation"), AstralGeoTransform.ZERO);
            boolean mirror = bool(object, "mirror", false);
            boolean neverRender = bool(object, "neverRender", false) || bool(object, "never_render", false);
            List<Cube> cubes = new ArrayList<>();
            if (object.has("cubes") && object.get("cubes").isJsonArray()) {
                for (JsonElement value : object.getAsJsonArray("cubes")) {
                    if (cubes.size() >= MAX_CUBES_PER_BONE) break;
                    if (value.isJsonObject()) cubes.add(Cube.read(value.getAsJsonObject(), mirror));
                }
            }
            Map<String, Locator> locators = new LinkedHashMap<>();
            if (object.has("locators") && object.get("locators").isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : object.getAsJsonObject("locators").entrySet()) {
                    if (locators.size() >= MAX_LOCATORS_PER_BONE) break;
                    locators.put(entry.getKey(), Locator.read(entry.getValue()));
                }
            }
            return new Bone(name, parent, pivot, rotation, mirror, neverRender, List.copyOf(cubes), Map.copyOf(locators));
        }
    }

    public record Cube(AstralGeoTransform origin, AstralGeoTransform size, AstralGeoTransform pivot,
                       AstralGeoTransform rotation, float inflate, boolean mirror, Uv uv) {
        private static Cube read(JsonObject object, boolean inheritedMirror) {
            AstralGeoTransform origin = AstralGeoTransform.read(object.get("origin"), AstralGeoTransform.ZERO);
            AstralGeoTransform size = AstralGeoTransform.read(object.get("size"), AstralGeoTransform.ZERO);
            AstralGeoTransform pivot = AstralGeoTransform.read(object.get("pivot"), origin.add(size.scale(0.5F)));
            AstralGeoTransform rotation = AstralGeoTransform.read(object.get("rotation"), AstralGeoTransform.ZERO);
            float inflate = object.has("inflate") ? object.get("inflate").getAsFloat() : 0.0F;
            boolean mirror = object.has("mirror") ? object.get("mirror").getAsBoolean() : inheritedMirror;
            return new Cube(origin, size, pivot, rotation, inflate, mirror, Uv.read(object.get("uv"), size));
        }
    }

    public record Uv(float boxU, float boxV, Map<String, FaceUv> faces) {
        private static Uv read(JsonElement element, AstralGeoTransform size) {
            if (element == null || element.isJsonNull()) return new Uv(0.0F, 0.0F, Map.of());
            if (element.isJsonArray()) {
                JsonArray array = element.getAsJsonArray();
                return new Uv(number(array, 0, 0.0F), number(array, 1, 0.0F), Map.of());
            }
            if (!element.isJsonObject()) return new Uv(0.0F, 0.0F, Map.of());
            Map<String, FaceUv> faces = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                if (entry.getValue().isJsonObject()) faces.put(entry.getKey(), FaceUv.read(entry.getValue().getAsJsonObject(), size));
            }
            return new Uv(0.0F, 0.0F, Map.copyOf(faces));
        }

        public FaceUv face(String face, AstralGeoTransform size) {
            FaceUv explicit = this.faces.get(face);
            if (explicit != null) return explicit;
            float x = Math.abs(size.x());
            float y = Math.abs(size.y());
            float z = Math.abs(size.z());
            return switch (face) {
                case "west" -> new FaceUv(this.boxU, this.boxV + z, z, y);
                case "north" -> new FaceUv(this.boxU + z, this.boxV + z, x, y);
                case "east" -> new FaceUv(this.boxU + z + x, this.boxV + z, z, y);
                case "south" -> new FaceUv(this.boxU + z + x + z, this.boxV + z, x, y);
                case "up" -> new FaceUv(this.boxU + z, this.boxV, x, z);
                default -> new FaceUv(this.boxU + z + x, this.boxV, x, z);
            };
        }
    }

    public record FaceUv(float u, float v, float width, float height) {
        private static FaceUv read(JsonObject object, AstralGeoTransform size) {
            JsonArray uv = object.has("uv") && object.get("uv").isJsonArray() ? object.getAsJsonArray("uv") : new JsonArray();
            JsonArray uvSize = object.has("uv_size") && object.get("uv_size").isJsonArray() ? object.getAsJsonArray("uv_size") : new JsonArray();
            return new FaceUv(number(uv, 0, 0.0F), number(uv, 1, 0.0F),
                    number(uvSize, 0, Math.abs(size.x())), number(uvSize, 1, Math.abs(size.y())));
        }
    }

    public record Locator(AstralGeoTransform offset, AstralGeoTransform rotation, boolean ignoreInheritedScale) {
        private static Locator read(JsonElement element) {
            if (element == null || element.isJsonNull()) return new Locator(AstralGeoTransform.ZERO, AstralGeoTransform.ZERO, false);
            if (element.isJsonArray()) return new Locator(AstralGeoTransform.read(element, AstralGeoTransform.ZERO), AstralGeoTransform.ZERO, false);
            if (!element.isJsonObject()) return new Locator(AstralGeoTransform.ZERO, AstralGeoTransform.ZERO, false);
            JsonObject object = element.getAsJsonObject();
            return new Locator(AstralGeoTransform.read(object.get("offset"), AstralGeoTransform.ZERO),
                    AstralGeoTransform.read(object.get("rotation"), AstralGeoTransform.ZERO), bool(object, "ignore_inherited_scale", false));
        }
    }

    private static String string(JsonObject object, String name, String fallback) {
        return object.has(name) && object.get(name).isJsonPrimitive() ? object.get(name).getAsString() : fallback;
    }

    private static boolean bool(JsonObject object, String name, boolean fallback) {
        return object.has(name) && object.get(name).isJsonPrimitive() ? object.get(name).getAsBoolean() : fallback;
    }

    private static float number(JsonArray array, int index, float fallback) {
        return array.size() > index && array.get(index).isJsonPrimitive() && array.get(index).getAsJsonPrimitive().isNumber() ? array.get(index).getAsFloat() : fallback;
    }
}

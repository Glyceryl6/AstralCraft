package com.astral_craft.client.model.character;

import java.util.List;
import java.util.Locale;

public record AstralGeoKeyframe(float time, AstralGeoTransform pre, AstralGeoTransform post,
                                String interpolation, String easing, List<Float> easingArgs) {

    public static AstralGeoTransform sample(List<AstralGeoKeyframe> frames, float time, AstralGeoTransform fallback) {
        if (frames == null || frames.isEmpty()) return fallback;
        if (frames.size() == 1 || time <= frames.getFirst().time()) return frames.getFirst().post();
        for (int i = 1; i < frames.size(); i++) {
            AstralGeoKeyframe previous = frames.get(i - 1);
            AstralGeoKeyframe next = frames.get(i);
            if (time <= next.time()) {
                float length = Math.max(0.0001F, next.time() - previous.time());
                float progress = Math.clamp((time - previous.time()) / length, 0.0F, 1.0F);
                String mode = normalize(next.interpolation());
                if ("step".equals(mode) || "stepped".equals(mode)) return previous.post();
                progress = ease(progress, next.easing(), next.easingArgs());
                if ("catmullrom".equals(mode) || "catmull_rom".equals(mode)) {
                    AstralGeoTransform p0 = i > 1 ? frames.get(i - 2).post() : previous.post();
                    AstralGeoTransform p3 = i + 1 < frames.size() ? frames.get(i + 1).pre() : next.pre();
                    return AstralGeoTransform.catmullRom(p0, previous.post(), next.pre(), p3, progress);
                }
                return previous.post().lerp(next.pre(), progress);
            }
        }
        return frames.getLast().post();
    }

    private static float ease(float value, String easing, List<Float> args) {
        String name = normalize(easing);
        if (name.isBlank() || "linear".equals(name)) return value;
        return switch (name) {
            case "easeinsine", "insine" -> 1.0F - (float) Math.cos(value * Math.PI * 0.5D);
            case "easeoutsine", "outsine" -> (float) Math.sin(value * Math.PI * 0.5D);
            case "easeinoutsine", "inoutsine" -> -(float) (Math.cos(Math.PI * value) - 1.0D) * 0.5F;
            case "easeinquad", "inquad" -> value * value;
            case "easeoutquad", "outquad" -> 1.0F - (1.0F - value) * (1.0F - value);
            case "easeinoutquad", "inoutquad" -> value < 0.5F ? 2.0F * value * value : 1.0F - (float) Math.pow(-2.0F * value + 2.0F, 2.0D) * 0.5F;
            case "easeincubic", "incubic" -> value * value * value;
            case "easeoutcubic", "outcubic" -> 1.0F - (float) Math.pow(1.0F - value, 3.0D);
            case "easeinoutcubic", "inoutcubic" -> value < 0.5F ? 4.0F * value * value * value : 1.0F - (float) Math.pow(-2.0F * value + 2.0F, 3.0D) * 0.5F;
            case "easeinback", "inback" -> {
                float c = args.isEmpty() ? 1.70158F : args.getFirst();
                yield (c + 1.0F) * value * value * value - c * value * value;
            }
            case "easeoutback", "outback" -> {
                float c = args.isEmpty() ? 1.70158F : args.getFirst();
                float t = value - 1.0F;
                yield 1.0F + (c + 1.0F) * t * t * t + c * t * t;
            }
            case "easeoutbounce", "outbounce", "bounce" -> bounce(value);
            default -> value;
        };
    }

    private static float bounce(float value) {
        float n = 7.5625F;
        float d = 2.75F;
        if (value < 1.0F / d) return n * value * value;
        if (value < 2.0F / d) {
            float t = value - 1.5F / d;
            return n * t * t + 0.75F;
        }
        if (value < 2.5F / d) {
            float t = value - 2.25F / d;
            return n * t * t + 0.9375F;
        }
        float t = value - 2.625F / d;
        return n * t * t + 0.984375F;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
    }
}

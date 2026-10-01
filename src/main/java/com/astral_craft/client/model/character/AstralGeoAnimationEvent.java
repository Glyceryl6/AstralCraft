package com.astral_craft.client.model.character;

public record AstralGeoAnimationEvent(float time, Type type, String effect, String locator, String script) {
    public enum Type {
        SOUND,
        PARTICLE,
        TIMELINE
    }
}

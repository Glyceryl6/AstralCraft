package com.astral_craft.client.model.character;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
public class AstralGeoAnimationManager extends SimpleJsonResourceReloadListener<AstralGeoAnimationSet> {

    public static final String DIRECTORY = "astral_party/character_animations";
    public static final AstralGeoAnimationManager INSTANCE = new AstralGeoAnimationManager();

    protected final Map<Identifier, AstralGeoAnimationSet> animationSets = new LinkedHashMap<>();

    public AstralGeoAnimationManager() {
        super(AstralGeoAnimationSet.CODEC, FileToIdConverter.json(DIRECTORY));
    }

    @Override
    protected void apply(Map<Identifier, AstralGeoAnimationSet> elements, ResourceManager resourceManager, ProfilerFiller profiler) {
        this.animationSets.clear();
        for (Map.Entry<Identifier, AstralGeoAnimationSet> entry : elements.entrySet()) {
            AstralGeoAnimationSet set = AstralGeoAnimationSet.read(entry.getKey(), entry.getValue().source());
            this.animationSets.put(entry.getKey(), set);
            String path = entry.getKey().getPath();
            if (path.endsWith(".animation")) this.animationSets.putIfAbsent(Identifier.fromNamespaceAndPath(entry.getKey().getNamespace(), path.substring(0, path.length() - 10)), set);
        }
    }

    public AstralGeoAnimationSet get(Identifier id) {
        return this.animationSets.get(id);
    }

    public AstralGeoPose sample(Identifier id, String action, String boneName, float timeSeconds) {
        AstralGeoAnimationSet set = this.get(id);
        if (set == null) return AstralGeoPose.IDENTITY;
        AstralGeoAnimationClip clip = set.clip(action);
        return clip == null ? AstralGeoPose.IDENTITY : clip.sample(boneName, timeSeconds);
    }


    public AstralGeoAnimationClip clip(Identifier id, String action) {
        AstralGeoAnimationSet set = this.get(id);
        return set == null ? null : set.clip(action);
    }

    public AstralGeoAnimationClip exactClip(Identifier id, String action) {
        AstralGeoAnimationSet set = this.get(id);
        return set == null ? null : set.exactClip(action);
    }

    public List<AstralGeoAnimationEvent> events(Identifier id, String action) {
        AstralGeoAnimationClip clip = this.clip(id, action);
        return clip == null ? List.of() : clip.events();
    }
    public List<String> animationNames(Identifier id) {
        AstralGeoAnimationSet set = this.get(id);
        return set == null ? List.of("idle") : set.clipNames();
    }

}

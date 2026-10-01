package com.astral_craft.client.model.character;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedHashMap;
import java.util.Map;

@ParametersAreNonnullByDefault
public class AstralGeoModelManager extends SimpleJsonResourceReloadListener<AstralGeoModelDefinition> {

    public static final String DIRECTORY = "astral_party/character_models";
    public static final AstralGeoModelManager INSTANCE = new AstralGeoModelManager();

    protected final Map<Identifier, AstralGeoModelDefinition> models = new LinkedHashMap<>();

    public AstralGeoModelManager() {
        super(AstralGeoModelDefinition.CODEC, FileToIdConverter.json(DIRECTORY));
    }

    @Override
    protected void apply(Map<Identifier, AstralGeoModelDefinition> elements, ResourceManager resourceManager, ProfilerFiller profiler) {
        this.models.clear();
        for (Map.Entry<Identifier, AstralGeoModelDefinition> entry : elements.entrySet()) {
            AstralGeoModelDefinition definition = AstralGeoModelDefinition.read(entry.getKey(), entry.getValue().source());
            this.models.put(entry.getKey(), definition);
            String path = entry.getKey().getPath();
            if (path.endsWith(".geo")) this.models.putIfAbsent(Identifier.fromNamespaceAndPath(entry.getKey().getNamespace(), path.substring(0, path.length() - 4)), definition);
        }
    }

    public AstralGeoModelDefinition get(Identifier id) {
        return this.models.get(id);
    }

}
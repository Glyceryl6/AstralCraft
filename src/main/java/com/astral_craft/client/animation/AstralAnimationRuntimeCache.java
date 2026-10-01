package com.astral_craft.client.animation;

import com.astral_craft.common.network.s2c.CharacterAnimationCommandPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class AstralAnimationRuntimeCache {

    private static final int MAX_RUNTIMES = 512;
    private static final Map<UUID, AstralAnimationRuntime> RUNTIMES = new LinkedHashMap<>(64, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, AstralAnimationRuntime> eldest) {
            return this.size() > MAX_RUNTIMES;
        }
    };

    public static AstralAnimationRuntime get(UUID entityId) {
        return RUNTIMES.computeIfAbsent(entityId, ignored -> new AstralAnimationRuntime());
    }

    public static void handle(CharacterAnimationCommandPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null) return;
            Entity entity = minecraft.level.getEntity(payload.entityId());
            if (entity == null) return;
            get(entity.getUUID()).apply(payload.command(), payload.startGameTick() / 20.0F);
        });
    }

    public static void clear() {
        RUNTIMES.clear();
    }

}
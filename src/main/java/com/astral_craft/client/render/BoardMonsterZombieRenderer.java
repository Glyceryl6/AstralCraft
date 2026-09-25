package com.astral_craft.client.render;

import com.astral_craft.client.gui.board.BoardHudOverlay;
import com.astral_craft.common.entity.BoardMonsterZombieEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.world.entity.monster.zombie.Zombie;

public class BoardMonsterZombieRenderer extends ZombieRenderer {

    public BoardMonsterZombieRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(Zombie entity, Frustum culler, double camX, double camY, double camZ) {
        if (entity instanceof BoardMonsterZombieEntity monster
                && monster.boardSessionId().filter(BoardHudOverlay::isTracking).isEmpty()) return false;
        return super.shouldRender(entity, culler, camX, camY, camZ);
    }

}
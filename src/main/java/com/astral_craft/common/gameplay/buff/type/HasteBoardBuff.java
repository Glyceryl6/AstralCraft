package com.astral_craft.common.gameplay.buff.type;

import com.astral_craft.common.gameplay.buff.BoardBuff;
import com.astral_craft.common.gameplay.buff.BoardBuffInstance;

public class HasteBoardBuff extends BoardBuff {

    public HasteBoardBuff(int color) {
        super(Properties.of(color).permanent().consumeAfterMoveRoll());
    }

    @Override
    public BoardBuffInstance merge(BoardBuffInstance current, BoardBuffInstance incoming) {
        return incoming == null ? current : this.normalize(incoming);
    }

    @Override
    public int moveDiceModifier(BoardBuffInstance instance) {
        return Math.max(0, instance.value() * instance.level());
    }

}
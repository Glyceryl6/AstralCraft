package com.astral_craft.common.entity;

import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Optional;
import java.util.UUID;

/** Zombie-shaped board monster whose facing is controlled entirely by the board route. */
public class BoardMonsterZombieEntity extends Zombie {

    private static final EntityDataAccessor<Integer> DATA_BOARD_DIRECTION = SynchedEntityData.defineId(
            BoardMonsterZombieEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_BOARD_SESSION_ID = SynchedEntityData.defineId(
            BoardMonsterZombieEntity.class, EntityDataSerializers.STRING);

    public BoardMonsterZombieEntity(EntityType<? extends BoardMonsterZombieEntity> type, Level level) {
        super(type, level);
        this.setNoAi(true);
        this.setCanPickUpLoot(false);
        this.setInvulnerable(true);
        this.setNoGravity(true);
        this.setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BOARD_DIRECTION, Direction.NORTH.get2DDataValue());
        builder.define(DATA_BOARD_SESSION_ID, "");
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (DATA_BOARD_DIRECTION.equals(accessor)) this.applyBoardRotation(this.boardDirection());
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(0.0D, 0.0D, 0.0D);
        this.applyBoardRotation(this.boardDirection());
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }

    public Direction boardDirection() {
        return Direction.from2DDataValue(Math.floorMod(this.entityData.get(DATA_BOARD_DIRECTION), 4));
    }

    public void setBoardDirection(Direction direction) {
        if (direction == null || !direction.getAxis().isHorizontal()) return;
        this.entityData.set(DATA_BOARD_DIRECTION, direction.get2DDataValue());
        this.applyBoardRotation(direction);
    }

    public void setBoardSessionId(UUID boardSessionId) {
        this.entityData.set(DATA_BOARD_SESSION_ID, boardSessionId == null ? "" : boardSessionId.toString());
    }

    public Optional<UUID> boardSessionId() {
        String raw = this.entityData.get(DATA_BOARD_SESSION_ID);
        if (raw.isBlank()) return Optional.empty();
        try {
            return Optional.of(UUID.fromString(raw));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(DATA_BOARD_DIRECTION, input.getIntOr("board_direction", Direction.NORTH.get2DDataValue()));
        this.entityData.set(DATA_BOARD_SESSION_ID, input.getStringOr("board_session_id", ""));
        this.applyBoardRotation(this.boardDirection());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("board_direction", this.entityData.get(DATA_BOARD_DIRECTION));
        output.putString("board_session_id", this.entityData.get(DATA_BOARD_SESSION_ID));
    }

    private void applyBoardRotation(Direction direction) {
        float yaw = direction.toYRot();
        this.setYRot(yaw);
        this.setYBodyRot(yaw);
        this.setYHeadRot(yaw);
        this.yRotO = yaw;
        this.yBodyRotO = yaw;
        this.yHeadRotO = yaw;
    }

}
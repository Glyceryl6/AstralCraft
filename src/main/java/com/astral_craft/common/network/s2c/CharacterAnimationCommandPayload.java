package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import com.astral_craft.api.animation.AstralAnimationCommand;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

public record CharacterAnimationCommandPayload(int entityId, long startGameTick, AstralAnimationCommand command) implements CustomPacketPayload {

    public static final Type<CharacterAnimationCommandPayload> TYPE = new Type<>(AstralCraft.prefix("character_animation_command"));
    public static final StreamCodec<ByteBuf, CharacterAnimationCommandPayload> STREAM_CODEC = StreamCodec.ofMember(
            CharacterAnimationCommandPayload::encode, CharacterAnimationCommandPayload::new);
    private static final int MAX_BONE_MASK = 128;

    private CharacterAnimationCommandPayload(ByteBuf buffer) {
        this(ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.VAR_LONG.decode(buffer), decodeCommand(buffer));
    }

    private void encode(ByteBuf buffer) {
        ByteBufCodecs.VAR_INT.encode(buffer, this.entityId);
        ByteBufCodecs.VAR_LONG.encode(buffer, this.startGameTick);
        encodeCommand(buffer, this.command);
    }

    private static AstralAnimationCommand decodeCommand(ByteBuf buffer) {
        AstralAnimationCommand.Operation operation = enumValue(AstralAnimationCommand.Operation.values(), ByteBufCodecs.VAR_INT.decode(buffer));
        String controller = ByteBufCodecs.STRING_UTF8.decode(buffer);
        String animation = ByteBufCodecs.STRING_UTF8.decode(buffer);
        int transitionTicks = ByteBufCodecs.VAR_INT.decode(buffer);
        float speed = ByteBufCodecs.FLOAT.decode(buffer);
        int priority = buffer.readInt();
        AstralAnimationCommand.BlendMode blendMode = enumValue(AstralAnimationCommand.BlendMode.values(), ByteBufCodecs.VAR_INT.decode(buffer));
        AstralAnimationCommand.LoopMode loopMode = enumValue(AstralAnimationCommand.LoopMode.values(), ByteBufCodecs.VAR_INT.decode(buffer));
        List<String> boneMask = ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(MAX_BONE_MASK)).decode(buffer);
        float seekSeconds = ByteBufCodecs.FLOAT.decode(buffer);
        return new AstralAnimationCommand(operation, controller, animation, transitionTicks, speed, priority, blendMode, loopMode, boneMask, seekSeconds);
    }

    private static void encodeCommand(ByteBuf buffer, AstralAnimationCommand command) {
        ByteBufCodecs.VAR_INT.encode(buffer, command.operation().ordinal());
        ByteBufCodecs.STRING_UTF8.encode(buffer, command.controller());
        ByteBufCodecs.STRING_UTF8.encode(buffer, command.animation());
        ByteBufCodecs.VAR_INT.encode(buffer, command.transitionTicks());
        ByteBufCodecs.FLOAT.encode(buffer, command.speed());
        buffer.writeInt(command.priority());
        ByteBufCodecs.VAR_INT.encode(buffer, command.blendMode().ordinal());
        ByteBufCodecs.VAR_INT.encode(buffer, command.loopMode().ordinal());
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(MAX_BONE_MASK)).encode(buffer, command.boneMask());
        ByteBufCodecs.FLOAT.encode(buffer, command.seekSeconds());
    }

    private static <T> T enumValue(T[] values, int index) {
        return values[Math.clamp(index, 0, values.length - 1)];
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
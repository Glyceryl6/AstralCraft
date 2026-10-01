package com.astral_craft.common.network.c2s;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.entity.character.ExhibitionCharacterEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ExhibitionCharacterConfigPayload(
        int entityId,
        Identifier characterId,
        String skinId,
        double x,
        double y,
        double z,
        float yaw,
        float scale,
        String customName,
        boolean showName,
        String speechText,
        String speechImage,
        boolean faceLookingPlayer,
        float speechBubbleOffsetX,
        float speechBubbleOffsetY,
        float speechBubbleWidth,
        float speechBubbleScale,
        boolean customSkinEnabled,
        boolean customSkinPlayer,
        String customSkinSource,
        boolean remove) implements CustomPacketPayload {

    public static final Type<ExhibitionCharacterConfigPayload> TYPE = new Type<>(AstralCraft.prefix("exhibition_character_config"));
    public static final StreamCodec<ByteBuf, ExhibitionCharacterConfigPayload> STREAM_CODEC = StreamCodec.ofMember(
            ExhibitionCharacterConfigPayload::encode, ExhibitionCharacterConfigPayload::new);

    private ExhibitionCharacterConfigPayload(ByteBuf buffer) {
        this(ByteBufCodecs.VAR_INT.decode(buffer), Identifier.STREAM_CODEC.decode(buffer), ByteBufCodecs.STRING_UTF8.decode(buffer),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), ByteBufCodecs.FLOAT.decode(buffer), ByteBufCodecs.FLOAT.decode(buffer),
                ByteBufCodecs.stringUtf8(ExhibitionCharacterEntity.MAX_CUSTOM_NAME_LENGTH).decode(buffer), ByteBufCodecs.BOOL.decode(buffer),
                ByteBufCodecs.stringUtf8(ExhibitionCharacterEntity.MAX_SPEECH_LENGTH).decode(buffer),
                ByteBufCodecs.stringUtf8(ExhibitionCharacterEntity.MAX_SPEECH_IMAGE_SOURCE_LENGTH).decode(buffer), ByteBufCodecs.BOOL.decode(buffer),
                ByteBufCodecs.FLOAT.decode(buffer), ByteBufCodecs.FLOAT.decode(buffer), ByteBufCodecs.FLOAT.decode(buffer), ByteBufCodecs.FLOAT.decode(buffer),
                ByteBufCodecs.BOOL.decode(buffer), ByteBufCodecs.BOOL.decode(buffer),
                ByteBufCodecs.stringUtf8(ExhibitionCharacterEntity.MAX_CUSTOM_SKIN_SOURCE_LENGTH).decode(buffer), ByteBufCodecs.BOOL.decode(buffer));
    }

    private void encode(ByteBuf buffer) {
        ByteBufCodecs.VAR_INT.encode(buffer, this.entityId);
        Identifier.STREAM_CODEC.encode(buffer, this.characterId);
        ByteBufCodecs.STRING_UTF8.encode(buffer, this.skinId);
        buffer.writeDouble(this.x);
        buffer.writeDouble(this.y);
        buffer.writeDouble(this.z);
        ByteBufCodecs.FLOAT.encode(buffer, this.yaw);
        ByteBufCodecs.FLOAT.encode(buffer, this.scale);
        ByteBufCodecs.stringUtf8(ExhibitionCharacterEntity.MAX_CUSTOM_NAME_LENGTH).encode(buffer, this.customName);
        ByteBufCodecs.BOOL.encode(buffer, this.showName);
        ByteBufCodecs.stringUtf8(ExhibitionCharacterEntity.MAX_SPEECH_LENGTH).encode(buffer, this.speechText);
        ByteBufCodecs.stringUtf8(ExhibitionCharacterEntity.MAX_SPEECH_IMAGE_SOURCE_LENGTH).encode(buffer, this.speechImage);
        ByteBufCodecs.BOOL.encode(buffer, this.faceLookingPlayer);
        ByteBufCodecs.FLOAT.encode(buffer, this.speechBubbleOffsetX);
        ByteBufCodecs.FLOAT.encode(buffer, this.speechBubbleOffsetY);
        ByteBufCodecs.FLOAT.encode(buffer, this.speechBubbleWidth);
        ByteBufCodecs.FLOAT.encode(buffer, this.speechBubbleScale);
        ByteBufCodecs.BOOL.encode(buffer, this.customSkinEnabled);
        ByteBufCodecs.BOOL.encode(buffer, this.customSkinPlayer);
        ByteBufCodecs.stringUtf8(ExhibitionCharacterEntity.MAX_CUSTOM_SKIN_SOURCE_LENGTH).encode(buffer, this.customSkinSource);
        ByteBufCodecs.BOOL.encode(buffer, this.remove);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
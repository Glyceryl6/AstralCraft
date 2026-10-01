package com.astral_craft.common.gameplay.character.skin;

import com.astral_craft.AstralCraft;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public record CharacterSkinAddition(
        Identifier character, String id, String nameKey, Identifier texture, boolean unlockedByDefault, String rarity,
        Optional<Identifier> model, Optional<Identifier> animationSet, CharacterSkinDefinition.BattlePresentation battlePresentation) {

    public static final Codec<CharacterSkinAddition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("character").forGetter(CharacterSkinAddition::character),
            Codec.STRING.fieldOf("id").forGetter(CharacterSkinAddition::id),
            Codec.STRING.optionalFieldOf("name_key", "").forGetter(CharacterSkinAddition::nameKey),
            Identifier.CODEC.fieldOf("texture").forGetter(CharacterSkinAddition::texture),
            Codec.BOOL.optionalFieldOf("unlocked_by_default", false).forGetter(CharacterSkinAddition::unlockedByDefault),
            Codec.STRING.optionalFieldOf("rarity", "none").forGetter(CharacterSkinAddition::rarity),
            Identifier.CODEC.optionalFieldOf("model").forGetter(CharacterSkinAddition::model),
            Identifier.CODEC.optionalFieldOf("animation_set").forGetter(CharacterSkinAddition::animationSet),
            CharacterSkinDefinition.BattlePresentation.CODEC.optionalFieldOf("battle", CharacterSkinDefinition.BattlePresentation.NONE)
                    .forGetter(CharacterSkinAddition::battlePresentation)
    ).apply(instance, CharacterSkinAddition::new));

    public CharacterSkinAddition(Identifier character, String id, String nameKey, Identifier texture, boolean unlockedByDefault,
                                 String rarity, CharacterSkinDefinition.BattlePresentation battlePresentation) {
        this(character, id, nameKey, texture, unlockedByDefault, rarity, Optional.empty(), Optional.empty(), battlePresentation);
    }

    public CharacterSkinAddition(Identifier character, String id, String nameKey, Identifier texture, boolean unlockedByDefault, String rarity) {
        this(character, id, nameKey, texture, unlockedByDefault, rarity, CharacterSkinDefinition.BattlePresentation.NONE);
    }

    public CharacterSkinDefinition toSkinDefinition(Identifier sourceFile) {
        String safeId = this.id.isBlank() ? sourceFile.getPath() : this.id;
        Identifier safeTexture = this.texture.equals(AstralCraft.prefix("entity/character/default.png"))
                ? CharacterSkinManager.defaultSkinTexture(this.character, safeId) : this.texture;
        String safeNameKey = this.nameKey.isBlank()
                ? "character." + this.character.getNamespace() + "." + this.character.getPath() + ".skin." + safeId : this.nameKey;
        String safeRarity = this.rarity.isBlank() ? "none" : this.rarity;
        return new CharacterSkinDefinition(safeId, safeNameKey, safeTexture, this.unlockedByDefault,
                safeRarity, this.model, this.animationSet, this.battlePresentation);
    }

}
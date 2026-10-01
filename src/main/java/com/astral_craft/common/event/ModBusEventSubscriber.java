package com.astral_craft.common.event;

import com.astral_craft.AstralCraft;
import com.astral_craft.AstralVersion;
import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.gameplay.character.skin.CharacterSkinAddition;
import com.astral_craft.common.gameplay.character.skin.CharacterSkinRarityDefinition;
import com.astral_craft.common.gameplay.event.AstralEventDefinition;
import com.astral_craft.common.gameplay.fortune.BoardFortuneDefinition;
import com.astral_craft.common.network.registration.AstralPayloadRegistrars;
import com.astral_craft.common.registry.AstralAttributes;
import com.astral_craft.common.registry.AstralEntities;
import com.astral_craft.common.registry.bootstrap.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

@EventBusSubscriber(modid = AstralCraft.MOD_ID)
public class ModBusEventSubscriber {

    @SubscribeEvent
    public static void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(AstralEntities.ASTRAL_CHARACTER.get(), AstralCharacterEntity.createAttributes().build());
        event.put(AstralEntities.EXHIBITION_CHARACTER.get(), AstralCharacterEntity.createAttributes().build());
        event.put(AstralEntities.BOARD_MONSTER_ZOMBIE.get(), Zombie.createAttributes().build());
    }

    @SubscribeEvent
    public static void modifyDefaultAttributes(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, AstralAttributes.HAND_CARD_RANGE, 0.0D);
    }

    @SubscribeEvent
    public static void registerDatapackRegistries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(AstralCharacterSkinBootstrap.CHARACTER_SKINS, CharacterSkinAddition.CODEC, CharacterSkinAddition.CODEC);
        event.dataPackRegistry(AstralSkinRarityBootstrap.SKIN_RARITIES, CharacterSkinRarityDefinition.CODEC, CharacterSkinRarityDefinition.CODEC);
        event.dataPackRegistry(AstralEventBootstrap.EVENTS, AstralEventDefinition.CODEC, AstralEventDefinition.CODEC);
        event.dataPackRegistry(AstralFortuneBootstrap.FORTUNES, BoardFortuneDefinition.CODEC, BoardFortuneDefinition.CODEC);
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(AstralVersion.networkProtocol());
        AstralPayloadRegistrars.register(registrar);
    }

}

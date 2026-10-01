package com.astral_craft.common.gameplay.event.effects;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.board.BoardEventTargets;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

import java.util.List;
import java.util.Optional;

public record GiveItemEventEffect(List<ItemStackTemplate> items) implements AstralEventEffect {

    public static final MapCodec<GiveItemEventEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ItemStackTemplate.CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(GiveItemEventEffect::items),
            Item.CODEC.optionalFieldOf("id").forGetter(effect -> Optional.<Holder<Item>>empty()),
            Codec.INT.optionalFieldOf("count", 1).forGetter(effect -> 1)
    ).apply(instance, GiveItemEventEffect::decode));

    public GiveItemEventEffect {
        items = List.copyOf(items);
    }

    public GiveItemEventEffect(Holder<Item> item, int count) {
        this(List.of(new ItemStackTemplate(item, Math.max(1, count))));
    }

    private static GiveItemEventEffect decode(List<ItemStackTemplate> items, Optional<Holder<Item>> legacyItem, int legacyCount) {
        if (!items.isEmpty()) return new GiveItemEventEffect(items);
        return legacyItem.map(item -> new GiveItemEventEffect(item, legacyCount)).orElseGet(() -> new GiveItemEventEffect(List.of()));
    }

    @Override
    public String typeId() {
        return AstralCraft.prefix("give_item").toString();
    }

    @Override
    public MapCodec<? extends AstralEventEffect> codec() {
        return CODEC;
    }

    @Override
    public void apply(AstralEventContext context) {
        if (this.items.isEmpty()) return;
        var boardTarget = BoardEventTargets.resolve(context);
        if (boardTarget.isPresent()) {
            var target = boardTarget.get();
            var updated = target.participant();
            for (ItemStackTemplate template : this.items) {
                ItemStack stack = template.create();
                if (stack.isEmpty()) continue;
                var cardId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                for (int index = 0; index < stack.getCount(); index++) updated = updated.addCard(cardId);
            }
            BoardSessionManager.updateParticipant(target.level(), target.session(), updated);
            return;
        }

        ServerPlayer receiver = context.targetPlayer() != null ? context.targetPlayer() : context.triggerPlayer();
        if (receiver == null) return;
        for (ItemStackTemplate template : this.items) {
            ItemStack stack = template.create();
            if (stack.isEmpty()) continue;
            if (!receiver.addItem(stack) && !stack.isEmpty()) receiver.drop(stack, false);
        }
    }

}

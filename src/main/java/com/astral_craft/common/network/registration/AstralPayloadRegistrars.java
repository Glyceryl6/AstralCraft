package com.astral_craft.common.network.registration;

import com.astral_craft.common.network.AstralServerPayloadHandlers;
import com.astral_craft.common.network.c2s.*;
import com.astral_craft.common.network.s2c.*;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Functional-domain payload registration. Keep protocol/version ownership in ModBusEventSubscriber,
 * but keep feature packet lists next to their domain rather than one monolithic registration method.
 */
public class AstralPayloadRegistrars {

    public static void register(PayloadRegistrar registrar) {
        registerCards(registrar);
        registerBoard(registrar);
        registerCharacters(registrar);
        registerAppearance(registrar);
    }

    private static void registerCards(PayloadRegistrar registrar) {
        registrar.playToClient(CardRevealPayload.TYPE, CardRevealPayload.STREAM_CODEC);
        registrar.playToClient(CardRevealControlPayload.TYPE, CardRevealControlPayload.STREAM_CODEC);
        registrar.playToClient(CardRevealEntityPayload.TYPE, CardRevealEntityPayload.STREAM_CODEC);
        registrar.playToClient(OpenTargetSelectionPayload.TYPE, OpenTargetSelectionPayload.STREAM_CODEC);
        registrar.playToClient(OpenCardNumberSelectionPayload.TYPE, OpenCardNumberSelectionPayload.STREAM_CODEC);
        registrar.playToClient(OpenHandCardDeckPayload.TYPE, OpenHandCardDeckPayload.STREAM_CODEC);
        registrar.playToServer(CardTargetSelectionPayload.TYPE, CardTargetSelectionPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleCardTargets);
        registrar.playToServer(CardNumberSelectionPayload.TYPE, CardNumberSelectionPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleCardNumberSelection);
        registrar.playToServer(RequestHandCardDeckPayload.TYPE, RequestHandCardDeckPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleRequestHandCardDeck);
        registrar.playToServer(UseHandCardFromDeckPayload.TYPE, UseHandCardFromDeckPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleUseHandCardFromDeck);
    }

    private static void registerBoard(PayloadRegistrar registrar) {
        registrar.playToClient(OpenChipSelectionPayload.TYPE, OpenChipSelectionPayload.STREAM_CODEC);
        registrar.playToClient(BoardHudSnapshotPayload.TYPE, BoardHudSnapshotPayload.STREAM_CODEC);
        registrar.playToClient(BoardTimeBombRollPayload.TYPE, BoardTimeBombRollPayload.STREAM_CODEC);
        registrar.playToClient(BoardAnnouncementPayload.TYPE, BoardAnnouncementPayload.STREAM_CODEC);
        registrar.playToClient(OpenBoardCharacterSelectionPayload.TYPE, OpenBoardCharacterSelectionPayload.STREAM_CODEC);
        registrar.playToClient(OpenBoardMatchmakingModeSelectionPayload.TYPE, OpenBoardMatchmakingModeSelectionPayload.STREAM_CODEC);
        registrar.playToClient(OpenBoardDeveloperPayload.TYPE, OpenBoardDeveloperPayload.STREAM_CODEC);
        registrar.playToClient(OpenBoardProjectorConfirmPayload.TYPE, OpenBoardProjectorConfirmPayload.STREAM_CODEC);
        registrar.playToClient(OpenBoardModeSelectionPayload.TYPE, OpenBoardModeSelectionPayload.STREAM_CODEC);
        registrar.playToClient(OpenBoardDivinationPayload.TYPE, OpenBoardDivinationPayload.STREAM_CODEC);
        registrar.playToClient(ResolveBoardDivinationPayload.TYPE, ResolveBoardDivinationPayload.STREAM_CODEC);
        registrar.playToClient(OpenBoardDismantleConfirmPayload.TYPE, OpenBoardDismantleConfirmPayload.STREAM_CODEC);
        registrar.playToClient(OpenBoardTurnPayload.TYPE, OpenBoardTurnPayload.STREAM_CODEC);
        registrar.playToClient(OpenBoardDiscardPayload.TYPE, OpenBoardDiscardPayload.STREAM_CODEC);
        registrar.playToClient(OpenBoardEncounterPayload.TYPE, OpenBoardEncounterPayload.STREAM_CODEC);
        registrar.playToClient(CloseBoardEncounterPayload.TYPE, CloseBoardEncounterPayload.STREAM_CODEC);
        registrar.playToClient(CloseBoardPresentationPayload.TYPE, CloseBoardPresentationPayload.STREAM_CODEC);
        registrar.playToClient(OpenBoardBattlePayload.TYPE, OpenBoardBattlePayload.STREAM_CODEC);
        registrar.playToClient(OpenBoardStartChoicePayload.TYPE, OpenBoardStartChoicePayload.STREAM_CODEC);
        registrar.playToClient(OpenBoardLotteryNumberPayload.TYPE, OpenBoardLotteryNumberPayload.STREAM_CODEC);
        registrar.playToClient(CloseBoardLotteryNumberPayload.TYPE, CloseBoardLotteryNumberPayload.STREAM_CODEC);
        registrar.playToClient(OpenBoardGamblePayload.TYPE, OpenBoardGamblePayload.STREAM_CODEC);
        registrar.playToClient(CloseBoardGamblePayload.TYPE, CloseBoardGamblePayload.STREAM_CODEC);
        registrar.playToClient(OpenBoardLotteryDrawPayload.TYPE, OpenBoardLotteryDrawPayload.STREAM_CODEC);
        registrar.playToClient(OpenBoardHospitalPayload.TYPE, OpenBoardHospitalPayload.STREAM_CODEC);
        registrar.playToClient(CloseBoardHospitalPayload.TYPE, CloseBoardHospitalPayload.STREAM_CODEC);
        registrar.playToClient(OpenBoardPlatformTargetPayload.TYPE, OpenBoardPlatformTargetPayload.STREAM_CODEC);
        registrar.playToClient(CloseBoardPlatformTargetPayload.TYPE, CloseBoardPlatformTargetPayload.STREAM_CODEC);
        registrar.playToClient(CloseBoardLotteryDrawPayload.TYPE, CloseBoardLotteryDrawPayload.STREAM_CODEC);
        registrar.playToClient(OpenBoardShopPayload.TYPE, OpenBoardShopPayload.STREAM_CODEC);
        registrar.playToClient(OpenBoardRelicShopPayload.TYPE, OpenBoardRelicShopPayload.STREAM_CODEC);
        registrar.playToClient(OpenBoardPanelSelectionPayload.TYPE, OpenBoardPanelSelectionPayload.STREAM_CODEC);
        registrar.playToClient(BoardRouteStatePayload.TYPE, BoardRouteStatePayload.STREAM_CODEC);
        registrar.playToServer(ChipSelectionPayload.TYPE, ChipSelectionPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleChipSelection);
        registrar.playToServer(BoardCharacterSelectionPayload.TYPE, BoardCharacterSelectionPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleBoardCharacterSelection);
        registrar.playToServer(BoardMatchmakingModeSelectionPayload.TYPE, BoardMatchmakingModeSelectionPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleBoardMatchmakingModeSelection);
        registrar.playToServer(BoardMatchmakingCancelPayload.TYPE, BoardMatchmakingCancelPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleBoardMatchmakingCancel);
        registrar.playToServer(BoardCharacterSelectionExitPayload.TYPE, BoardCharacterSelectionExitPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleBoardCharacterSelectionExit);
        registrar.playToServer(BoardDeveloperConfigPayload.TYPE, BoardDeveloperConfigPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleBoardDeveloperConfig);
        registrar.playToServer(BoardProjectorConfirmPayload.TYPE, BoardProjectorConfirmPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleBoardProjectorConfirm);
        registrar.playToServer(BoardModeSelectionPayload.TYPE, BoardModeSelectionPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleBoardModeSelection);
        registrar.playToServer(BoardDivinationChoicePayload.TYPE, BoardDivinationChoicePayload.STREAM_CODEC, AstralServerPayloadHandlers::handleBoardDivinationChoice);
        registrar.playToServer(BoardDismantleConfirmPayload.TYPE, BoardDismantleConfirmPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleBoardDismantleConfirm);
        registrar.playToServer(UseBoardCardPayload.TYPE, UseBoardCardPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleUseBoardCard);
        registrar.playToServer(BoardCounterResponsePayload.TYPE, BoardCounterResponsePayload.STREAM_CODEC, AstralServerPayloadHandlers::handleBoardCounterResponse);
        registrar.playToServer(BoardMoveRequestPayload.TYPE, BoardMoveRequestPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleBoardMove);
        registrar.playToServer(BoardSkillRequestPayload.TYPE, BoardSkillRequestPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleBoardSkill);
        registrar.playToServer(BoardDiscardPayload.TYPE, BoardDiscardPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleBoardDiscard);
        registrar.playToServer(BoardEncounterChoicePayload.TYPE, BoardEncounterChoicePayload.STREAM_CODEC, AstralServerPayloadHandlers::handleBoardEncounter);
        registrar.playToServer(BoardBattleActionPayload.TYPE, BoardBattleActionPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleBoardBattle);
        registrar.playToServer(BoardTutorialHintDismissPayload.TYPE, BoardTutorialHintDismissPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleBoardTutorialHintDismiss);
        registrar.playToServer(BoardStartChoicePayload.TYPE, BoardStartChoicePayload.STREAM_CODEC, AstralServerPayloadHandlers::handleBoardStartChoice);
        registrar.playToServer(BoardLotteryNumberPayload.TYPE, BoardLotteryNumberPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleBoardLotteryNumber);
        registrar.playToServer(BoardGambleChoicePayload.TYPE, BoardGambleChoicePayload.STREAM_CODEC, AstralServerPayloadHandlers::handleBoardGambleChoice);
        registrar.playToServer(BoardPlatformTargetPayload.TYPE, BoardPlatformTargetPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleBoardPlatformTarget);
        registrar.playToServer(BoardLeavePayload.TYPE, BoardLeavePayload.STREAM_CODEC, AstralServerPayloadHandlers::handleBoardLeave);
        registrar.playToServer(BoardShopActionPayload.TYPE, BoardShopActionPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleBoardShop);
        registrar.playToServer(BoardRelicShopActionPayload.TYPE, BoardRelicShopActionPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleBoardRelicShop);
        registrar.playToServer(BoardPanelSelectionPayload.TYPE, BoardPanelSelectionPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleBoardPanelSelection);
    }

    private static void registerCharacters(PayloadRegistrar registrar) {
        registrar.playToClient(OpenCharacterSettingsPayload.TYPE, OpenCharacterSettingsPayload.STREAM_CODEC);
        registrar.playToClient(CharacterSkillCutinPayload.TYPE, CharacterSkillCutinPayload.STREAM_CODEC);
        registrar.playToClient(CharacterAnimationCommandPayload.TYPE, CharacterAnimationCommandPayload.STREAM_CODEC);
        registrar.playToClient(OpenExhibitionCharacterConfigPayload.TYPE, OpenExhibitionCharacterConfigPayload.STREAM_CODEC);
        registrar.playToServer(RequestCharacterSettingsPayload.TYPE, RequestCharacterSettingsPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleRequestCharacterSettings);
        registrar.playToServer(RequestCharacterSkillPayload.TYPE, RequestCharacterSkillPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleRequestCharacterSkill);
        registrar.playToServer(CharacterSelectionPayload.TYPE, CharacterSelectionPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleCharacterSelection);
        registrar.playToServer(UnlockAllCharactersPayload.TYPE, UnlockAllCharactersPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleUnlockAllCharacters);
        registrar.playToServer(ActivateCharacterPotentialPayload.TYPE, ActivateCharacterPotentialPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleActivateCharacterPotential);
        registrar.playToServer(CharacterSkinSelectionPayload.TYPE, CharacterSkinSelectionPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleCharacterSkinSelection);
        registrar.playToServer(ExhibitionCharacterConfigPayload.TYPE, ExhibitionCharacterConfigPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleExhibitionCharacterConfig);
    }

    private static void registerAppearance(PayloadRegistrar registrar) {
        registrar.playToClient(OpenCardBackSelectionPayload.TYPE, OpenCardBackSelectionPayload.STREAM_CODEC);
        registrar.playToClient(OpenCustomPaintingConfigPayload.TYPE, OpenCustomPaintingConfigPayload.STREAM_CODEC);
        registrar.playToServer(RequestCardBackSelectionPayload.TYPE, RequestCardBackSelectionPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleRequestCardBackSelection);
        registrar.playToServer(CardBackSelectionPayload.TYPE, CardBackSelectionPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleCardBackSelection);
        registrar.playToServer(CustomPaintingConfigPayload.TYPE, CustomPaintingConfigPayload.STREAM_CODEC, AstralServerPayloadHandlers::handleCustomPaintingConfig);
    }
}

package mech.mania.starterpack.game.character.action;

import mech.mania.starterpack.game.util.Position;

public record AbilityAction(
        String executingCharacterId,
        String characterIdTarget,
        Position positionalTarget,
        AbilityActionType type
) {}

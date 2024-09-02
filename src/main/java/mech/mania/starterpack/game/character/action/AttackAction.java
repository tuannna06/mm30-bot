package mech.mania.starterpack.game.character.action;

public record AttackAction(String executingCharacterId, String attackingId, AttackActionType type) {}

package mech.mania.starterpack.game.character;

import mech.mania.starterpack.game.util.Position;

public record MoveAction(String executingCharacterId, Position destination) {}

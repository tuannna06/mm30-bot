package mech.mania.starterpack.strategy;

import mech.mania.starterpack.game.GameState;
import mech.mania.starterpack.game.character.MoveAction;
import mech.mania.starterpack.game.character.action.AbilityAction;
import mech.mania.starterpack.game.character.action.AttackAction;
import mech.mania.starterpack.game.character.action.CharacterClassType;

import java.util.List;
import java.util.Map;

/**
 * Abstract class representing a strategy for bot decision-making. YOU SHOULD NOT EDIT THIS!
 */
public abstract class Strategy {

    /**
     * Decide the character classes your humans will use (only called on humans' first turn).
     *
     * @param possibleClasses     A list of the possible classes you can select from.
     * @param numToPick           The total number of classes you are allowed to select.
     * @param maxPerSameClass     The max number of characters you can have in the same class.
     * @return                    A dictionary of class type to the number you want to use of that class.
     */
    public abstract Map<CharacterClassType, Integer> decideCharacterClasses(
            List<CharacterClassType> possibleClasses,
            int numToPick,
            int maxPerSameClass
    );

    /**
     * Decide the moves for each character based on the current game state.
     *
     * @param possibleMoves  Maps character id to its possible moves. You can use this to validate if a move is possible, or pick from this list.
     * @param gameState      The current state of all characters and terrain on the map.
     * @return               A list of MoveAction objects representing the chosen moves.
     */
    public abstract List<MoveAction> decideMoves(
            Map<String, List<MoveAction>> possibleMoves,
            GameState gameState
    );

    /**
     * Decide the attacks for each character based on the current game state.
     *
     * @param possibleAttacks  Maps character id to its possible attacks. You can use this to validate if an attack is possible, or pick from this list.
     * @param gameState        The current state of all characters and terrain on the map.
     * @return                 A list of AttackAction objects representing the chosen attacks.
     */
    public abstract List<AttackAction> decideAttacks(
            Map<String, List<AttackAction>> possibleAttacks,
            GameState gameState
    );

    /**
     * Decide the abilities for each character based on the current game state.
     *
     * @param possibleAbilities  Maps character id to its possible abilities. You can use this to validate if an ability is possible, or pick from this list.
     * @param gameState          The current state of all characters and terrain on the map.
     * @return                   A list of AbilityAction objects representing the chosen abilities.
     */
    public abstract List<AbilityAction> decideAbilities(
            Map<String, List<AbilityAction>> possibleAbilities,
            GameState gameState
    );
}
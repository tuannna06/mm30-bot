package mech.mania.starterpack.game.character.action;

public enum CharacterClassType {
    NORMAL("NORMAL"),
    ZOMBIE("ZOMBIE"),
    MARKSMAN("MARKSMAN"),
    TRACEUR("TRACEUR"),
    MEDIC("MEDIC"),
    BUILDER("BUILDER"),
    DEMOLITIONIST("DEMOLITIONIST");

    private final String value;

    CharacterClassType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}

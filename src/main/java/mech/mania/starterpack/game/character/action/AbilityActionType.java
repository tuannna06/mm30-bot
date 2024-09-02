package mech.mania.starterpack.game.character.action;

public enum AbilityActionType {
    BUILD_BARRICADE("BUILD_BARRICADE"),
    HEAL("HEAL");

    private final String value;

    AbilityActionType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}

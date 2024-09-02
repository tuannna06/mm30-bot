package mech.mania.starterpack.game.character.action;

public enum AttackActionType {
    CHARACTER("CHARACTER"),
    TERRAIN("TERRAIN");

    private final String value;

    AttackActionType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}

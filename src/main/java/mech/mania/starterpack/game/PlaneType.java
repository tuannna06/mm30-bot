package mech.mania.starterpack.game;

public enum PlaneType {
    BASIC("BASIC");
    private final String value;

    PlaneType(String value) {
            this.value = value;
        }

    public String getValue() {
        return value;
    }
}

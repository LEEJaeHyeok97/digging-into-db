public enum GlobalVariables {
    MAGIC_NUMBER(0xCAFEBABE), VERSION(1);

    private final int value;

    public int getValue() {
        return value;
    }

    GlobalVariables(int value) {
        this.value = value;
    }
}

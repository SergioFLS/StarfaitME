package invalid.sergonezero.starfaitme;

public class Function {
    String name;
    int occurrenceCount;
    int firstAddress;

    public Function(String name, int occurrenceCount, int firstAddress) {
        this.name = name;
        this.occurrenceCount = occurrenceCount;
        this.firstAddress = firstAddress;
    }

    public String toString() {
        return "Function{" +
            "name='" + name + '\'' +
            ", occurrenceCount=" + occurrenceCount +
            ", firstAddress=" + firstAddress +
            '}';
    }
}

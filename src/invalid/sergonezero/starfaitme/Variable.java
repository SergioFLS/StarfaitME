package invalid.sergonezero.starfaitme;

public class Variable {
    String name;
    int instanceCount;
    int variableId;
    int occurrenceCount;
    int firstAddress;

    public Variable(String name, int instanceCount, int variableId, int occurrenceCount, int firstAddress) {
        this.name = name;
        this.instanceCount = instanceCount;
        this.variableId = variableId;
        this.occurrenceCount = occurrenceCount;
        this.firstAddress = firstAddress;
    }

    public String toString() {
        return "Variable{" +
            "name='" + name + '\'' +
            ", instanceCount=" + instanceCount +
            ", variableId=" + variableId +
            ", occurrenceCount=" + occurrenceCount +
            ", firstAddress=" + firstAddress +
            '}';
    }
}

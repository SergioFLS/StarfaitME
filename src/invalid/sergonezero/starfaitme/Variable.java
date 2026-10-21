package invalid.sergonezero.starfaitme;

public class Variable {
    String name;
    int instanceCount;
    int variableId;
    int occurenceCount;
    int firstAddress;

    public Variable(String name, int instanceCount, int variableId, int occurenceCount, int firstAddress) {
        this.name = name;
        this.instanceCount = instanceCount;
        this.variableId = variableId;
        this.occurenceCount = occurenceCount;
        this.firstAddress = firstAddress;
    }

    public String toString() {
        return "Variable("+name+","+instanceCount+","+variableId+","+occurenceCount+","+firstAddress+")";
    }
}

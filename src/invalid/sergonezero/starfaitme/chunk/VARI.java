package invalid.sergonezero.starfaitme.chunk;

import invalid.sergonezero.starfaitme.LittleStream;
import invalid.sergonezero.starfaitme.Variable;

import java.io.DataInputStream;
import java.io.IOException;

public class VARI {
    public static final int ID = 0x49524156;

    private static final int ENTRY_SIZE = 5*4; // entry values * sizeof int
    
    public Variable[] variables = null;

    public VARI(DataInputStream dis, LittleStream ls, int chunkSize, STRG strg) throws IOException {
        if (ls == null) {
            ls = new LittleStream(dis);
        }

        int globalVariables = ls.readInt();
        int instanceVariables = ls.readInt();
        int localVariables = ls.readInt();

        int entriesCount = (chunkSize - (3*4)) / ENTRY_SIZE;
        variables = new Variable[chunkSize];
        System.out.println(entriesCount + " entries in VARI");
        System.out.println(strg.toString());

        for (int i = 0; i < entriesCount; i++) {
            int namePointer = ls.readInt() - 4; // TODO make this prettier maybe
            String name = strg.getStringFromAddress(namePointer);
            int instanceCount = ls.readInt();
            int variableId = ls.readInt();
            int occurenceCount = ls.readInt();
            int firstAddress = ls.readInt();
            variables[i] = new Variable(name, instanceCount, variableId, occurenceCount, firstAddress);
            System.out.println("created new " + variables[i].toString());
        }
    }
}

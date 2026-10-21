package invalid.sergonezero.starfaitme.chunk;

import invalid.sergonezero.starfaitme.Function;
import invalid.sergonezero.starfaitme.LittleStream;

import java.io.DataInputStream;
import java.io.IOException;

public class FUNC {
    public static final int ID = 0x434e5546;

    public Function[] functions = null;
    public FUNC(DataInputStream dis, LittleStream ls, STRG strg) throws IOException {
        if (ls == null) {
            ls = new  LittleStream(dis);
        }

        int count = ls.readInt();
        functions = new Function[count];
        for (int i = 0; i < count; i++) {
            int namePointer = ls.readInt();
            String name = strg.getStringFromAddress(namePointer);
            int occurrenceCount = ls.readInt();
            int firstAddress = ls.readInt();

            functions[i] = new Function(name, occurrenceCount, firstAddress);
            System.out.println("created new " + functions[i].toString());
        }
    }
}

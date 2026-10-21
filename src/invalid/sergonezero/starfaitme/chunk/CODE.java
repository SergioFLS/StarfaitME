package invalid.sergonezero.starfaitme.chunk;

import invalid.sergonezero.starfaitme.LittleStream;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class CODE {
    public static final int ID = 1162104643;

    public byte[][] codes;

    public CODE(int rawFileSize, int chunkSize, DataInputStream d, LittleStream ls) throws IOException {
        if (ls == null) {
            ls = new LittleStream(d);
        }
        //System.err.println("CODE constructor");

        int rawPosition = rawFileSize - d.available();
        byte[] raw = new byte[chunkSize];
        d.readFully(raw);

        ByteArrayInputStream bais = new ByteArrayInputStream(raw);
        bais.mark(raw.length);
        LittleStream lbs = new LittleStream(bais);
        // offset list
        int addressesCount = lbs.readInt();
        int[] addresses = new int[addressesCount];
        codes = new byte[addressesCount][];
        for (int i = 0; i < addressesCount; i++) {
            addresses[i] = lbs.readInt();
        }

        for (int i = 0; i < addressesCount; i++) {
            int address = addresses[i];
            bais.reset();
            bais.skip(address - rawPosition);
            //System.out.println("skipping " + bais.skip(rawFileSize - address) + " from " + (rawFileSize - address));

            int namePointer = lbs.readInt();
            if (namePointer != 0xAD8) { throw new IllegalArgumentException("bad offset"); }
            int length = lbs.readInt();
            int localsCount = lbs.readShort() & 0xFFFF;
            int argumentsCount = lbs.readShort() & 0xFFFF;
            int relativeOffsetFieldPosition = rawPosition + (chunkSize - bais.available());
            int relativeOffset = lbs.readInt();
            int offset = lbs.readInt();

            bais.reset();
            bais.skip(relativeOffsetFieldPosition + relativeOffset - rawPosition);
            byte[] codeData = new byte[length];
            bais.read(codeData);
            codes[i] = codeData;

//            System.out.println("[");
//            for (int j = 0; j < length; j++) {
//                System.out.println(codes[i][j]);
//            }
//            System.out.println("]");
        }
    }
}

package invalid.sergonezero.starfaitme.chunk;

import invalid.sergonezero.starfaitme.LittleStream;

import java.io.DataInputStream;
import java.io.IOException;

public class CODE {
    public CODE(int rawSize, DataInputStream d, LittleStream ls) throws IOException {
        if (ls == null) {
            ls = new LittleStream(d);
        }
        System.err.println("CODE constructor");
        int lastPosition = rawSize - d.available();
        System.err.println(lastPosition);

        int lenOffsets = ls.readInt();
        int[] offsets = new int[lenOffsets];
        for (int i = 0; i < lenOffsets; i++) {
            offsets[i] = ls.readInt();
            //System.out.println((offsets[i] - lastPosition) - lastPosition);
        }


    }
}

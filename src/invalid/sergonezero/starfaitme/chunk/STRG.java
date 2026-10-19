package invalid.sergonezero.starfaitme.chunk;

import invalid.sergonezero.starfaitme.LittleStream;

import java.io.DataInputStream;
import java.io.IOException;

public class STRG {
    public int[] offsets;
    public String[] strings;

    public STRG(DataInputStream d, LittleStream ls) throws IOException {
        if (ls == null) {
            ls = new LittleStream(d);
        }

        int lenOffsets = ls.readInt();
        offsets = new int[lenOffsets];
        strings = new String[lenOffsets];
        for (int i = 0; i < lenOffsets; i++) {
            offsets[i] = ls.readInt();
        }

        // assuming the offsets are sorted because IDK how to seek here...
        for (int i = 0; i < lenOffsets; i++) {
            int stringLength = ls.readInt();
            byte[] rawString = new byte[stringLength];
            StringBuffer buf = new StringBuffer();
            for (int j = 0; j < stringLength; j++) {
                rawString[j] = d.readByte();
                buf.append((char) rawString[j]); // TODO decode to UTF-8?
            }
            d.skip(1); // NUL terminator
            strings[i] = buf.toString();
        }
    }

    public int getLength() {
        return offsets.length;
    }

    public String getStringFromOffset(int offset) {
        int index = -1;
        for (int i = 0; i < offsets.length; i++) {
            if (offsets[i] == offset) {
                index = i;
            }
        }

        if (index == -1) return null;
        return strings[index];
    }
}

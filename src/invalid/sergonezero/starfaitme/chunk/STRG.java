package invalid.sergonezero.starfaitme.chunk;

import invalid.sergonezero.starfaitme.LittleStream;

import java.io.DataInputStream;
import java.io.IOException;

public class STRG {
    public static final int ID = 1196577875;
    public int[] addresses;
    public String[] strings;

    public STRG(DataInputStream d, LittleStream ls) throws IOException {
        if (ls == null) {
            ls = new LittleStream(d);
        }

        int lenAddresses = ls.readInt();
        addresses = new int[lenAddresses];
        strings = new String[lenAddresses];
        for (int i = 0; i < lenAddresses; i++) {
            addresses[i] = ls.readInt();
        }

        // assuming the addresses are sorted because IDK how to seek here...
        for (int i = 0; i < lenAddresses; i++) {
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
        return addresses.length;
    }

    public String getStringFromAddress(int offset) {
        int index = -1;
        for (int i = 0; i < addresses.length; i++) {
            if (addresses[i] == offset) {
                index = i;
                break;
            }
        }

        if (index == -1) return null;
        return strings[index];
    }
}

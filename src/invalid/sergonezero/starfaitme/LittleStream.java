package invalid.sergonezero.starfaitme;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class LittleStream {
    private final DataInputStream dis;
    public LittleStream(DataInputStream dis) {
        this.dis = dis;
    }

    public LittleStream(ByteArrayInputStream bais) {
        this.dis = new DataInputStream(bais);
    }

    public int readInt() throws IOException {
        return EndianUtil.swap(dis.readInt());
    }

    public short readShort() throws IOException {
        return EndianUtil.swap(dis.readShort());
    }

    public boolean isEndOfStream() throws IOException {
        return dis.available() <= 0;
    }

    private static class EndianUtil {
        static int swap(int i) {
            return ((i >> 24) & 0xFF) | ((i >> 8) & 0xFF00) | ((i << 8) & 0xFF0000) | ((i & 0xFF) << 24);
        }

        static short swap(short s) {
            return (short) (((s >> 8) & 0xFF) | ((s & 0xFF) << 8));
        }
    }
}
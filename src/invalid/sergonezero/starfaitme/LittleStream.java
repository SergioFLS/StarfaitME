package invalid.sergonezero.starfaitme;

import java.io.DataInputStream;
import java.io.IOException;

public class LittleStream {
    private DataInputStream dis;
    public LittleStream(DataInputStream dis) {
        this.dis = dis;
    }

    public int readInt() throws IOException {
        int raw = dis.readInt();
        //System.err.println("raw: " + raw);
        return ((raw >> 24) & 0xFF) | ((raw >> 8) & 0xFF00) | ((raw << 8) & 0xFF0000) | ((raw & 0xFF) << 24);
    }

    public boolean isEndOfStream() throws IOException {
        return dis.available() <= 0;
    }
}
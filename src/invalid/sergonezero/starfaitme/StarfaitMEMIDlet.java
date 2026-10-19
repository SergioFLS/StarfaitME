package invalid.sergonezero.starfaitme;
import invalid.sergonezero.starfaitme.chunk.CODE;
import invalid.sergonezero.starfaitme.chunk.STRG;

import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Form;
import javax.microedition.midlet.MIDlet;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class StarfaitMEMIDlet extends MIDlet {
    private static final int ID_FORM = 0x464f524d;
    private static final int ID_GEN8 = 944653639;
    private static final int ID_OPTN = 1314148431;
    private static final int ID_LANG = 1196310860;
    private static final int ID_FONT = 1414418246;
    private static final int ID_CODE = 1162104643;
    private static final int ID_STRG = 1196577875;

    protected void startApp() {
        Form f = new Form("StarfaitME");
        f.append("Your MIDlet seems to run.");
        Display.getDisplay(this).setCurrent(f);
        InputStream is = this.getClass().getResourceAsStream("/data.win");
        if (is == null) {
            System.err.println("Could not find resource 'data.win'");
            return;
        }
        DataInputStream d = new DataInputStream(is);
        LittleStream ls = new LittleStream(d);
        try {
            int rawSize = d.available();
            System.out.println(d.readInt() == ID_FORM);
            int size = ls.readInt();
            System.out.println(size);

            while (!ls.isEndOfStream()) {
                int chunkId =  ls.readInt();
                int chunkSize = ls.readInt();
                d.mark(chunkSize);
                System.out.println(chunkId + " " + chunkSize);
                switch (chunkId) {
                    case ID_STRG:
                        new STRG(d, ls);
                        break;
                    case ID_CODE:
                        new CODE(rawSize, d, ls);
                        break;
                }
                d.reset();
                d.skip(chunkSize);
                //System.out.println("skipping " + d.skip(chunkSize) + " bytes");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void pauseApp() {
    }

    protected void destroyApp(boolean unconditional) {
    }
}

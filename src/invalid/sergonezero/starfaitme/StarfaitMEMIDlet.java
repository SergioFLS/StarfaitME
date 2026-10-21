package invalid.sergonezero.starfaitme;
import invalid.sergonezero.starfaitme.chunk.CODE;
import invalid.sergonezero.starfaitme.chunk.FUNC;
import invalid.sergonezero.starfaitme.chunk.STRG;
import invalid.sergonezero.starfaitme.chunk.VARI;

import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Form;
import javax.microedition.midlet.MIDlet;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;

public class StarfaitMEMIDlet extends MIDlet {
    private static final int ID_FORM = 0x464f524d;
    private static String message = "no message";
    private static STRG strg = null;
    private static CODE code = null;
    private static VARI vari = null;
    private static FUNC func = null;

    protected void startApp() {
        InputStream is = this.getClass().getResourceAsStream("/data.win");
        if (is == null) {
            System.err.println("Could not find resource 'data.win'");
            return;
        }
        DataInputStream d = new DataInputStream(is);
        LittleStream ls = new LittleStream(d);
        try {
            int rawFileSize = d.available();
            System.out.println(d.readInt() == ID_FORM);
            int size = ls.readInt();
            System.out.println(size);

            // find the STRG chunk first
            d.mark(size);
            while (!ls.isEndOfStream() && strg == null) {
                int chunkId =  ls.readInt();
                int chunkSize = ls.readInt();

                if (chunkId != STRG.ID) {
                    d.skip(chunkSize);
                    continue;
                }

                strg = new STRG(d, ls);
            }
            d.reset();

            if (strg == null)
                throw new IllegalStateException("STRG chunk not found");

            while (!ls.isEndOfStream()) {
                int chunkId =  ls.readInt();
                int chunkSize = ls.readInt();
                d.mark(chunkSize);
                System.out.println(chunkId + " " + chunkSize);
                switch (chunkId) {
                    case STRG.ID:
                        // STRG should have been already parsed
                        break;
                    case CODE.ID:
                        code = new CODE(rawFileSize, chunkSize, d, ls);
                        break;
                    case VARI.ID:
                        vari = new VARI(d, ls, chunkSize, strg);
                        break;
                    case FUNC.ID:
                        func = new FUNC(d, ls, strg);
                        break;
                }
                d.reset();
                d.skip(chunkSize);
                //System.out.println("skipping " + d.skip(chunkSize) + " bytes");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        interpreter();

        Form f = new Form("StarfaitME");
        f.append("Message: " + message);
        Display.getDisplay(this).setCurrent(f);
    }

    private void interpreter() {
        if (strg == null) { throw new NullPointerException(); }
        if (code == null) { throw new NullPointerException(); }
        byte[] interpretingCode = code.codes[0];
        ByteArrayInputStream bais = new ByteArrayInputStream(interpretingCode);
        LittleStream ls = new LittleStream(bais);
        Stack stack = new Stack();

        try {
            while (!ls.isEndOfStream()) {
                int opword = ls.readInt();
                int opcode = (opword >> 24) & 0xFF;
                int type1 = (opword >> 16) & 0xFF;
                short extra = (short)opword;

                System.out.println(
                    "Opcode: " + Integer.toHexString(opcode)
                    + ", Type1: " + Integer.toHexString(type1)
                    + ", Extra: " + extra);

                switch (opcode) {
                    case 0xC0: { // PUSH
                        if (type1 != 6)
                            throw new IllegalStateException("Unknown type1: " + type1);

                        int stringIndex = ls.readInt();
                        String stringToAdd = strg.strings[stringIndex];
                        System.out.println("String: " + stringToAdd);
                        stack.push(stringToAdd);
                        break;
                    }
                    case 0x07: // CONV, no-op for now
                        break;
                    case 0xD9: { // CALL
                        Stack arguments = new Stack();

                        for (int i = 0; i < extra; i++) {
                            arguments.push(stack.pop());
                        }

                        // not the real function index, yet
                        // see note in real TinyGameMakerVM
                        int functionIndex = ls.readInt();

                        // assuming this is 'show_debug_message'
                        String arg0 = (String)arguments.firstElement().toString();
                        System.out.println("Game: " + arg0);
                        message = arg0;

                        stack.push(null);
                        break;
                    }
                    case 0x9E: { // POPZ
                        stack.pop();
                        break;
                    }
                    default:
                        throw new IllegalStateException("Unknown opcode: " + Integer.toHexString(opcode));
                }
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

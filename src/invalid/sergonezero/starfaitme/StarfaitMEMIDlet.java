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

            interpreter();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Form f = new Form("StarfaitME");
        f.append("Message: " + message);
        Display.getDisplay(this).setCurrent(f);
    }

    private void interpreter() throws IOException {
        if (strg == null) throw new NullPointerException();
        if (code == null) throw new NullPointerException();

        remapReferences();
        byte[] interpretingCode = code.codes[0];
        ByteArrayInputStream bais = new ByteArrayInputStream(interpretingCode);
        LittleStream ls = new LittleStream(bais);
        Stack stack = new Stack();

        while (!ls.isEndOfStream()) {
            int opword = ls.readInt();
            int opcode = (opword >> 24) & 0xFF;
            int type1 = (opword >> 16) & 0xFF;
            short extra = (short) opword;

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

                    int functionIndex = ls.readInt();
                    System.out.println("CALL function index: " + functionIndex);
                    System.out.println(func.functions[functionIndex]);

                    // assuming this is 'show_debug_message'
                    String arg0 = (String) arguments.firstElement().toString();
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
    }

    private void remapReferences() throws IOException {
        // "I'm not sure WHY the YoYo Runner does this!!" SAME
        System.out.println("remapReferences");
        remapVariableReferences();
        remapFunctionReferences();

    }

    private void remapVariableReferences() {
        // count how many variables have an address
        int variablesWithAddressesCount = 0;
        for (int i = 0; i < vari.variables.length; i++) {
            Variable variable = vari.variables[i];
            if (variable.firstAddress != -1)
                variablesWithAddressesCount++;
        }

        if (variablesWithAddressesCount < 1)
            return;

        throw new IllegalStateException("not implemented");
//        Variable[] variables = new Variable[variablesWithAddressesCount];
//
//        {
//            int i = 0;
//
//            for (int j = 0; j < vari.variables.length; j++) {
//                Variable variable = vari.variables[j];
//                if (variable.firstAddress == -1)
//                    continue;
//
//                i++;
//            }
//        }
    }
    private void remapFunctionReferences() throws IOException {
        byte[] patchingCode = code.codes[0];
        ByteArrayInputStream bais = new ByteArrayInputStream(patchingCode);
        LittleStream ls = new LittleStream(bais);

        bais.mark(patchingCode.length);
        System.out.println("patchingCode.length: " + patchingCode.length);
        for (int i = 0; i < func.functions.length; i++) {
            Function function = func.functions[i];
            if (function.firstAddress == -1)
                continue;

            System.out.println("function.firstAddress: " + function.firstAddress);
            // "This points to the INSTRUCTION address, NOT the operand address, which is why we do +4"
            int operandAddress = (function.firstAddress - code.postAddressesPosition) +  4;
            System.out.println("operandAddress: "  + operandAddress);
            for (int j = 0; j < function.occurrenceCount; j++) {
                // goto operand
                bais.reset();
                bais.skip(operandAddress);
                // read int
                int operand = ls.readInt();
                // delta = (read int) & 0x0FFFFFFF
                int delta = operand & 0x0FFFFFFF;
                // overwrite where the read int originally was with 'i'
                // I don't want to bother using OutputStream lol
                patchingCode[operandAddress]        = (byte)(i & 0xFF);
                patchingCode[operandAddress + 1]    = (byte)((i >> 8) & 0xFF);
                patchingCode[operandAddress + 2]    = (byte)((i >> 16) & 0xFF);
                patchingCode[operandAddress + 3]    = (byte)((i >> 24) & 0xFF);
                // operandAddress += delta

                bais.reset();
                bais.skip(operandAddress);
                System.out.println("patched with " + ls.readInt());

                operandAddress += delta;
            }
        }
    }

    protected void pauseApp() {
    }

    protected void destroyApp(boolean unconditional) {
    }
}

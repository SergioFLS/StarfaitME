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
            int type1 = (opword >> 16) & 0xF;
            int type2 = (opword >> 20) & 0xF;
            short extra = (short) opword;

            System.out.println(
                "Opcode: " + Integer.toHexString(opcode)
                    + ", Type1: " + Integer.toHexString(type1)
                    + ", Type2: " + Integer.toHexString(type2)
                    + ", Extra: " + extra);

            switch (opcode) {
                case 0xC0: { // PUSH
                    if (type1 == 5) { // variable
                        int operand = ls.readInt();
                        System.out.println("PUSH operand: " + operand);
                        break;
                    } else if (type1 != 6) // string
                        throw new IllegalStateException("Unknown PUSH type1: " + type1);

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
                case 0x84: { // PUSH Immediate
                    System.out.println("Immediate value: " + extra);
                    stack.push(new Short(extra));
                    break;
                }
                case 0x45: { // POP
                    if (type1 != 5) throw new IllegalStateException("Unknown POP type1: " + type1);
                    if (type2 != 2) throw new IllegalStateException("Unknown POP type2: " + type2);

                    int operand = ls.readInt();
                    System.out.println("operand: " +  operand);
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

    private void remapVariableReferences() throws IOException {
        // count how many variables have an address
        int variablesWithAddressesCount = 0;
        int allocatedBuiltinVariablesCount = 0;
        int allocatedRegularVariablesCount = 0;
        for (int i = 0; i < vari.variables.length; i++) {
            Variable variable = vari.variables[i];
            if (variable.firstAddress != -1) {
                variablesWithAddressesCount++;
                if (variable.variableId == -6)
                    allocatedBuiltinVariablesCount++;
                else
                    allocatedRegularVariablesCount++;
            }
        }

        if (variablesWithAddressesCount < 1)
            return;

        byte[] patchingCode = code.codes[0];
        ByteArrayInputStream bais = new ByteArrayInputStream(patchingCode);
        LittleStream ls = new LittleStream(bais);

        Variable[] variablesWithAddresses = new Variable[variablesWithAddressesCount];
        int variableWithAddressIndex = 0;
        for (int i = 0; i < vari.variables.length; i++) {
            Variable variable = vari.variables[i];
            if (variable.firstAddress != -1) {
                variablesWithAddresses[variableWithAddressIndex] = variable;
                variableWithAddressIndex++;
            }
        }

        int allocatedBuiltinVariableIndex = 0;
        Variable[] allocatedBuiltinVariables = new Variable[allocatedBuiltinVariablesCount];
        int allocatedRegularVariableIndex = 0;
        Variable[] allocatedRegularVariables = new Variable[allocatedRegularVariablesCount];

        for (int i = 0; i < variablesWithAddresses.length; i++) {
            Variable variable = variablesWithAddresses[i];
            /* "Here's the thing:
             *  For builtin variables, we store it sequentially, starting from 0
             *  For regular variables, we store it sequentially, starting from REGULAR_VARIABLES_BASE
             *  This way we can differentiate directly on the operand itself, and we can store the handlers densely"
             */
            int variableHandlerId;
            if (variable.variableId == -6) {
                int index = allocatedBuiltinVariableIndex;
                allocatedBuiltinVariables[allocatedBuiltinVariableIndex] = variable;
                variableHandlerId = index;
                allocatedBuiltinVariableIndex++;
            } else {
                int index = allocatedRegularVariableIndex;
                allocatedRegularVariables[allocatedRegularVariableIndex] = variable;
                variableHandlerId = index + 100000;
                allocatedRegularVariableIndex++;
            }

            // "This points to the INSTRUCTION address, NOT the operand address, which is why we do +4"
            int operandAddress = (variable.firstAddress - code.postAddressesPosition) +  4;

            for (int j = 0; j < variable.occurrenceCount; j++) {
                // goto operand
                bais.reset();
                bais.skip(operandAddress);
                // read int
                int operand = ls.readInt();
                // delta
                int delta = operand & 0x0FFFFFFF;
                // overwrite where the read int originally was
                // I don't want to bother using OutputStream lol
                int output = (operand & 0xF0000000) | (variableHandlerId & 0x0FFFFFFF);
                patchingCode[operandAddress]        = (byte)(output & 0xFF);
                patchingCode[operandAddress + 1]    = (byte)((output >> 8) & 0xFF);
                patchingCode[operandAddress + 2]    = (byte)((output >> 16) & 0xFF);
                patchingCode[operandAddress + 3]    = (byte)((output >> 24) & 0xFF);
                // operandAddress += delta

                bais.reset();
                bais.skip(operandAddress);
                System.out.println("patched with " + ls.readInt() + " which should be " + output);

                operandAddress += delta;
            }
        }
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

/*************************************************************************
 *  Compilation:  javac-introcs TOY.java
 *  Execution:    java-introcs TOY [--verbose] filename.toy 
 *  Dependencies: StdIn.java In.java
 *
 *  We use variables of type int to store the TOY registers, main
 *  memory, and program counter even though in TOY these quantities
 *  are 16 and 8 bit integers. Java does not have an 8-bit unsigned
 *  type. The type short in Java does represent 16-bit 2's complement
 *  integers, but using it requires alot of casting. Instead, we are
 *  careful to treat all of the variable as if they were the appropriate
 *  type so that the behavior truly models the TOY machine.
 *
 *  % more multiply.toy
 *  10: 8AFF   read R[A]
 *  11: 8BFF   read R[B]
 *  12: 7C00   R[C] <- 0000
 *  13: 7101   R[1] <- 0001
 *  14: CA18   if (R[A] == 0) goto 18
 *  15: 1CCB   R[C] <- R[C] + R[B]
 *  16: 2AA1   R[A] <- R[A] - R[1]
 *  17: C014   goto 14
 *  18: 9CFF   write R[C]
 *  19: 0000   halt                        
 *
 *  % java-introcs TOY multiply.toy
 *  0002
 *  0004
 *  0008
 *
 *  % java-introcs TOY --verbose multiply.toy
 *  [core dump]
 *  0002
 *  0004
 *  0008
 *  [core dump]
 *
 *************************************************************************/

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class TOY { 
    private int pc;                     // program counter
    private int[] reg = new int[16];    // 16 registers
    private int[] mem = new int[256];   // 256 main memory locations

    // create a new TOY VM and load with program from specified file
    public TOY(String filename) {
        this(filename, 0x10);
    }

    public TOY(String filename, int pc) {
        this.pc = pc;
        In in = new In(filename);

       /****************************************************************
        *  Read in memory location and instruction.         
        *  A valid input line consists of 2 hex digits followed by a 
        *  colon, followed by any number of spaces, followed by 4
        *  hex digits. The rest of the line is ignored.
        ****************************************************************/
        String regexp = "^([0-9A-Fa-f]{2}):[ \t]*([0-9A-Fa-f]{4})";
        Pattern pattern = Pattern.compile(regexp);
        while (in.hasNextLine()) {
            String line = in.readLine();
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                int addr = fromHex(matcher.group(1));
                int inst = fromHex(matcher.group(2));
                mem[addr] = inst;
            }
        }
    }

    // return a 4-digit hex string corresponding to 16-bit integer n
    public static String toHex(int n) {
        return String.format("%04X", n & 0xFFFF);
    }

    // return a 16-bit integer corresponding to the 4-digit hex string s
    public static int fromHex(String s) {
        return Integer.parseInt(s, 16) & 0xFFFF;
    }

    // write to an array of hex integers, 8 per line to standard output
    public static void show(int[] a) {
        for (int i = 0; i < a.length; i++) {
            StdOut.print(toHex(a[i]) + " ");
            if (i % 8 == 7) StdOut.println();
        }
    }


    // print core dump of TOY to standard output
    public void dump() {
        StdOut.println("PC:");
        StdOut.printf("%02X\n", pc);
        StdOut.println();
        StdOut.println("Registers:");
        show(reg);
        StdOut.println();
        StdOut.println("Main memory:");
        show(mem);
        StdOut.println();
    }


    public void run() {

        while (true) {

            // Fetch and parse
            int inst = mem[pc++];            // fetch next instruction
            int op   = (inst >> 12) &  15;   // get opcode (bits 12-15)
            int d    = (inst >>  8) &  15;   // get dest   (bits  8-11)
            int s    = (inst >>  4) &  15;   // get s      (bits  4- 7)
            int t    = inst         &  15;   // get t      (bits  0- 3)
            int addr = inst         & 255;   // get addr   (bits  0- 7)

            // halt
            if (op == 0) break;

            // stdin 
            if ((addr == 255 && op == 8) || (reg[t] == 255 && op == 10))
                mem[255] = fromHex(StdIn.readString());

            // Execute
            switch (op) {
                case  1: reg[d] = reg[s] +  reg[t];           break;    // add
                case  2: reg[d] = reg[s] -  reg[t];           break;    // subtract
                case  3: reg[d] = reg[s] &  reg[t];           break;    // bitwise and
                case  4: reg[d] = reg[s] ^  reg[t];           break;    // bitwise xor
                case  5: reg[d] = reg[s] << reg[t];           break;    // shift left
                case  6: reg[d] = (short) reg[s] >> reg[t];   break;    // shift right
                case  7: reg[d] = addr;                       break;    // load address
                case  8: reg[d] = mem[addr];                  break;    // load
                case  9: mem[addr] = reg[d];                  break;    // store
                case 10: reg[d] = mem[reg[t] & 255];          break;    // load indirect
                case 11: mem[reg[t] & 255] = reg[d];          break;    // store indirect
                case 12: if ((short) reg[d] == 0) pc = addr;  break;    // branch if zero
                case 13: if ((short) reg[d] >  0) pc = addr;  break;    // branch if positive
                case 14: pc = reg[d];                         break;    // jump indirect
                case 15: reg[d] = pc; pc = addr;              break;    // jump and link
            }

            // stdout
            if ((addr == 255 && op == 9) || (reg[t] == 255 && op == 11))
                StdOut.println(toHex(mem[255]));

            reg[0] = 0;                // ensure reg[0] is always 0
            reg[d] = reg[d] & 0xFFFF;  // don't let reg[d] overflow a 16-bit integer
            pc = pc & 0xFF;            // don't let pc overflow an 8-bit integer

        }
    }


    // run the TOY simulator with specified file
    public static void main(String[] args) { 

        // -v or --verbose is an optional first command-line argument
        boolean isVerbose = false;
        if (args.length > 0 && (args[0].equals("-v") || args[0].equals("--verbose"))) {
            isVerbose = true;
        }

        // the filename is the next command-line argument
        String filename = null;
        if (!isVerbose && args.length > 0) filename = args[0];
        if ( isVerbose && args.length > 1) filename = args[1];

        // the initial value of the PC is an optional last command-line argument
        int pc = 0x10;
        if (!isVerbose && args.length > 1) pc = fromHex(args[1]);
        if ( isVerbose && args.length > 2) pc = fromHex(args[2]);

        // no command-line arguments
        if (args.length == 0) {
            System.err.println("TOY:   invalid command-line options");
            System.err.println("usage: java-introcs TOY [--verbose] filename.toy [pc]");
            return;
        }

        TOY toy = new TOY(filename, pc);

        if (isVerbose) {
            StdOut.println("Core Dump of TOY Before Executing");
            StdOut.println("---------------------------------------");
            toy.dump();

            StdOut.println("Terminal");
            StdOut.println("---------------------------------------");
        }

        toy.run();

        if (isVerbose) {
            StdOut.println();
            StdOut.println("Core Dump of TOY After Executing");
            StdOut.println("---------------------------------------");
            toy.dump();

        }

    }
}

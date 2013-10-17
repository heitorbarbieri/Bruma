package bruma.tools;

import bruma.BrumaException;
import bruma.master.Field;
import bruma.master.Master;
import bruma.master.MasterFactory;
import bruma.master.Record;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

/**
 *
 * @author Heitor Barbieri
 * @date 20120601
 */
public class InvisibleChars {
    public static final int[] DEFAULT_VISIBLE_CHARS = {
    32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50,
    51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69,
    70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88,
    89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105,
    106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120,
    121, 122, 123, 124, 125, 126, 127, 131, 133, 135, 137, 145, 146, 147, 148,
    149, 150, 151, 152, 159, 160, 161, 162, 163, 165, 167, 168, 169, 170, 171,
    172, 173, 174, 176, 177, 178, 179, 180, 181, 182, 184, 185, 186, 187, 188,
    189, 190, 191, 192, 193, 194, 195, 196, 197, 198, 199, 200, 201, 202, 203,
    204, 205, 206, 207, 209, 210, 211, 212, 213, 214, 215, 217, 218, 219, 220,
    221, 223, 224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236,
    237, 238, 239, 241, 242, 243, 244, 245, 246, 247, 249, 250, 251, 252, 253,
    255};
    
    private static void usage() {
        System.err.println(
                   "usage: InvisibleChars <dbName> [<visibleCharListFile>]");
        System.exit(0);
    }   
    
    public static void main(final String args[]) throws BrumaException, 
                                                        IOException {
        if (args.length < 1) {
            usage();
        }
        
        final Set<Character> visible;
        
        if (args.length > 1) {
            visible = loadTable(new File(args[1]));
        } else {
            visible = loadTable(DEFAULT_VISIBLE_CHARS);
        }
        final Master mst = MasterFactory.getInstance(args[0]).open();
        
        for (Record rec : mst) {
            if (rec.isActive()) {
                final Set<Character> invisible = check(rec, visible);
                
                if ((invisible != null) && !invisible.isEmpty()) {
                    System.out.print("mfn:" + rec.getMfn() + " {");
                    for (Character ch : invisible) {
                        System.out.print(" " + (int)ch + "[" + ch + "]");
                    }
                    System.out.println(" }");
                }
            }
        }
    }
    
    private static Set<Character> loadTable(final File file) 
                                                            throws IOException {
        if (file == null) {
            throw new IllegalArgumentException();
        }
        final HashSet<Character> visible = new HashSet<Character> ();
        final Scanner scan = new Scanner(file);
        int value;

        visible.clear();
        scan.useDelimiter("[\\s+\\,;]");
        while (scan.hasNextInt()) {
            value = scan.nextInt();
            if (value > Character.MAX_VALUE) {
                throw new IllegalArgumentException(Integer.toString(value));
            }
            visible.add((char)value);
        }
        scan.close();
        
        return visible;
    }
    
    private static Set<Character> loadTable(final int[] in) {
        assert in != null;

        final HashSet<Character> visible = new HashSet<Character> ();
        int value;

        for (int index = 0; index < in.length; index++) {
           value = in[index];

           if (value > Character.MAX_VALUE) {
                throw new IllegalArgumentException(Integer.toString(value));
            }
            visible.add((char) value);
        }
        
        return visible;
    }
    
    private static Set<Character> check(final Record rec,
                                        final Set<Character> visible) 
                                                         throws BrumaException {
        assert rec != null;
        
        Set<Character> invisible = null;
        
        for (Field fld : rec) {
            final String content = fld.getContent();            
            final int len = content.length();            
            
            for (int index = 0; index < len; index++) {
                final char val = content.charAt(index);
                if (!visible.contains(val)) {
                    if (invisible == null) {
                        invisible = new HashSet<Character> ();
                    }
                    invisible.add(val);
                }
            }
        }
        return invisible;
    }
}

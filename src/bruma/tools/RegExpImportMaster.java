/*=========================================================================

    Copyright © 2012 BIREME/PAHO/WHO

    This file is part of Bruma.

    Bruma is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as 
    published by the Free Software Foundation, either version 3 of 
    the License, or (at your option) any later version.

    Bruma is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public 
    License along with Bruma. If not, see <http://www.gnu.org/licenses/>.

=========================================================================*/

package bruma.tools;

import bruma.BrumaException;
import bruma.master.Master;
import bruma.master.MasterFactory;
import bruma.master.Record;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 *
 * @author Heitor Barbieri
 * @date 31052012
 */
public class RegExpImportMaster {
    private static void usage() {
        System.err.println("usage: RegExpImportMaster -in={stdin|<filename>}"
         + " -recDelim=<regexp> {-fieldDelim|-fieldPattern}=<regexp>\n"
         + " [{-occ|-nocc}=<num>] -out=<toDbname> [-outEncoding=<encoding>]\n" 
         + " [--isFFI]");
        System.exit(1);
    }

    public static void main(final String[] args) throws BrumaException,
                                                        IOException {
        if (args.length < 4) {
            usage();
        }

        String in = null;
        String recDelim = null;
        String fieldDelim = null;
        String fieldPattern = null;
        int occ = 1;
        int nocc = Integer.MAX_VALUE;
        String out = null;
        String outEncoding = Master.DEFAULT_ENCODING;
        boolean isFFI = false;
        
        for (int idx = 0; idx < args.length; idx++) {
            if (args[idx].startsWith("-in=")) {
                in = args[idx].substring(4);
            } else if (args[idx].startsWith("-recDelim=")) {
                recDelim = args[idx].substring(10);
            } else if (args[idx].startsWith("-fieldDelim=")) {
                fieldDelim = args[idx].substring(12);
            } else if (args[idx].startsWith("-fieldPattern=")) {
                fieldPattern = args[idx].substring(14);
            } else if (args[idx].startsWith("-occ=")) {
                occ = Integer.parseInt(args[idx].substring(5));
                nocc = 0;
            } else if (args[idx].startsWith("-nocc=")) {
                nocc = Integer.parseInt(args[idx].substring(6));
            } else if (args[idx].startsWith("-out=")) {
                out = args[idx].substring(5);
            } else if (args[idx].startsWith("-outEncoding=")) {
                outEncoding = args[idx].substring(13);
            } else if (args[idx].equals("--isFFI")) {
                isFFI = true;
            } else {
                usage();
            }
        }
        final Scanner scanRec;        
        final Master to = (Master)MasterFactory.getInstance(out)
                                               .setEncoding(outEncoding)
                                               .setFFI(isFFI)
                                               .forceCreate();
        final Record rec = to.newRecord();
        final Pattern pat = Pattern.compile(
                              (fieldDelim == null) ? fieldPattern : fieldDelim);

        if (in.equals("stdin")) {
            scanRec = new Scanner(System.in);
        } else {
            scanRec = new Scanner(new File(in));
        }
        scanRec.useDelimiter(recDelim);
        
        while (scanRec.hasNext()) {
            final Scanner scanFld = new Scanner(scanRec.next());
            int curOcc = 0;
            String content;
            
            rec.changeToNew();
            if (fieldDelim == null) {
                while (scanFld.hasNext(pat)) {
                    content = scanFld.next();
                    if (nocc == 0) {
                        if (++curOcc == occ) {
                            rec.addField(1, content);
                            break;
                        }
                    } else if (++curOcc <= nocc) {
                        rec.addField(1, content);                        
                    }
                }                
            } else {
                scanFld.useDelimiter(pat);
                while (scanFld.hasNext()) {
                    content = scanFld.next();
                    if (nocc == 0) {
                        if (++curOcc == occ) {
                            rec.addField(1, content);
                            break;
                        }
                    } else if (++curOcc <= nocc) {
                        rec.addField(1, content);                        
                    }
                }
            }
            to.writeRecord(rec);
        }
        
        scanRec.close();
        to.close();
    }
}

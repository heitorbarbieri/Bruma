/*=========================================================================

    Copyright © 2011 BIREME/PAHO/WHO

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

package bruma.utils;

import bruma.BrumaException;
import bruma.master.Field;
import bruma.master.Master;
import bruma.master.MasterFactory;
import bruma.master.Record;
import static bruma.utils.ISO8859OrIBM850.values_850;
import static bruma.utils.ISO8859OrIBM850.values_8859_1;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Heitor Barbieri
 * date 20141222
 */
public class ISO8859OrIBM850Report {
    final Set<Integer> set_88591;
    final Set<Integer> set_850;
    
    public ISO8859OrIBM850Report() {
        set_88591 = new HashSet<Integer>();
        set_850 = new HashSet<Integer>();
        
        for (int value : values_8859_1) {
            set_88591.add(value);
        }
        for (int value : values_850) {
            set_850.add(value);
        }
    }
    
    public void report(final String master,
                       final List<Integer> tags,
                       final int from,
                       final int to) throws BrumaException {
        if (master == null) {
            throw new NullPointerException("master");
        }
        if (from <= 0) {
            throw new IllegalArgumentException("from[" + from + "] <= 0");
        }
        if (to < from) {
            throw new IllegalArgumentException("to[" + to + "] < from[" + from
                                                                         + "]");
        }
        
        final Master mst = MasterFactory.getInstance(master)
                                        .setEncoding(Master.DEFAULT_ENCODING)
                                        .open();
        final int last = mst.getControlRecord().getNxtmfn() - 1;        
        if (from > last) {
            throw new IllegalArgumentException("from[" + from + "] > last[" 
                                                                  + last + "]");
        }
        final int to2 = Math.min(to, last);
        
        for (int mfn = from; mfn <= to2; mfn++) {
            reportRecord(mst, mfn, tags);
        }
        
        mst.close();
    }
    
    public void reportRecord(final Master mst,
                             final int mfn,
                             final List<Integer> tags) throws BrumaException {
        if (mst == null) {
            throw new NullPointerException("mst");
        }
        if (mfn <= 0) {
            throw new IllegalArgumentException("mfn[" + mfn + "] <= 0");
        }
        final Record rec = mst.getRecord(mfn);
        if (rec.isActive()) {
            System.out.println("\n--------------------------- " + mfn +
                                                " ---------------------------");
            reportFields(rec, tags);
        }
    }
    
    public void reportFields(final Record rec,
                             final List<Integer> tags) {
        if (rec == null) {
            throw new NullPointerException("rec");
        }
        for (Field fld : rec) {
            final int id = fld.getId();
            if ((tags == null) || tags.contains(id)) {
                System.out.println(rec.getMfn() + " " + id + ": " 
                                             + guessEncoding(fld.getContent()));
            }               
        }
    }
    
    public String guessEncoding(final String str) {
        if (str == null) {
            throw new NullPointerException("str");
        }
        final int len = str.length();
        int count_8859_1 = 0;
        int count_850 = 0;
        
        for (int pos = 0; pos < len; pos++) {
            final int val = str.charAt(pos);
            if (set_88591.contains(val)) {
                count_8859_1++;
            } else if (set_850.contains(val)) {
                count_850++;
            }
        }
        return (count_8859_1 < count_850) ? "ISO_8859-1" :
               (count_8859_1 == count_850) ? "???" : "IBM-850";                
    }
    
    private static void usage() {
        System.err.println("usage: ISO8859OrIBM850Report <master> " +
                "[-fields=<fld1>,<fld2>,...,<fldn>] [-from=<num>] [-to=<num>]");
        System.exit(1);
    }
    
    public static void main(final String[] args) throws BrumaException {
        if (args.length < 1) {
            usage();
        }
        List<Integer> tags = null;
        int from = 1;
        int to = Integer.MAX_VALUE;
        
        for (int idx = 1; idx < args.length; idx++) {
            if (args[idx].startsWith("-fields=")) {
                final String[] split = args[idx].substring(8).split(" *, *");
                tags = new ArrayList<Integer>();
                for (String element : split) {
                    tags.add(Integer.parseInt(element));
                }
            } else if (args[idx].startsWith("-from=")) {
                from = Integer.parseInt(args[idx].substring(6));
            } else if (args[idx].startsWith("-to=")) {
                to = Integer.parseInt(args[idx].substring(4));
            } else {
                usage();
            }
        }
        new ISO8859OrIBM850Report().report(args[0], tags, from, to);
    }      
}

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
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Heitor Barbieri
 */
public class ISO8859OrIBM850 {
    public static final int[] values_8859_1 = {
        191, 192, 193, 194, 195, 201, 202, 204, 205, 206, 213, 218, 219, 221, 
        224, 225, 227, 231, 232, 233, 234, 237, 238, 240, 241, 242, 243, 244, 
        245, 250, 251, 253 };

    public static final int[] values_850 = {
        128, 129, 130, 131, 133, 135, 136, 137, 138, 140, 141, 144, 147, 148, 
        149, 150, 151, 160, 161, 162, 163, 164, 165, 181, 182, 183, 198, 208, 
        215, 222, 229 };

    public static final int DIFF_RANGE = 20;
    public static final int SMART_MAX_CHECKED = 2000;

    int count_8859_1;
    int count_850;
    int count_rec;
    final boolean smart;
    final String mstName;
    final Set<Integer> set_88591;
    final Set<Integer> set_850;

    public ISO8859OrIBM850(final String mstName,
                           final boolean smart) throws BrumaException {
        if (mstName == null) {
            throw new BrumaException("null master name");
        }
        count_8859_1 = 0;
        count_850 = 0;
        count_rec = 0;
        set_88591 = new HashSet<Integer>();
        set_850 = new HashSet<Integer>();
        this.mstName = mstName;
        this.smart = smart;

        for (int value : values_8859_1) {
            set_88591.add(value);
        }
        for (int value : values_850) {
            set_850.add(value);
        }
    }
    
    public int get88591Count() {
        return count_8859_1;
    }

    public int get850Count() {
        return count_850;
    }   

    public String guessEncoding() throws BrumaException {
        final Master mst = MasterFactory.getInstance(mstName)
                                        .setEncoding(Master.DEFAULT_ENCODING)
                                        .open();
        Charset encoding;

        count_rec = 0;

        for (Record rec : mst) {
            if (rec.getStatus() == Record.Status.ACTIVE) {
                if (smart) {
                    count_rec++;
                }
                for (Field fld : rec) {
                    addText(fld.getContent());
                }
            }
            if (smart && canGuess()) {
                break;
            }
        }

        mst.close();

        encoding = guessResult();

        return (encoding == null ? null : encoding.displayName());
    }

    private boolean canGuess() {
        return ((count_rec > SMART_MAX_CHECKED) ||
                (Math.abs(count_8859_1 - count_850) > DIFF_RANGE));
    }
    
    private Charset guessResult() {
        Charset ret = null;

        if (canGuess()) {
            if (count_8859_1 < count_850) {
                ret = Charset.forName("IBM850");
            } else {
                ret = Charset.forName("ISO8859-1");
            }
        }

        return ret;
    }

    private void addText(final String str) {
        if (str == null) {
            throw new NullPointerException();
        }
        final int len = str.length();
        int val;

        for (int pos = 0; pos < len; pos++) {
            val = str.charAt(pos);
            if (set_88591.contains(val)) {
                count_8859_1++;
            } else if (set_850.contains(val)) {
                count_850++;
            }
        }
    }
    
    private static void usage() {
        System.err.println("usage: Iso8859Or850 <dbname> [--smart]");
        System.exit(1);
    }

    public static void main(final String[] args) throws BrumaException {
        if (args.length < 1) {
            usage();
        }

        final boolean smart = ((args.length > 1) && (args[1].equals("--smart")));
        final ISO8859OrIBM850 what = new ISO8859OrIBM850(args[0], smart);

        System.out.println("Possible encoding: " + what.guessEncoding());
    }
}

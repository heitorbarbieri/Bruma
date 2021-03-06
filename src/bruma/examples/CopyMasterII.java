
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

package bruma.examples;

import bruma.BrumaException;
import bruma.master.Master;
import bruma.master.MasterFactory;
import bruma.master.Record;
import java.nio.ByteOrder;
import java.util.GregorianCalendar;

/**
 * Creates a new Isis database with records from another one.
 *
 * <p><hr><blockquote><pre>
 *
 *    private static void usage() {
 *       System.err.println("usage: CopyMasterII &lt;dbFrom&gt; &lt;dbTo&gt; \n"
 *             + "            [--from=&lt;mfn&gt;]\n"
 *             + "            [--to=&lt;mfn&gt;]\n"
 *             + "            [--fromEncoding=&lt;charSet&gt;]\n"
 *             + "            [--toIsFFI]\n"
 *             + "            [{--toDbLinux|--toDbWin}]\n"
 *             + "            [--toEncoding=&lt;charSet&gt;]\n"
 *             + "            [--toMaxGigaSize=&lt;n&gt;]\n"
 *             + "            [--tell=&lt;n&gt;]\n"
 *             + "            [--adjustMfn]");
 *       System.exit(1);
 *    }
 * 
 *    public static void main(final String[] args) throws BrumaException {
 *        if (args.length < 2) {
 *            usage();
 *        }
 *        final long itime = (new GregorianCalendar()).getTimeInMillis();
 *        final long ctime;
 *        final Master fromMf;
 *        final Master toMf;
 *        final boolean toAlignment;
 *        final int toAlignmentVal;
 *        final boolean swapBytes;
 *        int from = 1;
 *        int to = 0;
 *        String fromEncoding = Master.DEFAULT_ENCODING;
 *        boolean toIsFFI = false;
 *        boolean toDbLinux = false;
 *        boolean toDbWin = false;
 *        String toEncoding = null;
 *        int toMaxGigaSize = -1;
 *        int tell = Integer.MAX_VALUE;
 *        boolean adjustMfn = false;
 *        int adjust = 0;
 *
 *        int cur = 0;
 *        Record rec;
 *
 *        for (int idx = 2; idx < args.length; idx++) {
 *            if (args[idx].startsWith("--from=")) {
 *                from = Integer.parseInt(args[idx].substring(7));
 *            } else if (args[idx].startsWith("--to=")) {
 *                to = Integer.parseInt(args[idx].substring(5));
 *            } else if (args[idx].startsWith("--fromEncoding=")) {
 *                fromEncoding = args[idx].substring(15);
 *            } else if (args[idx].equals("--toIsFFI")) {
 *                toIsFFI = true;
 *            } else if (args[idx].equals("--toDbLinux")) {
 *                toDbLinux = true;
 *            } else if (args[idx].equals("--toDbWin")) {
 *                toDbWin = true;
 *            } else if (args[idx].startsWith("--toEncoding=")) {
 *                toEncoding = args[idx].substring(13);
 *            } else if (args[idx].startsWith("--toMaxGigaSize=")) {
 *                toMaxGigaSize = Integer.parseInt(args[idx].substring(16));
 *            } else if (args[idx].startsWith("--tell=")) {
 *                tell = Integer.parseInt(args[idx].substring(7));
 *            } else if (args[idx].equals("--adjustMfn")) {
 *                adjustMfn = true;
 *            } else {
 *                usage();
 *            }
 *        }
*
*        toAlignment = toDbLinux ||
*         (toDbWin ? false : !System.getProperty("os.name").startsWith("Win"));
*        toAlignmentVal = toAlignment ? 2 : 0;
*        swapBytes = (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN);
*
*        fromMf = MasterFactory.getInstance(args[0])
*                              .setEncoding(fromEncoding)
*                              .getMaster().
*                              open();
*
*        if (toMaxGigaSize == -1) {
*            toMaxGigaSize = fromMf.getGigaSize();
*        }
*        if (to == 0) {
*            to = fromMf.getControlRecord().getNxtmfn() - 1;
*        }
*        if (adjustMfn) {
*            adjust = from - 1;
*        }
*        if (toEncoding == null) {
*            toEncoding = fromMf.getEncoding();
*        }
*
*        toMf = MasterFactory.getInstance(args[1])
*                            .setFFI(toIsFFI)
*                            .setEncoding(toEncoding)
*                            .setDataAlignment(toAlignmentVal)
*                            .setSwapped(swapBytes)
*                            .setMaxGigaSize(toMaxGigaSize)
*                            .create();
*
*        for (int mfn = from; mfn <= to; mfn++) {
*            rec = fromMf.getRecord(mfn);
*            rec.setMfn(mfn - adjust);
*            toMf.writeRecord(rec);
*            if (++cur % tell == 0) {
*                System.out.println("+++" + cur);
*            }
*        }
*
*        fromMf.close();
*        toMf.close();
*
*        ctime = (new GregorianCalendar()).getTimeInMillis();
*        System.out.println("Elapsed time : " + ((ctime - itime)/1000) + "s");
*    }
* </pre></blockquote></hr></p>
 * @author Heitor Barbieri
 */
public class CopyMasterII {
    private CopyMasterII() {
    }

    private static void usage() {
        System.err.println("usage: CopyMasterII <dbFrom> <dbTo> \n"
              + "            [--from=<mfn>]\n"
              + "            [--to=<mfn>]\n"
              + "            [--fromEncoding=<charSet>]\n"
              + "            [--toIsFFI]\n"
              + "            [{--toDbLinux|--toDbWin}]\n"
              + "            [--toEncoding=<charSet>]\n"
              + "            [--toMaxGigaSize=<n>]\n"
              + "            [--tell=<n>]\n"
              + "            [--adjustMfn]");
        System.exit(1);
    }

    /**
     * @param args 
     * @throws BrumaException
     */
    public static void main(final String[] args) throws BrumaException {
        if (args.length < 2) {
            usage();
        }
        final long itime = (new GregorianCalendar()).getTimeInMillis();
        final long ctime;
        final Master fromMf;
        final Master toMf;
        final boolean toAlignment;
        final int toAlignmentVal;
        final boolean swapBytes;
        int from = 1;
        int to = 0;
        String fromEncoding = Master.DEFAULT_ENCODING;
        boolean toIsFFI = false;
        boolean toDbLinux = false;
        boolean toDbWin = false;
        String toEncoding = null;
        int toMaxGigaSize = -1;
        int tell = Integer.MAX_VALUE;
        boolean adjustMfn = false;
        int adjust = 0;

        int cur = 0;
        Record rec;

        for (int idx = 2; idx < args.length; idx++) {
            if (args[idx].startsWith("--from=")) {
                from = Integer.parseInt(args[idx].substring(7));
            } else if (args[idx].startsWith("--to=")) {
                to = Integer.parseInt(args[idx].substring(5));
            } else if (args[idx].startsWith("--fromEncoding=")) {
                fromEncoding = args[idx].substring(15);
            } else if (args[idx].equals("--toIsFFI")) {
                toIsFFI = true;
            } else if (args[idx].equals("--toDbLinux")) {
                toDbLinux = true;
            } else if (args[idx].equals("--toDbWin")) {
                toDbWin = true;
            } else if (args[idx].startsWith("--toEncoding=")) {
                toEncoding = args[idx].substring(13);
            } else if (args[idx].startsWith("--toMaxGigaSize=")) {
                toMaxGigaSize = Integer.parseInt(args[idx].substring(16));
            } else if (args[idx].startsWith("--tell=")) {
                tell = Integer.parseInt(args[idx].substring(7));
            } else if (args[idx].equals("--adjustMfn")) {
                adjustMfn = true;
            } else {
                usage();
            }
        }

        toAlignment = toDbLinux ||
         (toDbWin ? false : !System.getProperty("os.name").startsWith("Win"));
        toAlignmentVal = toAlignment ? 2 : 0;
        swapBytes = (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN);

        fromMf = MasterFactory.getInstance(args[0])
                              .setEncoding(fromEncoding)
                              .open();

        if (toMaxGigaSize == -1) {
            toMaxGigaSize = fromMf.getGigaSize();
        }
        if (to == 0) {
            to = fromMf.getControlRecord().getNxtmfn() - 1;
        }
        if (adjustMfn) {
            adjust = from - 1;
        }
        if (toEncoding == null) {
            toEncoding = fromMf.getEncoding();
        }

        toMf = (Master)MasterFactory.getInstance(args[1])
                                    .setFFI(toIsFFI)
                                    .setEncoding(toEncoding)
                                    .setDataAlignment(toAlignmentVal)
                                    .setSwapped(swapBytes)
                                    .setMaxGigaSize(toMaxGigaSize)
                                    .create();
        for (int mfn = from; mfn <= to; mfn++) {
            rec = fromMf.getRecord(mfn);
            rec.setMfn(mfn - adjust);
            toMf.writeRecord(rec);
            if (++cur % tell == 0) {
                System.out.println("+++" + cur);
            }
        }

        fromMf.close();
        toMf.close();

        ctime = (new GregorianCalendar()).getTimeInMillis();
        System.out.println("Elapsed time : " + ((ctime - itime)/1000) + "s");
    }
}
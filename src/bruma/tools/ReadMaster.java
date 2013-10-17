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

package bruma.tools;

import bruma.BrumaException;
import bruma.master.Leader;
import bruma.master.Master;
import bruma.master.MasterFactory;
import bruma.master.Record;

/**
 *
 * @author Heitor Barbieri
 * @date 11/08/2008
 */
public class ReadMaster {
    private static void usage() {
        System.err.println("usage: ReadMaster <dbFrom> [--from=<mfn>] "
                                    + "[--to=<mfn>] [--encoding=<charSet>]"
                                    + "[--showLeader]");
        System.exit(1);
    }

    public static void main(final String[] args) throws BrumaException {
        if (args.length < 1) {
            usage();
        }
        int from = 1;
        int to = -1;
        String encoding = Master.DEFAULT_ENCODING;
        final Master mst;
        Record rec;
        Leader leader;
        boolean showLeader = false;
        Record.Status status;

        for (int idx = 1; idx < args.length; idx++) {
            if (args[idx].startsWith("--from=")) {
                from = Integer.parseInt(args[idx].substring(7));
            } else if (args[idx].startsWith("--to=")) {
                to = Integer.parseInt(args[idx].substring(5));
            } else if (args[idx].startsWith("--encoding=")) {
                encoding = args[idx].substring(11);
            } else if (args[idx].equals("--showLeader")) {
                showLeader = true;
            } else {
                usage();
            }
        }

        mst = MasterFactory.getInstance(args[0])
                           .setEncoding(encoding)
                           .open();

        if (to == -1) {
            to = mst.getControlRecord().getNxtmfn() - 1;
        }
        if (to < from) {
            throw new IllegalArgumentException("to[" + to + "] < from["
                                                                 + from + "]");
        }

        for (int mfn = from; mfn <= to; mfn++) {
            rec = mst.getRecord(mfn);
            status = rec.getStatus();

            System.out.println("--------------------------- " + mfn
             + ((status != Record.Status.ACTIVE) ? " <deleted> " : " ---------")
             + "-------------------------\n");

            if ((status == Record.Status.ACTIVE)
                    || (status == Record.Status.LOGDEL)) {
                if (showLeader) {
                    leader = rec.getLeader(mst.getEncoding(), mst.isFFI());
                     System.out.println("mfn=" + mfn + " status="
                             + leader.getStatus() + " len=" + leader.getMfrl());
                }
                System.out.println(rec);
            }
        }

        mst.close();
    }
}
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

/*
 * Esta classe muda o status dos apontadores do arquivo xrf para indicar que os
 * registros apontados sao novos (new).
 */

package bruma.tools;

import bruma.BrumaException;
import bruma.master.Master;
import bruma.master.MasterFactory;
import bruma.master.Record;
import bruma.master.XrfFile;
import bruma.master.XrfFile.XrfInfo;

/**
 *
 * @author Heitor Barbieri
 * @date  15/12/2008
 */

public class ChangeXrf2New {
    private ChangeXrf2New() {
    }

    public static void change(final String dbname,
                              final boolean isFFI) throws BrumaException {
        if (dbname == null) {
            throw new IllegalArgumentException();
        }

        final int next;
        final XrfFile xrf;
        final int tell;
        XrfInfo info;
        final Master mst = MasterFactory.getInstance(dbname)
                           .setFFI(isFFI)
                           .setInMemoryXrf(false)
                           .open();
        next = mst.getControlRecord().getNxtmfn();
        xrf = mst.getXrf();
        tell = 10000;

        for (int mfn = 1; mfn < next; mfn++) {
            if (mfn % tell == 0) {
            System.out.println("+++" + mfn);
            }
            info = xrf.readXrfInfo(mfn);
            if (info.getStatus() == Record.Status.ACTIVE) {
                if (info.getActStatus() != Record.ActiveStatus.NEW) {
                    info.setActStatus(Record.ActiveStatus.NEW);
                    info.setOffset(info.getOffset() + 1024);
                    xrf.writeXrfInfo(info);
                }
            }
        }

        mst.close();
    }

    private static void usage() {
        System.err.println("usage: ChangeXrf2New <dbname> [--isFFI]");
        System.exit(1);
    }

    public static void main(final String[] args) throws BrumaException {
        if (args.length < 1) {
            usage();
        }
        final boolean isFFI =
                             ((args.length > 1) && (args[1].equals("--isFFI")));

        change(args[0], isFFI);
    }
}

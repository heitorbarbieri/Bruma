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
import bruma.master.Master;
import bruma.master.MasterFactory;
import bruma.master.XrfFile;

/**
 *
 * @author Heitor Barbieri
 */
public class DumpXrf {
    private static void usage() {
        System.err.println("usage: DumpXrf <dbname>");
        System.exit(1);
    }

    public static void main(final String[] args) throws BrumaException {
        if (args.length != 1) {
            usage();
        }

        final Master mst = MasterFactory.getInstance(args[0])
                                        .setEncoding("ISO8859-1").open();
        final XrfFile xrf = mst.getXrf();
        final int nxtmfn = mst.getControlRecord().getNxtmfn();

        for (int mfn = 1; mfn < nxtmfn; mfn++) {
            System.out.println(xrf.readXrfInfo(mfn));
        }

        mst.close();
    }
}

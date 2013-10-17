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

/**
 * Dumps all active records from an Isis database.
* <p><hr><blockquote><pre>
*   private static void usage() {
*       System.err.println("usage: DumpDb &lt;dbname&gt; [&lt;encoding&gt;]");
*       System.exit(1);
*   }
*
*   public static void main(final String[] args) throws BrumaException {
*        if (args.length < 1) {
*            usage();
*        }
*        final String encoding = (args.length > 1) ? args[1]
*                                                  : Master.DEFAULT_ENCODING;
*        final Master mst = MasterFactory.getInstance(args[0])
*                                        .setEncoding(encoding)
*                                        .open();
*        for (Record rec : mst) {
*            if (rec.getStatus() == Record.Status.ACTIVE) {
*                System.out.println(rec);
*            }
*        }
*
*        mst.close();
*    }
* </pre></blockquote></hr></p>
 * 
 * @author Heitor Barbieri
 */
public class DumpDb {
    private static void usage() {
        System.err.println("usage: DumpDb <dbname> [<encoding>]");
        System.exit(1);
    }

    /**
     * @param args <dbname> [<encoding>]
     * @throws BrumaException
     */
    public static void main(final String[] args) throws BrumaException {
        if (args.length < 1) {
            usage();
        }
        final String encoding = Master.GUESS_ISO_IBM_ENCODING;/*(args.length > 1) ? args[1]
                                                  : Master.DEFAULT_ENCODING;*/
        final Master mst = MasterFactory.getInstance(args[0]) 
                                        .setEncoding(encoding)
                                        .open();
        boolean first = true;

        for (Record rec : mst) {
            if (rec.getStatus() == Record.Status.ACTIVE) {
                if (first) {
                    first = false;
                } else {
                    System.out.println(",");
                }
                System.out.print(rec.toJSON3(0));
            }
        }
        System.out.println();

        mst.close();
    }
}

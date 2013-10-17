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
 * Creates a new Isis database with records from another one.
 * <p><hr><blockquote><pre>
 * private static void usage() {
 *     System.err.println("usage: CopyMaster &lt;dbfrom&gt; &lt;encoding&gt; &lt;dbto&gt;");
 *     System.exit(1);
 * }
 *
 * public static void main(final String[] args) throws BrumaException {
 *      if (args.length != 3) {
 *          usage();
 *      }
 *
 *      final Master from = MasterFactory.getInstance(args[0])
 *                                       .setEncoding(args[1])
 *                                       .open();
 *      final Master to = MasterFactory.getInstance(args[2])
 *                                     .asAnotherMaster(from)
 *                                     .create();
 *
 *      for (Record rec : from) {
 *          to.writeRecord(rec);
 *      }
 *
 *      from.close();
 *      to.close();
 *  }
 * </pre></blockquote></hr></p>
 * @author Heitor Barbieri
 */
public class CopyMaster {
    private static void usage() {
        System.err.println("usage: CopyMaster <dbfrom> <encoding> <dbto>");
        System.exit(1);
    }

    /**
     * @param args
     * @throws BrumaException
     */

    public static void main(final String[] args) throws BrumaException {
        if (args.length != 3) {
            usage();
        }

        final Master from = MasterFactory.getInstance(args[0])
                                         .setEncoding(args[1])
                                         .open();
        final Master to = (Master)MasterFactory.getInstance(args[2])
                                               .asAnotherMaster(from)
                                               .create();

        for (Record rec : from) {
            to.writeRecord(rec);
        }

        from.close();
        to.close();
    }
}

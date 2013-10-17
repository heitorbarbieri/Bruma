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
import bruma.master.Field;
import bruma.master.Master;
import bruma.master.MasterFactory;
import bruma.master.Record;
import bruma.master.Subfield;

/**
 * Dumps all active records from an Isis database.
 *
 * <p><hr><blockquote><pre>
 *   private static void usage() {
 *       System.err.println("usage: DumpDbII &lt;dbname&gt; [&lt;encoding&gt;]");
 *       System.exit(1);
 *   }
 *
 *    public static void main(final String[] args) throws BrumaException {
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
 *                System.out.println("mfn=" + rec.getMfn());
 *                for (Field field : rec) {
 *                    System.out.println("tag=" + field.getId());
 *                    for (Subfield subfield : field) {
 *                        System.out.println("sub id=" + subfield.getId());
 *                        System.out.println("sub content="
 *                                                    + subfield.getContent());
 *                  }
 *              }
 *        }
 *
 *        mst.close();
 *    }
 * </pre></blockquote></hr></p>
 * 
 * @author Heitor Barbieri
 */
public class DumpDbII {
    private static void usage() {
        System.err.println("usage: DumpDbII <dbname> [<encoding>]");
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
        final String encoding = (args.length > 1) ? args[1]
                                                : Master.GUESS_ISO_IBM_ENCODING;
        final Master mst = MasterFactory.getInstance(args[0])
                                        .setEncoding(encoding)
                                        .open();
        for (Record rec : mst) {
            if (rec.getStatus() == Record.Status.ACTIVE) {
                System.out.println("mfn=" + rec.getMfn());
                for (Field field : rec) {
                    System.out.println("tag=" + field.getId());
                    for (Subfield subfield : field) {
                        System.out.println("sub id=" + subfield.getId());
                        System.out.println("sub content="
                                                      + subfield.getContent());
                    }
                }
                System.out.println();
            }
        }

        mst.close();
    }
}

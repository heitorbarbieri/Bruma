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
import bruma.iterator.AbstractRecordIterator;
import bruma.iterator.ISO2709RecordIterator;
import bruma.iterator.IdFileRecordIterator;
import bruma.iterator.IsisRecordIterator;
import bruma.master.MasterFactory;
import bruma.master.MasterInterface;
import bruma.master.Record;
import java.io.File;

/**
* Imports an Isis database from a Isis database or ISO2709 file or from an
* Id file to a memory master.
*
* <p><hr><blockquote><pre>
*    public static void main(final String[] args) throws BrumaException {
*        if (args.length != 3) {
*            usage();
*        }
*
*        AbstractRecordIterator iterator = null;
*
*        if (args[0].startsWith("isis=")) {
*            iterator = new IsisRecordIterator(args[0].substring(5), args[1]);
*        } else if (args[0].startsWith("iso=")) {
*            iterator = new ISO2709RecordIterator(args[0].substring(4), args[1]);
*        } else if (args[0].startsWith("id=")) {
*            iterator = new IdFileRecordIterator(
*                                      new File(args[0].substring(3)), args[1]);
*        } else {
*            usage();
*        }
*
*        final Master to = (Master)MasterFactory.getInstance(args[2])
*                                               .setEncoding(args[1])
*                                               .setInMemoryMst(true)
*                                               .create();
*
*        for (Record rec : iterator) {
*            to.writeRecord(rec);
*        }
*        iterator.close();
*        
*        for (Record rec : to) {
*            System.out.println(rec);
*        }
*
*    }
*
</pre></blockquote></hr></p>
*
* @author Heitor Barbieri
**/
public class ImportMasterII {
        private static void usage() {
        System.err.println(
        "usage: ImportMasterII {isis|iso|id}=<filename> <encoding> <toDbname>");
        System.exit(1);
    }

    /**
     *
     * @param args {isis|iso|id}=<filename> <encoding> <toDbname>
     * @throws BrumaException
     */
    public static void main(final String[] args) throws BrumaException {
        if (args.length != 3) {
            usage();
        }

        AbstractRecordIterator iterator = null;

        if (args[0].startsWith("isis=")) {
            iterator = new IsisRecordIterator(args[0].substring(5), args[1]);
        } else if (args[0].startsWith("iso=")) {
            iterator = new ISO2709RecordIterator(args[0].substring(4), args[1]);
        } else if (args[0].startsWith("id=")) {
            iterator = new IdFileRecordIterator(
                                       new File(args[0].substring(3)), args[1]);
        } else {
            usage();
        }

        final MasterInterface to = MasterFactory.getInstance(args[2])
                                                .setEncoding(args[1])
                                                .setInMemoryMst(true)
                                                .create();

        for (Record rec : iterator) {
            to.writeRecord(rec);
        }
        iterator.close();
        
        final MasterInterface to2 = MasterFactory.getInstance(args[2])
                                                .setEncoding(args[1])
                                                .setInMemoryMst(false)
                                                .create();
        for (Record rec : to2) {
            System.out.println(rec);
        }
    }
}

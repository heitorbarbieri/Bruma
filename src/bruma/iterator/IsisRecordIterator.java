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

package bruma.iterator;

import bruma.BrumaException;
import bruma.master.Master;
import bruma.master.MasterFactory;
import bruma.master.Record;
import bruma.master.Record.Status;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Heitor Barbieri
 * @date 04/04/2008
 */
public class IsisRecordIterator extends AbstractRecordIterator {
    private Master master;
    private int id;
    private int last;

    public IsisRecordIterator(final String dbName) {
        this(dbName, null);
    }

    public IsisRecordIterator(final String dbName,
                              final String encoding) {
        super();
        try {
            String encod = (encoding == null) ? Master.GUESS_ISO_IBM_ENCODING
                                              : encoding;
            id = 0;
            master = MasterFactory.getInstance(dbName)
                                  .setEncoding(encod)
                                  .open();
            last = master.getControlRecord().getNxtmfn() - 1;
            getNextRecord();
        } catch (BrumaException ex) {
            Logger.getLogger(IsisRecordIterator.class.getName()).
                                                   log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void close() {
        if (master != null) {
            try {
                master.close();
            } catch (BrumaException ex) {
                Logger.getLogger(IsisRecordIterator.class.getName()).
                                                    log(Level.SEVERE, null, ex);
            }
            master = null;
        }
    }

    public boolean isFFI() {
        return master.isFFI();
    }

    public int getGigaSize() {
        return master.getGigaSize();
    }

    public String getEncoding() {
        return master.getEncoding();
    }

    @Override
    public Record getNextRecord() {
        assert master != null;

        Record rec;

        record = null;

        try {
            while (++id <= last) {
                rec = master.getRecord(id);
                if (rec.getStatus() == Status.ACTIVE) {
                    record = rec;
                    break;
                }
            }
        } catch (BrumaException ex) {
            Logger.getLogger(IsisRecordIterator.class.getName()).
                                                    log(Level.SEVERE, null, ex);
        }

        return record;
    }

    private static void usage() {
        System.err.println("usage: IsisRecordIterator <dbname> [<encoding>]");
        System.exit(1);
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            usage();
        }

        final String encoding = (args.length) > 1 ? args[1] : null;
        final AbstractRecordIterator iterator =
                                     new IsisRecordIterator(args[0], encoding);

        for (Record rec : iterator) {
            System.out.println(rec);
        }

        iterator.close();
    }
}

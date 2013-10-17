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
import bruma.impexp.ISO2709I;
import bruma.master.Record;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Heitor Barbieri
 * @date 09/05/2008
 */
public class ISO2709RecordIterator extends AbstractRecordIterator {
    private ISO2709I iso;

    public ISO2709RecordIterator(final String isoName) {
        this(isoName, null);
    }

    public ISO2709RecordIterator(final String isoName,
                                 final String encoding){
        super();

        try {
            iso = new ISO2709I(isoName, encoding);
            getNextRecord();
        } catch (BrumaException ex) {
            Logger.getLogger(ISO2709RecordIterator.class.getName()).
                                                   log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void close() {
        try {
            if (iso != null) {
                iso.close();
                iso = null;
            }
        } catch (BrumaException ex) {
            Logger.getLogger(ISO2709RecordIterator.class.getName()).
                                                   log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public Record getNextRecord() {

        try {
            record = iso.readRecord();
        } catch (BrumaException ex) {
            Logger.getLogger(ISO2709RecordIterator.class.getName())
                                                 .log(Level.SEVERE, null, ex);
            record = null;
        }

        return record;
    }

    private static void usage() {
        System.err.println("usage: ISO2709RecordIterator <dbname>");
        System.exit(1);
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            usage();
        }

        AbstractRecordIterator iterator = new ISO2709RecordIterator(args[0]);

        for (Record rec : iterator) {
            System.out.println(rec);
        }

        iterator.close();
    }
}

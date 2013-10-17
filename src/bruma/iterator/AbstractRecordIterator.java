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

import bruma.master.Record;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 *
 * @author Heitor Barbieri
 * @date 04/04/2008
 */
public abstract class AbstractRecordIterator implements
                                    Iterable<Record>, Iterator<Record> {
    protected Record record;
    protected boolean first;

    protected AbstractRecordIterator() {
        record = null;
        first = true;
    }

    public abstract void close();
    protected abstract Record getNextRecord();

    @Override
    public boolean hasNext() {
        return (record != null);
    }

    @Override
    public Record next() {
        Record rec = record;

        if (!first && !hasNext()) {
            throw new NoSuchElementException();
        }

        record = getNextRecord();
        first = false;

        return rec;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<Record> iterator() {
        return this;
    }
}

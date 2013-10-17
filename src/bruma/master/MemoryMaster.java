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

package bruma.master;

import bruma.BrumaException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Heitor Barbieri
 * @date 20111215
 */
public class MemoryMaster implements MasterInterface {
    /**
     * Implements an iterator of the database records.
     */
    public class MstIterator implements Iterator<Record> {
        private final int lastMfn;
        private int curMfn;
        private Record rec;

        private MstIterator() throws BrumaException {
            lastMfn = getControlRecord().getNxtmfn() - 1;
            curMfn = 0;
        }
        @Override
        public boolean hasNext() {
            return (curMfn + 1) <= lastMfn;
        }
        @Override
        public Record next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            try {
                rec = getRecord(++curMfn);
            } catch (BrumaException zex) {
                rec = null;
                Logger.getGlobal().severe(zex.getMessage());
            }
            return rec;
        }
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private final List<Record> recs;
    private MasterPlatformInfo info;

    MemoryMaster(final MasterPlatformInfo mpi) {
        assert mpi != null;
        
        recs = new ArrayList<Record>();
        info = mpi;
    }
    
    MemoryMaster create() {
        recs.clear();
        
        return this;
    }
    
    /**
     * Closes frees all database used resources.
     * @throws BrumaException
     */
    @Override
    public void close() throws BrumaException {  
        recs.clear();
    }

    /**
     * Deletes Isis master files (mst and xrf)
     * @return true if all files were deleted, false otherwise.
     * @throws BrumaException
     */
    @Override
    public boolean delete() throws BrumaException {
        recs.clear();
        
        return true;
    }

    /**
     * Deletes an Isis record.
     * @param mfn - master file number of the record.
     * @throws org.bruma.BrumaException
     */
    @Override
    public void deleteRecord(final int mfn) throws BrumaException {
        final int last = recs.size();
        
        if ((mfn <= 0) || (mfn > last)) {
            throw new BrumaException("deleteRecord/mfn[" + mfn 
                                                 + "] is outside valid range.");
        }
        recs.get(mfn).setStatus(Record.Status.LOGDEL);        
    }

    /**
     * Forces the unlock of a Isis record.
     * @param mfn - record number to unlock
     * @throws org.bruma.BrumaException
     */
    @Override
    public void forceUnlockRecord(final int mfn) throws BrumaException {
        throw new UnsupportedOperationException();
    }

    /**
     * Reads the master control record.
     * @return the control record object
     * @throws org.bruma.BrumaException
     */
    @Override
    public Control getControlRecord() throws BrumaException {
        final Control ctl = new Control();
        
        ctl.setNxtmfn(recs.size() + 1);
        
        return ctl;
    }

    @Override
    public int getDataAlignment() {
        return info.getDataAlignment();
    }

    @Override
    /**
     * @return the database encoding.
     */
    public String getEncoding() {
        return info.getEncoding();
    }

    /**
     * @return the maximum database size (in gigabytes).
     * Returning zero means 512 megabytes.
     */
    @Override
    public int getGigaSize() {
        return info.getMaxGigaSize();
    }

    /**
     * Locks a database record, avoiding the write to it for anyone who doesnt
     * have the lock.
     * @param mfn
     * @param recLock
     * @return the locked record.
     * @throws org.bruma.BrumaException
     */
    @Override
    public Record getLockRecord(final int mfn, 
                                 final Lock.RecordLock[] recLock) 
                                                        throws BrumaException {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the name of the database.
     */
    @Override
    public String getMasterName() {
        return info.getMstName();
    }

    @Override
    public int getMaxRecSize() throws BrumaException {
        int max = 0;
        int recLen;

        for (Record rec : this) {
            if (rec.getStatus() == Record.Status.ACTIVE) {
                recLen = rec.getRecordLength(info.getEncoding(), info.isFfi());
                max = Math.max(max, recLen);
            }
        }

        return max;
    }

    /**
     * Reads a record from the database.
     * @param mfn record master file number
     * @return the readen record.
     * @throws org.bruma.BrumaException
     */
    @Override
    public Record getRecord(final int mfn) throws BrumaException {
        final int last = recs.size();
        
        if ((mfn <= 0) || (mfn > last)) {
            throw new BrumaException("getRecord/mfn[" + mfn 
                                                 + "] is outside valid range.");
        }
        Record rec = recs.get(mfn - 1);
        if (rec == null) {
            rec = new Record();
            rec.setMfn(mfn);
        }
        
        return rec;
    }

    /**
     * @return tells if the database was created with extended mode
     * (record size > 32 bytes).
     */
    @Override
    public boolean isFFI() {
        return info.isFfi();
    }
    
    /**
     * @return tells if the master is in memory.
     */
    @Override
    public boolean isInMemoryMst() {
        return info.isInMemoryMst();
    }

    /**
     * @return if the database operation is set to multi or monouser mode.
     */
    @Override
    public boolean isMultiuser() {
        return info.isMultiuser();
    }

    @Override
    public Iterator<Record> iterator() {
        Iterator<Record> iter = null;

        try {
            iter = new MstIterator();
        } catch (BrumaException zex) {
            iter = null;
        }

        return iter;

    }

    @Override
    public Lock.RecordLock lockRecord(final Record record) 
                                                         throws BrumaException {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a new empty record.
     * @return Isis record
     * @throws org.bruma.BrumaException
     */
    @Override
    public Record newRecord() throws BrumaException {
        return new Record();
    }

    @Override
    public List<Integer> regExpSearch(final String expression, 
                                       final int maxHits) 
                                                         throws BrumaException {
        if (expression == null) {
            throw new BrumaException("null regular expression");
        }
        if (maxHits < 1) {
            throw new BrumaException("maxHits <= 0");
        }
        final List<Integer> ret = new ArrayList<Integer>();
        final Matcher mat = Pattern.compile(expression).matcher("");
        int curHits = 0;

        for (Record rec : this) {
            if (rec.getStatus() == Record.Status.ACTIVE) {
                for (Field fld : rec) {
                    mat.reset(fld.getContent());
                    if (mat.find()) {
                        if (++curHits > maxHits) {
                            break;
                        }
                        ret.add(rec.getMfn());
                    }
                }
            }
        }

        return ret;        
    }

    /**
     * Resets the EWL and DEL flags, even if the caller doesnt have the unlock
     * right.
     * @throws org.bruma.BrumaException
     */
    @Override
    public void unlock() throws BrumaException {
        throw new UnsupportedOperationException();
    }

    /**
     * Forces the unlock of a Isis record.
     * @param recLock - the lock token
     * @throws org.bruma.BrumaException
     */
    @Override
    public void unlockRecord(final Lock.RecordLock recLock) 
                                                         throws BrumaException {
        throw new UnsupportedOperationException();
    }

    /**
     * Writes a record into a database.
     * @param record Isis record
     * @throws org.bruma.BrumaException
     * @return record master file number
     */
    @Override
    public int writeRecord(final Record record) throws BrumaException {
        if (record == null) throw new BrumaException("writeRecord/null record");
        
        final boolean isFFI = info.isFfi();
        final int size = 
                      record.getRecordLength(info.getEncoding(), isFFI);
        if (isFFI) {
            if (size > MasterInterface.MAXMFRL_POSSIBLE) {
                throw new BrumaException("writeRecord/record size[" + size 
                        + "] > " +  MasterInterface.MAXMFRL_POSSIBLE);
            }
        } else {
            if (size > MasterInterface.MAXMFRL_ISIS) {
                throw new BrumaException("writeRecord/record size[" + size 
                        + "] > " +  MasterInterface.MAXMFRL_ISIS);
            }
        }
        
        final int last = recs.size();
        int mfn = record.getMfn();
        
        if (mfn == 0) {
            mfn = last + 1;
            record.setMfn(mfn);            
        }
        
        for (int cur = last + 1; cur < mfn; cur++) {
            recs.add(new Record().setMfn(cur));
        }
        recs.add(mfn - 1, record);
        
        return mfn;
    }    
}

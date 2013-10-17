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

package bruma.master.lock;

/**
 *
 * @author Heitor Barbieri
 * @date 10/06/2010
 */
/*public class Lock {
    private static final int DEFAULT_DEEWL_TIMES = 3000;
    private static final int DEFAULT_RECORDL_TIMES = 3000;

    private class TwoLockVals {
        private int ewl = 0;
        private int del = 0;
    }

    public class EWLock {
        private boolean hasEWL;

        private EWLock() {
            hasEWL = true;
        }
        private boolean isUnlocked() {
            return !hasEWL;
        }
        private void reset() {
            hasEWL = false;
        }
    }

    public class RecordLock {
        private final int mfn;

        private RecordLock(final int mfn) {
            this.mfn = mfn;
        }
    }

    private final Master mst;
    private final String tmpDir;
    private final File lockFile;
    private RandomAccessFile raf;
    private int deewlTimes;
    private int reclTimes;

    public Lock(final Master mst) throws BrumaException {
        if (mst == null) {
            throw new BrumaException("Lock/null master file");
        }
        try {
            this.mst = mst;
            tmpDir = System.getProperty("java.io.tmpdir");
            raf = new RandomAccessFile(
                         new File(tmpDir, mst.getMasterName() + ".lck"), "rwd");
            lockFile = new File(tmpDir, mst.getMasterName() + ".ltmp");
            deewlTimes = DEFAULT_DEEWL_TIMES;
            reclTimes = DEFAULT_RECORDL_TIMES;
        } catch(Exception ex) {
            throw new BrumaException(ex);
        }
    }

    public void close() throws BrumaException {
        try {
            raf.close();
        } catch (Exception ex) {
            throw new BrumaException(ex);
        }
    }

    private boolean lockFile() {
        boolean success = false;

        try {
            success = lockFile.createNewFile();
        } catch(Exception ex) {
        }
        return success;
    }

    private void unlockFile() throws BrumaException {
       if (!lockFile.delete()) {
           throw new BrumaException("");
        }
    }

    private void setFileByte(final long pos,
                             final int value) throws BrumaException {
        assert pos >= 0;
        assert value >= 0;

        try {
            raf.seek(pos);
            raf.write(value);
        } catch(IOException ioe) {
            throw new BrumaException(ioe);
        }
    }

    private int getFileByte(final long pos) throws BrumaException {
        assert pos >= 0;

        final int ret;

        try {
            raf.seek(pos);
            ret = raf.read();
        } catch(IOException ioe) {
            throw new BrumaException(ioe);
        }
        return ret;
    }

    private void setTwoLockVals(final TwoLockVals vals) throws BrumaException {
        assert vals != null;

        try {
            raf.seek(0);
            raf.write(vals.ewl);
            raf.write(vals.del);
        } catch(IOException ioe) {
            throw new BrumaException(ioe);
        }
    }

    private TwoLockVals getTwoLockVals() throws BrumaException {
        final TwoLockVals vals = new TwoLockVals();

        try {
            raf.seek(0);
            vals.ewl = raf.read();
            vals.del = raf.read();
        } catch(IOException ioe) {
            throw new BrumaException(ioe);
        }
        return vals;
    }

    public boolean setDataEntryLock() throws BrumaException {
        boolean success = false;
        TwoLockVals vals;

        for (int counter = 0;  counter < deewlTimes; counter++) {
            if (lockFile()) {
                try {
                    vals = getTwoLockVals();
                    if (vals.ewl == 0) {
                        vals.del++;
                        setTwoLockVals(vals);
                        success = true;
                    }                    
                } finally {
                    unlockFile();
                }
                if (success) {
                    break;
                }
            }            
        }
        return success;
    }

    public boolean resetDataEntryLock() throws BrumaException {
        boolean success = false;
        TwoLockVals vals;

        for (int counter = 0;  counter < deewlTimes; counter++) {
            if (lockFile()) {
                try {
                    vals = getTwoLockVals();
                    if (vals.ewl == 0) {
                        if (vals.del == 0) {
                            throw new BrumaException(
                                            "DEL should be greater than zero");
                        }
                        vals.del--;
                        setTwoLockVals(vals);
                        success = true;
                    } else {
                        throw new BrumaException("EWL should be zero");
                    }
                } finally {
                    unlockFile();
                }
                if (success) {
                    break;
                }
            }
        }
        return success;
    }

    public EWLock setExclusiveWriteLock() throws BrumaException {
        EWLock lock = null;
        TwoLockVals vals;

        for (int counter = 0;  counter < deewlTimes; counter++) {
            if (lockFile()) {
                try {
                    vals = getTwoLockVals();
                    if (vals.ewl == 0) {
                        if (vals.del == 0) {
                            vals.ewl = 1;
                            setTwoLockVals(vals);
                            lock = new EWLock();
                        }
                    }
                } finally {
                    unlockFile();
                }
                if (lock != null) {
                    break;
                }
            }
        }
        return lock;
    }

    public boolean resetExclusiveWriteLock(final EWLock lock)
                                                         throws BrumaException {
        boolean success = false;
        TwoLockVals vals;

        if ((lock == null) || (lock.isUnlocked())) {
            throw new BrumaException("You should have the DEL");
        }
        for (int counter = 0;  counter < deewlTimes; counter++) {
            if (lockFile()) {
                try {
                    vals = getTwoLockVals();
                    if (vals.ewl == 1) {
                        if (vals.del != 0) {
                            throw new BrumaException("DEL should be zero");
                        }
                        vals.ewl = 0;
                        setTwoLockVals(vals);
                        lock.reset();
                        success = true;
                    } else {
                        throw new BrumaException("EWL should be one");
                    }
                } finally {
                    unlockFile();
                }
                if (success) {
                    break;
                }
            }
        }
        return success;
    }

    public void forceResetControlLocks() throws BrumaException {
        setTwoLockVals(new TwoLockVals());
    }

    public RecordLock lockRecord(final int mfn) throws BrumaException {
        if (mfn <= 0) {
            throw new BrumaException("mfn[" + mfn + "] <= 0");
        }

        final Record.Status[] recStatus = new Record.Status[1];
        final Record.ActiveStatus[] actStatus = new Record.ActiveStatus[1];
        long pos = mst.getMasterPosition(mfn, recStatus, actStatus);
        SegmentLock segLock = null;
        boolean delLock = false;  // DEL
        boolean sLock = false;
        int val;

        if (recStatus[0] != Record.Status.ACTIVE) {
            throw new BrumaException("lockRecord/record is not active");
        }

        try {
            for (int counter = 0;  counter < reclTimes; counter++) {
                if setDataEntryLock() {
                    if (lockFile()) {
                        val = getFileByte(mfn * 8 + 1);
                    }
                

                        segLock = readLockSegment(pos);
                sLock = true;

                pos = mst.getMasterPosition(mfn, recStatus, actStatus);
                if (recStatus[0] != Record.Status.ACTIVE) {
                    throw new BrumaException("lockRecord/record is not active");
                }

                if (segLock.val2 >= 0) { // Record not locked
                    segLock.val2 *= -1; // Lock record
                    writeUnlockSegment(segLock);
                    sLock = false;
                    resetDataEntryLock();
                    delLock = false;
                    break;
                }
                releaseLockSegment(segLock);
                sLock = false;
                resetDataEntryLock();
                delLock = false;
            }
        } catch(BrumaException exc) {
            if (sLock) {
                releaseLockSegment(segLock);
            }
            if (delLock) {
                resetDataEntryLock();
            }
            throw exc;
        }

        if (counter == reclTimes) { // Record locked
            throw new BrumaException("lockRecord/record locked by another");
        }

        return new RecordLock(mfn);
    }
}
*/
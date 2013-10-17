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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.GregorianCalendar;

/**
 * 
 * @author Heitor Barbieri
 */
public class Lock {
    public static final int LOCK_FIELD = 9999;

    private static final int SEGMENT_CONTROL_POS = 24;
    private static final int SEGMENT_LENGTH = 8;
    private static final int DEFAULT_SEGMENT_TIMES = 3000;
    private static final int DEFAULT_DEEWL_TIMES = 3000;
    private static final int DEFAULT_RECORDL_TIMES = 3000;
    private static long lockcount = 0L;

    public class SegmentLock {
        private long pos;
        private int len;
        private int val1;
        private int val2;
        private FileLock lock;

        private SegmentLock(final long pos,
                            final int len) {
            assert (pos >= 0) : "SegmentLock/pos < 0";
            assert (len >= 0) : "SegmentLock/len < 0";

            this.pos = pos;
            this.len = len;
            this.val1 = 0;
            this.val2 = 0;
            this.lock = null;
        }

        void reset() {
            pos = 0;
            len = 0;
            val1 = 0;
            val2 = 0;
            lock = null;
        }

        boolean isUnlocked() {
            return (lock == null);
        }
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

    private final FileChannel fc;
    private final ByteOrder order;
    private final Master mst;
    private final boolean FFI;
    private final int segmentTimes;
    private final Object fileSync;
    private int deewlTimes;
    private int reclTimes;

    public Lock(final Master mst) throws BrumaException {
        if (mst == null) {
            throw new BrumaException("Lock/null master file");
        }
        try {
            this.mst = mst;
            this.FFI = mst.isFFI();
            segmentTimes = DEFAULT_SEGMENT_TIMES;
            deewlTimes = DEFAULT_DEEWL_TIMES;
            reclTimes = DEFAULT_RECORDL_TIMES;
            fc = new RandomAccessFile(mst.getMasterName() + ".mst", "rw").
                                                                   getChannel();
            order = (mst.isSwapped() ?  ByteOrder.LITTLE_ENDIAN
                                     :  ByteOrder.BIG_ENDIAN);
            fileSync = new Object();
        } catch(Exception ex) {
            throw new BrumaException(ex.toString());
        }
    }

    public void setDbTimes(final int deewl_times) {
        this.deewlTimes =
                      ((deewl_times < 1) ? DEFAULT_DEEWL_TIMES : deewl_times);
    }

    public SegmentLock lockSegment(final long pos,
                                   final int len,
                                   final boolean shared) throws BrumaException {
        if (pos < 0) {
            throw new BrumaException("lockSegment/pos < 0");
        }
        if (len < 0) {
            throw new BrumaException("lockSegment/len < 0");
        }

        final SegmentLock ret = new SegmentLock(pos, len);

        try {
            for (int counter = 0; counter < segmentTimes; counter++) {
                synchronized(fileSync) {
                    ret.lock = fc.tryLock(pos, len, shared);
                }
                if (ret.lock != null) {
                    break;
                }
                Thread.sleep(50);
            }
        }  catch(Exception ex) {
            throw new BrumaException("lockSegment/" + ex.toString());
        }

        if (ret.lock == null) {
            throw new BrumaException(
                    "lockSegment/segment is locked by another");
        }

        assert (!ret.isUnlocked()) : "lockSegment/isUnlocked()";

        return ret;
    }

    public void releaseLockSegment(final SegmentLock sLock)
                                                         throws BrumaException {
        if ((sLock == null) || (sLock.lock == null)) {
            throw new BrumaException("releaseLockSegment/invalid SegmentLock");
        }

        try {
            if (sLock.lock != null) {
                sLock.lock.release();
            }
        } catch(IOException ioe) {
            throw new BrumaException("releaseLockSegment/ " + ioe.toString());
        } finally {
            sLock.reset();
        }

        assert (sLock.isUnlocked()) :
                                     "releaseLockSegment/isUnlocked() == false";
    }

    private SegmentLock readLockSegment(final long pos) throws BrumaException {
        assert pos >= 0 : "readLockSegment/pos < 0";
        if (pos < 0) {
            throw new BrumaException("readLockSegment/pos < 0");
        }

        final ByteBuffer bb = ByteBuffer.allocate(SEGMENT_LENGTH).order(order);
        final SegmentLock segLock = lockSegment(pos, SEGMENT_LENGTH, true);

        try {
            synchronized(fileSync) {
                fc.position(pos);
                if (fc.read(bb) != SEGMENT_LENGTH) {
                    throw new BrumaException("readLockSegment/file read failed");
                }
            }
            bb.rewind();
            segLock.val1 = bb.getInt();
            if ((pos == SEGMENT_CONTROL_POS) || FFI) {
                segLock.val2 = bb.getInt();
            } else {
                segLock.val2 = bb.getShort();
            }
        } catch(IOException ioe) {
            releaseLockSegment(segLock);
            throw new BrumaException("readLockSegment/" + ioe.toString());
        }

        return segLock;
    }

    private void writeUnlockSegment(final SegmentLock segLock)
                                                         throws BrumaException {
        assert(segLock != null) : "writeUnlockSegment/segLock == null";

        final long pos = segLock.pos;
        final ByteBuffer bb = ByteBuffer.allocate(SEGMENT_LENGTH).order(order);

        try {
            bb.putInt(segLock.val1);
            if ((pos == SEGMENT_CONTROL_POS) || FFI) {
                bb.putInt(segLock.val2);
            } else {
                bb.putShort((short)segLock.val2);
            }
            bb.rewind();
            synchronized(fileSync) {
                fc.position(pos);
                fc.write(bb);
            }
        } catch (IOException ioe) {
            throw new BrumaException("writeUnlockSegment/" + ioe.toString());
        } finally {
            releaseLockSegment(segLock);
        }
        assert (segLock.isUnlocked()) :
                                    "writeUnlockSegment/isUnlocked() == false";
    }

    public void setDataEntryLock() throws BrumaException {
        int counter = 0;
        SegmentLock segLock = null;

        while (counter++ < deewlTimes) {
            segLock = readLockSegment(SEGMENT_CONTROL_POS);

            if (segLock.val1 < 0) { // DEL_Flag
                releaseLockSegment(segLock);
                throw new BrumaException("setDataEntryLock/DEL_Flag["
                                                    + segLock.val1 +  "] < 0");
            }
            if (segLock.val2 == 0) { // EWL_Flag
                break;
            }
            releaseLockSegment(segLock);
        }

        if (segLock.val2 != 0) { // EWL_Flag
            releaseLockSegment(segLock);
            throw new BrumaException("setDataEntryLock/EWL_Flag["
                                                    + segLock.val2 + "] != 0");
        }

        segLock.val1++; // DEL_Flag
        writeUnlockSegment(segLock);
    }

    public void resetDataEntryLock() throws BrumaException {
        int counter = 0;
        SegmentLock segLock = null;

        while (counter++ < deewlTimes) {
            segLock = readLockSegment(SEGMENT_CONTROL_POS);
            if (segLock.val1 <= 0) { // DEL_Flag
                releaseLockSegment(segLock);
                throw new BrumaException("resetDataEntryLock/DEL_Flag["
                                                    + segLock.val1 +  "] <= 0");
            }
            if (segLock.val2 == 0) { // EWL_Flag
                break;
            }
            releaseLockSegment(segLock);
        }

        if (segLock.val2 != 0) { // EWL_Flag
            releaseLockSegment(segLock);
            throw new BrumaException("(resetDataEntryLock/EWL_Flag["
                                                    + segLock.val2 + "] != 0)");
        }

        segLock.val1--; // DEL_Flag

        writeUnlockSegment(segLock);
    }

    public EWLock setExclusiveWriteLock() throws BrumaException {
        int counter = 0;
        SegmentLock segLock = null;

        while(counter++ < deewlTimes) {
            segLock = readLockSegment(SEGMENT_CONTROL_POS);
            // DEL_Flag, EWL_Flag
            if ((segLock.val1 == 0) && (segLock.val2 == 0)) {
                break;
            }
            releaseLockSegment(segLock);
        }
        if (segLock.val1 != 0) { // DEL_Flag
            releaseLockSegment(segLock);
            throw new BrumaException("setExclusiveWriteLock/DEL_Flag["
                                                    + segLock.val1 +  "] < 0");
        }
        if (segLock.val2 != 0) { // EWL_Flag
            releaseLockSegment(segLock);
            throw new BrumaException("setExclusiveWriteLock/EWL_Flag["
                                                    + segLock.val2 + "] != 0)");
        }

        segLock.val2++;  // EWL_Flag
        writeUnlockSegment(segLock);

        return new EWLock();
    }

    public void resetExclusiveWriteLock(final EWLock lock)
                                                          throws BrumaException {
        if (lock == null) {
            throw new BrumaException(
                        "resetExclusiveWriteLock/does not has EWL onwership");
        }
        if (!lock.isUnlocked()) {
            throw new BrumaException(
                        "resetExclusiveWriteLock/does not has EWL onwership");
        }

        final SegmentLock segLock = readLockSegment(SEGMENT_CONTROL_POS);

        if (segLock.val1 != 0) { // DEL_Flag
            releaseLockSegment(segLock);
            throw new BrumaException("resetExclusiveWriteLock/DEL_Flag["
                                                    + segLock.val1 +  "] != 0");
        }
        if (segLock.val2 != 1) { // EWL_Flag
            releaseLockSegment(segLock);
            throw new BrumaException("resetExclusiveWriteLock/EWL_Flag["
                                                    + segLock.val2 +  "] != 1");
        }

        segLock.val2--; // EWL_Flag
        lock.reset();

        writeUnlockSegment(segLock);
    }

    public void forceResetControlLocks() throws BrumaException {
        final SegmentLock segLock = readLockSegment(SEGMENT_CONTROL_POS);

        segLock.val1 = 0; // DEL_Flag
        segLock.val2 = 0; // EWL_Flag

        writeUnlockSegment(segLock);
    }

    public RecordLock lockRecord(final int mfn) throws BrumaException {
        if (mfn <= 0) {
            throw new BrumaException("lockRecord/mfn[" + mfn + "] <= 0");
        }

        final Record.Status[] recStatus = new Record.Status[1];
        final Record.ActiveStatus[] actStatus = new Record.ActiveStatus[1];
        long pos = mst.getMasterPosition(mfn, recStatus, actStatus);
        int counter = 0;
        SegmentLock segLock = null;
        boolean delLock = false;  // DEL
        boolean sLock = false;

        if (recStatus[0] != Record.Status.ACTIVE) {
            throw new BrumaException("lockRecord/record is not active");
        }

        try {
            while (counter++ < reclTimes) {
                setDataEntryLock();
                delLock = true;
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

    public void unlockRecord(final RecordLock lock) throws BrumaException {
        if (lock == null) {
            throw new BrumaException("unlockRecord/null _lock");
        }

        final Record.Status[] status = new Record.Status[1];
        final Record.ActiveStatus[] actStatus = new Record.ActiveStatus[1];
        long pos = mst.getMasterPosition(lock.mfn, status, actStatus);
        SegmentLock segLock = null;
        boolean delLock = false;  // DEL
        boolean sLock = false;

        if (status[0] != Record.Status.ACTIVE) {
            throw new BrumaException("unlockRecord/record is not active");
        }

        try {
            //lock.mfn = 0;
            setDataEntryLock();
            delLock = true;
            segLock = readLockSegment(pos);
            sLock = true;

            mst.getMasterPosition(lock.mfn, status, actStatus);
            if (status[0] != Record.Status.ACTIVE) {
                throw new BrumaException("unlockRecord/record is not active");
            }

            if (segLock.val2 >= 0) { // Record unlocked
                throw new BrumaException("unlockRecord/record is not locked");
            }
            segLock.val2 *= -1; // Unlock record

            writeUnlockSegment(segLock);
            sLock = false;
            resetDataEntryLock();
        } catch(BrumaException exc) {
            if (sLock == true) {
                releaseLockSegment(segLock);
            }
            if (delLock == true) {
                resetDataEntryLock();
            }
            throw exc;
        }
    }

    public void forceUnlockRecord(final int mfn) throws BrumaException {
        if (mfn <= 0) {
            throw new BrumaException("forceUnlockRecord/mfn[" + mfn + "] < 0");
        }

        final Record.Status[] status = new Record.Status[1];
        final Record.ActiveStatus[] actStatus = new Record.ActiveStatus[1];
        long pos = mst.getMasterPosition(mfn, status, actStatus);
        SegmentLock segLock = null;
        boolean sLock = false;

        try {
            segLock = readLockSegment(pos);
            sLock = true;

            mst.getMasterPosition(mfn, status, actStatus);
            if (status[0] != Record.Status.ACTIVE) {
                throw new BrumaException(
                                    "forceUnlockRecord/record is not active");
            }

            if (segLock.val2 >= 0) { // Record unlocked
                throw new BrumaException(
                                    "forceUnlockRecord/record is not locked");
            }
            segLock.val2 *= -1; // Unlock record

            writeUnlockSegment(segLock);
        } catch(BrumaException exc) {
            if (sLock) {
                releaseLockSegment(segLock);
            }
            throw exc;
        }
    }

    public void close() throws BrumaException {
        try {
            if (fc != null) {
                fc.close();
            }
        } catch (IOException ioe) {
            throw new BrumaException(ioe.toString());
        }
    }

    public static boolean isRecordLockedByAnother(final String ownerId,
                                                  final Record rec)
                                                        throws BrumaException {
        assert (ownerId != null) : "isRecordLockedByAnother/null owner_id";
        assert (rec != null) : "isRecordLockedByAnother/null record";

        final long ctime = new GregorianCalendar().getTimeInMillis();
        final String field = rec.getField(LOCK_FIELD, 1).getContent();
        final String ident;
        final String time;
        final String[] sub;
        final long ltime;
        boolean ret = false;

        if (field.length() != 0) {
            sub = field.split("\\^\\w");
            if (sub.length != 3) {
                throw new BrumaException(
                               "isRecordLockedByAnother/invalid record field");
            }
            ident = sub[1].trim();
            if (ident.length() == 0) {
                throw new BrumaException(
                               "isRecordLockedByAnother/invalid id lock field");
            }
            time = sub[2].trim();
            if (time.length() == 0) {
                throw new BrumaException(
                             "isRecordLockedByAnother/invalid time lock field");
            }

            if (ident.compareTo(ownerId) != 0) {
                try {
                    ltime = Long.parseLong(time);
                } catch(NumberFormatException _nfe) {
                    throw new BrumaException("isRecordLockedByAnother/"
                                                          + _nfe.getMessage());
                }

                ret = (ltime >= ctime);
            }
        }
        return ret;
    }

    synchronized static String getNewLockID() {
        return System.currentTimeMillis() + ":" + (++lockcount);
    }

    public static void main(final String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("\nusage: Lock <master> [--isFFI[");
            System.exit(1);
        }

        final boolean isFFI = args.length > 1;
        final Master mst = MasterFactory.getInstance(args[0]).setFFI(isFFI)
                                                             .open();
        final Lock il = new Lock(mst);
        final BufferedReader br = new BufferedReader(
                                            new InputStreamReader(System.in));
        String opt;
        String eMess = null;

        while (true) {
            System.out.println(
                        "-------------------------------------------------\n");
            System.err.println("  <option>: SDEL - set data entry lock");
            System.err.println("            RDEL - reset data entry lock");
            System.err.println("            SEWL - set exclusive write lock");
            System.err.println("            UFCTL - DEL & EWL unlock force");
            System.err.println("            LREC=<mfn> - record lock (mfn)");
            System.err.println(
                        "            UFREC=<mfn> - record unlock force (mfn)");
            System.err.println("            EXIT - exit the program");
            System.err.print("\n  option = " );

            try {
                opt = br.readLine();

                if (opt.compareTo("SDEL") == 0) {
                    il.setDataEntryLock();
                } else if (opt.compareTo("RDEL") == 0) {
                    il.resetDataEntryLock();
                } else if (opt.compareTo("SEWL") == 0) {
                    il.setExclusiveWriteLock();
                } else if (opt.startsWith("UFCTL")) {
                    il.forceResetControlLocks();
                } else if (opt.startsWith("LREC=")) {
                    il.lockRecord(Integer.parseInt(opt.substring(5)));
                } else if (opt.startsWith("UFREC=")) {
                    il.forceUnlockRecord(Integer.parseInt(opt.substring(6)));
                } else if (opt.compareTo("EXIT") == 0) {
                    break;
                } else {
                    throw new IllegalArgumentException("unknown operation - "
                                                                        + opt);
                }
            } catch (Exception ex) {
                eMess = ex.getMessage();
            }

            System.out.print("  status: ");
            if (eMess == null) {
                System.out.println("success");
            } else {
                System.out.println("failed - " + eMess);
            }
        }

        mst.close();
        il.close();
    }
}

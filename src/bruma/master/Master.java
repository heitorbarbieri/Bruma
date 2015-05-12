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
import bruma.master.Lock.SegmentLock;
import bruma.master.XrfFile.XrfInfo;
import bruma.utils.ISO8859OrIBM850;
import bruma.utils.Util;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* OBS - Se o valor de campo nxtmfp do registro de controle para uma base com
         R registros contem o valor X, entao quando a base tiver R+1 registros,
         xref referente ao registro R+1 apontara para a posicao X-1 */

/**
 * The Master class represents an Isis master file.
 * @author Heitor Barbieri
 */
public class Master implements MasterInterface {
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

    static final int MSNVSPLT = 20;  // cisis em ffi
    static final int MF_BLOCKSIZE = 512;

    private static final int CONTROL_SIZE = 32;
    private static final int LEADER_SIZE = 26;
    private static final int RECORD_LOCK_SIZE = 8;

    private final byte[] page;
    private MasterPlatformInfo info;
    private String dbName;
    private String encoding;
    private boolean swapped;
    private boolean autoCommit;
    private boolean multiUser;
    private int filler;
    private int neverSplit;
    private int neverSplitRec;
    private RecordByteBuffer rbb;
    private RandomAccessFile raf;
    private FileChannel fc;
    private XrfFile xrf;
    private Lock lock;
    private boolean FFI;
    private int sysfile;
    private int shift;
    private int maxmfrl;
    private ByteBuffer bBuffer;
    private String mstExtension;
    private String xrfExtension;
    private Map<Integer,String> tags;
    private Map<String,Integer> stags;

    /**
     * The constructor of the Master class returned by the MasterFactory object.
     * @param mpi internal information about the database to be opened or created.
     * @exception BrumaException
     */
    Master(final MasterPlatformInfo mpi) throws BrumaException {
        assert mpi != null;
        final int ffiSize;
        final int align = mpi.getDataAlignment();

        info = mpi;
        page = new byte[MF_BLOCKSIZE];
        java.util.Arrays.fill(page, (byte) 0);
        dbName = mpi.getMstName();
        encoding = mpi.getEncoding();
        swapped = mpi.isSwapped();
        autoCommit = mpi.isXrfWriteCommit();
        multiUser = false;
        filler = (align == 0) ? 0 : 2;
        //filler = (align == 0) ? 0 : (align - 2); // 2 = short size
        rbb = null;
        raf = null;
        lock = null;
        FFI = mpi.isFfi();
        ffiSize = FFI ? 2 : 0;
        neverSplit = (FFI ? 22 : 18) + filler;
        neverSplitRec =
                    MF_BLOCKSIZE - (4 + ffiSize + filler + 4 + 2 + ffiSize) - 1;
        sysfile = 0;
        shift = mpi.getShift();
        reallocBuffer(CONTROL_SIZE);
        mstExtension = ".mst";
        xrfExtension = ".xrf";
        tags = null;
        stags = null;
    }

    final void reallocBuffer(final int newSize) throws BrumaException {
        assert newSize > 0;

        if (maxmfrl < newSize) {
            if (newSize < MSNVSPLT) {
                throw new BrumaException("mfrl[" + newSize + "] <" + MSNVSPLT);
            }
            if (newSize > MAXMFRL_POSSIBLE) {
                throw new BrumaException("mfrl[" + newSize + "] >"
                                                            + MAXMFRL_POSSIBLE);
            }
            if (!FFI && (newSize > MAXMFRL_ISIS)) {
                throw new BrumaException("mfrl[" + newSize + "] >"
                                                            + MAXMFRL_ISIS);
            }
            final int minAlloc = MF_BLOCKSIZE;
            final int div = newSize/minAlloc;
            final int rem = newSize%minAlloc;

            if (div == 0) {
                maxmfrl = minAlloc;
            } else {
               maxmfrl = (rem == 0) ? (div * minAlloc) : ((div + 1) * minAlloc);
            }
            bBuffer = ByteBuffer.allocateDirect(maxmfrl)
                            .order(swapped ? ByteOrder.LITTLE_ENDIAN :
                                             ByteOrder.BIG_ENDIAN);
        }
        bBuffer.limit(newSize);
        bBuffer.rewind();
    }

    /**
     * Checks if an Isis database exists.
     * @param dbName the database name.
     * @return true if the database exists or false if not.
     * @exception BrumaException
     */
    public static boolean exists(final String dbName) throws BrumaException {
        boolean ret = false;

        if (dbName != null) {
            final String name = Util.changeFileExtension(dbName, null);

            if (new File(name + ".mst").exists()) {
                if (new File(name + ".xrf").exists()) {
                    ret = true;
                } else if (new File(name + ".XRF").exists()) {
                    ret = true;
                }
            } else if (new File(name + ".MST").exists()) {
                if (new File(name + ".xrf").exists()) {
                    ret = true;
                } else if (new File(name + ".XRF").exists()) {
                    ret = true;
                }
            }
        }

        return ret;
    }

    /**
     * Opens an existing Isis database.
     * @return this Master object
     * @exception BrumaException
     */
    Master open() throws BrumaException {
        assert info != null;
        try {
            final String mode;
            final Control ctl;
            final DbType dbt;
            final DbType.Type type;
            final int ffiSize;
            File file = new File(dbName + ".mst");
            File xfile = new File(dbName + ".xrf");

            /*if (maxmfrl > CONTROL_SIZE) {
                throw new BrumaException("open/create a new Master object");

            }*/
            if (file.exists()) {
                mstExtension = ".mst";
            } else {
                file = new File(dbName + ".MST");
                if (file.exists()) {
                    mstExtension = ".MST";
                } else {
                    throw new BrumaException("open/missing master file : "
                                                                     + dbName);
                }
            }
            if (xfile.exists()) {
                xrfExtension = ".xrf";
            } else {
                xfile = new File(dbName + ".XRF");
                if (xfile.exists()) {
                    xrfExtension = ".XRF";
                } else {
                    throw new BrumaException(
                               "open/missing cross reference file : " + dbName);
                }
            }
            if (!file.canRead()) {
                throw new BrumaException("open/cant read master file : "
                                                                      + dbName);
            }
            mode = (file.canWrite() ? "rw": "r");
            raf = new RandomAccessFile(file, mode);
            if (raf.length() < MF_BLOCKSIZE) {
                throw new BrumaException("open/MF_BLOCKSIZE");
            }
            fc = raf.getChannel();
            dbt = new DbType(fc);
            if (swapped != dbt.isSwapped()) {
                reallocBuffer(maxmfrl);
                swapped = dbt.isSwapped();
            }
            if (encoding.equals(GUESS_ISO_IBM_ENCODING)) {
                final ISO8859OrIBM850 guess = new ISO8859OrIBM850(dbName, true);
                final String encName = guess.guessEncoding();
                encoding = (encName == null) ? Master.DEFAULT_ENCODING :encName;
            }
            ctl = getControlRecord();
            info.setSwapped(swapped);
            info.setShift(shift);
            sysfile = ctl.getMftype();
            if (sysfile != 0) {
                throw new IOException("open/unsupported master file type");
            }
            if ((multiUser) || (!info.isInMemoryXrf())) {
                xrf = new XrfFile(dbName + xrfExtension, shift, swapped, false);
                if (multiUser) {
                    lock = new Lock(this);
                }
            } else {
                xrf = new BufferedXrfFile(dbName + xrfExtension, shift, swapped,
                                        false, ctl.getNxtmfn() - 1, autoCommit);
            }
            if (ctl.getNxtmfn() == 1) {  // Empty master
                FFI = false;
                filler = (System.getProperty("os.name").startsWith("Win"))
                                                                       ? 0 : 2;
                info.setDataAlignment(filler);
            } else {
                type = dbt.getType(xrf);
                if (!type.recognized) {
                    throw new IOException(
                                  "open/could not recognize master file info");
                }
                FFI = !type.IsisStandard;
                filler = type.align == 0 ? 0 : 2;
                info.setDataAlignment(type.align);
            }
            info.setFfi(FFI);
            neverSplit = (FFI ? 22 : 18) + filler;
            ffiSize = FFI ? 2 : 0;
            neverSplitRec =
                    MF_BLOCKSIZE - (4 + ffiSize + filler + 4 + 2 + ffiSize) - 1;
            //rba = new RecordByteArray(FFI, swapped, filler, shift);
            rbb = new RecordByteBuffer(FFI, swapped, filler, shift);
            reallocBuffer(FFI ? DEFMFRL_FFI : MAXMFRL_ISIS);
        } catch (IOException ioex) {
            try {
                if (raf != null) {
                    raf.close();
                    raf = null;
                }
            } catch (IOException ioex2) {
            }
            throw new BrumaException(ioex);
        }
        return this;
    }

    /**
     * Creates a new Isis database.
     * @return this Master object
     * @exception BrumaException
     */
    Master create() throws BrumaException {
        try {
            final File file = new File(dbName + ".mst");

            mstExtension = ".mst";
            xrfExtension = ".xrf";

            /*if (maxmfrl > CONTROL_SIZE) {
                throw new BrumaException("create/create a new Master object");

            }*/
            if (file.exists()) {
                throw new BrumaException("create/master file : " + dbName +
                                        " already created");
            }
            /*if (!FFI && (shift != 0)) {
                throw new BrumaException(
                           "create/standard isis master should have shift=0");
            }*/
            reallocBuffer(FFI ? DEFMFRL_FFI : MAXMFRL_ISIS);
            raf = new RandomAccessFile(file, "rw");
            fc = raf.getChannel();
            xrf = null;
            lock = null;
            if (encoding.equals(GUESS_ISO_IBM_ENCODING)) {
                encoding = Master.DEFAULT_ENCODING;
            }
            //rba = new RecordByteArray(FFI, swapped, filler, shift);
            rbb = new RecordByteBuffer(FFI, swapped, filler, shift);
            sysfile = reset().getMftype();
            if (sysfile != 0) {
                throw new IOException("create/unsupported master file type");
            }
            if ((multiUser) || (!info.isInMemoryXrf())) {
                if (multiUser) {
                    lock = new Lock(this);
                }
                xrf = new XrfFile(dbName + xrfExtension, shift, swapped, true);
            } else {
                xrf = new BufferedXrfFile(
                    dbName + xrfExtension, shift, swapped, true, 0, autoCommit);
            }
//System.out.println("FFI = " + FFI + " shift = " + shift);
        } catch (IOException ioex) {
            try {
                if (raf != null) {
                    raf.close();
                    raf = null;
                }
            } catch (IOException ioex2) {
            }
            throw new BrumaException(ioex);
        }
        return this;
    }

    /**
     * Deletes Isis master files (mst and xrf)
     * @return true if all files were deleted, false otherwise.
     * @exception BrumaException
     */
    @Override
    public boolean delete() throws BrumaException {
        boolean ret = true;

        if (exists(dbName)) {
            ret = ret && new File(dbName + mstExtension).delete();
            ret = ret && new File(dbName + xrfExtension).delete();
        }

        return ret;
    }

    @Override
    public Iterator<Record> iterator() {
        Iterator<Record> iter = null;

        if (raf != null) {
            try {
                iter = new MstIterator();
            } catch (BrumaException zex) {
                iter = null;
            }
        }

        return iter;
    }

    /**
     * Convert the gigabytes dbase size to the associated ((power of 2) - 1)
     * shift
     * @param maxGigaSize dbase gigabytes size (0 means the default isis size =
     *                                          512 megabytes)
     * @return the power of 2 shift
     */
    static int convertToShift(final int maxGigaSize) {
        assert (maxGigaSize >= 0) && (maxGigaSize % 2 == 0);

        int cap = 1;
        int ret;

        if (maxGigaSize == 0) {
            ret = 0;
        } else {
            ret = 1;
            while (cap < maxGigaSize) {
                ret++;
                cap *= 2;
            }
        }

        return ret;
    }

    static int convertToGigaSize(final int pshift) {
        assert (pshift > 0);

        int ret = 2;

        if (pshift < 2) {
            ret = 0;
        } else {
            for (int pow = 2; pow < pshift; pow++) {
                ret *= 2;
            }
        }

        return ret;
    }

    /**
     * @return the maximum database size (in gigabytes).
     * Returning zero means 512 megabytes.
     */
    @Override
    public int getGigaSize() {
        int ret = 2;

        if (shift < 2) {
            ret = 0;
        } else {
            for (int pow = 2; pow < shift; pow++) {
                ret *= 2;
            }
        }

        return ret;
    }

    @Override
    public int getMaxRecSize() throws BrumaException {
        int max = 0;
        int recLen;

        for (Record rec : this) {
            if (rec == null) {
                throw new BrumaException("Master probably corrupted");
            }
            if (rec.getStatus() == Record.Status.ACTIVE) {
                recLen = rec.getRecordLength(encoding, FFI);
                max = Math.max(max, recLen);
            }
        }
//System.out.println("!!!!!mfrl=" + max);
        return max;
    }

    @Override
    public int getDataAlignment() {
        return filler;
    }

    /**
     * @return the database cross reference object.
     */
    public XrfFile getXrf() {
        return xrf;
    }

    /**
     * Closes frees all database used resources.
     * @exception BrumaException
     */
    @Override
    public void close() throws BrumaException {
        try {
            if (raf != null) {
                raf.close();
                raf = null;
            }
            if (xrf != null) {
                xrf.close();
                xrf = null;
            }
            if (lock != null) {
                lock.close();
                lock = null;
            }
        } catch (IOException ioe) {
            throw new BrumaException(ioe.toString());
        }
    }

    public int getShift() {
        return shift;
    }

    /**
     * @return tells if the database file byte order is litttle or big endian.
     */
    public boolean isSwapped() {
        return swapped;
    }

    /**
     * @return tells if the database was created with extended mode
     * (record size > 32 bytes).
     */
    @Override
    public boolean isFFI() {
        return FFI;
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
        return multiUser;
    }

    /**
     * @return the database encoding.
     */
    @Override
    public String getEncoding() {
        return encoding;
    }

    /**
     * @return the name of the database.
     */
    @Override
    public String getMasterName() {
        return dbName;
    }

    private Control reset() throws BrumaException {
        final int nxtmfp = adjustFilePos(2 * CONTROL_SIZE) + 1;
        final Control ctl = new Control(0,  // ctlmfn
                                        1,  // nxtmfn
                                        1,  // nxtmfb
                                        nxtmfp,
                                        0,  // mftype
                                        0,  // reccnt
                                        0,  // mfcxx1
                                        0,  // mfcxx2
                                        0); // mfcxx3
        try {
            raf.setLength(0);
            bBuffer.clear();
            bBuffer.limit(MF_BLOCKSIZE);
            bBuffer.put(page, 0, MF_BLOCKSIZE);
            bBuffer.rewind();
            if (fc.write(bBuffer, 0) != MF_BLOCKSIZE) {
                throw new BrumaException("write error");
            }
            writeControlRecord(ctl);

            if (xrf != null) {
                xrf.reset();
            }
        } catch (IOException ioe) {
            throw new BrumaException(ioe.toString());
        }
        return ctl;
    }

    /**
     * Resets the EWL and DEL flags, even if the caller doesnt have the unlock
     * right.
     * @exception BrumaException
     */
    @Override
    public void unlock() throws BrumaException {
        if (raf == null) {
            throw new BrumaException("master file is not opened");
        }
        if (lock == null) {
            throw new BrumaException("unlock/mono user mode");
        }
        lock.forceResetControlLocks();
    }

    /**
     * Reads the master control record.
     * @return the control record object
     * @exception BrumaException
     */
    @Override
    public Control getControlRecord() throws BrumaException {
        final Control ctl;
        SegmentLock sl = null;

        if (raf == null) {
            throw new BrumaException("master file is not opened");
        }
        try {
            if (lock != null) {
                sl = lock.lockSegment(0, CONTROL_SIZE, true);
            }
            bBuffer.clear();
            bBuffer.limit(CONTROL_SIZE);
            if (fc.read(bBuffer, 0) != CONTROL_SIZE) {
                throw new BrumaException("read error");
            }
            bBuffer.rewind();
            ctl = new Control(bBuffer.getInt(),
                              bBuffer.getInt(),
                              bBuffer.getInt(),
                              bBuffer.getShort(),
                              bBuffer.getShort(),
                              bBuffer.getInt(),
                              bBuffer.getInt(),
                              bBuffer.getInt(),
                              bBuffer.getInt());
            shift = ctl.getMftype() / 256;
            ctl.setMftype(ctl.getMftype() & 0x00FF);

            assert (shift >= 0) : "invalid db shift[" + shift + "]";
        } catch (IOException ioe) {
            throw new BrumaException(ioe.toString());
        } finally {
            if (lock != null) {
                lock.releaseLockSegment(sl);
            }
        }
        return ctl;
    }

    @Override
    public Lock.RecordLock lockRecord(final Record record)
                                                         throws BrumaException {
        if (raf == null) {
            throw new BrumaException("master file is not opened");
        }
        if (record == null) {
            throw new BrumaException("lockRecord/null record");
        }
        if (lock == null) {
            throw new BrumaException("lockRecord/mono user mode");
        }
        if (record.getStatus() != Record.Status.ACTIVE) {
            throw new BrumaException("lockRecord/record status is not " +
                                        "active: " + record.getStatus());
        }

        final Lock.RecordLock rlock = lock.lockRecord(record.getMfn());

        //record.setStatus(Record.Status.ACTIVE);
        record.setLockStatus(Record.LockStatus.LOCKED);

        return rlock;
    }

    /**
     * Locks a database record, avoiding the write to it for anyone who doesnt
     * have the lock.
     * @param mfn
     * @param recLock
     * @return the locked record.
     * @exception BrumaException
     */
    @Override
    public Record getLockRecord(final int mfn,
                                final Lock.RecordLock[] recLock)
                                                        throws BrumaException {
        final Record rec;

        if (raf == null) {
            throw new BrumaException("master file is not opened");
        }
        if (mfn <= 0) {
            throw new BrumaException("getLockRecord/id[" + mfn + "] <= 0");
        }
        if (lock == null) {
            throw new BrumaException("getLockRecord/mono user mode");
        }
        if (recLock == null) {
            throw new BrumaException("getLockRecord/null record lock array");
        }

        recLock[0] = lock.lockRecord(mfn);

        rec = getRecord(mfn);
        rec.setLockStatus(Record.LockStatus.LOCKED);

        return rec;
    }

    /**
     * Reads a record from the database.
     * @param mfn record master file number
     * @return the readen record.
     * @exception BrumaException
     */
    @Override
    public Record getRecord(final int mfn) throws BrumaException {
        Record record = null;
        final long position;
        final Record.Status recStatus[] = new Record.Status[1];
        final Record.ActiveStatus actStatus[] = new Record.ActiveStatus[1];
        final int auxMfn;
        final int size = 4 + (FFI ? 4 : 2);

        int mfrl;
        int aMfrl;
        SegmentLock sl = null;
        boolean locked = false;

        if (raf == null) {
            throw new BrumaException("master file is not opened");
        }
        if (mfn <= 0) {
            throw new BrumaException("getRecord/id[" + mfn + "] <= 0");
        }

        try {
            position = getMasterPosition(mfn, recStatus, actStatus);
/*System.out.println("mfn=" + mfn + " position=" + position +
                " recStatus=" + recStatus[0] + " actStatus=" + actStatus[0]);*/

            if (actStatus[0] == Record.ActiveStatus.NEW) {
                actStatus[0] = Record.ActiveStatus.NORMAL;
            }

            if (recStatus[0] == Record.Status.PHYDEL) {
                record = new Record(shift, filler);
                record.setStatus(Record.Status.PHYDEL);
                record.setActiveStatus(null);
                record.setLockStatus(null);
                record.setMfn(mfn);
            } else {
                if (lock != null) {
                    sl = lock.lockSegment(position, RECORD_LOCK_SIZE, true);
                }
                bBuffer.clear();
                bBuffer.limit(size);
                if (fc.read(bBuffer, position) != size) {
                    throw new BrumaException("read/1 error");
                }
                bBuffer.rewind();

                auxMfn = bBuffer.getInt();
//System.out.println("read auxMfn = " + auxMfn);
                mfrl = readInt(bBuffer);
                aMfrl = Math.abs(mfrl);
//System.out.println("status=" + recStatus[0]);

                if (aMfrl < neverSplit) {
                    throw new BrumaException("mfrl[" + aMfrl + "] < "
                                                                  + neverSplit);
                }
                reallocBuffer(aMfrl);
                if (mfn == auxMfn) {
                    if (mfrl < 0) {
                        locked = true;
                    }
                    bBuffer.clear();
                    bBuffer.limit(aMfrl);
                    if (fc.read(bBuffer, position) != aMfrl) {
                        throw new BrumaException("read/2 error");
                    }
                    //bBuffer.rewind();
                    //bBuffer.get(buffer, 0, mfrl);
//System.out.println ("auxMfn=" + auxMfn + " mfrl=" + mfrl + " buffer len="
                                              //+ buffer.length);
                    //record = rbb.fromByteArray(buffer, encoding);
                    record = rbb.fromByteBuffer(bBuffer, encoding);
                    record.setStatus(recStatus[0]);
                    if (recStatus[0] == Record.Status.LOGDEL) {
                        record.setLockStatus(null);
                        record.setActiveStatus(null);
                    } else {
                        if (locked) {
                            record.setLockStatus(Record.LockStatus.LOCKED);
                        } else {
                            record.setLockStatus(Record.LockStatus.NORMAL);
                        }
                        record.setActiveStatus(actStatus[0]);
                    }
                    //System.out.println ("status2 = " + record.getStatus());
                } else {
                    throw new BrumaException("getRecord/MST_FIL_BAD/[mfn=" + mfn
                                                    + " != " + auxMfn + "]");
                }
            }
        } catch (Exception ex) {
            throw new BrumaException("getRecord[mfn=" + mfn + "]/" + ex);
        } finally {
            if (lock != null) {
                lock.releaseLockSegment(sl);
            }
        }

        return record;
    }

    /**
     * Forces the unlock of a Isis record.
     * @param recLock - the lock token
     * @exception BrumaException
     */
    @Override
    public void unlockRecord(final Lock.RecordLock recLock)
                                                        throws BrumaException {
        if (raf == null) {
            throw new BrumaException("master file is not opened");
        }
        if (recLock == null) {
            throw new BrumaException("unlockRecord/null lock");
        }
        if (lock == null) {
            throw new BrumaException("unlockRecord/mono user mode");
        }
        lock.unlockRecord(recLock);
    }

    /**
     * Forces the unlock of a Isis record.
     * @param mfn - record number to unlock
     * @exception BrumaException
     */
    @Override
    public void forceUnlockRecord(final int mfn) throws BrumaException {
        if (raf == null) {
            throw new BrumaException("master file is not opened");
        }
        if (lock == null) {
            throw new BrumaException("forceUnlockRecord/mono user mode");
        }
        if (mfn <= 0) {
            throw new BrumaException(
                                "forceUnlockRecord/mfn[" + mfn + "] < = 0");
        }

        lock.forceUnlockRecord(mfn);
    }

    private int readInt(final ByteBuffer buffer) throws IOException {
        assert (buffer != null) : "readInt/null ByteBuffer";

        return (FFI ? buffer.getInt() : buffer.getShort());
    }

    /*private void writeInt(final int intVal,
                  final ByteBuffer buffer) throws IOException {
        assert (buffer != null) : "writeInt/null ByteBuffer";

        if (FFI) {
            buffer.putInt(intVal);
        }   else    {
            buffer.putShort((short)intVal);
        }
    }*/

    public long getMasterPosition(final int mfn,
                                  final Record.Status recStatus[],
                                  final Record.ActiveStatus activeStatus[])
                                                        throws BrumaException {
        assert (mfn > 0) : "getMasterPosition/mfn[" + mfn + "] <= 0";
        assert (recStatus != null) : "getMasterPosition/null record status";
        assert (activeStatus != null) :
                                 "getMasterPosition/null record active status";

        final long pos;

        if (sysfile == 0) { // user data base file
            final XrfInfo xinfo = xrf.readXrfInfo(mfn);
            long auxPos = (Math.abs(xinfo.getBlock()) - 1);

            auxPos *= MF_BLOCKSIZE;
            pos = auxPos + (xinfo.getOffset() & 0X1ff);
/*if (pos < 0)             {
    System.err.println("mfn=" + mfn + " pos=" + pos
               + " bloco=" + xinfo.getBlock() + " offset=" + xinfo.getOffset());
}*/
            recStatus[0] = xinfo.getStatus();
            activeStatus[0] = xinfo.getActStatus();
        } else { // system message files
            pos = mfn * MF_BLOCKSIZE;
            recStatus[0] = Record.Status.ACTIVE;
            activeStatus[0] = Record.ActiveStatus.NORMAL;
        }

        assert (pos >= 0) : "mst file position[" + pos + "] < 0";

        return pos;
    }

    private Leader readLeader(final long position) throws BrumaException {
        assert (position >= 0) : "readLeader/position[" + position + "] < 0";

        final Leader ret = new Leader();
        final Record.Status status;

        try {
            bBuffer.clear();
            bBuffer.limit(LEADER_SIZE);
            if (fc.read(bBuffer, position) != LEADER_SIZE) {
                throw new BrumaException("read error");
            }
            bBuffer.rewind();

            ret.setMfn(bBuffer.getInt());
            ret.setMfrl(Math.abs(readInt(bBuffer)));
            //System.out.println("position = "+ position + " auxMfn="
            //+  ret.auxMfn
            // + " len=" +  ret.mfrl + " bloco=" + ret.mfbwb + " desloc="
            // + ret.mfbwp + " base=" + ret.base + " nvf=" + ret.nvf +
            // " status=" + ret.status);            
            if ((!FFI) && (filler != 0)) {
                bBuffer.getShort();
            }
            ret.setMfbwb(bBuffer.getInt());
            ret.setMfbwp(bBuffer.getShort());
            if ((FFI) && (filler != 0)) {
                bBuffer.getShort();
            }
            ret.setBase(readInt(bBuffer));
            ret.setNvf(bBuffer.getShort());
            status = ((bBuffer.getShort() == 0) ? Record.Status.ACTIVE
                                                : Record.Status.LOGDEL);
            ret.setStatus(status);
            
            reallocBuffer(ret.getMfrl());
        } catch (IOException ioex) {
            throw new BrumaException(ioex.toString());
        }

        return ret;
    }

    private void writeControlRecord(final Control ctl) throws BrumaException {
        assert (ctl != null) : "writeControlRecord/null control";

        final int mftype = ((shift * 256) + ctl.getMftype());

        try {
            bBuffer.clear();
            bBuffer.limit(CONTROL_SIZE);
            bBuffer.putInt(ctl.getCtlmfn());
            bBuffer.putInt(ctl.getNxtmfn());
            bBuffer.putInt(ctl.getNxtmfb());
            bBuffer.putShort((short)(ctl.getNxtmfp()));
            bBuffer.putShort((short)mftype);
            bBuffer.putInt(ctl.getReccnt());
            bBuffer.putInt(ctl.getMfcxx1());
            bBuffer.putInt(ctl.getMfcxx2());
            bBuffer.putInt(ctl.getMfcxx3());
            bBuffer.rewind();
            if (fc.write(bBuffer, 0) != CONTROL_SIZE) {
                throw new BrumaException("writeControlRecord/write error");
            }
        } catch (IOException ioe) {
            throw new BrumaException("writeControlRecord/" + ioe.getMessage());
        }
    }

    private void writeRecord(final Control ctl,
                             final Record record,
                             final int block,
                             final int offset,
                             final boolean wasNew) throws BrumaException {

        assert(ctl != null) : "writeRecord2/null control";
        assert(record != null) : "writeRecord2/null record";
        assert(block > 0) : "writeRecord2/block[" + block + "] <= 0";
        assert(offset >= 0) : "writeRecord2/offset[" + offset + "] < 0";

        final boolean writeAtTheEnd = ((block == ctl.getNxtmfb())
                                    && (offset == ctl.getNxtmfp() - 1));
        final boolean isLastMfn = record.getMfn() == ctl.getNxtmfn() - 1;
        //final byte[] rec;
        final Record.Status status = record.getStatus();
        final Record.ActiveStatus actStatus = record.getActiveStatus();

        long filepos;
        SegmentLock sl = null;
        int nxtmfp;
//System.out.println("mfn=" + ctl.getNxtmfn());
//System.out.println("write : mfn=" + record.getMfn() + " bloco=" + block
        //+ " offset=" + offset);
        try {
            final Leader leaders = record.getLeader(encoding, FFI);

            // Checa se auxMfn e' maior que o proximo da base
            if (leaders.getMfn() > ctl.getNxtmfn()) {
                throw new BrumaException("id[" + leaders.getMfn() +
                          "] > ctl.nxtmfn[" + ctl.getNxtmfn() + "]");
            }

            // Checa tamanho do registro
            final int recLen = leaders.getMfrl();
            if (recLen < neverSplit) {
                throw new BrumaException("mfrl[" + recLen + "] <" + neverSplit);
            }

            reallocBuffer(recLen);
            rbb.toByteBuffer(record, encoding, bBuffer);

            if (bBuffer.position() != recLen) {
                throw new BrumaException("bBuffer.position()["
                   + bBuffer.position() + "] != leaders.mfrl[" + recLen +  "]");
            }
            filepos = ((long)(block - 1) * MF_BLOCKSIZE) + offset;
            assert filepos > 0 : "filepos=" + filepos;
//System.out.println("recLen=" + recLen + " filepos=" + filepos);
            // Ajusta buffer 
            bBuffer.rewind();

            // Bloqueia registro
            if (lock != null) {
                sl = lock.lockSegment(filepos, RECORD_LOCK_SIZE, false);
            }
            // Grava registro.
            if (fc.write(bBuffer, filepos) != recLen) {
                throw new BrumaException("write/1 error");
            }

            // Grava registro no final do master.
            if (writeAtTheEnd || isLastMfn) {
                // Preencher o que falta para completar um bloco de memoria
                final int rem =
                    MF_BLOCKSIZE - (int)((filepos + recLen) % MF_BLOCKSIZE);
                assert ((rem > 0) && (rem <= MF_BLOCKSIZE)): "rem=" + rem;
                if (rem != MF_BLOCKSIZE) {
                    bBuffer.clear();
                    bBuffer.limit(rem);
                    bBuffer.put(page, 0, rem);
                    bBuffer.rewind();
                    if (fc.write(bBuffer, filepos + recLen) != rem) {
                        throw new BrumaException("write/2 error");
                    }
                }
                // Atualiza registro de controle.
                filepos += recLen;
                ctl.setNxtmfb(((int)(filepos / MF_BLOCKSIZE) + 1));
                nxtmfp = (int)(filepos % MF_BLOCKSIZE) + 1;

                //Se estiver no fim do bloco, comeca no proximo
// Dando folga maior por causa da diferenca do tamanho do leader nas maquinas.
                //if (nxtmfp > 488) {
                if (nxtmfp >= neverSplitRec) {
                    ctl.setNxtmfb(ctl.getNxtmfb() + 1);
                    nxtmfp = 1;
                }
                ctl.setNxtmfp(nxtmfp);
                writeControlRecord(ctl);
            }

            // Libera registro.
            if (lock != null) {
                lock.releaseLockSegment(sl);
                sl = null;
            }

            if (status == Record.Status.ACTIVE) {
                if (actStatus == Record.ActiveStatus.NEW) { // Registro novo
                    // Soma 1024 para indicar que a atualizacao inv esta
                    // pendente e que e novo.
                    xrf.writeXrfInfo(xrf.new XrfInfo(leaders.getMfn(),
                                                 block,
                                                 (offset + 1024),
                                                  status,
                                                  actStatus));
                } else {
                    // Soma 512 para indicar que a atualizacao inv esta
                    // pendente.
                    xrf.writeXrfInfo(xrf.new XrfInfo(leaders.getMfn(),
                                                     block,
                                                    (offset + 512),
                                                     status,
                                                     actStatus));
                }
            } else if (status == Record.Status.LOGDEL) {
                if  (wasNew) {
                    xrf.writeXrfInfo(xrf.new XrfInfo(leaders.getMfn(),
                                                        -block,
                                                        offset + 1024,
                                                        status,
                                                        actStatus));
                } else {
                    xrf.writeXrfInfo(xrf.new XrfInfo(leaders.getMfn(),
                                                        -block,
                                                        offset + 512,
                                                        status,
                                                        actStatus));
                }
            } else { // fisicamente apagado
                xrf.writeXrfInfo(xrf.new XrfInfo(leaders.getMfn(),
                                                    -1,
                                                    0,
                                                    status,
                                                    actStatus));
            }
        } catch (IOException ioe) {
            throw new BrumaException(ioe.toString());
        } finally {
            if (lock != null) {
                if (sl != null) {
                    lock.releaseLockSegment(sl);
                }
            }
        }
    }

    private void createNDeletedRecords(final int recNum) throws BrumaException {
        assert recNum > 0;

        SegmentLock sl = null;

        try {
            if (lock != null) {
                sl = lock.lockSegment(0, CONTROL_SIZE, false);
            }
            final Control ctl = getControlRecord();
            int mfn = ctl.getNxtmfn();

            for (int counter = 0; counter < recNum; counter++) {
                xrf.writeXrfInfo(
                       xrf.new XrfInfo(mfn, -1, 0, Record.Status.PHYDEL, null));
                ctl.setNxtmfn(++mfn);
            }
            writeControlRecord(ctl);
        } finally {
            if (sl != null) {
                lock.releaseLockSegment(sl);
            }
        }
    }

    private int newRecord(final Control ctl,
                          final Record record) throws BrumaException {
        assert (ctl != null): "newRecord/null control";
        assert (record != null) : "newRecord/null record";

        final int nxtmfn = ctl.getNxtmfn();
        int mfn = record.getMfn();
        assert ((mfn == 0) || (mfn >= nxtmfn)) : "mfn[" + mfn + "] < " + nxtmfn;

        // Ajusta mfn
        if (mfn == 0) {
            mfn = nxtmfn;
            record.setMfn(nxtmfn);
        }

        if (record.getStatus() == Record.Status.PHYDEL) {
            createNDeletedRecords(1);
        } else {
            if (record.getStatus() == Record.Status.ACTIVE) {
                // Muda status do registro para novo
                record.setActiveStatus(Record.ActiveStatus.NEW);
            }

            // Ajusta apontadores para versao anterior
            record.setBlockNumber(0);
            record.setBlockPos(0);

            final int dif = mfn - nxtmfn;
            if (dif > 0) {
                createNDeletedRecords(dif);
            }
            ctl.setNxtmfn(mfn + 1);

            // Escreve no fim do mst.
            writeRecord(ctl, record, ctl.getNxtmfb(),
                                                ctl.getNxtmfp() - 1, false);
        }
        return mfn;
    }

    private int updateRecord(final Control ctl,
                             final Record record,
                             final boolean allowDeleted) throws BrumaException {
        assert (ctl != null) : "updateRecord/null control";
        assert (record != null) : "updateRecord/null record";

        final Record.Status status[] = new Record.Status[1];
        final Record.ActiveStatus actStatus[] = new Record.ActiveStatus[1];
        final int mfn = record.getMfn();
        final int nxtmfn = ctl.getNxtmfn();
        final long fpos = getMasterPosition(mfn, status, actStatus);
        boolean phyDel = false;

        // Atualização de registro já apagado.
        if (status[0] != Record.Status.ACTIVE) {
            if (allowDeleted) {
                if (status[0] == Record.Status.PHYDEL) {
                    phyDel = true;
                }
            } else {
                throw new BrumaException("updateRecord/record is deleted");
            }            
        }

        final int bl = (int)((fpos / XrfFile.XRF_BLOCKSIZE) + 1);
        final int pos = (int)(fpos % XrfFile.XRF_BLOCKSIZE);
        final Leader leader = (phyDel ? new Leader() : readLeader(fpos));
        boolean wasNew = false;

        // Aponta para onde estava apontando a versao anterior.
        if (phyDel) {
            record.setBlockNumber(0);
            record.setBlockPos(0);
        } else {
            record.setBlockNumber(leader.getMfbwb());
            record.setBlockPos(leader.getMfbwp());
        }

        if (record.getStatus() == Record.Status.ACTIVE) {
            if (phyDel) {
                record.setActiveStatus(Record.ActiveStatus.NEW);
                wasNew = true;
            } else if (actStatus[0] == Record.ActiveStatus.PENDING) {
                record.setActiveStatus(Record.ActiveStatus.PENDING);
            } else if (actStatus[0] == Record.ActiveStatus.NEW) {
                record.setActiveStatus(Record.ActiveStatus.NEW);
                wasNew = true;
            } else { // NORMAL
                record.setActiveStatus(Record.ActiveStatus.PENDING);
            }
        } else if (actStatus[0] == Record.ActiveStatus.NEW) {
            wasNew = true;
        }
        
        if (phyDel) { // Grava no final do arquivo master.
            writeRecord(ctl, record, ctl.getNxtmfb(),
                                                  ctl.getNxtmfp() - 1, wasNew);
        } else if (mfn == nxtmfn-1) { // Ultimo registro da base.
            // Grava no mesmo local da versao anterior do registro.
            writeRecord(ctl, record, bl, pos, wasNew);
        } else if (leader.getMfrl() < record.getRecordLength(encoding, FFI)) {
            // Grava no final do arquivo master.
//System.out.println("grava no final");
            writeRecord(ctl, record, ctl.getNxtmfb(),
                                                  ctl.getNxtmfp() - 1, wasNew);
        } else { // Grava no mesmo local da versao anterior do registro.
//System.out.println("grava no mesmo local");
            writeRecord(ctl, record, bl, pos, wasNew);
        }

        return mfn;
    }

    /*    
    private int updateRecord(final Control ctl,
                             final Record record) throws BrumaException {
        assert (ctl != null) : "updateRecord/null control";
        assert (record != null) : "updateRecord/null record";

        final Record.Status status[] = new Record.Status[1];
        final Record.ActiveStatus actStatus[] = new Record.ActiveStatus[1];
        final int mfn = record.getMfn();
        final int nxtmfn = ctl.getNxtmfn();
        final long fpos = getMasterPosition(mfn, status, actStatus);

        // Atualização de registro já apagado.
        if (status[0] != Record.Status.ACTIVE) {
            //throw new BrumaException("updateRecord/record is deleted");
            int x = 0;
        }

        final int bl = (int)((fpos / XrfFile.XRF_BLOCKSIZE) + 1);
        final int pos = (int)(fpos % XrfFile.XRF_BLOCKSIZE);
        final Leader leader = readLeader(fpos);
        boolean wasNew = false;

        // Aponta para onde estava apontando a versao anterior.
        record.setBlockNumber(leader.getMfbwb());
        record.setBlockPos(leader.getMfbwp());

        if (record.getStatus() == Record.Status.ACTIVE) {
            if (actStatus[0] == Record.ActiveStatus.PENDING) {
                record.setActiveStatus(Record.ActiveStatus.PENDING);
            } else if (actStatus[0] == Record.ActiveStatus.NEW) {
                record.setActiveStatus(Record.ActiveStatus.NEW);
                wasNew = true;
            } else { // NORMAL
                record.setActiveStatus(Record.ActiveStatus.PENDING);
            }
        } else if (actStatus[0] == Record.ActiveStatus.NEW) {
            wasNew = true;
        }
        
        if (mfn == nxtmfn-1) { // Ultimo registro da base.
            // Grava no mesmo local da versao anterior do registro.
            writeRecord(ctl, record, bl, pos, wasNew);
        } else if (leader.getMfrl() < record.getRecordLength(encoding, FFI)) {
            // Grava no final do arquivo master.
//System.out.println("grava no final");
            writeRecord(ctl, record, ctl.getNxtmfb(),
                                                  ctl.getNxtmfp() - 1, wasNew);
        } else { // Grava no mesmo local da versao anterior do registro.
//System.out.println("grava no mesmo local");
            writeRecord(ctl, record, bl, pos, wasNew);
        }

        return mfn;
    }    
    */
    
    private int adjustFilePos(final int fpos) {
        assert fpos > 0;

        int nfpos = fpos;

        if (shift > 0) {
            int vtot = 1;

            for (int pow = 1; pow <= shift; pow++) {
                vtot *= 2;
            }
            vtot--;
            while ((nfpos & vtot) != 0) {
                nfpos++;
            }
        }

        return nfpos;
    }

    /**
     * Writes a record into a database.
     * @param record Isis record
     * @exception BrumaException
     * @return record master file number
     */
    @Override
    public int writeRecord(final Record record) throws BrumaException {
        final Control ctl;
        final int nxtmfn;
        final int len;
        int mfn;
        Lock.RecordLock recLock = null;
        SegmentLock sl = null;
        boolean del = false; //data entry lock
        boolean rl = false;  //record lock

        if (raf == null) {
            throw new BrumaException("writeRecord/master file is not opened");
        }
        if (record == null) {
            throw new BrumaException("writeRecord/null record");
        }
        /*if (record.getStatus() != Record.Status.ACTIVE) {
            throw new BrumaException("writeRecord/record status is not " +
                                        "active: " + record.getStatus());
        }*/

        record.setShift(shift);
        record.setFiller(filler);
        len = record.getRecordLength(encoding, FFI);

        reallocBuffer(len);
        mfn = record.getMfn();
        if (mfn < 0) {
            throw new BrumaException("writeRecord/id out of range < 0");
        }

        if (lock != null) {
            lock.setDataEntryLock();
            del = true;
            sl = lock.lockSegment(0, CONTROL_SIZE, false);
        }
        try {
            ctl = getControlRecord();
            //ctl.setNxtmfp(ctl.getNxtmfp() - 1);
            // ajuste pois posicao de arquivo comeca de 0.

            nxtmfn = ctl.getNxtmfn();

            if ((mfn == 0) || (mfn >= nxtmfn)) {  // Registro novo
                mfn = newRecord(ctl, record);
            } else { // Registro existente.
                if (lock != null) {
                    recLock = lock.lockRecord(mfn);
                    rl = true;
                }
                mfn = updateRecord(ctl, record, true);
            }
        } finally {
            if (lock != null) {
                if (del) {
                    if (sl != null) {
                        lock.releaseLockSegment(sl);
                    }
                    if (rl) {
                        lock.unlockRecord(recLock);
                    }
                    lock.resetDataEntryLock();
                }
            }
        }
        return mfn;
    }

    /**
     * Deletes an Isis record.
     * @param mfn - master file number of the record.
     * @exception BrumaException
     */
    @Override
    public void deleteRecord(final int mfn) throws BrumaException {
        if (raf == null) {
            throw new BrumaException("deleteRecord/master file is not opened");
        }
        if (mfn <= 0) {
            throw new BrumaException("deleteRecord/mfn <= 0");
        }

        final Record rec;
        Lock.RecordLock[] recLock = null;
        boolean delock = false;
        boolean rlock = false;

        try {
            if (lock == null) {
                rec = getRecord(mfn);
            } else {
                recLock = new Lock.RecordLock[1];
                lock.setDataEntryLock();
                delock = true;
                rec = getLockRecord(mfn, recLock);
                rlock = true;
            }
            if (rec.getStatus() != Record.Status.ACTIVE) {
                throw new BrumaException("deleteRecord/record status is not " +
                                        "active: " + rec.getStatus());
            }
            rec.setStatus(Record.Status.LOGDEL);
            rec.setActiveStatus(null);
            writeRecord(rec);
        } finally {
            if (lock != null) {
                if (rlock) {
                    unlockRecord(recLock[0]);
                }
                if (delock) {
                    lock.resetDataEntryLock();
                }
            }
        }
    }

    @Override
    public List<Integer> regExpSearch(final String expression,
                                      final int maxHits) throws BrumaException {
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
     * Creates a new empty record.
     * @return Isis record
     * @exception BrumaException
     */
    @Override
    public Record newRecord() throws BrumaException {
        return new Record(shift, filler);
    }

    /**
     * Sets a map of tags and its names
     * @param tags tags and tags names association
     */
    public void setTags(final Map<Integer,String> tags) {
        this.tags = tags;
        this.stags = new HashMap<String,Integer>();

        for (Map.Entry<Integer,String> entry : tags.entrySet()) {
            stags.put(entry.getValue(), entry.getKey());
        }
        if (rbb != null) {
            rbb.setTags(tags, stags);
        }
    }

    public Map<Integer,String> getTags() {
        return tags;
    }

    public Map<String,Integer> getSTags() {
        return stags;
    }
}

/*=========================================================================

    Copyright © 2012 BIREME/PAHO/WHO

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
import bruma.master.XrfFile.XrfInfo;
import bruma.utils.ISO8859OrIBM850;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

/* OBS - Se o valor de campo nxtmfp do registro de controle para uma base com
         R registros contem o valor X, entao quando a base tiver R+1 registros,
         xref referente ao registro R+1 apontara para a posicao X-1 */

/**
 * The Master class represents an Isis master file.
 * @author Heitor Barbieri
 */
public class RecriateXrf {
    static final int MSNVSPLT = 20;  // cisis em ffi
    static final int MF_BLOCKSIZE = 512;

    private static final int CONTROL_SIZE = 32;
    private static final int LEADER_SIZE = 26;

    private final byte[] page;
    private MasterPlatformInfo info;
    private String dbName;
    private String encoding;
    private boolean swapped;
    private int filler;
    private int neverSplitRec;
    private RandomAccessFile raf;
    private FileChannel fc;
    private XrfFile xrf;
    private boolean FFI;
    private int sysfile;
    private int shift;
    private int maxmfrl;
    private ByteBuffer bBuffer;

    /**
     * The constructor of the Master class returned by the MasterFactory object.
     * @param mpi internal information about the database to be opened or created.
     * @throws BrumaException
     */
    RecriateXrf(final MasterPlatformInfo mpi) throws BrumaException {
        assert mpi != null;
        final int ffiSize;
        final int align = mpi.getDataAlignment();

        info = mpi;
        page = new byte[MF_BLOCKSIZE];
        java.util.Arrays.fill(page, (byte) 0);
        dbName = mpi.getMstName();
        encoding = mpi.getEncoding();
        swapped = mpi.isSwapped();
        filler = (align == 0) ? 0 : 2;
        raf = null;
        FFI = mpi.isFfi();
        ffiSize = FFI ? 2 : 0;
        neverSplitRec =
                    MF_BLOCKSIZE - (4 + ffiSize + filler + 4 + 2 + ffiSize) - 1;
        sysfile = 0;
        shift = mpi.getShift();
        reallocBuffer(CONTROL_SIZE);
    }

    final void reallocBuffer(final int newSize) throws BrumaException {
        assert newSize > 0;

        if (maxmfrl < newSize) {
            if (newSize < MSNVSPLT) {
                throw new BrumaException("mfrl[" + newSize + "] <" + MSNVSPLT);
            }
            if (newSize > Master.MAXMFRL_POSSIBLE) {
                throw new BrumaException("mfrl[" + newSize + "] >"
                                                    + Master.MAXMFRL_POSSIBLE);
            }
            if (!FFI && (newSize > Master.MAXMFRL_ISIS)) {
                throw new BrumaException("mfrl[" + newSize + "] >"
                                                        + Master.MAXMFRL_ISIS);
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
     * Opens an existing Isis database.
     * @return this Master object
     * @throws BrumaException
     */
    void open() throws BrumaException {
        assert info != null;
        try {
            final String mode;
            final Control ctl;
            final DbType dbt;
            final DbType.Type type;
            final int ffiSize;
            File file = new File(dbName + ".mst");
            File xfile = new File(dbName + ".xrf");

            if (!file.exists()) {
                    throw new BrumaException("open/missing master file : "
                                                                     + dbName);
            }
            if (xfile.exists()) {
                xfile.delete();
            } else {
                xfile = new File(dbName + ".XRF");
                if (xfile.exists()) {
                    xfile.delete();
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
            if (encoding.equals(Master.GUESS_ISO_IBM_ENCODING)) {
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
            xrf = new XrfFile(dbName + ".xrf", shift, swapped, true);
            if (ctl.getNxtmfn() == 1) {  // Empty master
                FFI = false;
                filler = (System.getProperty("os.name").startsWith("Win"))
                                                                       ? 0 : 2;
                info.setDataAlignment(filler);
            } else {
                type = dbt.getType(adjustFilePos(2 * CONTROL_SIZE));
                if (!type.recognized) {
                    throw new IOException(
                                  "open/could not recognize master file info");
                }
                FFI = !type.IsisStandard;
                filler = type.align == 0 ? 0 : 2;
                info.setDataAlignment(type.align);
            }
            info.setFfi(FFI);
            ffiSize = FFI ? 2 : 0;
            neverSplitRec =
                    MF_BLOCKSIZE - (4 + ffiSize + filler + 4 + 2 + ffiSize) - 1;
            reallocBuffer(FFI ? Master.DEFMFRL_FFI : Master.MAXMFRL_ISIS);
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
    }

    /**
     * Closes frees all database used resources.
     * @throws BrumaException
     */
    void close() throws BrumaException {
        try {
            if (raf != null) {
                raf.close();
                raf = null;
            }
            if (xrf != null) {
                xrf.close();
                xrf = null;
            }
        } catch (IOException ioe) {
            throw new BrumaException(ioe.toString());
        }
    }

    /**
     * Reads the master control record.
     * @return the control record object
     * @throws org.bruma.BrumaException
     */
    Control getControlRecord() throws BrumaException {
        final Control ctl;

        if (raf == null) {
            throw new BrumaException("master file is not opened");
        }
        try {
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
        }
        
        return ctl;
    }

    private int readInt(final ByteBuffer buffer) throws IOException {
        assert (buffer != null) : "readInt/null ByteBuffer";

        return (FFI ? buffer.getInt() : buffer.getShort());
    }

    private Leader readLeader(final long position) throws BrumaException {
        assert (position >= 0) : "readLeader/position[" + position + "] < 0";

        final Leader ret = new Leader();

        try {
            bBuffer.clear();
            bBuffer.limit(LEADER_SIZE);
            if (fc.read(bBuffer, position) != LEADER_SIZE) {
                throw new BrumaException("read error");
            }
            bBuffer.rewind();

            ret.setMfn(bBuffer.getInt());
            ret.setMfrl(Math.abs(readInt(bBuffer)));
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
            final Record.Status status = ((bBuffer.getShort() == 0) 
                                                ? Record.Status.ACTIVE
                                                : Record.Status.LOGDEL);
            ret.setStatus(status);
            
            reallocBuffer(ret.getMfrl());
        } catch (IOException ioex) {
            throw new BrumaException(ioex.toString());
        }

        return ret;
    }
    
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


    private void recriate(final int tell) throws BrumaException {
        if (tell <= 0) {
            throw new IllegalArgumentException("tell <= 0");
        }
        
        final long fsize;
        long fpos = adjustFilePos(2 * CONTROL_SIZE);
        int lastMfn = 0;
                
        open();
        
        try {
            fsize = raf.length();
        } catch (IOException ioe) {
            throw new BrumaException(ioe);
        }
                        
        while (true) {
            if (fpos >= (fsize - LEADER_SIZE)) {
                break;
            }
            
            final Leader leader = readLeader(fpos);
            int block = (int)((fpos / XrfFile.XRF_BLOCKSIZE) + 1);
            int offset = (int)(fpos % XrfFile.XRF_BLOCKSIZE);
            int mfn = leader.getMfn();
            
            if (mfn == 0) {
                break;
            }
            
            // Escreve registros fisicamente apagados.
            for (int auxMfn = lastMfn+1; auxMfn < mfn; auxMfn++) {
                xrf.writeXrfInfo(xrf.new XrfInfo(auxMfn,
                                                     -1,
                                                     0,
                                                     Record.Status.PHYDEL,
                                                     Record.ActiveStatus.NEW));
            }
            if (mfn % tell == 0) {
                System.out.println("+++" + mfn);
            }

            // Soma 1024 para indicar que a atualizacao inv esta 
            // pendente e que e novo.
            xrf.writeXrfInfo(xrf.new XrfInfo(mfn,
                                             block,
                                             (offset + 1024),
                                             Record.Status.ACTIVE,
                                             Record.ActiveStatus.NEW));

            fpos += leader.getMfrl();
            
            block = (int)((fpos / XrfFile.XRF_BLOCKSIZE) + 1);
            offset = (int)(fpos % XrfFile.XRF_BLOCKSIZE);
            if (offset >= neverSplitRec) {
                block++;
                offset = 1;
                fpos = ((long)(block - 1) * MF_BLOCKSIZE) + offset;
            }
            lastMfn = mfn;
        }
        
        close();
    }
    
    private static void usage() {
        System.err.println("usage: RecriateXrf <mst> [<tell>]");
        System.exit(1);
    }
    
    public static void main(final String[] args) throws BrumaException {
        if (args.length < 1) {
            usage();
        }
        final int tell = (args.length > 1) ? Integer.parseInt(args[1]) 
                                           : Integer.MAX_VALUE;
        final MasterPlatformInfo info = new MasterPlatformInfo(args[0]);

        info.setEncoding("ISO8859-1");
        
        new RecriateXrf(info).recriate(tell);
    }
}

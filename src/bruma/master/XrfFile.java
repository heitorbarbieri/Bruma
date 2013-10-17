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
import bruma.master.Record.ActiveStatus;
import bruma.master.Record.Status;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.Arrays;

//  bits 01 a 11 - deslocamento, sendo bit 10 (precisa atualizar) e bit 11 (novo
//                 registro)
//  bits 12 a 32 - bloco. Valor negativo indica registro apagado.
// O numero de bits do bloco pode aumentar conforme o valor do shift, que retira
// bits do deslocamento e os passa para o bloco.
public class XrfFile {
    private final static int MF_BLOCKSIZE = 512;
    final static int XRF_BLOCKSIZE = 512;
    final static int XRF_PTRSIZE = 4; // size of packed pointer info in xrf file
    final static int XRF_NRINBUFFER = 127;  // number of xrf_pointers in each block
    private final static int XRF_BUFSIZE = (XRF_NRINBUFFER + 1) * XRF_PTRSIZE;

    private final byte[] page;
    final int shift;
    final FileChannel fc;

    private RandomAccessFile raf;
    ByteBuffer bBuffer;
    boolean swapped;

    public class XrfInfo {
        private int mfn;
        private int block;
        private int offset;
        private Record.Status status;
        private Record.ActiveStatus actStatus;

        XrfInfo(final int mfn,
                final int block,
                final int offset,
                final Record.Status status,
                final Record.ActiveStatus actStatus) {
            assert mfn > 0;

            this.mfn = mfn;
            this.block = block;
            this.offset = offset;
            this.status = status;
            this.actStatus = actStatus;
        }

        public void setBlock(final int block) {
            this.block = block;
        }

        public int getBlock() {
            return block;
        }

        public int getMfn() {
            return mfn;
        }

        public void setMfn(final int mfn) {
            this.mfn = mfn;
        }

        public void setOffset(final int offset) {
            this.offset = offset;
        }

        public int getOffset() {
            return offset;
        }

        public ActiveStatus getActStatus() {
            return actStatus;
        }

        public void setActStatus(final ActiveStatus actStatus) {
            this.actStatus = actStatus;
        }

        public void setStatus(final Status status) {
            this.status = status;
        }

        public Status getStatus() {
            return status;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();

            sb.append("mfn=");
            sb.append(mfn);
            sb.append(" block=");
            sb.append(block);
            sb.append(" offset=");
            sb.append(offset);
            sb.append(" status=");
            sb.append(status);
            sb.append(" active_status=");
            sb.append(actStatus);

            return sb.toString();
        }
    }

    XrfFile(final String dbasename,
            final int shift,
            final boolean swapped,
            final boolean create) throws BrumaException {

        if (dbasename == null) {
            throw new BrumaException("XrfFile/null dbname");
        }
        if (shift < 0) {
            throw new BrumaException("XrfFile/invalid shift[" + shift
                                                                   + "] value");
        }

        this.shift = shift;
        this.swapped = swapped;
        page = new byte[XRF_BLOCKSIZE];
        bBuffer = ByteBuffer.allocate(XRF_BLOCKSIZE)
        //bBuffer = ByteBuffer.allocateDirect(XRF_BLOCKSIZE)
                                .order(swapped ? ByteOrder.LITTLE_ENDIAN :
                                                 ByteOrder.BIG_ENDIAN);

        try {
            final File file = new File(dbasename);

            if (file.exists() && file.isFile()) {
                if (file.length() < XRF_BLOCKSIZE) {
                    throw new BrumaException("XrfFile/XRF_BLOCKSIZE");
                }
            } else {
                if (!create) {
                    throw new BrumaException("XrfFile/missing "
                                                       + dbasename + " file");
                }
            }
            final String mode = ((!file.isFile() || file.canWrite()) ?
                                                                    "rw": "r");
            Arrays.fill(page, (byte) 0);
            raf = new RandomAccessFile(file, mode);
            fc = raf.getChannel();
            if (create) {
                reset();
            }
        } catch (IOException ioe) {
            try {
                if (raf != null) {
                    raf.close();
                    raf = null;
                }
            } catch(IOException ioe2) {
                throw new BrumaException("XrfFile/" + ioe2.getMessage());
            }
            throw new BrumaException("XrfFile/" + ioe.getMessage());
        }
    }

    void close() throws BrumaException {
        try {
            if (raf != null) {
                raf.close();
                raf = null;
            }
        } catch(IOException ioe) {
            throw new BrumaException("close/" + ioe.getMessage());
        }
    }

    void reset() throws BrumaException {
        try {
            raf.setLength(0);
            bBuffer.clear();
            bBuffer.limit(XRF_BLOCKSIZE);
            bBuffer.putInt(-1);
            bBuffer.put(page, 0, (XRF_BLOCKSIZE - XRF_PTRSIZE));
            bBuffer.rewind();
            if (fc.write(bBuffer, 0) != XRF_BLOCKSIZE) {
                throw new IOException("reset/write error");
            }
        } catch(IOException ioe) {
            throw new BrumaException("reset/" + ioe.getMessage());
        }
    }

    public XrfInfo readXrfInfo(final int mfn) throws BrumaException {
        if (mfn <= 0) {
            throw new BrumaException("readXrfInfo/mfn <= 0");
        }

        final int packedMfp;

        try {
            bBuffer.clear();
            bBuffer.limit(4);
            if (fc.read(bBuffer, calcXrfPos(mfn)) != 4) {
                throw new IOException("reset/read error");
            }
            bBuffer.rewind();
            packedMfp = bBuffer.getInt();
            /*ByteBuffer auxBuffer = ByteBuffer.allocate(12)
                                .order(swapped ? ByteOrder.LITTLE_ENDIAN :
                                                 ByteOrder.BIG_ENDIAN);
            long pos = calcXrfPos(mfn);
            fc.position(calcXrfPos(mfn));
            auxBuffer.clear();
            fc.read(auxBuffer);
            auxBuffer.rewind();
            packedMfp = auxBuffer.getInt();
            int i2 = auxBuffer.getInt();
            int i3 = auxBuffer.getInt();
            int i4 = 0;*/
        } catch(IOException ioe) {
            throw new BrumaException("getXrfInfo/" + ioe.getMessage());
        }
//System.out.println("mfn=" + mfn + " packedMfp=" + packedMfp);
        return readXrfInfoAux(mfn, packedMfp);
    }

    protected XrfInfo readXrfInfoAux(final int mfn,
                                     final int pckMfn) throws BrumaException {
        if (mfn <= 0) {
            throw new BrumaException("readXrfInfoAux/mfn <= 0");
        }
        if (pckMfn == 0) {
            throw new BrumaException("readXrfInfoAux/pckMfn == 0");
        }
        final Record.Status status;
        final int omax = (2 << 9) - 1;  // 1 bit = new and 1 bit = pending
        final int rshift = (11 - shift);
        final boolean pending;
        final boolean isNew;

        Record.ActiveStatus actStatus = null;
        int block;
        int offset;
        int packedMfp = pckMfn;
        boolean positiveBlock = true;

        if (packedMfp < 0) {
            packedMfp *= -1;
            positiveBlock = false;
        }

        block = packedMfp >>> rshift;
        offset = packedMfp & (omax >>> shift);
        offset = (offset << shift);
        pending = (packedMfp & (0x600 >>> shift)) > 0;
        isNew = (packedMfp & (0x400 >>> shift)) > 0;

        if (block == 0) {
            throw new BrumaException("getXrfInfo/XRF_FIL_BAD");
        } else if (positiveBlock) {
            status = Record.Status.ACTIVE;
            if (isNew) {
                actStatus = Record.ActiveStatus.NEW;
            } else if (pending) {
                actStatus = Record.ActiveStatus.PENDING;
            } else {
                actStatus = Record.ActiveStatus.NORMAL;
            }
        } else { // BL < 0
            if ((block == 1) && (offset == 0)) {
                status = Record.Status.PHYDEL;
            } else {
                // strangely enough logically del
                status = Record.Status.LOGDEL;
            }
        }

        //mfpos = ((Math.abs(block) - 1) * MF_BLOCKSIZE) + (offset & 0X1ff);

        return new XrfInfo(mfn, block, offset, status, actStatus);
    }

    public int writeXrfInfo(final XrfInfo info) throws BrumaException {
        if (info == null) {
            throw new BrumaException("writeXrfInfo/null info");
        }

        final long block = Math.abs(info.block);
        //final int bmax = (2 << (21 + shift - 1));   // valor maximo do bloco
        //final int bmax = (1 << (21 + shift)) - 1;   // valor maximo do bloco
        final int bmax = (1 << (20 + shift)) - 1;   // valor maximo do bloco
        final int lshift = (11 - shift);  // left shift
        int packedMfp;

        if (info.mfn <= 0) {
            throw new BrumaException("writeMstPos/mfn <= 0");
        }
        if ((info.offset < 0) || (info.offset >= 2048)) {
            throw new BrumaException("writeMstPos/invalid offset[" + info.offset
                                                            + "] value");
        }
        if (block > bmax) {
            throw new BrumaException("writeMstPos/block[" + info.block + "] > "
             + bmax + ". Try increasing the master max size (shift) parameter");
        }

        try {
            final int blk = ((info.mfn - 1) / XRF_NRINBUFFER);
            final int pos = (((info.mfn - 1) % XRF_NRINBUFFER) * XRF_PTRSIZE
                                                                + XRF_PTRSIZE);
            final int len = (int) raf.length();
            final int lblk = (((len / XRF_PTRSIZE) / (XRF_NRINBUFFER + 1)) - 1);
            final long blkPos = blk * XRF_BUFSIZE;
            final long filePos = blkPos + pos;

            if (blk > (lblk + 1)) {
                throw new BrumaException(
                        "writeMstPos/illegal argument id value = " + info.mfn);
            }
            final long nblock = (block << lshift);

            packedMfp = (int)(nblock + (info.offset >>> shift));
            if (packedMfp < 0) {
/*System.err.println("bmax=" + bmax);
System.err.println("block=" + block);
System.err.println("lshift=" + lshift);
System.err.println("info.offset=" + info.offset);
System.err.println("shift=" + shift);
System.err.println("(block << lshift)=" + (block << lshift));
System.err.println("(info.offset >>> shift)" + (info.offset >>> shift));
System.err.println("nblock=" + nblock);
System.err.println("packedMfp=" + packedMfp);*/
                throw new BrumaException("writeMstPos/packedMfp < 0");
            }
            if (info.block < 0) {
                packedMfp = -packedMfp;
            }

            if (pos == (MF_BLOCKSIZE - XRF_PTRSIZE)) {// Ultima posicao do bloco
                if (blk == lblk) {
                    // Troca numero numero negativo do bloco (ultimo)
                    // para numero do bloco
                    bBuffer.clear();
                    bBuffer.limit(4);
                    bBuffer.putInt(blk + 1);
                    bBuffer.rewind();
                    if (fc.write(bBuffer, blkPos) != 4) {
                        throw new IOException("writeMstPos/1/write error");
                    }
                }

                // Escreve ultimo endereco no bloco
                bBuffer.clear();
                bBuffer.limit(4);
                bBuffer.putInt(packedMfp);
                bBuffer.rewind();
                if (fc.write(bBuffer, filePos) != 4) {
                    throw new IOException("writeMstPos/2/write error");
                }

                if (blk == lblk) {
                    // Escreve numero negativo do proximo bloco (ultimo)
                    bBuffer.clear();
                    bBuffer.limit(4 + XRF_BUFSIZE - XRF_PTRSIZE);
                    bBuffer.putInt(-1 * (blk + 2));
                    bBuffer.put(page, 0, (XRF_BUFSIZE - XRF_PTRSIZE));
                    bBuffer.rewind();
                    if (fc.write(bBuffer, filePos + 4)
                                            != (4 + XRF_BUFSIZE - XRF_PTRSIZE)) {
                        throw new IOException("writeMstPos/3/write error");
                    }
                }
            } else {
                bBuffer.clear();
                bBuffer.limit(4);
                bBuffer.putInt(packedMfp);
                bBuffer.rewind();
                if (fc.write(bBuffer, filePos) != 4) {
                    throw new IOException("writeMstPos/4/write error");
                }
            }
        } catch(IOException ioe) {
            throw new BrumaException("writeMstPos/" + ioe.getMessage());
        }

        return packedMfp;
    }

    private int calcXrfPos(final int mfn) throws BrumaException {
        assert (mfn > 0);

        final int quot = ((mfn - 1) / XRF_NRINBUFFER); //mfn starting with 1
        final int rem = ((mfn - 1) % XRF_NRINBUFFER);
        final int pos = ((quot * XRF_BLOCKSIZE) + ((rem + 1) * XRF_PTRSIZE));
//System.out.println("xrf offset para mfn=" +  mfn + " e : " +  offset);
        try {
            if (pos > raf.length()) {
                throw new BrumaException("calcXrfPos/id[" + mfn + "] too big");
            }
        } catch(IOException ioe) {
            throw new BrumaException("calcXrfPos/" + ioe.getMessage());
        }

        return pos;
    }
}

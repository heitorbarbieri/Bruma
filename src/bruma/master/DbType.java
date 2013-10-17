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
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

/**
 * Esta classe tenta reconhecer caracteristicas da base de dados tais como:
 * isis/ffi, big/litle endian e alinhamento de memoria.
 * @author Heitor Barbieri
 */
class DbType {
    class Type {
        boolean recognized;
        boolean IsisStandard;
        boolean swapped;
        int align;
    }

    private final FileChannel fc;
    private final boolean swapped;
    private ByteBuffer bBuffer;

    DbType(final FileChannel fc) throws IOException {
        assert fc != null;

        int block;
        int pos;
        long fpos;
        final long size = fc.size();

        this.fc = fc;
        bBuffer = ByteBuffer.allocateDirect(22).order(ByteOrder.LITTLE_ENDIAN);
        bBuffer.clear();
        bBuffer.limit(6);
        fc.position(8);
        if (fc.read(bBuffer) != 6) {
            throw new IOException("DbType/swapped read error");
        }
        bBuffer.rewind();
        block = bBuffer.getInt();
        if (block < 1) {
            throw new IOException("DbType/block[" + block + "] < 1");
        }
        pos = bBuffer.getShort();
        if (pos < 1) {
            throw new IOException("DbType/pos[" + pos + "] < 1");
        }
        //fpos = ((block > 0) ? ((long)(block - 1) * 512) : 0) + pos - 1;
        fpos = (long)(pos == 1 ? (block - 1) : block) * 512;
//System.out.println("bloco=" + block + " pos=" + pos + " fpos=" + fpos);        
        //if (size == fpos) {
        //if ((size >= fpos - 512) && (size <= fpos + 512)) {
        if ((size >= fpos - 1024) && (size <= fpos + 1024)) {
            swapped = true;
        } else {
            bBuffer =
                  ByteBuffer.allocateDirect(22).order(ByteOrder.BIG_ENDIAN);
            bBuffer.clear();
            bBuffer.limit(6);
            fc.position(8);
            if (fc.read(bBuffer) != 6) {
                throw new IOException("DbType/swapped read error");
            }
            bBuffer.rewind();
            block = bBuffer.getInt();
            pos = bBuffer.getShort();
            //fpos = ((block > 0) ? ((long)(block - 1) * 512) : 0) + pos - 1;
            //if (size == fpos) {
            fpos = (long)(pos == 1 ? (block - 1) : block) * 512;
            if ((size >= fpos - 512) && (size <= fpos + 512)) {
                swapped = false;
            } else {
                throw new IOException("DbType/invalid master block number");
            }
        }
    }

    boolean isSwapped() {
        return swapped;
    }

    Type getType(final XrfFile xrf) throws BrumaException, IOException {
        assert xrf != null;

        final Type ret = new Type();
        final XrfFile.XrfInfo info =
                xrf.new XrfInfo(1, 0, 0, Record.Status.ACTIVE,
                                         Record.ActiveStatus.NORMAL);
        long position;

        info.setMfn(0);

        while (!ret.recognized) {
            info.setMfn(info.getMfn() + 1);
            position = getNextRecPos(xrf, info);
            if (position == -1) {
                break;
            }
            bBuffer.clear();
            fc.position(position);
            if (fc.read(bBuffer) != 22) {
                throw new IOException("swapped read error");
            }
            if (readIsisAlign0(bBuffer)) {
                ret.recognized = true;
                ret.IsisStandard = true;
                ret.align = 0;
            } else if (readIsisAlign2(bBuffer)) {
                ret.recognized = true;
                ret.IsisStandard = true;
                ret.align = 2;
            } else if (readFfiAlign0(bBuffer)) {
                ret.recognized = true;
                ret.IsisStandard = false;
                ret.align = 0;
            } else if (readFfiAlign2(bBuffer)) {
                ret.recognized = true;
                ret.IsisStandard = false;
                ret.align = 2;
            }            
        }
        ret.swapped = swapped;

        return ret;
    }

    Type getType(final long position) throws BrumaException, IOException {
        assert position > 0;

        final Type ret = new Type();

        bBuffer.clear();
        fc.position(position);
        if (fc.read(bBuffer) != 22) {
            throw new IOException("swapped read error");
        }
        if (readIsisAlign0(bBuffer)) {
            ret.recognized = true;
            ret.IsisStandard = true;
            ret.align = 0;
        } else if (readIsisAlign2(bBuffer)) {
            ret.recognized = true;
            ret.IsisStandard = true;
            ret.align = 2;
        } else if (readFfiAlign0(bBuffer)) {
            ret.recognized = true;
            ret.IsisStandard = false;
            ret.align = 0;
        } else if (readFfiAlign2(bBuffer)) {
            ret.recognized = true;
            ret.IsisStandard = false;
            ret.align = 2;
        }            
        ret.swapped = swapped;

        return ret;
    }
    
    private long getNextRecPos(final XrfFile xrf,
                               final XrfFile.XrfInfo xinfo)
                                                         throws BrumaException {
        assert xrf != null;
        assert xinfo.getMfn() > 0;

        long position = -1;
        XrfFile.XrfInfo auxInfo;
        int lastMfn;

        try {            
            bBuffer.clear();
            bBuffer.limit(4);
            fc.position(4);
            if (fc.read(bBuffer) != 4) {
                throw new BrumaException("lastMfn read error");
            }
            bBuffer.rewind();
            lastMfn = bBuffer.getInt() - 1;
            assert lastMfn >= 0;
        } catch(IOException ioe) {
            throw new BrumaException(ioe);
        }

        for (int mfn = xinfo.getMfn(); mfn <= lastMfn; mfn++) {
            auxInfo = xrf.readXrfInfo(mfn);
            if (auxInfo.getStatus() != Record.Status.PHYDEL) {
                long auxPos = (Math.abs(auxInfo.getBlock()) - 1);
                auxPos *= Master.MF_BLOCKSIZE;
                position = auxPos + (auxInfo.getOffset() & 0X1ff);
                xinfo.setActStatus(auxInfo.getActStatus());
                xinfo.setBlock(auxInfo.getBlock());
                xinfo.setOffset(auxInfo.getOffset());
                xinfo.setStatus(auxInfo.getStatus());
                break;
            }
        }

        return position;
    }

    private boolean readIsisAlign0(final ByteBuffer bBuffer) throws IOException,
                                                                BrumaException {
        assert bBuffer != null;

        final int base;
        final int nvf;
        final int auxBase;
        boolean ret;

        bBuffer.rewind();
        bBuffer.getInt(); // mfn
        bBuffer.getShort(); // mfrl
        bBuffer.getInt(); // mfbwb
        bBuffer.getShort(); // mfbwp

        base = bBuffer.getShort();
        nvf = bBuffer.getShort();

        //if (nvf > 0) { registro pode nao ter campos
            auxBase = 18 + (nvf * 6);
            ret = (base == auxBase);
        //}

        return ret;
    }

    private boolean readIsisAlign2(final ByteBuffer bBuffer) throws IOException,
                                                                BrumaException {
        assert bBuffer != null;

        final int base;
        final int nvf;
        final int auxBase;
        boolean ret;

        bBuffer.rewind();
        bBuffer.getInt(); // mfn
        bBuffer.getShort(); // mfrl
        bBuffer.getShort(); // filler
        bBuffer.getInt(); // mfbwb
        bBuffer.getShort(); // mfbwp
        base = bBuffer.getShort();
        nvf = bBuffer.getShort();

        //if (nvf > 0) { registro pode nao ter campos
            auxBase = 18 + 2 + (nvf * 6);
            ret = (base == auxBase);
        //}

        return ret;
    }

    private boolean readFfiAlign0(final ByteBuffer bBuffer) throws IOException,
                                                                BrumaException {
        assert bBuffer != null;

        final int base;
        final int nvf;
        final int auxBase;
        boolean ret;

        bBuffer.rewind();
        bBuffer.getInt(); // mfn
        bBuffer.getInt(); // mfrl
        bBuffer.getInt(); // mfbwb
        bBuffer.getShort(); // mfbwp
        base = bBuffer.getInt();
        nvf = bBuffer.getShort();

        //if (nvf > 0) { registro pode nao ter campos
            auxBase = 22 + (nvf * 10);
            ret = (base == auxBase);
        //}

        return ret;
    }

    private boolean readFfiAlign2(final ByteBuffer bBuffer) throws IOException,
                                                                BrumaException {
        assert bBuffer != null;

        final int base;
        final int nvf;
        final int auxBase;
        boolean ret;

        bBuffer.rewind();
        bBuffer.getInt(); // mfn
        bBuffer.getInt(); // mfrl
        bBuffer.getInt(); // mfbwb
        bBuffer.getShort(); // mfbwp
        bBuffer.getShort(); // filler
        base = bBuffer.getInt();
        nvf = bBuffer.getShort();

        //if (nvf > 0) { registro pode nao ter campos
            auxBase = 22 + 2 + (nvf * (10 + 2));
            ret = (base == auxBase);
        //}

        return ret;
    }
}
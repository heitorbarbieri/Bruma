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
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author Heitor Barbieri
 * @date 09/06/2008
 */
class RecordByteArray {
    private final boolean isFFI;
    private final boolean swapped;
    private final int filler;  // 2 bytes para ajuste de alinhamento da estrutura M1STRU
    private final int shift;
    private byte[] buffer;
    private final int size;

    RecordByteArray(final boolean isFFI,
                    final boolean swapped,
                    final int filler,
                    final int shift) {
        assert (filler == 0) || (filler == 2);
        assert shift >= 0;

        this.isFFI = isFFI;
        this.swapped = swapped;
        this.filler = filler;
        this.shift = shift;
        buffer = new byte[isFFI ? Master.DEFMFRL_FFI : Master.MAXMFRL_ISIS];
        size = isFFI ? 4 : 2;
    }

    Record fromByteArray(final byte[] rec,
                         final String encoding) throws BrumaException {
        assert rec != null;
        assert encoding != null;
        final Record record = new Record(shift, filler);
        final int status;
        final int base;
        final int nvf;
        Record.Status recStatus = Record.Status.ACTIVE;
        int tag;
        int pos;
        int len;
        int fpos;
        int[] index = new int[1];

        index[0] = 0;

        try {
            int mfn = readInt(rec, index);
            record.setMfn(mfn);
            readShInt(rec, index); // mfrl
//int zz = mfrl;
//zz++;
            if ((!isFFI) && (filler != 0)) {
            //if (filler != 0) {
                index[0] += 2;  //readShort(rec, index);
            }
            int mfbwb = readInt(rec, index);
            record.setBlockNumber(mfbwb);
            int mfbwp = (int)readShort(rec, index);
            record.setBlockPos(mfbwp);
            if (isFFI && (filler != 0)) {
                readShort(rec, index);
            }
            base = readShInt(rec, index);
            nvf = (int)readShort(rec, index);
            status = readShort(rec, index);
/*System.out.println("isFFI=" + isFFI + " filler=" + filler + " mfn=" + mfn +  " mfrl=" + mfrl
 + " mfbwb=" + mfbwb +  " mfbwp=" + mfbwp +  " base=" + base + " nvf="
 + nvf + " status= " + status);*/

            if (status == 0) {
                recStatus = Record.Status.ACTIVE;
            } else if (status == 1) {
                recStatus = Record.Status.LOGDEL;
            } else {
                throw new BrumaException("fromByteArray/invalid status["
                                                                + status + "]");
            }

            record.setStatus(recStatus);

            for (int counter = 0; counter < nvf; counter++) {
                tag = (int)readShort(rec, index);
                if (isFFI && (filler != 0)) {
                    index[0] += 2; //readShort(rec, index);
                }
                pos = readShInt(rec, index);
                len = readShInt(rec, index);
                fpos = base + pos;
//System.out.println("mfn=" + mfn + " tag=" + tag + " pos=" + pos + " len=" + len + " reclen="  + rec.length + " encod=" + encoding);
                try {
                    record.addField(tag, new String(rec, fpos, len, encoding));
                } catch(StringIndexOutOfBoundsException siobe){
                    System.out.println("mfn=" + mfn + " tag=" + tag + " fpos=" +
                            fpos + " pos=" + pos + " len=" + len + " reclen="  +
                            rec.length + " encod=" + encoding);
                }
/*for(int co=0; co<len; co++) {
    byte x = (byte)_rec[fpos+co];
System.out.print("(" + x + "," + (char)x + ")");
}*/
//System.out.println("\n{" + new String(_rec, fpos, len, encoding) + "}");
            }
        } catch (Exception ex) {
            //ex.printStackTrace();
            throw new BrumaException("fromByteArray/" + ex);
        }

        return record;
    }

    byte[] toByteArray(final Record record,
                       final String encoding) throws BrumaException {
        assert record != null;
        assert encoding != null;

        record.setShift(shift);
        record.setFiller(filler);

        final int recLen = record.getRecordLength(encoding, isFFI);
        final int fill = record.getFillSize(recLen);
        final List<Field> fields = record.getFields();
        final int fsize = fields.size();
        final Record.Status recStatus = record.getStatus();
        final short status;
        final byte[] rec = new byte[recLen + fill];
        final int[] index = new int[1];
        int auxIndex = 0;
        byte[] fld;
        Field field;
        int pos = 0;

        index[0] = 0;

//System.out.println("toByteArray");
        try {
            if (buffer.length < recLen) {
                buffer = new byte[recLen];
            }
            status = (short)((recStatus == Record.Status.ACTIVE) ? 0 : 1);
            writeInt(record.getMfn(), rec, index);
            writeShInt((recLen + fill), rec, index);
            if ((!isFFI) && (filler != 0)) {
                writeShort((short)0, rec, index);
            }
            writeInt(record.getBlockNumber(), rec, index);
            writeShort((short)record.getBlockPos(), rec, index);
            if (isFFI && (filler != 0)) {
                writeShort((short)0, rec, index);
            }
/*System.out.println("isFFI=" + isFFI + " size=" + fsize + " filler=" + filler
    + " base=" + ((isFFI ? 22 : 18) + filler
    + (fsize * (isFFI ? (10 + filler) : 6)))); */
            writeShInt(((isFFI ? 22 : 18) + filler +
                       (fsize * (isFFI ? (10 + filler) : 6))), rec, index);
            writeShort((short)fsize, rec, index); // nvf
            writeShort((short)status, rec, index);
//System.out.println("filler=" + filler + " mfn=" + record.getMfn());
//System.out.println("record2 fsize=" + fsize);
            for (int counter = 0; counter < fsize; counter++) {
                field = fields.get(counter);
                fld = field.getContent().getBytes(encoding);
//System.out.println("index[0]=" + index[0] + ":" + fe.tag + "[" + fe.getField() + "]");
                writeShort((short)field.getId(), rec, index);
//System.out.println("index[0]=" + index[0] + ":" + pos);
                if (isFFI && (filler != 0)) {
                    writeShort((short)0, rec, index);
                }
                writeShInt(pos, rec, index);
//System.out.println("index[0]=" + index[0] + ":" + fld.length);
                writeShInt(fld.length, rec, index);
//System.out.println("index[0]=" + index[0]);
                System.arraycopy(fld, 0, buffer, auxIndex, fld.length);
                auxIndex += fld.length;
                pos += fld.length;
            }
//System.out.println("xxxxbuffer len=" + buffer.length + " index[0]=" + index[0] + " auxIndex=" + auxIndex + " recLen=" + rec.length);
            System.arraycopy(buffer, 0, rec, index[0], auxIndex);
            index[0] += auxIndex;

            // Preenche com espacos.
            if (fill > 0) {
                Arrays.fill(rec, index[0], (index[0] + fill), (byte)32);
            }
        } catch (Exception ex) {
            Logger.getGlobal().severe(ex.getMessage());
            throw new BrumaException(ex);
        }
        return rec;
    }

    private short readShort(final byte[] buffer,
                            final int[] index) {
        assert buffer != null;
        assert index != null;
        assert index[0] >= 0;

        final int idx = index[0];

        short ret = 0;

        /* 
        for (int i = 0; i < 2; i++) {
            if (swapped) {
                ret += (buffer[idx + i] & 0x000000FF) << (i * 8);
            } else {
                ret += (buffer[idx + i] & 0x000000FF) << ((2 - i - 1) * 8);
            }
        } */
        if (swapped) {
            ret += (buffer[idx] & 0x000000FF);
            ret += (buffer[idx + 1] & 0x000000FF) << 8;
        } else {
            ret += (buffer[idx] & 0x000000FF) << 8;
            ret += (buffer[idx + 1] & 0x000000FF);
        }

        index[0]+= 2;

        return ret;
    }

    private void writeShort(final short shortVal,
                            final byte[] buffer,
                            final int[] index) {
        assert buffer != null;
        assert index != null;
        assert index[0] >= 0;

        final int idx = index[0];

        if (swapped) {
            buffer[idx] = (byte)(shortVal & 0x00FF);
            buffer[idx + 1] = (byte)((shortVal & 0xFF00) >>> 8);
        } else {
            buffer[idx] = (byte)((shortVal & 0xFF00) >>> 8);
            buffer[idx + 1] = (byte)(shortVal & 0x00FF);
        }

        index[0]+= 2;
    }

    private int readInt(final byte[] buffer,
                        final int[] index) {
        assert buffer != null;
        assert index != null;
        assert index[0] >= 0;

        final int idx = index[0];
        int ret = 0;

        for (int i = 0; i < 4; i++) {
            if (swapped) {
                ret += (buffer[idx + i] & 0x000000FF) << (i * 8);
            } else {
                ret += (buffer[idx + i] & 0x000000FF) << ((4 - i - 1) * 8);
            }
        }

        index[0]+= 4;

        return ret;
    }

    private void writeInt(final int intVal,
                          final byte[] buffer,
                          final int[] index) {
        assert buffer != null;
        assert index != null;
        assert index[0] >= 0;

        int idx = index[0];

        for (int i = 0; i < 4; i++) {
            if (swapped) {
                buffer[idx + i] = (byte)((intVal >>>(i * 8)) & 0x000000FF);
            } else {
                buffer[idx + i] =
                         (byte)((intVal >>> ((4 - i - 1) * 8)) & (0x000000FF));
            }
        }

        index[0]+= 4;
    }

    private int readShInt(final byte[] buffer,
                          final int[] index) {
        //return isFFI ? readInt(buffer, index) : readShort(buffer, index);

        assert buffer != null;
        assert index != null;
        assert index[0] >= 0;

        final int idx = index[0];
        int ret = 0;

        for (int i = 0; i < size; i++) {
            if (swapped) {
                ret += (buffer[idx + i] & 0x000000FF) << (i * 8);
            } else {
                ret += (buffer[idx + i] & 0x000000FF) << ((size - i - 1) * 8);
            }
        }

        index[0]+= size;

        return ret;
    }

    private void writeShInt(final int intVal,
                            final byte[] buffer,
                            final int[] index) {
        /*if (isFFI) {
            writeInt(intVal, buffer, index);
        } else {
            writeShort((short)intVal, buffer, index);
        }*/

        assert buffer != null;
        assert index != null;
        assert index[0] >= 0;

        int idx = index[0];
        int aux;
        int auxShift;
//System.out.println("size=" + size);
        for (int i = 0; i < size; i++) {
            auxShift = swapped ? (i * 8) : ((size - i - 1) * 8);
            aux = ((intVal & (0x000000FF << auxShift)) >> auxShift);
            buffer[idx + i] = (byte)aux;
//System.out.println("buffer[" + (idx + i) + "]=" + buffer[idx + i]);
        }

        index[0]+= size;
    }
}
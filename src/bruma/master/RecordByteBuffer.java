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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Heitor Barbieri
 * @date 09/06/2008
 */
class RecordByteBuffer {
    private static final byte[] fillArray = new byte[512];
    
    private final boolean isFFI;
    private final boolean swapped;
    private final int filler;  // 2 bytes para ajuste de alinhamento da estrutura M1STRU
    private final int shift;
    private Map<Integer,String> tags;
    private Map<String,Integer> stags;

    RecordByteBuffer(final boolean isFFI,
                     final boolean swapped,
                     final int filler,
                     final int shift) throws BrumaException {
        if ((filler != 0) && (filler != 2)) {
            throw new BrumaException("filler != 0 && filler != 2");
        }
        if (shift < 0) {
            throw new BrumaException("shift < 0");
        }

        this.isFFI = isFFI;
        this.swapped = swapped;
        this.filler = filler;
        this.shift = shift;
        this.tags = null;
        this.stags = null;
        Arrays.fill(fillArray, (byte)32);
    }

    RecordByteBuffer setTags(final Map<Integer,String> tags,
                             final Map<String,Integer> stags) {
        this.tags = tags;
        this.stags = stags;
        
        return this;
    }
    
    Record fromByteBuffer(final ByteBuffer bbuffer,
                          final String encoding) throws BrumaException {
        if (bbuffer == null) {
            throw new NullPointerException("bbuffer");
        }
        if (encoding == null) {
            throw new NullPointerException("encoding");
        }
                
        final Record record = new Record(shift, filler);        
                    
        try {            
            final Record.Status recStatus;
            final Charset charset = Charset.forName(encoding); 
            final CharsetDecoder decoder = charset.newDecoder();
        
            bbuffer.order(swapped ? ByteOrder.LITTLE_ENDIAN 
                                  : ByteOrder.BIG_ENDIAN);
            bbuffer.rewind();
            
            final int mfn = bbuffer.getInt();                        
            record.setMfn(mfn);
            if (isFFI) {
                bbuffer.getInt();
            } else {
                bbuffer.getShort();
            } //mfrl
            if ((!isFFI) && (filler != 0)) {
                bbuffer.getShort();
            }
            
            final int mfbwb = bbuffer.getInt();
            record.setBlockNumber(mfbwb);
            
            final int mfbwp = bbuffer.getShort();
            record.setBlockPos(mfbwp);
            if (isFFI && (filler != 0)) {
                bbuffer.getShort();
            }
            
            final int base = (isFFI ? bbuffer.getInt() : bbuffer.getShort());
            final int nvf = bbuffer.getShort();
            final int status = bbuffer.getShort();
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
                final int tag = bbuffer.getShort();
                if (isFFI && (filler != 0)){
                    bbuffer.getShort();
                }
                final int pos = (isFFI ? bbuffer.getInt() : bbuffer.getShort());
                final int len = (isFFI ? bbuffer.getInt() : bbuffer.getShort());
                final int bpos = base + pos;
                
                bbuffer.mark();
                bbuffer.position(bpos);
                bbuffer.limit(bpos + len);
                record.addField(tag, (tags == null) ? null : tags.get(tag), 
                                                      decoder.decode(bbuffer));
                bbuffer.reset();
            }            
        } catch (Exception ex) {
            //ex.printStackTrace();
            throw new BrumaException("fromByteArray/" + ex);
        }

        return record;
    }

    void toByteBuffer(final Record record,
                      final String encoding,
                      final ByteBuffer bbuffer) throws BrumaException {
        if (record == null) {
            throw new NullPointerException("record");
        }
        if (encoding == null) {
            throw new NullPointerException("encoding");
        }
        if (bbuffer == null) {
            throw new NullPointerException("bbuffer");
        }

        record.setShift(shift);
        record.setFiller(filler);

        final int recLen = record.getRecordLength(encoding, isFFI);
        final List<Field> fields = record.getFields();
        final int fsize = fields.size();
        final int dirSize = (fsize * (isFFI ? (10 + filler) : 6));
        final int base = ((isFFI ? 22 : 18) + filler + dirSize);        
        final Record.Status recStatus = record.getStatus();
        final short status;
        int tag;
        int pos = 0;
        byte[] fld;
        int fldLen;

//System.out.println("toByteArray");
        try {
            if (bbuffer.capacity() < recLen) {
                throw new BrumaException("bbuffer.capacity() < recLen");
            }
            bbuffer.clear();            
            status = (short)((recStatus == Record.Status.ACTIVE) ? 0 : 1);
            bbuffer.putInt(record.getMfn());
            if (isFFI) {
                bbuffer.putInt(recLen);
            } else {
                bbuffer.putShort(((short)(recLen)));
            }
            if ((!isFFI) && (filler != 0)) {
                bbuffer.putShort((short)0);
            }
            bbuffer.putInt(record.getBlockNumber());
            bbuffer.putShort((short)record.getBlockPos());
            if (isFFI && (filler != 0)) {
                bbuffer.putShort((short)0);
            }
            if (isFFI) {
                bbuffer.putInt(base);
            } else {
                bbuffer.putShort((short)base);
            }
            bbuffer.putShort((short)fsize); // nvf
            bbuffer.putShort((short)status);
            bbuffer.mark();
            for (Field field : fields) {             
                bbuffer.reset();
                tag = field.getId();
                if (tag == Field.NO_TAG) {
                    if (stags == null) {
                        throw new BrumaException("null field string tags");
                    }
                    final Integer id = stags.get(field.getIdStr());                    
                    if (id == null) {
                        throw new BrumaException("unknown field id number");
                    }
                    tag = id;
                } 
                bbuffer.putShort((short)tag); //tag
                if (isFFI && (filler != 0)) {
                    bbuffer.putShort((short)0);
                }                
                if (isFFI) {
                    bbuffer.putInt(pos);
                } else {
                    bbuffer.putShort((short)pos);
                }
                fld = field.getContent().getBytes(encoding);
                fldLen = fld.length;
                if (isFFI) {
                    bbuffer.putInt(fldLen);
                } else {
                    bbuffer.putShort((short)fldLen);
                }
                bbuffer.mark();
                bbuffer.position(base + pos);
                bbuffer.put(fld);                
                pos += fldLen;
            }

            // Preenche com espacos.
            final int fill = record.getFillSize(base + pos);
            if (fill > 0) {
                bbuffer.put(fillArray, 0, fill);                
            }
            bbuffer.limit(recLen);
        } catch (Exception ex) {
            //ex.printStackTrace();
            throw new BrumaException(ex);
        }
    }   
}
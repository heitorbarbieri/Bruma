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

package bruma.impexp;

import bruma.BrumaException;
import bruma.master.DirEntry;
import bruma.master.Field;
import bruma.master.Record;
import java.io.StringWriter;

/**
 *
 * @author Heitor Barbieri
 * @date 07/01/2010
 */
class MyWriter extends StringWriter {
    private static final String SEPARATOR =
                                          System.getProperty("line.separator");

    private int lineSize;
    private int curPos;

    MyWriter(final int lineSize) {
        super();
        this.lineSize = lineSize;
        curPos = 1;
    }

    MyWriter(final int lineSize,
             final int initialSize) {
        super(initialSize);
        this.lineSize = (lineSize + 1);
        curPos = 1;
    }

    public void write(char c) {
        if (curPos == (lineSize + 1)) {
            super.write(SEPARATOR);
            curPos = 1;
        }

        super.write(c);
        curPos++;
    }

    @Override
    public void write(String str) {
        int pos = 0;
        int size = str.length();
        int rem = lineSize - curPos + 1;

        while(size > 0) {
            if (rem == 0) {
                super.write(SEPARATOR);
                curPos = 1;
                rem = lineSize;
            } else if (rem >= size) {
                super.write(str.substring(pos, pos + size));
                curPos += size;
                rem -= size;
                size = 0;
            } else { // rem < size
                super.write(str.substring(pos, pos + rem));
                pos += rem;
                size -= rem;
                rem = 0;
            }
        }
    }

    void newLine() {
        super.write(SEPARATOR);
        curPos = 1;
    }
}

public class ISO2709E {
    private static final int ISO_LEADER_LENGHT = 24;
    private static final int ISO_TAG_LENGHT = 3;
    private static final int ISO_FLDLEN_LENGHT = 4;
    private static final int ISO_FLDLOC_LENGHT = 5;
    private static final int ISO_DIRECTORY_LENGHT = ISO_TAG_LENGHT
                                                  + ISO_FLDLEN_LENGHT
                                                  + ISO_FLDLOC_LENGHT;
    private static final char ISO_FLDSEP = '#';
    private static final int  ISO_FLDSEP_LENGHT = 1;
    private static final char ISO_RECSEP = '#';
    private static final int  ISO_RECSEP_LENGHT = 1;
    private static final int  ISO_LINE_LENGHT = 80;
    
    public static String exportRecord(final Record rec) throws BrumaException {
        if (rec == null) {
            throw new BrumaException("exportRecord/null document");
        }

        final MyWriter writer = new MyWriter(ISO_LINE_LENGHT);

        final int nvf = rec.getNvf();
        int total_dir = nvf * ISO_DIRECTORY_LENGHT;
        int total_data = 0;
        int total_record;
        int base_address = ISO_LEADER_LENGHT + total_dir + ISO_FLDSEP_LENGHT;
        int tag;
        int pos = 0;
        int len;
        String aux;
        DirEntry dirE;

        // .................................... calculate quantity of bytes
        for (int idx = 0; idx < nvf; idx++) {
            dirE = rec.getDirectoryEntry(idx);
            total_data += dirE.getLen() + ISO_FLDSEP_LENGHT;
        }
        total_record = base_address + total_data + ISO_RECSEP_LENGHT;

        // ..................................................... leader
        aux = String.format("%1$05d0000002%2$05d000%3$01d%4$01d00",
                                    total_record,base_address,
                                    ISO_FLDLEN_LENGHT,ISO_FLDLOC_LENGHT);
        writer.write(aux);

        // ...................................................... directory
        for (int idx = 0; idx < nvf; idx++) {
            dirE = rec.getDirectoryEntry(idx);
            tag = dirE.getTag();
            if (tag == 1000) {
                tag = 1;
            }
            if (tag > 999) {
                tag %= 1000;
            }
            len = dirE.getLen() + ISO_FLDSEP_LENGHT;
            aux = String.format("%1$03d%2$04d%3$05d", tag, len , pos);
            pos += len;
            writer.write(aux);
        }

        // .................................................... data fields
        writer.write(ISO_FLDSEP);

        for (Field field: rec) {
            writer.write(field.getContent());
            writer.write(ISO_FLDSEP);
        }

        writer.write(ISO_RECSEP);
        writer.newLine();

        return writer.toString();
    }
}

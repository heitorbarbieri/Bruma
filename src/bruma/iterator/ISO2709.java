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

package bruma.iterator;

import bruma.master.Master;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Heitor Barbieri
 */
class MyReader {
    private static final int LINE_LENGTH = 80;

    private final int lineLength;
    private final BufferedReader reader;

    private int linePos;

    MyReader(final String fileName) throws IOException {
        this(fileName, LINE_LENGTH, null);
    }

    MyReader(final String fileName,
             final String encoding) throws IOException {
        this(fileName, LINE_LENGTH, null);
    }

    MyReader(final String fileName,
             final int lineLength,
             final String encoding) throws IOException {

        assert lineLength > 0;

        final String encod = (encoding == null) ? Master.DEFAULT_ENCODING
                                                : encoding;
        this.lineLength = ((lineLength > 0) ? lineLength : LINE_LENGTH);
        linePos = 0;
        reader = new BufferedReader(new InputStreamReader(
                                          new FileInputStream(fileName),encod));
    }

    void close() throws IOException {
        if (reader != null) {
            reader.close();
        }
    }

    void readBreak() throws IOException {
        int c = reader.read();

        if (c == '\r') {
            reader.read(); // read /n
        }
        linePos = 0;
    }

    int read(final byte[] cbuf,
             final int off,
             final int len) throws IOException {
        assert cbuf != null;
        assert off >= 0 ;
        assert len > 0;

        int c = 0;
        int total = 0;

        while (total < len) {
            if (linePos == lineLength) {
                readBreak();
            }
            if (c == -1) {
                return total;
            }

            while ((total < len) && (linePos < lineLength)) {
                c = reader.read();
                if (c == -1) { // || (c == '\r') || (c == '\n')) {
                    return total;
                }
                cbuf[total++] = (byte)c;
                linePos++;
            }
        }

        return total;
    }
}

/**
 * 
 * @author Heitor Barbieri
 */
public class ISO2709 {
    public class Field {
        private final int tag;
        private final List<String> field;

        Field(final int tag) {
            this.tag = tag;
            field = new ArrayList<String>();
        }

        void add(final String content) {
            if (content != null) {
                field.add(content);
            }
        }

        public int getTag() {
            return tag;
        }

        public List<String> getFields() {
            return field;
        }
    }

    private static final int LEADER_LENGTH = 24;

    private final MyReader mr;

    private String errMessage;
    private int nvf;
    private int dirEntrySize;
    private boolean eof;

    // Leader variables
    private int recordLength;
    private int recordStatus;
    private int implementationCodes;
    private int indicatorLength;
    private int subfieldIndicatorLength;
    private int baseAddressData;
    private int forUserSystems;
    private int lengthLengthEntryField;
    private int lengthStartingCharacterPosition;
    private int forFutureUse;

    // Directoy
    private byte[] dir;

    // Fields
    private byte[] fields;

    public ISO2709(final String isoFileName) throws IOException {
        this (isoFileName, null);
    }

    public ISO2709(final String isoFileName,
                   final String encoding) throws IOException {
        if (isoFileName == null) {
            throw new IllegalArgumentException("null iso file name");
        }

        errMessage = null;
        mr = new MyReader(isoFileName, encoding);
        dir = null;
        fields = null;
        nvf = 0;
        dirEntrySize = 0;
        eof = false;
    }

    public void close() throws IOException {
        if (mr != null) {
            mr.close();
        }
    }

    public String getErrorMessage() {
        return ((errMessage != null) ? errMessage : null);
    }

    private int parseInt(final byte [] buffer,
                         final int offset,
                         final int length) {
        assert buffer != null;
        assert offset >= 0;
        assert length > 0;

        int ret = 0;
        char c;

        for (int index = 0; index < length; index++) {
            c = (char)buffer[offset + index];
            if ((c < '0') || (c > '9')) {
                throw new IllegalArgumentException(
                                           "parseInt/ (c < '0') || (c > '9')");
            }
            ret *= 10;
            ret += (c - '0');
        }
        return ret;
    }

    private boolean readLeader() {
        final byte [] leader = new byte[LEADER_LENGTH];
        final int len;
        boolean ret = false;

        try {
            len = mr.read(leader, 0, LEADER_LENGTH);
            if (len == 0) {
                eof = true;
            } else if (len != LEADER_LENGTH) {
                errMessage = "readLeader/read";
            } else {
                recordLength = parseInt(leader, 0, 5);
                recordStatus = parseInt(leader, 5, 1);
                implementationCodes = parseInt(leader, 6, 4);
                indicatorLength = parseInt(leader, 10, 1);
                subfieldIndicatorLength = parseInt(leader, 11, 1);
                baseAddressData = parseInt(leader, 12, 5);
                forUserSystems = parseInt(leader, 17, 3);
                lengthLengthEntryField = parseInt(leader, 20, 1);
                lengthStartingCharacterPosition = parseInt(leader, 21, 1);
                forFutureUse = parseInt(leader, 22, 2);

                eof = false;
                ret = true;
            }
        } catch (Exception ex) {
            errMessage =  "readLeader/" + ex.toString();
        }

        return ret;
    }

    private boolean readDirectory() {
        boolean ret = false;

        try {
            final int dirLength = baseAddressData - LEADER_LENGTH - 1;

            if ((dir == null) || (dir.length < dirLength)) {
                dir = new byte[dirLength];
            }

            if (mr.read(dir, 0, dirLength) == dirLength) {
                dirEntrySize = 3 + lengthLengthEntryField
                                             + lengthStartingCharacterPosition;
                nvf = dirLength / dirEntrySize;
                ret = true;
            } else {
                errMessage =  "readDirectory/dirLength";
            }
        } catch (Exception ex) {
            errMessage =  "readDirectory/" + ex.toString();
        }

        return ret;
    }

    private boolean readFields() {
        boolean ret = false;
        try {
            final int fieldsLength = recordLength - baseAddressData + 1;
            int read;

            if ((fields == null) || (fields.length < fieldsLength)) {
                fields = new byte[fieldsLength];
            }

            read = mr.read(fields, 0, fieldsLength);
            if (read == fieldsLength) {
                mr.readBreak();
                ret = true;
            } else {
                errMessage =  "readFields/fieldsLength [" + read
                                                  + " < " + fieldsLength + "]";
            }
        } catch (Exception ex) {
            errMessage =  "readFields/" + ex.toString();
        }

        return ret;
    }

    private boolean insertField(final List<Field> record,
                                final int tag,
                                final String content) {
        assert record != null;
        assert tag >= 0;
        assert content != null;

        int endIndex  = record.size() - 1;
        int beginIndex = 0;
        int curIndex;
        Field field = null;
        boolean found = false;
        boolean ret = false;

        if (tag <= 0) {
            errMessage = "insertField/tag[" + tag + " <= 0";
        } else if (content == null) {
            errMessage = "insertField/field content is null";
        } else {
            while (beginIndex <= endIndex) {
                curIndex = beginIndex + ((endIndex - beginIndex) / 2);
                field = record.get(curIndex);
                if (tag > field.getTag()) {
                    beginIndex = curIndex + 1;
                } else if (tag < field.getTag()) {
                    endIndex = curIndex - 1;
                } else {
                    found = true;
                    break;
                }
            }

            if ((field == null) || !found) {
                field = new Field(tag);
                field.add(content);
                if (tag < field.getTag()) {
                    record.add((endIndex - 1), field);
                } else {
                    record.add((endIndex + 1), field);
                }
            } else {
                field.add(content);
            }
            ret = true;
        }

        return ret;
    }

    public List<Field> getNextIsoRecord() {
        List<Field> record;
        int dpos;
        int tag;
        int pos;
        int len;
        String content;

        errMessage = null;
        if ((!readLeader()) && (!eof)) {
            errMessage = "getNextIsoRecord/" + errMessage;
            record = null;
        } else if (!readDirectory()) {
            errMessage = "getNextIsoRecord/" + errMessage;
            record = null;
        } else if (!readFields()) {
            errMessage = "getNextIsoRecord/" + errMessage;
            record = null;
        } else {
            record = new ArrayList<Field>();

            for (int index = 0; index < nvf; index++) {
                dpos = index * dirEntrySize;
                tag = parseInt(dir, dpos, 3);
                if ((tag <= 0) || (tag > 9999)) {
                    errMessage = "getNextIsoRecord/tag" + errMessage;
                    record = null;
                    break;
                }
                dpos += 3;
                len = parseInt(dir, dpos, lengthLengthEntryField);
                if (len < 0) {
                    errMessage = "getNextIsoRecord/len" + errMessage;
                    record = null;
                    break;
                }
                dpos += lengthLengthEntryField;
                pos = parseInt(dir, dpos, lengthStartingCharacterPosition);
                if (pos < 0) {
                    errMessage = "getNextIsoRecord/pos" + errMessage;
                    record = null;
                    break;
                }
                try {
                    content = new String(fields, pos + 1 ,
                                                 len - 1, "ISO-8859-1");
                    if (!insertField(record, tag, content)) {
                        errMessage = "getNextIsoRecord/" + errMessage;
                        record = null;
                        break;
                    }
                } catch (java.io.UnsupportedEncodingException ueex) {
                    errMessage = "getNextIsoRecord" + ueex.toString();
                    record = null;
                    break;
                }
            }
        }

        return record;
    }
}
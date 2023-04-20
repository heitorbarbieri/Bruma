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
import bruma.master.Master;
import bruma.master.Record;
import java.io.FileInputStream;
import java.io.IOException;

/**
 *
 * @author Heitor Barbieri
 * @date 07/01/2010
 */

class MyReader {
    private static final int ISO_LINE_LENGHT = 80;

    private final int lineLength;
    private final FileInputStream reader;

    private int linePos;

    MyReader(final String fileName) throws IOException {
        this(fileName, ISO_LINE_LENGHT);
    }

    MyReader(final String fileName,
             final int lineLength) throws IOException {

        assert lineLength > 0;

        final String encod = Master.DEFAULT_ENCODING;
        
        this.lineLength = ((lineLength > 0) ? lineLength : ISO_LINE_LENGHT);
        linePos = 0;
        reader = new FileInputStream(fileName);
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

/*
class MyReader {
    private static final int ISO_LINE_LENGHT = 80;

    private final int lineLength;
    private final BufferedReader reader;

    private int linePos;

    MyReader(final String fileName) throws IOException {
        this(fileName, ISO_LINE_LENGHT, null);
    }

    MyReader(final String fileName,
             final String encoding) throws IOException {
        this(fileName, ISO_LINE_LENGHT, encoding);
    }

    MyReader(final String fileName,
             final int lineLength,
             final String encoding) throws IOException {

        assert lineLength > 0;

        final String encod = (encoding == null) ? Master.DEFAULT_ENCODING
                                                : encoding;
        this.lineLength = ((lineLength > 0) ? lineLength : ISO_LINE_LENGHT);
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

    int read(final char[] cbuf,
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
                cbuf[total++] = (char)c;
                linePos++;
            }
        }

        return total;
    }
}

*/

public class ISO2709I {
    private static final int LEADER_LENGTH = 24;

    private final MyReader mr;
    private String encoding;
    
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

    public ISO2709I(final String isoFileName) throws BrumaException {
        this (isoFileName, null);
    }

    public ISO2709I(final String isoFileName,
                    final String encoding) throws BrumaException {
        if (isoFileName == null) {
            throw new BrumaException("null iso file name");
        }

        try {
            mr = new MyReader(isoFileName);
        } catch(IOException ioe) {
            throw new BrumaException(ioe);
        }
        this.encoding = (encoding == null) ? Master.DEFAULT_ENCODING : encoding;
        dir = null;
        fields = null;
        nvf = 0;
        dirEntrySize = 0;
        eof = false;
    }

    public void close() throws BrumaException {
        if (mr != null) {
            try {
                mr.close();
            } catch(IOException ioe) {
                throw new BrumaException(ioe);
            }
        }
    }

    private int parseInt(final byte[] buffer,
                         final int offset,
                         final int length) throws BrumaException {
        assert buffer != null;
        assert offset >= 0;
        assert length > 0;

        int ret = 0;
        char c;

        for (int index = 0; index < length; index++) {
            c = (char)buffer[offset + index];
            if ((c < '0') || (c > '9')) {
                throw new BrumaException("parseInt/ (c < '0') || (c > '9') => c == " + c);
            }
            ret *= 10;
            ret += (c - '0');
        }
        return ret;
    }

    private void readLeader() throws BrumaException {
        try {
            final byte[] leader = new byte[LEADER_LENGTH];
            final int len = mr.read(leader, 0, LEADER_LENGTH);

            if (len == 0) {
                eof = true;
            } else {
                if (len != LEADER_LENGTH) {
                    throw new BrumaException("readLeader/read");
                }
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
            }
        } catch (IOException ex) {
            throw new BrumaException(ex);
        }
    }

    private void readDirectory() throws BrumaException {
        try {
            final int dirLength = baseAddressData - LEADER_LENGTH - 1;

            if ((dir == null) || (dir.length < dirLength)) {
                dir = new byte[dirLength];
            }

            if (mr.read(dir, 0, dirLength) == dirLength) {
                dirEntrySize = 3 + lengthLengthEntryField
                                             + lengthStartingCharacterPosition;
                nvf = dirLength / dirEntrySize;
            } else {
                throw new BrumaException("readDirectory/dirLength");
            }
        } catch (IOException ex) {
            throw new BrumaException(ex);
        }
    }

    private void readFields() throws BrumaException {
        try {
            final int fieldsLength = recordLength - baseAddressData + 1;
            int read = 0;

            if ((fields == null) || (fields.length < fieldsLength)) {
                fields = new byte[fieldsLength];
            }

            read = mr.read(fields, 0, fieldsLength);
            if (read == fieldsLength) {
                mr.readBreak();
            } else {
                throw new BrumaException("readFields/fieldsLength [" + read
                                                  + " < " + fieldsLength + "]");
            }
        } catch (IOException ex) {
            throw new BrumaException(ex);
        }
    }
    
    public Record readRecord() throws BrumaException {
        Record record = null;
        int dpos;
        int tag;
        int pos;
        int len;
        String content;

        readLeader();
        if (!eof) {
            readDirectory();
            readFields();
            record = new Record();

            for (int index = 0; index < nvf; index++) {
                dpos = index * dirEntrySize;
                tag = parseInt(dir, dpos, 3);
                if ((tag <= 0) || (tag > 9999)) {
                    throw new BrumaException("readRecord/tag[" + tag + "]");
                }
                dpos += 3;
                len = parseInt(dir, dpos, lengthLengthEntryField);
                if (len < 0) {
                    throw new BrumaException("readRecord/len < 0");
                }
                dpos += lengthLengthEntryField;
                pos = parseInt(dir, dpos, lengthStartingCharacterPosition);
                if (pos < 0) {
                    throw new BrumaException("readRecord/pos < 0");
                }
                try {
                    content = new String(fields, pos + 1, len - 1, encoding);
                } catch (IOException ex) {
                    throw new BrumaException(ex);
                }
                record.addField(tag, content);
            }
        }

        return record;
    }   
}
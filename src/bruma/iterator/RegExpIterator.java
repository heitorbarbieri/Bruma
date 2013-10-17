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

import bruma.BrumaException;
import bruma.master.Master;
import bruma.master.Record;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Heitor Barbieri
 * @date 27/11/2009
 */
public class RegExpIterator extends AbstractRecordIterator {
    final static String NEWLINESEPARATOR = "\\r?\\n";
    final static String PIPESEPARATOR = "\\|";

    final Matcher fldDelim;
    final Scanner recDelim;

    public RegExpIterator(final String source) {
        this(source, PIPESEPARATOR, NEWLINESEPARATOR);
    }

    public RegExpIterator(final File source,
                          final String encoding) throws BrumaException {
        this(source, encoding, PIPESEPARATOR, NEWLINESEPARATOR);
    }

    public RegExpIterator(final String source,
                          final String fieldDelimRE,
                          final String recordDelimRE) {
        super();

        if (source == null) {
            throw new NullPointerException("source");
        }
        if (fieldDelimRE == null) {
            throw new NullPointerException("fieldDelimRE");
        }
        if (recordDelimRE == null) {
            throw new NullPointerException("recordDelimRE");
        }
        fldDelim = Pattern.compile(fieldDelimRE).matcher("");
        recDelim = new Scanner(source);
        getNextRecord();
    }

    public RegExpIterator(final File source,
                          final String encoding,
                          final String fieldDelimRE,
                          final String recordDelimRE) throws BrumaException {
        super();

        if (source == null) {
            throw new NullPointerException("source");
        }
        if (fieldDelimRE == null) {
            throw new NullPointerException("fieldDelimRE");
        }
        if (recordDelimRE == null) {
            throw new NullPointerException("recordDelimRE");
        }
        fldDelim = Pattern.compile(fieldDelimRE).matcher("");
        try {
            recDelim = new Scanner(source, (encoding == null)
                                               ? Master.DEFAULT_ENCODING
                                               : encoding);
            recDelim.useDelimiter(recordDelimRE);
            getNextRecord();
        } catch (FileNotFoundException ex) {
            throw new BrumaException(ex.fillInStackTrace());
        }
    }

    @Override
    protected Record getNextRecord() {
        record = null;

        if (recDelim.hasNext()) {
            final String buffer = recDelim.next();
            int tag = 1;
            int beginFldPos = 0;
            String field;

            fldDelim.reset(buffer);

            while (fldDelim.find()) {
                field = buffer.substring(beginFldPos, fldDelim.start());
                beginFldPos = fldDelim.end();
                try {
                    if (record == null) {
                        record = new Record();
                    }
                    record.addField(tag++, field);
                } catch (BrumaException ex) {
                        Logger.getLogger(RegExpIterator.class.getName())
                                                 .log(Level.SEVERE, null, ex);
                }
            }
        }

        return record;
    }

    @Override
    public void close() {
        recDelim.close();
    }
}

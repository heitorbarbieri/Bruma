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
import bruma.master.Record;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Heitor Barbieri
 * @date 04/04/2008
 */
public class IdFileRecordIterator extends AbstractRecordIterator {
    private Matcher mat;
    private BufferedReader reader;
    private String lastLine;

    public IdFileRecordIterator(final File fname) {
        this(fname, null);
    }

    public IdFileRecordIterator(final File fname,
                                final String encoding) {
        super();

        final String encod = (encoding == null) ? Master.DEFAULT_ENCODING
                                                : encoding;
        mat = Pattern.compile("\\!v(\\d{1,9})\\!(.*)").matcher("");
//InputStreamReader on a FileInputStream.
        try {
            reader = new BufferedReader(new InputStreamReader(
                                            new FileInputStream(fname),encod));
            lastLine = null;
            getNextRecord();
        } catch (IOException ex) {
            Logger.getLogger(IdFileRecordIterator.class.getName()).
                                                    log(Level.SEVERE, null, ex);
        }
    }

    public IdFileRecordIterator(final String idString) {
        super();

        mat = Pattern.compile("\\!v(\\d{1,9})\\!(.*)").matcher("");
        reader = new BufferedReader(new StringReader(idString));
        lastLine = null;
        getNextRecord();
    }

    @Override
    public void close() {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException ex) {
                Logger.getLogger(IdFileRecordIterator.class.getName()).
                                                    log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public Record getNextRecord() {
        assert reader != null;

        final StringBuilder builder = new StringBuilder();
        String line;
        int id;
        int fldId = 0;
        boolean readId = false;
        boolean insideField = false;

        record = null;

        try {
            while (true) {
                if (lastLine == null) {
                    line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                } else {
                    line = lastLine;
                    lastLine = null;
                }

                if (line.startsWith("!ID")) {
                    if (readId) {
                        lastLine = line;
                        break;
                    } else {
                        id = Integer.parseInt(line.substring(4));
                        record = new Record();
                        record.setMfn(id);
                        readId = true;
                        insideField = false;
                    }
                } else {
                    if (!readId) {
                        throw new IllegalArgumentException("missing record id");
                    }
                    lastLine = null;
                    mat.reset(line);
                    if (mat.matches()) {
                        insideField = true;

                        if (builder.length() > 0) {
                            record.addField(fldId, builder.toString());
                            builder.setLength(0);
                        }
                        fldId = Integer.parseInt(mat.group(1));
                        builder.append(mat.group(2));
                    } else {
                        if (!insideField) {
                            throw new IllegalArgumentException(
                                                            "missing field id");
                        }
                        builder.append(line);
                    }
                }
            }

            if (builder.length() > 0) {
                record.addField(fldId, builder.toString());
            }
        } catch (Exception ex) {
            Logger.getLogger(IdFileRecordIterator.class.getName()).
                                                    log(Level.SEVERE, null, ex);
        }

        return record;
    }

    private static void usage() {
        System.err.println("usage: IdFileRecordIterator <idFileName>");
        System.exit(1);
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            usage();
        }

        final AbstractRecordIterator iterator =
                                              new IdFileRecordIterator(args[0]);

        for (Record rec : iterator) {
            System.out.println(rec);
        }

        iterator.close();
    }
}
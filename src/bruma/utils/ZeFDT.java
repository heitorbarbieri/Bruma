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

/*
 * ZeFDT.java
 *
 * Created on 8 de Abril de 2005, 12:18
 */

package bruma.utils;

import bruma.BrumaException;
import java.io.*;
import java.util.*;

/**
 *
 * @author Heitor Barbieri
 */

/**
 the traditional FDT

According to the CDS/ISIS manual, the field definition table is displayed like

44  Serie                          300 X R vz

and contains for each field tag:

    * a field description (up to 30 characters)
    * maximum length (1 to 1650)
    * field type:
      X for alphanum, A for strictly alpha (not including space), N for numeric (decimal digits), P for pattern (see below).
    * repeatability indicator
      R meaning field is repeatable, N else
    * format (subfield list or pattern)
      For a P field, this gives a COBOL-PIC-style pattern consisting of X (alnum), A (alpha), 9 (num) and literal letters, e.g. 99-999/AA to allow for input like 35-674/XE.
      For other field types, this is a list of legal subfields, e.g. vz to allow for input like foo^vbar^zbaz. Patterns are not supported by winisis as of Version 1.4.


The actual file format looks like

Series                        vz                  44 300 0 1

which is 30 characters name, 20 characters subfield/pattern, the field tag, maximum length, a number encoding the type (0 alphanum, 1 alpha, 2 numeric, 3 pattern), and a number 1 for repeatable / 0 for non repeatable fields.

Moreover, the FDT defines the available worksheets W (.fmt), printformats F (.pft) and field selections S (.fst). Each such definition is on a line starting with the key letter and a colon, and followed by (blank padded) 6 character fields of file basenames.

F:THES  THES1 THES2
*/

public class ZeFDT {
    public static final String DEFAULT_EXTENSION = "fdt";

    public static enum FIELD_TYPE {alphanum, alpha, numeric, pattern}
    public static enum REPEATABLE {nonrepeatable, repeatable}

    public class ZeFDTField {
        private String description;
        private String format;
        private int tag;
        private int maximumLength;
        private FIELD_TYPE type;
        private REPEATABLE repeatable;

        public ZeFDTField(final String _description,
                          final String _format,
                          final int _tag,
                          final int _maximumLength,
                          final FIELD_TYPE _type,
                          final REPEATABLE _repeatable) {
            if (_description == null) {
                throw new NullPointerException("ZeFDTField/null description");
            }
            if (_description.length() > 30) {
                throw new IllegalArgumentException("ZeFDTField/"
                                       + "description exceeded 30 characteres");
            }
            if (_format == null) {
                throw new NullPointerException("ZeFDTField/null format");
            }
            if (_format.length() > 20) {
                throw new IllegalArgumentException("ZeFDTField/" +
                                            "format exceeded 20 characteres");
            }
            if ((_tag <= 0) || (_tag > 9999)) {
                throw new IllegalArgumentException("ZE__FDT_Field/tag[" + _tag
                                                                  + "] > 9999");
            }
            if (_maximumLength > 1650) {   // Valor do site Open Isis
                throw new IllegalArgumentException(
                                               "ZeFDTField/null maximumLength");
            }

            description = _description.trim();
            format = _format.trim();
            tag = _tag;
            maximumLength = _maximumLength;
            type = _type;
            repeatable = _repeatable;
        }

        ZeFDTField() {
            description = null;
            format = null;
            tag = 0;
            maximumLength = 0;
            type = null;
            repeatable = null;
        }

        public int getTag() {
            return tag;
        }

        public String getDescription() {
            return description;
        }

        public boolean isNumeric() {
            return (type == FIELD_TYPE.numeric);
        }

        public boolean isAlpha() {
            return (type == FIELD_TYPE.alpha);
        }
        
        public boolean isRepetable() {
            return (repeatable == REPEATABLE.repeatable);
        }

        public int getMaximumLength() {
            return maximumLength;
        }

        public String getFormat() {
            return format;
        }

        public ZeFDTField fromString(final String in) {
            if (in == null) {
                throw new NullPointerException("fromString/null in string");
            }
            if (in.length() < 57) {
                throw new IllegalArgumentException("fromString/invalid format: "
                                                                        + in);
            }
            String[] elems = in.substring(30).split("\\s+");
            int val;
            boolean hasFormat = (elems.length == 5);

            if (elems.length < 4) {
                throw new IllegalArgumentException("fromString/invalid format: "
                                                                        + in);
            }

            description = in.substring(0, 30).trim();

            format = hasFormat ? elems[0] : "";
            /*if (format.length() > 20) {
                throw new IllegalArgumentException("fromString/"
                                            + "format exceeded 20 characteres");
            }*/

            try {
                tag = Integer.parseInt(elems[hasFormat ? 1 : 0]);
                if ((tag <= 0) || (tag > 9999)) {
                    throw new IllegalArgumentException("fromString/tag[" + tag
                             + "] > 9999 in " + in);
                }

                maximumLength = Integer.parseInt(elems[hasFormat ? 2 : 1]);
                /*if (maximumLength > 1650) {   // Valor do site Open Isis
                    throw new IllegalArgumentException(
                                               "fromString/null maximumLength");
                }*/

                val = Integer.parseInt(elems[hasFormat ? 3 : 2]);
                if (val == 0) {
                    type = FIELD_TYPE.alphanum;
                } else if (val == 1) {
                    type = FIELD_TYPE.alpha;
                } else if (val == 2) {
                    type = FIELD_TYPE.numeric;
                } else if (val == 3) {
                    type = FIELD_TYPE.pattern;
                } else {
                    throw new IllegalArgumentException(
                                            "fromString/invalid type[" + val
                                            + "] value in " + in);
                }

                val = Integer.parseInt(elems[hasFormat ? 4 : 3]);
                if (val == 0) {
                    repeatable = REPEATABLE.nonrepeatable;
                } else if (val == 1) {
                    repeatable = REPEATABLE.repeatable;
                } else {
                    throw new IllegalArgumentException(
                                            "fromString/invalid repeatable["
                                            + repeatable + "] value in " + in);
                }
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException(
                       "fromString/invalid number specification in "  + in + " "
                                        + nfe.getMessage());
            }

            return this;
        }

        public String toString(final int maxSize) {
            assert maxSize >= 0;

            final StringBuilder sBuilder = new StringBuilder();

            sBuilder.append(paddingWhites(description, 30));
            sBuilder.append(paddingWhites(format, maxSize + 1));
            sBuilder.append(tag);
            sBuilder.append(" ");
            sBuilder.append(maximumLength);
            sBuilder.append(" ");
            sBuilder.append(type.ordinal());
            sBuilder.append(" ");
            sBuilder.append(repeatable.ordinal());

            return sBuilder.toString();
        }
    }

    private List <String> worksheets;
    private List <String> printFormats;
    private List <String> fieldSelections;
    private List <ZeFDTField> fieldDescriptions;

    /**
     * Creates a new instance of ZeFDT.
     */
    public ZeFDT() {
        worksheets = new ArrayList <String> ();
        printFormats = new ArrayList <String> ();
        fieldSelections = new ArrayList <String> ();
        fieldDescriptions = new ArrayList <ZeFDTField> ();
    }

    public void addWorksheet(final String worksheet) {
        if (worksheet == null) {
            throw new NullPointerException("addWorksheet/null worksheet");
        }
        final String wsheet = worksheet.trim();
        if (wsheet.length() == 0) {
            throw new IllegalArgumentException("addWorksheet/empty worksheet");
        }
        worksheets.add(wsheet);
    }

    public void clearWorksheets() {
        worksheets.clear();
    }

    public List < String > getWorksheets() {
        return new ArrayList < String > (worksheets);
    }

    public void addPrintFormats(final String printFormat) {
        if (printFormat == null) {
            throw new NullPointerException(
                                            "addPrintFormats/null printFormat");
        }
        final String prtFormat = printFormat.trim();
        if (prtFormat.length() == 0) {
            throw new IllegalArgumentException(
                                           "addPrintFormats/empty printFormat");
        }
        printFormats.add(prtFormat);
    }

    public void clearPrintFormats() {
        printFormats.clear();
    }

    public List <String> getPrintFormats() {
        return new ArrayList<String> (printFormats);
    }

    public void addFieldSelections(final String fieldSelection) {

        if (fieldSelection == null) {
            throw new NullPointerException(
                                      "addFieldSelections/null fieldSelection");
        }
        final String fldSelection = fieldSelection.trim();
        if (fldSelection.length() == 0) {
            throw new IllegalArgumentException(
                                     "addFieldSelections/empty fieldSelection");
        }
        fieldSelections.add(fldSelection);
    }

    public void clearFieldSelections() {
        fieldSelections.clear();
    }

    public List <String> getFieldSelections() {
        return new ArrayList <String> (fieldSelections);
    }

    public void addFieldDescription(final ZeFDTField fieldDescription) {

        if (fieldDescription == null) {
            throw new NullPointerException(
                                  "addFieldDescription/null fieldDescription");
        }
        fieldDescriptions.add(fieldDescription);
    }

    public void clearFieldDescriptions() {
        fieldDescriptions.clear();
    }

    public List<ZeFDTField> getFieldDescriptions() {
        return new ArrayList<ZeFDTField> (fieldDescriptions);
    }

    public Map<Integer,String> getFieldDescriptionMap() {
        final Map<Integer,String> ret = new TreeMap<Integer,String>();

        for (ZeFDTField fld : fieldDescriptions) {
            ret.put(fld.getTag(), fld.getDescription());
        }

        return ret;
    }
    
    public Map<Integer,ZeFDTField> getFieldDescriptionMapEx() {
        final Map<Integer,ZeFDTField> ret = new TreeMap<Integer,ZeFDTField>();

        for (ZeFDTField fld : fieldDescriptions) {
            ret.put(fld.getTag(), fld);
        }

        return ret;
    }

    public ZeFDT fromFile(final String fdtFile) throws BrumaException {
        if (fdtFile == null) {
            throw new NullPointerException("fromFile/null fdt file name");
        }
        final String fileName =
                           Util.changeFileExtension(fdtFile, DEFAULT_EXTENSION);
        String content = null;
        String line = null;
        String[] elems;
        int curLine = 0;

        if (!new File(fileName).isFile()) {
            throw new BrumaException("fromFile/file[" + fileName
                                   + "] is not a file");
        }
        try {
            content = readParameters(fileName);
        } catch(IOException ioe) {
            throw new BrumaException(ioe);
        }
        final String[] lines = content.split("\\r?\\n");

        worksheets.clear();
        printFormats.clear();
        fieldSelections.clear();
        fieldDescriptions.clear();

        for ( ; curLine < lines.length; curLine++) {
            line = lines[curLine];
            if (line.startsWith("W:")) {
                elems = line.substring(2).split("\\s+");
                for (int counter2 = 0; counter2 < elems.length; counter2++) {
                    worksheets.add(elems[counter2].trim());
                }
            } else {
                break;
            }
        }
        if (curLine == lines.length) {
            throw new IllegalArgumentException("fromFile/invalid file format");
        }
        for ( ; curLine < lines.length; curLine++) {
            line = lines[curLine];
            if (line.startsWith("F:")) {
                elems = line.substring(2).split("\\s+");
                for (int counter2 = 0; counter2 < elems.length; counter2++) {
                    printFormats.add(elems[counter2].trim());
                }
            } else {
                break;
            }
        }
        if (curLine == lines.length) {
            throw new IllegalArgumentException("fromFile/invalid file format");
        }
        for ( ; curLine < lines.length; curLine++) {
            line = lines[curLine];
            if (line.startsWith("S:")) {
                elems = line.substring(2).split("\\s+");
                for (int counter2 = 0; counter2 < elems.length; counter2++) {
                    fieldSelections.add(elems[counter2].trim());
                }
            } else {
                break;
            }
        }
        if (curLine == lines.length) {
            throw new IllegalArgumentException("fromFile/invalid file format");
        }
        if (line.compareTo("***") != 0) {
            throw new IllegalArgumentException("fromFile/invalid file format");
        }
        curLine++;
        for ( ; curLine < lines.length; curLine++) {
            fieldDescriptions.add(new ZeFDTField().fromString(lines[curLine]));
        }

        return this;
    }

    public void toFile(final String fdtFile) throws BrumaException {
        if (fdtFile == null) {
            throw new NullPointerException("toFile/null fdtFile name");
        }

        String collectionName =
                        Util.changeFileExtension(fdtFile, DEFAULT_EXTENSION);
        FileWriter fWriter = null;

        try {
            fWriter = new FileWriter(collectionName);
            fWriter.write(toString());
        } catch (IOException ioe) {
            throw new BrumaException(ioe);
        } finally {
            try {
                if (fWriter != null) {
                    fWriter.close();
                }
            } catch (IOException ioe) {
            }
        }
    }

    @Override
    public String toString() {
        final StringBuilder sBuilder = new StringBuilder();
        int cur = 0;
        int maxSize = 0;
        Iterator<String> iterator;

        if (!worksheets.isEmpty()) {
            iterator = worksheets.iterator();
            
            while (true) {
                if (!iterator.hasNext()) {
                    break;
                }
                if (cur == 8) {
                    sBuilder.append("\n");
                    cur = 0;
                }
                if (cur == 0) {
                    sBuilder.append("W:");
                }
                sBuilder.append(paddingWhites(iterator.next(), 6));
                cur++;
            }
            if (cur != 0) {
                sBuilder.append("\n");
            }
        }
        if (!printFormats.isEmpty()) {
            iterator = printFormats.iterator();
            while (true) {
                if (!iterator.hasNext()) {
                    break;
                }
                if (cur == 8) {
                    sBuilder.append("\n");
                    cur = 0;
                }
                if (cur == 0) {
                    sBuilder.append("F:");
                }
                sBuilder.append(paddingWhites(iterator.next(), 6));
                cur++;
            }
            if (cur != 0) {
                sBuilder.append("\n");
            }
        }
        if (!fieldSelections.isEmpty()) {
            iterator = fieldSelections.iterator();
            while (true) {
                if (!iterator.hasNext()) {
                    break;
                }
                if (cur == 8) {
                    sBuilder.append("\n");
                    cur = 0;
                }
                if (cur == 0) {
                    sBuilder.append("S:");
                }
                sBuilder.append(paddingWhites(iterator.next(), 6));
                cur++;
            }
            if (cur != 0) {
                sBuilder.append("\n");
            }
        }
        sBuilder.append("***");

        for (ZeFDTField field : fieldDescriptions) {
            maxSize = Math.max(maxSize, field.getFormat().length());
        }
        for (ZeFDTField field : fieldDescriptions) {
            sBuilder.append("\n");
            sBuilder.append(field.toString(maxSize));
        }

        return sBuilder.toString();
    }

    static String paddingWhites(final String in,
                                final int outSize) {
        assert (in != null) : "paddingWhites/null in";
        assert (outSize > 0) : "paddingWhites/outSize <= 0";

        String ret = in;

        if (in.length() < outSize) {
            StringBuilder out = new StringBuilder(in);
            int rem = outSize - in.length();

            if (rem > 0) {
                char[] whites = new char[rem];

                Arrays.fill(whites, ' ');
                out.append(whites);
            }
            ret = out.toString();
        }

        return ret;
    }

    String readParameters(final String param) throws IOException {
        String ret = null;
        final File f = new File(param);
        final StringBuilder out;
        BufferedReader in = null;        
        String line;
        boolean first = true;

        try {
            if (f.isFile() && f.canRead()) {
                in = new BufferedReader(new FileReader(f));
                out = new StringBuilder();

                while (true) {
                    if (first) {
                        first = false;
                    } else {
                        out.append("\n");
                    }
                    line = in.readLine();
                    if (line == null) {
                        break;
                    }
                    out.append(line);
                }
                ret = out.toString();
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }

        return ret;
    }
}

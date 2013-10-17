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
 * Gizmo.java
 *
 * Created on 10/10/2007, 09:41:05
 *
 */

package bruma.utils;

import bruma.BrumaException;
import bruma.master.Field;
import bruma.master.Record;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Heitor Barbieri
 */
public class Gizmo {
    private class Element {
        String fromKey;
        String toKey;
        int begin;
        int end;
        int len;

        Element(String fromKey,
                String toKey,
                int beginPos) {
            assert fromKey != null;
            assert toKey != null;

            this.fromKey = fromKey;
            this.toKey = toKey;
            setBegin(beginPos);
        }

        final void setBegin(final int beginPos) {
            begin = beginPos;
            len = fromKey.length();
            end = begin + len - 1;
        }
    }

    private class ElementCollection {
        private List<Element> list;
        private int total;

        ElementCollection() {
            list = new ArrayList<Element>();
            total = 0;
        }

        void addElement(final Element elem) {
            assert elem != null;

            list.add(elem);
        }

        void reset() {
            for (Element elem : list) {
                elem.begin = -1;
                elem.end = elem.begin + elem.len - 1;
            }
        }

        Element getLower() {
            Element ret = null;
            int min = Integer.MAX_VALUE;
            int begin;

            for (Element elem : list) {
                begin = elem.begin;
                if ((begin >= 0) && (begin < min)) {
                    min = begin;
                    ret = elem;
                    break;
                }
            }

            return ret;
        }

        Element getSamePos(int pos) {
            assert pos >= 0;

            Element ret = null;

            for (Element elem : list) {
                if (elem.begin == pos) {
                    ret = elem;
                    break;
                }
            }

            return ret;
        }

        int total() {
            return total;
        }

        void adjustElements(final String in,
                            final int initPos) {
            assert in != null;
            assert initPos >= 0;

            for (Element elem : list) {
                adjustElement(in, initPos, elem);
            }
        }

        void adjustElement(final String in,
                           final int initPos,
                           final Element elem) {
            assert in != null;
            assert elem != null;
            assert initPos >= 0;

            int pos = initPos;

            while (elem.begin < pos) {
                pos = in.indexOf(elem.fromKey, pos);
                if (pos == -1) {
                    if ((elem.begin >= 0) && (total > 0)) {
                        total--;
                    }
                    elem.setBegin(-1);
                    break;
                }
                final Element other = getSamePos(pos);
                if ((other == null) ||
                                (elem.fromKey.compareTo(other.fromKey) > 0)) {
                    if (elem.begin < 0) {
                        total++;
                    }
                    elem.setBegin(pos);
                    if (other != null) {
                        adjustElement(in, elem.end + 1, other);
                    }
                    break;
                }
                pos += other.len;
            }
        }
    }

    private ElementCollection elemCol;
    private Pattern pat;

    public Gizmo(final String in) throws IOException {
        if (in == null) {
            throw new IllegalArgumentException();
        }
        elemCol = new ElementCollection();
        pat = Pattern.compile("\\[([^\\,]+),([^\\]]+)");
        insertConvertion(in);
    }

    public Gizmo(final File in) throws IOException {
        if (in == null) {
            throw new IllegalArgumentException();
        }
        BufferedReader reader = new BufferedReader(new FileReader(in));
        String line;
        elemCol = new ElementCollection();
        pat = Pattern.compile("\\[([^\\,]+),([^\\]]+)");

        while (true) {
            line = reader.readLine();
            if (line == null) {
                break;
            }
            insertConvertion(line);
        }
        reader.close();
    }

    /**
     * Insere na lista o elemento encontrado na string e que tem o seguinte
     * formato:
     * [StringAntes,StringDepois]
     **/
    private void insertConvertion(final String in) {
        assert in != null;

        Matcher mat = pat.matcher(in);
        int index = 0;

        while (mat.find()) {
            elemCol.addElement(new Element(mat.group(1),
                                           mat.group(2), --index));
        }
    }

    public String gizmo(final String in) throws BrumaException {
        final StringBuilder out = new StringBuilder();
        boolean first = true;
        Element elem;
        int len = 0;
        int curPos = 0;

        if (in == null) {
            throw new BrumaException("null in");
        }        
        if (in.length() > 0) {
            elemCol.reset();
            elemCol.adjustElements(in, curPos);

            while (elemCol.total() > 0) {
                if (first) {
                    first = false;
                } else {
                    out.append("\n");
                }
                elem = elemCol.getLower();
                if (elem.begin > curPos) {
                    //out.append(line.substring(curPos, elem.begin));
                    out.append(in, curPos, elem.begin);
                }
                out.append(elem.toKey);
                curPos = elem.end + 1;
                elemCol.adjustElements(in, curPos);
            }
            if (curPos < (len - 1)) {
                //out.append(line.substring(curPos));
                out.append(in, curPos, len);
            }
        }
        return out.toString();
    }

    public Record gizmoFields(final int[] tags,
                              final Record rec) throws BrumaException {
        if (tags == null) {
            throw new BrumaException("null tags");
        }
        if (rec == null) {
            throw new BrumaException("null rec");
        }
        final Record out = new Record();
        int tag;
        String field;

        for (Field fld : rec) {
            tag = fld.getId();
            field = fld.getContent();

            if (Arrays.binarySearch(tags, tag) > 0) {
                field = gizmo(field);
            }
            out.addField(tag, field);
        }

        return out;
    }

    public Record gizmoRecord(final Record rec) throws BrumaException {
        if (rec == null) {
            throw new BrumaException("null rec");
        }
        final Record out = new Record();

        for (Field fld : rec) {
            out.addField(fld.getId(), gizmo(fld.getContent()));
        }

        return out;
    }
}
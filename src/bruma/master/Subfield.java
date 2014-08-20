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

/**
 *
 * @author Heitor Barbieri
 */
/** Represents a record subfield. */
public class Subfield implements Comparable<Subfield> {
    /** Default identifier for the first record subfield */
    public static final char FIRST_SUB_ID = '_';

    /** Subfield identifier. */
    private final char id;
    /** Subfield content. */
    private final String content;

    public Subfield(final char id,
                    final CharSequence content) throws BrumaException {
        if (id < 0) {
            throw new BrumaException("id < 0");
        }
        if (content == null) {
            throw new BrumaException("null content");
        }

        this.id = (id == 0) ? FIRST_SUB_ID : id;
        this.content = content.toString();
    }

    /** Get subfield content.
     * @return the subfield content.
     */
    public String getContent() {
        return content;
    }

    /* Get subbield identifier. */
    public char getId() {
        return id;
    }

    @Override
    public String toString() {
        final String ret;

        if (id == FIRST_SUB_ID) {
            ret = content;
        } else {
            ret = "^" + id + content;
        }
        return ret;
    }

    @Override
    public int compareTo(final Subfield other) {
        if (other == null) {
            throw new NullPointerException();
        }
        return this.getId() - other.getId();
    }
    
    public String toJSON() {        
        final String subContent = content.replace("\"", "\\\"");
        final String ret = "          { \"id\": \"" + id + "\" \"content\":  \""
                                                         + subContent + "\" }";
        return ret;
    }
    
    public void toJSON(final StringBuilder in) throws BrumaException {
        if (in == null) {
            throw new BrumaException("null in");
        }

        final String subContent = content.replace("\"", "\\\"");

        in.append("          { \"id\": \"");
        in.append(id);
        in.append("\", \"content\":  \"");
        in.append(subContent);
        in.append("\" }");
    }
}
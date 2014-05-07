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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Heitor Barbieri
 */
public class Field implements Iterable<Subfield>, Comparable<Field> {
    public static final int NO_TAG = -1;
    private static final Pattern pat = Pattern.compile(            
      "(\\^([A-Za-z0-9]))?(.*?)(?=\\^[A-Za-z0-9]|$)");//"(\\^(\\w))?([^\\^]+)");
    
    private final int tag;
    private final String tagStr;
    private final String content;
    private List<Subfield> subfields;

    public Field(final int tag,
                 final CharSequence content) throws BrumaException {
        if ((tag <= 0) || (tag > 99999)) {
            throw new BrumaException("Field/tag[" + tag
                                + "] parameter out of range[1, 99999]");
        }
        if (content == null) {
            throw new BrumaException("Field/null content");
        }
        this.tag = tag;
        this.tagStr = null;
        this.content = content.toString();
        subfields = null;
    }
    
    public Field(final int tag,
                 final List<Subfield> subfields) throws BrumaException {
        if ((tag <= 0) || (tag > 99999)) {
            throw new BrumaException("Field/tag[" + tag
                                + "] parameter out of range[1, 99999]");
        }
        if (subfields == null) {
            throw new BrumaException("Field/null content");
        }
        this.tag = tag;
        this.tagStr = null;
        this.content = null;
        this.subfields = subfields;
    }

    public Field(final CharSequence tagStr,
                 final CharSequence content) throws BrumaException {
        if (tagStr == null) {
            throw new BrumaException("Field/null strId");
        }
        if (content == null) {
            throw new BrumaException("Field/null content");
        }
        this.tag = NO_TAG;
        this.tagStr = tagStr.toString();
        this.content = content.toString();
        subfields = null;
    }
    
    public Field(final int tag,
                 final CharSequence tagStr,
                 final CharSequence content) throws BrumaException {
        if (((tag <= 0) || (tag > 32767)) && (tagStr == null)) {
            throw new BrumaException("Field/tag[" + tag
                                + "] parameter out of range[1, 32767]");
        }
        if (content == null) {
            throw new BrumaException("Field/null content");
        }
        this.tag = tag;
        this.tagStr = (tagStr == null) ? null : tagStr.toString();
        this.content = content.toString();
        subfields = null;
    }

    /* Get field identifier. */
    public int getId() {
        return tag;
    }

    /* Get field tag string. */
    public String getIdStr() {
        return tagStr;
    }

    /** Get field content.
     * @return the field content.
     */
    public String getContent() {
        final String ret;

        if (subfields == null) {
            ret = content;
        } else {
            final StringBuilder builder = new StringBuilder();
            boolean first = true;

            for (Subfield subfield : subfields) {
                if (first) {
                    if (subfield.getId() == Subfield.FIRST_SUB_ID) {
                        builder.append(subfield);
                    } else {
                        builder.append("^");
                        builder.append(subfield.getId());
                        builder.append(subfield.getContent());
                    }
                    first = false;
                } else {
                    builder.append("^");
                    builder.append(subfield.getId());
                    builder.append(subfield.getContent());
                }
            }
            ret = builder.toString();
        }

        return ret;
    }

    @Override
    public Iterator<Subfield> iterator() {
        Iterator<Subfield> ret;

        try {
            ret = getSubfields().iterator();
        } catch (BrumaException ze) {
            ret = null;
        }

        return ret;
    }

    /**
     * Add a subfield to this field
     * @param sub subfield to be added
     * @throws BrumaException 
     */
    public void addSubfield(final Subfield sub) throws BrumaException {
        if (sub == null) {
            throw new BrumaException("null subfield");
        }
        subfields.add(sub);
    }
    
    /**
     * Get a list of subfields.
     * @return a list of subfields.
     * @throws BrumaException
     */
    public List<Subfield> getSubfields() throws BrumaException {
        if (subfields == null) {
            final Matcher matcher = pat.matcher(content);

            subfields = new ArrayList<Subfield>();

            while (matcher.find()) {
                final String sid = matcher.group(2);
                final String subfield = matcher.group(3);
                
                if (sid == null) {
                    if (!subfield.isEmpty()) {
                        subfields.add(
                                 new Subfield(Subfield.FIRST_SUB_ID, subfield));
                    }
                } else {
                   subfields.add(new Subfield(sid.charAt(0), subfield));
                }                
            }
        }

        return subfields;
    }

     /**
     * Get a list of subfields.
     * @param id the subfield identifier
     * @return a list of subfields.
     * @throws BrumaException
     */
    public List<Subfield> getTagSubfields(final char id) throws BrumaException {
        final List<Subfield> ret = new ArrayList<Subfield>();
        
        if (id == Subfield.FIRST_SUB_ID) {
            final int idx = content.indexOf('^');
            if (idx == -1) {
                ret.add(new Subfield(Subfield.FIRST_SUB_ID, content));
            } else {
                ret.add(new Subfield(Subfield.FIRST_SUB_ID, 
                                                    content.substring(0, idx)));
            }
        } else {
            final Matcher matcher = pat.matcher(content);
            char id2;
            String sid;
            String subfield;

            while (matcher.find()) {
                sid = matcher.group(2);
                if ((sid != null) && !sid.isEmpty()) {
                    id2 = sid.charAt(0);
                    if (id == id2) {
                        subfield = matcher.group(3);
                        ret.add(new Subfield(id, subfield));
                    }
                }
            }
        }
        
        return ret;
    }

    /**
     *  Get a record subfield.
     *
     * @param id the subfield identifier
     * @param occ the subfield tag occurrence (occ > 0).
     * @return  the record subfield.
     * @exception  BrumaException
     */
    public Subfield getSubfield(final char id,
                                final int occ) throws BrumaException {
        if (occ <= 0) {
            throw new BrumaException("subfield occ[" + occ
                                                    + "] out of range <= 0");
        }
        final List<Subfield> sub = getTagSubfields(id);
        
        return (sub.size() >= occ) ? sub.get(occ - 1) : null;
    }

    @Override
    public int compareTo(final Field other) {
        if (other == null) {
            throw new NullPointerException();
        }
        return ((this.tagStr != null) && (other.tagStr != null))
                 ? this.tagStr.compareTo(other.getIdStr())
                 : this.getId() - other.getId();
    }

    public String toXML() {
        final String cont =
                        getContent().replace("<", "&lt;").replace(">", "&gt;");
        final StringBuilder sb = new StringBuilder();

        if (tagStr == null) {
            sb.append("<field tag='");
            sb.append(tag);
            sb.append("'>");
            sb.append(cont);
            sb.append("</field>");
        } else {
            sb.append("<");
            sb.append(tagStr);
            sb.append(">");
            sb.append(cont);
            sb.append("</");
            sb.append(tagStr);
            sb.append(">");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        final String cont = getContent();
                      //getContent().replace("<", "&lt;").replace(">", "&gt;");
        final StringBuilder sb = new StringBuilder();

        if (tagStr == null) {
            sb.append("<");
            sb.append(tag);
            sb.append(">");
            sb.append(cont);
            sb.append("</");
            sb.append(tag);
            sb.append(">");
        } else {
            sb.append("<");
            sb.append(tagStr);
            sb.append(">");
            sb.append(cont);
            sb.append("</");
            sb.append(tagStr);
            sb.append(">");
        }

        return sb.toString();
    }
}
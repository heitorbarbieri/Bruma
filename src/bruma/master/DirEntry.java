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

/**
 * <p>The <code>DirEntry</code> class represents an Isis database record
 * directory entry.</p>
 * @author Heitor Barbieri
 */
public class DirEntry {
    /** Record field length. */
    private final int len;

    /** Record field identifier. */
    private final int tag;

    /**
     * Get the field length.
     * @return the field length.
     */
    public int getLen() {
        return len;
    }

    /**
     * Get the field tag.
     * @return field tag.
     */
    public int getTag() {
        return tag;
    }

    /**
     * Creates a new instance of <code>DirEntry</code>.
     */
    DirEntry(final int tag,
             final int len) {
        this.tag = tag;
        this.len = len;
    }

    /**
     * Creates a new instance of <code>DirEntry</code>.
     */
    DirEntry() {
        this(0, 0);
    }
}


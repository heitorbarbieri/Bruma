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

package bruma.tools.lupa;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 *
 * @author Heitor Barbieri
 */
class FilterProfile {
    private boolean sortFields;
    private Set<Integer> showFields;

    FilterProfile() {
        sortFields = false;
        showFields = new LinkedHashSet<Integer>();
    }

    void setShowFields(Set<Integer> showFields) {
        for (Integer tag : showFields) {
            this.showFields.add(tag);
        }
    }

    void setSortFields(boolean sortFields) {
        this.sortFields = sortFields;
    }

    Set<Integer> getShowFields() {
        return showFields;
    }

    boolean isSortFields() {
        return sortFields;
    }
}

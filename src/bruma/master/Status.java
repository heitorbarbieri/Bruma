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
 *  Description of the Class
 *
 * @author     Heitor Barbieri - MTI
 * @created    28 de Outubro de 2004
 */

/**
 * The <code>Status</code> class indicates Isis master file and
 * record status.
 */
final class Status {
    /**
     *  Master file structure is corruped.
     */
    static final int MST_FIL_BAD = -2;
    /**
     *  Cross Reference structure is corruped.
     */
    static final int XRF_FIL_BAD = -1;
    /**
     *  Master file is ok.
     */
    static final int MST_OPEN_OK = 1;
    /**
     *  Master file is locked.
     */
    static final int MST_LOCKED = 2;

    private Status() {
    }
}


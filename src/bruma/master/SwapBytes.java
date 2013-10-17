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
 * 
 * @author Heitor Barbieri
 */
public class SwapBytes {
    private final boolean haveToSwap;

    public SwapBytes(final boolean swap) {
        haveToSwap = swap;
    }

    public int swap(final int intVal) {
        final int ret;

        if (haveToSwap) {
            ret = ((intVal & 0xFF000000) >>> 0x18)
                 + ((intVal & 0xFF0000) >>> 0x8)
                 + ((intVal & 0xFF00) << 0x8) + ((intVal & 0xFF) << 0x18);
        } else {
            ret = intVal;
        }
        return ret;
    }

    public short swap(final short shortVal) {
        final short ret;

        if (haveToSwap) {
            ret = (short) (((shortVal & 0xFF00) >>> 0x8)
                                                +  ((shortVal & 0xFF) << 0x8));
        } else {
            ret = shortVal;
        }
        return ret;
    }
}


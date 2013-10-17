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

import java.nio.ByteOrder;

/**
 *
 * @author Heitor Barbieri
 */
public class MasterPlatformInfo {
    private final String mstName;
    private String encoding;
    private boolean swapped;
    private boolean ffi;
    private int shift;
    private boolean multiuser;
    private int dataAlignment;
    private boolean inMemoryMst;
    private boolean inMemoryXrf;
    private boolean xrfWriteCommit;

    MasterPlatformInfo(final String mstName) {
        assert mstName != null;

        this.mstName = mstName;
        encoding = Master.GUESS_ISO_IBM_ENCODING;
        swapped = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
        ffi = false;
        shift = 0;
        multiuser = false;
        dataAlignment = System.getProperty("os.name").startsWith("Win")
                                                                     //? 2 : 4;
                                                                      ? 0 : 2;
        inMemoryMst = false;
        inMemoryXrf = true;
        xrfWriteCommit = false;
    }

    public String getMstName() {
        return mstName;
    }

    public String getEncoding() {
        return encoding;
    }

    void setEncoding(String encoding) {
        assert encoding != null;
        this.encoding = encoding;
        //Charset.forName(encoding);
    }

    public boolean isFfi() {
        return ffi;
    }

    void setFfi(boolean ffi) {
        this.ffi = ffi;
        if (ffi) {
            if (shift == 0) {
                shift = 3;
            }
        } else {
            //shift = 0; Supposing big non ffi master
        }
    }

    public boolean isMultiuser() {
        return multiuser;
    }

    void setMultiuser(final boolean multiuser) {
        /*throw new IllegalArgumentException(
                                      "Sorry, multiuser mode is not working");*/
        this.multiuser = multiuser;
    }

    public int getShift() {
        return shift;
    }

    public int getMaxGigaSize() {
        return Master.convertToGigaSize(shift);
    }

    void setShift(int shift) {
        assert (shift >= 0);
        this.shift = shift;
    }

    public boolean isSwapped() {
        return swapped;
    }

    void setSwapped(boolean swapped) {
        this.swapped = swapped;
    }

    public int getDataAlignment() {
        return dataAlignment;
    }

    void setDataAlignment(final int val) {
        assert (val >=0) && (val % 2 == 0);

        dataAlignment = val;
    }

    public boolean isInMemoryMst() {
        return inMemoryMst;
    }
    
    public boolean isInMemoryXrf() {
        return inMemoryXrf;
    }

    void setInMemoryMst(final boolean opt) {
        inMemoryMst = opt;
    }
    
    void setInMemoryXrf(final boolean opt) {
        inMemoryXrf = opt;
    }

    public boolean isXrfWriteCommit() {
        return xrfWriteCommit;
    }

    void setXrfWriteCommit(final boolean opt) {
        this.xrfWriteCommit = opt;
    }
}
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

import bruma.master.Master;
import java.io.File;

/**
 *
 * @author Heitor Barbieri
 */
class OpenProfile {
    private File master;
    private File dir;
    private boolean create;
    private boolean ffi;
    private boolean swapped;
    private boolean inmemory;
    private String encoding;
    private int gigasize;
    private int alignment;

    OpenProfile() {
        master = null;
        dir = null;
        create = false;
        ffi = false;
        swapped = true;
        inmemory = true;
        //encoding = Master.DEFAULT_ENCODING;
        encoding = Master.GUESS_ISO_IBM_ENCODING;
        gigasize = 0;
        alignment = 0;
    }

    OpenProfile(final OpenProfile other) {
        assert other != null;

        master = other.getMaster();
        dir = other.getMstDir();
        create = other.isCreate();
        ffi = other.isFFI();
        swapped = other.isSwapped();
        inmemory = other.isInmemory();
        encoding = other.getEncoding();
        gigasize = other.getGigasize();
        alignment = other.getAlignment();
    }

    public int getAlignment() {
        return alignment;
    }

    public void setAlignment(int alignment) {
        this.alignment = alignment;
    }

    public boolean isCreate() {
        return create;
    }

    public void setCreate(boolean create) {
        this.create = create;
    }

    public boolean isFFI() {
        return ffi;
    }

    public void setFFI(boolean ffi) {
        this.ffi = ffi;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public int getGigasize() {
        return gigasize;
    }

    public void setGigasize(int gigasize) {
        this.gigasize = gigasize;
    }

    public boolean isInmemory() {
        return inmemory;
    }

    public void setInmemory(boolean inmemory) {
        this.inmemory = inmemory;
    }

    public File getMaster() {
        return master;
    }

    public void setMaster(File master) {
        this.master = master;
        dir = (master == null) ? new File(".").getParentFile()
                               : master.getParentFile();
    }

    public boolean isSwapped() {
        return swapped;
    }

    public void setSwapped(boolean swapped) {
        this.swapped = swapped;
    }

    public void setMstDir(File dir) {
        this.dir = dir;
    }

    public File getMstDir() {
        return dir;
    }
}

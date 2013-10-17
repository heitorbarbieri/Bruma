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
import bruma.utils.ISO8859OrIBM850;
import bruma.utils.Util;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * Factory to create a Master object.
 * @author Heitor Barbieri
 */
public class MasterFactory {
    /**
     * Bruma version.
     */
    public static final String VERSION = "1.4";

    private final MasterPlatformInfo info;

    private MasterFactory(final String dbName) throws BrumaException {
        assert dbName != null;

        info = new MasterPlatformInfo(dbName);
    }

    /**
     * Creates an instance of the MasterFactory object.
     * @param dbProfile database profile file.
     * This file has the following pattern (one per line): PROPERTY=VALUE
     * Accepted properties are: DBNAME, ENCODING, SWAPPED, FFI, MAXGIGASIZE,
     *    MAXMFRL, DATAALIGNMENT, INMEMORYXRF, XRFWRITECOMMIT and MULTIUSER.
     * @return an instance of the MasterFactory object.
     * @throws IOException
     * @throws BrumaException
     */
    public static MasterFactory getInstance(final File dbProfile)
                                                        throws IOException,
                                                                BrumaException {
        if (dbProfile == null) {
            throw new IllegalArgumentException();
        }
        final MasterFactory factory;
        final Properties props = new Properties();
        final BufferedReader reader =
                                new BufferedReader(new FileReader(dbProfile));
        final String dbName;
        String prop;

        props.load(reader);
        dbName = props.getProperty("DBNAME");
        if (dbName == null) {
            throw new IOException("null database name");
        }
        factory = new MasterFactory(Util.changeFileExtension(dbName, null));
        prop = props.getProperty("ENCODING");
        if (prop != null) {
            factory.setEncoding(prop);
        }
        prop = props.getProperty("SWAPPED");
        if (prop != null) {
            factory.setSwapped(Boolean.parseBoolean(prop));
        }
        prop = props.getProperty("FFI");
        if (prop != null) {
            factory.setFFI(Boolean.parseBoolean(prop));
        }
        prop = props.getProperty("MAXGIGASIZE");
        if (prop != null) {
            factory.setMaxGigaSize(Integer.parseInt(prop));
        }
        prop = props.getProperty("DATAALIGNMENT");
        if (prop != null) {
            factory.setDataAlignment(Integer.parseInt(prop));
        }
        prop = props.getProperty("INMEMORYMST");
        if (prop != null) {
            factory.setInMemoryMst(Boolean.parseBoolean(prop));
        }
        prop = props.getProperty("INMEMORYXRF");
        if (prop != null) {
            factory.setInMemoryXrf(Boolean.parseBoolean(prop));
        }
        prop = props.getProperty("XRFWRITECOMMIT");
        if (prop != null) {
            factory.setXrfWriteCommit(Boolean.parseBoolean(prop));
        }
        prop = props.getProperty("MULTIUSER");
        if (prop != null) {
            factory.setMultiuser(Boolean.parseBoolean(prop));
        }
        reader.close();

        return factory;
    }

    /**
     * Creates an instance of the MasterFactory object.
     * @param dbName database name.
     * @return an instance of the MasterFactory object.
     * @throws BrumaException
     */
    public static MasterFactory getInstance(final String dbName)
                                                         throws BrumaException {
        if (dbName == null) {
            throw new IllegalArgumentException();
        }
        String mstName = dbName.trim();

        if (mstName.toLowerCase().endsWith(".mst")) {
            mstName = Util.changeFileExtension(mstName, null);
        }
        return new MasterFactory(mstName);
    }

    /**
     * Sets the database encoding.
     * @param encoding
     * @return this object
     * @throws BrumaException
     */
    public MasterFactory setEncoding(final String encoding)
                                                         throws BrumaException {
        if (encoding == null) {
            throw new BrumaException("null encoding");
        }
        String encod = encoding;

        if (encod.equals(Master.GUESS_ISO_IBM_ENCODING)) {
            final ISO8859OrIBM850 guess =
                                   new ISO8859OrIBM850(info.getMstName(), true);
            final String encName = guess.guessEncoding();
            encod = (encName == null) ? Master.DEFAULT_ENCODING : encName;
        }

        info.setEncoding(encod);

        return this;
    }

    /**
     * Tells if the database bytes order are big endian or little endian. This
     * function should be used only for creating a new database.
     * @param swapped true if little endian.
     * @return this object
     */
    public MasterFactory setSwapped(final boolean swapped) {
        if (isInMemoryMst()) {
            throw new IllegalArgumentException(
                            "this feature is not available with memory master");
        }
        info.setSwapped(swapped);

        return this;
    }

    /**
     * Tells if the database is extended (ffi), ie, record size greater than 32k.
     * @param ffi true if extended, false otherwise. This function should be
     * used only for creating a new database.
     * @return this object
     */
    public MasterFactory setFFI(final boolean ffi) {
        info.setFfi(ffi);

        return this;
    }

    /**
     * Sets the maximum database size.
     * @param size number of gigabytes. Zero means standard Isis size. This
     * function should be used only for creating a new database.
     * @return this object
     */
    public MasterFactory setMaxGigaSize(final int size) {
        if ((size < 0) || (size % 2 != 0)) {
            throw new IllegalArgumentException(
                                    "size is negative or not multiple of 2");
        }
        if (size > 512) {
            throw new IllegalArgumentException(
                                    "size is bigger than 512 gigabytes");
        }
        info.setShift(Master.convertToShift(size));

        return this;
    }

    /**
     * @return database memory data alignment. See setDataAlignment()
     */
    public int getDataAlignment() {
        if (isInMemoryMst()) {
            throw new IllegalArgumentException(
                            "this feature is not available with memory master");
        }

        return info.getDataAlignment();
    }

    /**
     * Sets the memory data alignment. Zero of multiple of 2. This function
     * should be used only for creating a new database.
     * @param val memory alignment value.
     * @return this object
     */
    public MasterFactory setDataAlignment(final int val) {
        if (isInMemoryMst()) {
            throw new IllegalArgumentException(
                            "this feature is not available with memory master");
        }

        if (val < 0) {
            throw new IllegalArgumentException("val is negative");
        }

        info.setDataAlignment(val);

        return this;
    }

    /**
     * @return if the master is only in memory. See setInMemoryMst()
     */
    public boolean isInMemoryMst() {
        return info.isInMemoryMst();
    }

    /**
     * Tells if the master should be only in memory.
     * @param opt true if it is in memory, false otherwise.
     * @return this object
     */
    public MasterFactory setInMemoryMst(final boolean opt) {
        info.setInMemoryMst(opt);

        return this;
    }

    /**
     * @return if the xrf file should be bufferized in memory.
     *                                                     See setInMemoryXrf()
     */
    public boolean isInMemoryXrf() {
        if (isInMemoryMst()) {
            throw new IllegalArgumentException(
                            "this feature is not available with memory master");
        }

        return info.isInMemoryXrf();
    }

    /**
     * Tells if the xrf file should be bufferized in memory.
     * @param opt true if bufferized, false otherwise.
     * @return this object
     */
    public MasterFactory setInMemoryXrf(final boolean opt) {
        if (isInMemoryMst()) {
            throw new IllegalArgumentException(
                            "this feature is not available with memory master");
        }

        info.setInMemoryXrf(opt);

        return this;
    }

    /**
     *
     * @return if every change in the bufferized xrf object (see setInMemoryXrf)
     * are written to file or not.
     */
    public boolean isXrfWriteCommit() {
        if (isInMemoryMst()) {
            throw new IllegalArgumentException(
                            "this feature is not available with memory master");
        }

        return info.isXrfWriteCommit();
    }

    /**
     * Tells if every change in the bufferized xrf object (see setInMemoryXrf)
     * should be written to file.
     * @param opt true if autocommit, false otherwise.
     * @return this object
     */
    public MasterFactory setXrfWriteCommit(final boolean opt) {
        if (isInMemoryMst()) {
            throw new IllegalArgumentException(
                            "this feature is not available with memory master");
        }

        info.setXrfWriteCommit(opt);

        return this;
    }

    /**
     * @return true if the master is in multiuser mode, false if in monouser mode
     */
    public boolean isMultiuser() {
        return info.isMultiuser();
    }

    /**
     * Tells if the master should be set to multiuser mode.
     * Warning - multiuse mode is not implemented.
     * @param opt true if multiuser, false otherwise.
     * @return this object
     */
    public MasterFactory setMultiuser(final boolean opt) {
        if (opt) {
            throw new IllegalArgumentException("feature not implemented");
        }
        //info.setMultiuser(opt);

        return this;
    }

    /**
     * Adjust this factory according to a master file settings.
     * @param other other master
     * @return this object
     * @throws BrumaException
     */
    public MasterFactory asAnotherMaster(final MasterInterface other)
                                                         throws BrumaException {
        if (other == null) {
            throw new BrumaException("null master");
        }
        if ((!isInMemoryMst()) && (!other.isInMemoryMst())) {
            setDataAlignment(other.getDataAlignment());
            setSwapped(((Master)other).isSwapped());
        }
        setEncoding(other.getEncoding());
        setFFI(other.isFFI());
        //setInMemoryXrf(other.)
        setMaxGigaSize(other.getGigaSize());
        setMultiuser(other.isMultiuser());

        return this;
    }

    public Master open() throws BrumaException {
        if (isInMemoryMst()) {
            throw new IllegalArgumentException(
                            "this feature is not available with memory master");
        }
        return new Master(info).open();
    }

    public MasterInterface create() throws BrumaException {
        return isInMemoryMst() ? new MemoryMaster(info).create()
                               : new Master(info).create();
    }

    public boolean exists() throws BrumaException {
        return isInMemoryMst() ? false : Master.exists(info.getMstName());
    }

    public MasterInterface forceCreate() throws BrumaException {
        if (isInMemoryMst()) {
            return new MemoryMaster(info);
        }

        final Master mst = new Master(info);
        mst.delete();
        mst.create();

        return mst;
    }
}

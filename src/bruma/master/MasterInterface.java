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
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Heitor Barbieri
 * @date 20111215
 */
public interface MasterInterface extends Iterable<Record> {
    /**
     * Default database character encoding.
     */
    String DEFAULT_ENCODING = "ISO-8859-1";
    /**
     * Extended database format (FFI) default record length.
     */
    int DEFMFRL_FFI = 1048576;
    /**
     * Guess encoding between ISO-8859-1 or IBM850
     */
    String GUESS_ISO_IBM_ENCODING = "ISO-8859-1 or IBM850";
    /**
     * Isis standard maximum record length.
     */
    int MAXMFRL_ISIS = 32767;
    /**
     * Maximum allowed record length.
     */
    int MAXMFRL_POSSIBLE = 67108864;

    /**
     * Closes frees all database used resources.
     * @throws BrumaException
     */
    void close() throws BrumaException;

    /**
     * Deletes Isis master files (mst and xrf)
     * @return true if all files were deleted, false otherwise.
     * @throws BrumaException
     */
    boolean delete() throws BrumaException;

    /**
     * Deletes an Isis record.
     * @param mfn - master file number of the record.
     * @throws org.bruma.BrumaException
     */
    void deleteRecord(final int mfn) throws BrumaException;

    /**
     * Forces the unlock of a Isis record.
     * @param mfn - record number to unlock
     * @throws org.bruma.BrumaException
     */
    void forceUnlockRecord(final int mfn) throws BrumaException;

    /**
     * Reads the master control record.
     * @return the control record object
     * @throws org.bruma.BrumaException
     */
    Control getControlRecord() throws BrumaException;

    int getDataAlignment();

    /**
     * @return the database encoding.
     */
    String getEncoding();

    /**
     * @return the maximum database size (in gigabytes).
     * Returning zero means 512 megabytes.
     */
    int getGigaSize();

    /**
     * Locks a database record, avoiding the write to it for anyone who doesnt
     * have the lock.
     * @param mfn
     * @param recLock
     * @return the locked record.
     * @throws org.bruma.BrumaException
     */
    Record getLockRecord(final int mfn, final Lock.RecordLock[] recLock) throws BrumaException;

    /**
     * @return the name of the database.
     */
    String getMasterName();

    int getMaxRecSize() throws BrumaException;

    /**
     * Reads a record from the database.
     * @param mfn record master file number
     * @return the readen record.
     * @throws org.bruma.BrumaException
     */
    Record getRecord(final int mfn) throws BrumaException;

    /**
     * @return tells if the database was created with extended mode
     * (record size > 32 bytes).
     */
    boolean isFFI();

    /**
     * @return tells if the master is in memory.
     */
    boolean isInMemoryMst();
    
    /**
     * @return if the database operation is set to multi or monouser mode.
     */
    boolean isMultiuser();

    Iterator<Record> iterator();

    Lock.RecordLock lockRecord(final Record record) throws BrumaException;

    /**
     * Creates a new empty record.
     * @return Isis record
     * @throws org.bruma.BrumaException
     */
    Record newRecord() throws BrumaException;

    List<Integer> regExpSearch(final String expression, final int maxHits) throws BrumaException;


    /**
     * Resets the EWL and DEL flags, even if the caller doesnt have the unlock
     * right.
     * @throws org.bruma.BrumaException
     */
    void unlock() throws BrumaException;

    /**
     * Forces the unlock of a Isis record.
     * @param recLock - the lock token
     * @throws org.bruma.BrumaException
     */
    void unlockRecord(final Lock.RecordLock recLock) throws BrumaException;

    /**
     * Writes a record into a database.
     * @param record Isis record
     * @throws org.bruma.BrumaException
     * @return record master file number
     */
    int writeRecord(final Record record) throws BrumaException;
    
}

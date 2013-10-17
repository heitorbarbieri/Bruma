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
 * <p>The <code>Control</code> class represents the Isis database control
 * record.</p>
 *
 * @author  Heitor Barbieri
 */
public class Control {
    /** Master file number (always zero). */
    private int ctlmfn;       /* ctlmfn */

    /** Next master file number to be assigned. */
    private int nxtmfn;       /* nxtmfn */

    /** Address of the next master file record: 512 bytes block number. */
    private int nxtmfb;       /* nxtmfb */

    /** Address of the next master file record: offset within the block. */
    private int nxtmfp;       /* nxtmfp - offset */

    /** Type of master file (always zero). */
    private int mftype;       /* mftype */

    /** Not used (always zero). */
    private int reccnt;       /* reccnt */

    /** Not used (always zero). */
    private int mfcxx1;       /* mfcxx1 */

    /** Zero or number of applications with data entry lock. */
    private int mfcxx2;       /* mfcxx2 */

    /** Zero or 1 for exclusive write lock granted to one application. */
    private int mfcxx3;       /* mfcxx3 */

    /**
     * Creates a new instance of <code>Control</code>.
     */
    Control(final int ctlmfn,
            final int nxtmfn,
            final int nxtmfb,
            final int nxtmfp,
            final int mftype,
            final int reccnt,
            final int mfcxx1,
            final int mfcxx2,
            final int mfcxx3) {
        assert ctlmfn >= 0;
        assert mfcxx1 >= 0;
        assert mfcxx2 >= 0;
        assert ((mfcxx3 >= 0) && (mfcxx3 <= 1));
        assert mftype >= 0;
        assert nxtmfb >= 0;
        assert nxtmfp >= 0;
        assert reccnt >= 0;

        this.ctlmfn = ctlmfn;
        this.nxtmfn = nxtmfn;
        this.nxtmfb = nxtmfb;
        this.nxtmfp = nxtmfp;
        this.mftype = mftype;
        this.reccnt = reccnt;
        this.mfcxx1 = mfcxx1;
        this.mfcxx2 = mfcxx2;
        this.mfcxx3 = mfcxx3;
    }

    /**
     * Creates a new instance of <code>Control</code>.
     */
    public Control() {
        this(0, 1, 1, 32, 0, 0, 0, 0, 0);
    }

    /**
     * Get the master file number.
     * @return the master file number.
     */
    public int getCtlmfn() {
        return ctlmfn;
    }

    /**
     * Sets the master file number.
     * @param ctlmfn the master file number
     */
    void setCtlmfn(final int ctlmfn) {
        assert ctlmfn >= 0;
        this.ctlmfn = ctlmfn;
    }

    /**
     * Not used (always zero).
     * @return (always zero).
     */
    public int getMfcxx1() {
        return mfcxx1;
    }

    /**
     * Should not be used (always zero).
     * @param mfcxx1
     */
    void setMfcxx1(final int mfcxx1) {
        assert mfcxx1 >= 0;
        this.mfcxx1 = mfcxx1;
    }

    /**
     * Get the number of applications with data entry lock.
     * @return the number of applications with data entry lock.
     */
    public int getMfcxx2() {
        return mfcxx2;
    }

    /**
     * Sets the number of applications with data entry lock.
     * @param mfcxx2 the number of applications with data entry lock.
     */
    void setMfcxx2(final int mfcxx2) {
        assert mfcxx2 >= 0;
        this.mfcxx2 = mfcxx2;
    }

    /**
     * Get the database exclusive write lock.
     * @return zero (not locked) or one (locked)
     */
    public int getMfcxx3() {
        return mfcxx3;
    }

    /**
     * Sets the database exclusive write lock.
     * @param mfcxx3 - zero or one (ewl)
     */
    void setMfcxx3(final int mfcxx3) {
        assert ((mfcxx3 >= 0) && (mfcxx3 <= 1));
        this.mfcxx3 = mfcxx3;
    }

    /**
     * Get the type of master file. Always zero.
     */
    public int getMftype() {
        return mftype;
    }

    /**
     * Sets the type of master file. Should not be used (always zero).
     * @param mftype
     */
    void setMftype(final int mftype) {
        assert mftype >= 0;
        this.mftype = mftype;
    }

    /**
     * Get the address of the next master file record: 512 bytes block number.
     * @return the address of the next master file record: 512 bytes block
     *  number.
     */
    public int getNxtmfb() {
        return nxtmfb;
    }

    /**
     * Sets the address of the next master file record: 512 bytes block number.
     * @param nxtmfb the address of the next master file record: 512 bytes
     * block number.
     */
    void setNxtmfb(final int nxtmfb) {
        assert nxtmfb >= 0;
        this.nxtmfb = nxtmfb;
    }

    /**
     * Get the next master file number to be assigned.
     * @return the next master file number to be assigned.
     */
    public int getNxtmfn() {
        return nxtmfn;
    }

    /**
     * Sets the next master file number to be assigned.
     * @param nxtmfn the next master file number to be assigned.
     */
    void setNxtmfn(final int nxtmfn) {
        assert nxtmfb > 0;
        this.nxtmfn = nxtmfn;
    }

    /**
     * Get the address of the next master file record: offset within the block.
     * @return the address of the next master file record: offset within
     * the block.
     */
    public int getNxtmfp() {
        return nxtmfp;
    }

    /**
     * Sets the address of the next master file record: offset within the block.
     * @param nxtmfp the address of the next master file record: offset within
     * the block.
     */
    void setNxtmfp(final int nxtmfp) {
        assert nxtmfp >= 0;
        this.nxtmfp = nxtmfp;
    }

    /**
     * Not used (always zero).
     * @return Always zero.
     */
    public int getReccnt() {
        return reccnt;
    }

    /**
     * Should not be used (always zero).
     * @param reccnt
     */
    void setReccnt(final int reccnt) {
        assert reccnt >= 0;
        this.reccnt = reccnt;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();

        builder.append("ctlmfn=");
        builder.append(ctlmfn);
        builder.append("\nnxtmfn=");
        builder.append(nxtmfn);
        builder.append("\nnxtmfb=");
        builder.append(nxtmfb);
        builder.append("\nnxtmfp=");
        builder.append(nxtmfp);
        builder.append("\nmftype=");
        builder.append(mftype);
        builder.append("\nreccnt=");
        builder.append(reccnt);
        builder.append("\nmfcxx1=");
        builder.append(mfcxx1);
        builder.append("\nmfcxx2=");
        builder.append(mfcxx2);
        builder.append("\nmfcxx3=");
        builder.append(mfcxx3);

        return builder.toString();
    }
}

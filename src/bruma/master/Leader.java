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
 * <p>The <code>Leader</code> class represents the Isis database record
 * leader structure.</p>
 * @author Heitor Barbieri
 */
public class Leader {
    /** master file record number. */
    private int mfn;      /* Master file number. */

    /** record length. */
    private int mfrl;     /* Record length. */

    /** record old version address: 512 bytes block number. */
    private int mfbwb;    /* Backward pointer (block). */

    /** record old version address: offset within the block. */
    private int mfbwp;    /* Backward pointer (offset). */

    /** record old version address: offset within the block.*/
    private int base;     /* Base address of data. */

    /** number of fields or number of entries in the directory. */
    private int nvf;      /* Number of fields in record. */

    /** record status. */
    private Record.Status status;   /* Record status */

    /**
     * Creates a new instance of <code>Leader</code>.
     * @param mfn master file record number.
     * @param mfrl record length.
     * @param mfbwb record old version address: 512 bytes block number.
     * @param mfbwp record old version address: offset within the block.
     * @param base record old version address: offset within the block.
     * @param nvf number of fields or number of entries in the directory.
     * @param status record status.
     */
    public Leader(final int mfn,
                  final int mfrl,
                  final int mfbwb,
                  final int mfbwp,
                  final int base,
                  final int nvf,
                  final Record.Status status) {
        this.mfn = mfn;
        this.mfrl = mfrl;
        this.mfbwb = mfbwb;
        this.mfbwp = mfbwp;
        this.base = base;
        this.nvf = nvf;
        this.status = status;
    }

    /**
     * Creates a new instance of <code>Leader</code>.
     */
    public Leader() {
        this(0, 0, 0, 0, 0, 0, Record.Status.ACTIVE);
    }

    public int getBase() {
        return base;
    }

    public void setBase(final int base) {
        assert base >= 0;
        this.base = base;
    }

    public int getMfbwb() {
        return mfbwb;
    }

    public void setMfbwb(final int mfbwb) {
        assert mfbwb >= 0;
        this. mfbwb = mfbwb;
    }

    public int getMfbwp() {
        return mfbwp;
    }

    public void setMfbwp(final int mfbwp) {
        assert mfbwp >= 0;
        this.mfbwp = mfbwp;
    }

    public int getMfn() {
        return mfn;
    }

    public void setMfn(final int mfn) {
        assert mfn > 0;
        this.mfn = mfn;
    }

    public int getMfrl() {
        return mfrl;
    }

    public void setMfrl(final int mfrl) {
        assert mfrl > 0;
        this.mfrl = mfrl;
    }

    public int getNvf() {
        return nvf;
    }

    public void setNvf(final int nvf) {
        assert nvf >= 0;
        this.nvf = nvf;
    }

    public Record.Status getStatus() {
        return status;
    }

    public void setStatus(final Record.Status status) {
        this.status = status;
    }

    /**
     * Dumps the leader content.
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();

        builder.append("mfn="); builder.append(mfn);
        builder.append("\nmfrl="); builder.append(mfrl);
        builder.append("\nmfbwb="); builder.append(mfbwb);
        builder.append("\nmfbwp="); builder.append(mfbwp);
        builder.append("\nbase="); builder.append(base);
        builder.append("\nnvf="); builder.append(nvf);
        builder.append("\nstatus="); builder.append(status);

        return builder.toString();
    }
}

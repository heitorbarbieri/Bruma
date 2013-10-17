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

package bruma.impexp;

import bruma.BrumaException;
import bruma.master.Master;
import bruma.master.MasterFactory;
import bruma.master.Record;

/**
 * Exports the master file to another Isis master file.
 * @author Heitor Barbieri
 * @date 06/01/2010
 */
public class IsisMasterExport extends AbstractMasterExport {
    private final String toMstName;
    private final boolean isToFFI;
    private final int toDataAlignment;
    private final String toEncoding;
    private final int toMaxGigaSize;
    private final boolean isToSwapped;
    private final boolean exportDeleted;

    public IsisMasterExport(final Master mst,
                            final String toMstName,
                            final boolean isToFFI,
                            final int toDataAlignment,
                            final int toMaxGigaSize) throws BrumaException {
        this(mst, toMstName, isToFFI, -1, null, toMaxGigaSize, true, true);
    }

    public IsisMasterExport(final Master mst,
                            final String toMstName,
                            final boolean isToFFI,
                            final int toDataAlignment,
                            final String toEncoding,
                            final int toMaxGigaSize,
                            final boolean isToSwapped,
                            final boolean exportDeleted) throws BrumaException {
        super(mst, "", null, false);

        if (toMstName == null) {
            throw new BrumaException("null toMstName parameter");
        }
        this.toMstName = toMstName;
        this.isToFFI = isToFFI;
        this.toDataAlignment = (toDataAlignment >= 0)
                                    ? toDataAlignment : mst.getDataAlignment();
        this.toEncoding = (toEncoding == null)
                                    ? mst.getEncoding() : toEncoding;
        this.toMaxGigaSize = (toMaxGigaSize >= 0)
                                    ? toMaxGigaSize : mst.getGigaSize();
        this.isToSwapped = isToSwapped;
        this.exportDeleted = exportDeleted;
    }

    @Override
    public void export(final int tell) throws BrumaException {
        final int xtell = (tell <= 0) ? Integer.MAX_VALUE : tell;

        final Master fromMst = MasterFactory.getInstance(mst.getMasterName())
                     .setDataAlignment(mst.getDataAlignment())
                     .setEncoding(mst.getEncoding())
                     .setFFI(mst.isFFI())
                     .setMaxGigaSize(mst.getGigaSize())
                     .setSwapped(mst.isSwapped())
                     .open();

        final Master toMst = (Master)MasterFactory.getInstance(toMstName)
                             .setFFI(isToFFI)
                             .setDataAlignment(toDataAlignment)
                             .setEncoding(toEncoding)
                             .setMaxGigaSize(toMaxGigaSize)
                             .setSwapped(isToSwapped)
                             .create();
        int mfn = 0;
        int cur = 0;

        for (Record rec : fromMst) {
            if (++cur % xtell == 0) {
                System.out.println("++" + cur);
            }
            if (exportDeleted) {
                toMst.writeRecord(rec);
            } else {
                if (rec.getStatus() == Record.Status.ACTIVE) {
                    rec.setMfn(++mfn);
                    toMst.writeRecord(rec);
                }
            }
        }

        fromMst.close();
        toMst.close();
    }

    @Override
    protected String getRecord(final Record rec) throws BrumaException {
        return null;
    }
}

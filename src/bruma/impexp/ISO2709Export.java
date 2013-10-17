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
import bruma.master.Record;

/**
 *
 * @author Heitor Barbieri
 */
public class ISO2709Export extends AbstractMasterExport {
    public ISO2709Export(final Master mst,
                         final String outFile,
                         final String outEncoding) throws BrumaException {
        super(mst, outFile, outEncoding, false);
    }

    @Override
    protected String getRecord(final Record rec) throws BrumaException {
        if (rec == null) {
            throw new BrumaException("null Record object");
        }

        return ISO2709E.exportRecord(rec);
    }
}

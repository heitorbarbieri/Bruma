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
 * Exports the master file using the JSON format.
 * @author Heitor Barbieri
 */
public class JSONMasterExport extends AbstractMasterExport {
    private boolean first;
    private int idTag;

    public JSONMasterExport(final Master mst,
                            final String outFile,
                            final String outEncoding,
                            final boolean useFdt,
                            final int idTag) throws BrumaException {
        super(mst, outFile, outEncoding, useFdt);
        this.first = true;
        this.idTag = idTag;
    }

    @Override
    protected String prologue() {
        return "{\n \"docs\": [\n";
    }

    @Override
    protected String epilogue() {
        return " ]\n}\n";
    }

    @Override
    protected String getRecord(final Record rec) throws BrumaException {
        assert rec != null;

        final String ret;
        
        if (first) {
            first = false;            
            ret = rec.toJSON3(idTag);
        } else {
            ret = ",\n" + rec.toJSON3(idTag);
        }
        
        return ret;
    }
}

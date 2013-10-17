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
 * Exports the master file using the XML format.
 * @author Heitor Barbieri
 */
public class XmlMasterExport extends AbstractMasterExport {

    public XmlMasterExport(final Master mst,
                           final String outFile,
                           final String outEncoding,
                           final boolean useFdt) throws BrumaException {
        super(mst, outFile, outEncoding, useFdt);
    }

    @Override
    protected String prologue() {
        final String[] split = mst.getMasterName().split("[\\\\/]");
        final String mstName = split[split.length - 1];
        
        return "<?xml version=\"1.0\" encoding=\"" + outEncoding
                + "\"?>\n<database name='" + mstName + "'>\n";
    }

    @Override
    protected String epilogue() {
        return "</database>";
    }

    @Override
    protected String getRecord(final Record rec) throws BrumaException {
        assert rec != null;

        return rec.toXML() + "\n";
    }
}

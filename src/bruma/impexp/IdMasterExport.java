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
import bruma.master.Field;
import bruma.master.Master;
import bruma.master.Record;

/**
 * Exports the master file using the ID format.
 * @author Heitor Barbieri
 * @date 27/11/2009
 */
public class IdMasterExport extends AbstractMasterExport {
    private static final String ZEROS = "000000";
    private final StringBuilder builder;

    public IdMasterExport(final Master mst,
                          final String outFile,
                          final String outEncoding) throws BrumaException {
        super(mst, outFile, outEncoding, false);
        builder = new StringBuilder();
    }

    @Override
    protected String getRecord(final Record rec) throws BrumaException {
        assert rec != null;

        builder.setLength(0);
        builder.append("!ID ");
        builder.append(prefix(rec.getMfn(), 7));
              
        for (Field fld : rec) {
            builder.append("\n");
            builder.append("!v");
            builder.append(prefix(fld.getId(), 3));
            builder.append("!");
            builder.append(fld.getContent());            
        }

        builder.append("\n");

        return builder.toString();
    }

    private String prefix(final int val,
                          final int finalSize) {
        assert finalSize > 0;

        final String sval = Integer.toString(val);
        final int difLen = finalSize - sval.length();

        return ((difLen > 0) ? (ZEROS.substring(0, difLen) + sval) : sval);
    }
}

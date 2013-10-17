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
 *
 * @author Heitor Barbieri
 */
public class DelimMasterExport extends AbstractMasterExport {
    private final StringBuilder builder;
    private final String tagDelim;
    private final String fieldDelim;
    private final String recDelim;

    public DelimMasterExport(final Master mst,
                             final String outFile,
                             final String outEncoding,
                             final String tagDelim,
                             final String fieldDelim,
                             final String recDelim,
                             final boolean useFdt) throws BrumaException {
        super(mst, outFile, outEncoding, useFdt);

        if (fieldDelim == null) {
            throw new NullPointerException("field delimiter");
        }
        if (recDelim == null) {
            throw new NullPointerException("record delimiter");
        }
        builder = new StringBuilder();
        this.tagDelim = tagDelim;
        this.fieldDelim = fieldDelim;
        this.recDelim = recDelim;
    }

    @Override
    protected String getRecord(final Record rec) throws BrumaException {
        assert rec != null;

       builder.setLength(0);

        for (Field fld : rec) {
            if (tagDelim != null) {
                if (tags == null) {
                    builder.append(fld.getId());
                } else {
                    builder.append(tags.get(fld.getId()));
                }
                builder.append(tagDelim);
            }
            builder.append(fld.getContent());
            builder.append(fieldDelim);
        }
        builder.append(recDelim);

        return builder.toString();
    }
}

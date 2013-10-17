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
import bruma.master.Control;
import bruma.master.Master;
import bruma.master.MasterFactory;
import bruma.master.Record;
import bruma.utils.Util;
import bruma.utils.ZeFDT;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;

/**
 *
 * @author Heitor Barbieri
 */
public abstract class AbstractMasterExport {
    protected final Master mst;
    protected final String outEncoding;
    protected final Map<Integer,String> tags;
    private final String outFile;
    
    public AbstractMasterExport(final Master mst,
                                final String outFile,
                                final String outputEncoding,
                                final boolean useFdt) throws BrumaException {
        if (mst == null) {
            throw new BrumaException("null Master object");
        }
        if (outFile == null) {
            throw new NullPointerException("output file");
        }

        final String inputEncoding = mst.getEncoding();
        final String fileName =
                             Util.changeFileExtension(mst.getMasterName(), "fdt");
        final boolean hasFdt = new File(fileName).isFile();

        this.mst = mst;
        this.outFile = outFile;
        this.tags = (useFdt && hasFdt) ?
              new ZeFDT().fromFile(mst.getMasterName()).getFieldDescriptionMap()
                           : null;

        outEncoding = (outputEncoding == null)
                         ? (inputEncoding == null
                                          ? Master.DEFAULT_ENCODING
                                          : inputEncoding)
                         : outputEncoding;
    }

    public void export(final int tell) throws BrumaException {
        export(1, Integer.MAX_VALUE, tell);
    }

    public void export(final int from,
                       final int to,
                       final int tell) throws BrumaException {
        final int xfrom = (from <= 0) ? 1 : from;
        final int xtell = (tell <= 0) ? Integer.MAX_VALUE : tell;
        final int xto;
        Writer writer = null;
        Master master = null;
        Control ctl;
        Record rec;
        
        String buff;
        int cur = 0;


        try {
            master = MasterFactory.getInstance(mst.getMasterName())
                     .setDataAlignment(mst.getDataAlignment())
                     .setEncoding(mst.getEncoding())
                     .setFFI(mst.isFFI())
                     .setMaxGigaSize(mst.getGigaSize())
                     .setSwapped(mst.isSwapped())
                     .open();
            ctl = master.getControlRecord();
            xto = Math.min(to, ctl.getNxtmfn() - 1);

            if (xto < xfrom) {
                throw new BrumaException("xto < xfrom");
            }
            writer = new BufferedWriter(
                        new OutputStreamWriter(
                            new FileOutputStream(outFile), outEncoding));

            writer.write(prologue());

            for (int mfn = xfrom; mfn <= xto; mfn++) {
                if (++cur % xtell == 0) {
                    System.out.println("++" + cur);
                }
                rec = master.getRecord(mfn);
                if ((rec != null) && (rec.getStatus() == Record.Status.ACTIVE)){
                    buff = getRecord(rec);
                    if ((buff != null) && (!buff.isEmpty())) {
                        writer.write(buff);
                    }
                }
            }
            writer.write(epilogue());
        } catch(IOException ioe) {
            throw new BrumaException(ioe);
        } finally {
            if (master != null) {
                master.close();
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch(IOException ioe) {
                }
            }
        }
    }

    protected String prologue() {
        return "";
    }

    protected String epilogue() {
        return "";
    }

    protected abstract String getRecord(final Record rec) throws BrumaException;
}
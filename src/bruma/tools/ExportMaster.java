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

package bruma.tools;

import bruma.BrumaException;
import bruma.impexp.AbstractMasterExport;
import bruma.impexp.ISO2709Export;
import bruma.impexp.IdMasterExport;
import bruma.impexp.JSONMasterExport;
import bruma.impexp.XmlMasterExport;
import bruma.master.Master;
import bruma.master.MasterFactory;

/**
 * Exports an Isis database from a Isis database or ISO2709 file or from an
 * Id file.
 * @author Heitor Barbieri
 */
public class ExportMaster {
    private static void usage() {
        System.err.println("usage: ExportMaster mst=<mstName>\nout=<outFile> " +
                           "outType={iso2709|id|json|xml}\n" +
                           "		    " +
                           "[from=<int>] [to=<int>] [tell=<num>]\n" +
                           "		    [mstEncod=<encoding>] " + 
                           "[idTag=<tag>] [outEncod=<encoding>]\n" +
                           "		    [--useFDT]");
        System.exit(1);
    }

    public static void main(final String[] args) throws BrumaException {
        if (args.length < 3) {
            usage();
        }
        String mst = null;
        String out = null;
        String outType = null;
        String mstEncod = Master.DEFAULT_ENCODING;
        String outEncod = null;
        int idTag = 0;
        int tell = Integer.MAX_VALUE;
        int from = 1;
        int to = Integer.MAX_VALUE;
        boolean useFDT = false;

        for (int index = 1; index < args.length; index++) {
            if (args[index].startsWith("mst=")) {
                mst = args[index].substring(4);
            } else if (args[index].startsWith("out=")) {
                out = args[index].substring(4);
            } else if (args[index].startsWith("outType=")) {
                outType = args[index].substring(8);
            } else if (args[index].startsWith("mstEncod=")) {
                mstEncod = args[index].substring(9);
            } else if (args[index].startsWith("outEncod=")) {
                outEncod = args[index].substring(9);
            } else if (args[index].startsWith("--useFDT")) {
                useFDT = true;
            } else if (args[index].startsWith("idTag=")) {
                idTag = Integer.parseInt(args[index].substring(6));
            } else if (args[index].startsWith("tell=")) {
                tell = Integer.parseInt(args[index].substring(5));
            } else if (args[index].startsWith("from=")) {
                from = Integer.parseInt(args[index].substring(5));
            } else if (args[index].startsWith("to=")) {
                to = Integer.parseInt(args[index].substring(3));
            } else {
                usage();
            }
        }
        
        if (mst == null) {
            throw new BrumaException("null master");
        }
        if (out == null) {
            throw new BrumaException("null output file");
        }
        if (tell  < 1) {
            throw new BrumaException("tell < 1");
        }
        if (from < 1) {
            throw new BrumaException("from < 1");
        }
        if (from > to) {
            throw new BrumaException("from > to");
        }
        if (outType == null) {
            throw new BrumaException("null output file type");
        }

        if (outType.equals("json")) {
            outEncod = "UTF-8";
        }
                
        final AbstractMasterExport ame;
        final Master imst = MasterFactory.getInstance(mst)
                                         .setEncoding(mstEncod).open();

        if (outType.equals("iso2709")) {
            ame = new ISO2709Export(imst, out, outEncod);
        } else if (outType.equals("id")) {
            ame = new IdMasterExport(imst, out, outEncod);
        } else if (outType.equals("xml")) {
            ame = new XmlMasterExport(imst, out, outEncod, useFDT);
        } else if (outType.equals("json")) {
            ame = new JSONMasterExport(imst, out, outEncod, useFDT, idTag);
        } else {
            imst.close();
            throw new BrumaException("invalid output file type [" + outType
                                                                  + "]");
        }        

        ame.export(from, to, tell);
    }
}

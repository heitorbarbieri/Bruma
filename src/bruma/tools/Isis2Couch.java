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

import bruma.master.Master;
import bruma.master.MasterFactory;
import bruma.master.Record;
import bruma.utils.TimeString;
import bruma.utils.Util;
import bruma.utils.ZeFDT;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Heitor Barbieri
 */
public class Isis2Couch {
    public static final int DEFAULT_COUCH_PORT = 5984;
    private static final int MAX_BUFFER_SIZE = 4194304; // 4 mbytes
    private static final int MAX_BULK_DOCS = 1000;

    private static void usage() {
        System.err.println("SYNOPSIS");
        System.err.println("       Isis2Couch <isismst> <couchhost> <couchdb> " 
                                                                   + "OPTIONS");
        System.err.println();
        System.err.println("DESCRIPTION");
        System.err.println("       Isis2Couch exports records from <isismst> " 
                           + "Isis master to the <couchdb> CouchDB database "
                           + "at the <couchhost> host machine.");
        System.err.println();
        System.err.println("OPTIONS");
        System.err.println("       --couchport=<num>");
        System.err.println("             CouchDB server port number.");
        System.err.println("       --couchuser=<user>");
        System.err.println("             CouchDB database admin username.");
        System.err.println("       --couchpsw=<password>");
        System.err.println("             CouchDB database admin password.");
        System.err.println("       --convtable=<file>");
        System.err.println("             Table to convert record tag to string.");
        System.err.println("             One conversion per line.");
        System.err.println("             ex:  10=Author.");
        System.err.println("       --fdt=<file>");
        System.err.println("             Uses an Isis fdt file to convert" +
                                         " record tag to string.");
        System.err.println("       --encoding=<str>");
        System.err.println("             Master record string encoding.");
        System.err.println("       --from=<mfn>");
        System.err.println("             Initial mfn to export.");
        System.err.println("       --to=<mfn>");
        System.err.println("             Final mfn to export.");
        System.err.println("       --tell=<num>");
        System.err.println("             Shows message at record with mfn that"
                                         + " are multiples of <num>.");
        System.err.println("       --idtag=<num>");
        System.err.println("             CouchDB _id field should be the content"
                                         + " of the record field <num>.");
        System.err.println("       --append");
        System.err.println("             Exported records show be appended to a"
                                         + " CouchDB database."); 
        System.err.println();
        System.err.println("EXIT STATUS");
        System.err.println("       Exits 0 on success, and >0 if an error "
                                   + "occurs.");
        System.exit(1);        
    }

    private static Map<Integer,String> parseConvTable(final String convTable) 
                                                            throws IOException {
        assert convTable != null;
        
        final Map<Integer,String> map = new HashMap<Integer,String>();
        final BufferedReader reader = new BufferedReader(
                                                     new FileReader(convTable));
        
        while (true) {
            final String line = reader.readLine();
            if (line == null) {
                break;
            }
            final String[] split = line.trim().split(" *= *", 2);
            if (split.length != 2) {
                throw new IOException("invalid convTable line: " + line);
            }
            map.put(Integer.getInteger(split[0]), split[1]);
        }
        reader.close();
        
        return map;
    }
    
    public static void main(final String[] args) throws Exception {
        if (args.length < 3) {
            usage();
        }
                
        final String isisMaster = args[0];
        final String couchHost = args[1];
        final String couchDbName = args[2];
        
        String couchPort = Integer.toString(DEFAULT_COUCH_PORT);
        String couchUser = null;
        String couchPswd = null;
        String convTable = null;
        String fdt = null;
        String encoding = Master.GUESS_ISO_IBM_ENCODING;
        int from = 1;
        int to = Integer.MAX_VALUE;
        int tell = Integer.MAX_VALUE;        
        int idTag = 0;
        boolean append = false;
        
        for (int idx = 3; idx < args.length; idx++) {
            if (args[idx].startsWith("--couchport=")) {
                couchPort= args[idx].substring(12);
            } else if (args[idx].startsWith("--couchuser=")) {
                couchUser = args[idx].substring(12);
            } else if (args[idx].startsWith("--couchpsw=")) {
                couchPswd = args[idx].substring(11);
            } else if (args[idx].startsWith("--convtable=")) {
                convTable = args[idx].substring(12);
                fdt = null;
            } else if (args[idx].startsWith("--fdt=")) {
                fdt = args[idx].substring(6);
                convTable = null;
            } else if (args[idx].startsWith("--encoding=")) {
                encoding = args[idx].substring(11);
            } else if (args[idx].startsWith("--from=")) {
                from = Integer.parseInt(args[idx].substring(7));
            } else if (args[idx].startsWith("--to=")) {
                to = Integer.parseInt(args[idx].substring(5));
            } else if (args[idx].startsWith("--tell=")) {
                tell = Integer.parseInt(args[idx].substring(7));                
            } else if (args[idx].startsWith("--idtag=")) {
                idTag = Integer.parseInt(args[idx].substring(8));                
            } else if (args[idx].equals("--append")) {
                append = true;
            } else {
                usage();
            }
        }

        if (isisMaster == null) {
            throw new IllegalArgumentException("null isisMaster");
        }
        if (to < from) {
            throw new IllegalArgumentException("to < from");
        }
        if (couchDbName == null) {
            throw new IllegalArgumentException("null couchDbName");
        }

        final Master mst = MasterFactory.getInstance(isisMaster)
                                      .setEncoding(encoding)                                      
                                      .open();
        final int xto = Math.min(to, mst.getControlRecord().getNxtmfn() - 1);
        final String fdtFileName = (fdt == null) ? null 
                                         : Util.changeFileExtension(fdt, "fdt");
        final boolean hasFdt = (fdt == null) ? false 
                                               : new File(fdtFileName).isFile();
        final boolean hasConvTable = (convTable == null) ? false 
                                               : new File(convTable).isFile();
        final Map<Integer,String> tags = hasFdt 
                   ? new ZeFDT().fromFile(fdtFileName).getFieldDescriptionMap()
                   : hasConvTable ? parseConvTable(convTable) : null;
        final StringBuilder builder = new StringBuilder("{\n");
        final TimeString ts = new TimeString();
        final SendJsonDocuments sender = new SendJsonDocuments(couchHost, 
                          couchPort, couchUser, couchPswd, couchDbName, append);
        boolean first = true;
        int bulkNum = 0;
        
        builder.append(" \"docs\": [\n");
                
        ts.start();
        
        System.out.println("\n== Isis2Couch ==\n\n");
        for (int mfn = from; mfn <= xto; mfn++) {
            if (mfn % tell == 0) {
                System.out.println("+++" + mfn);
            }
            final Record rec = mst.getRecord(mfn);
            if (rec.getStatus() == Record.Status.ACTIVE) {
                bulkNum++;
                if (first) {
                    first = false;
                } else {
                    builder.append(",");
                }
                builder.append(rec.toJSON3(idTag, tags));
                builder.append("\n");
                if ((builder.length() >= MAX_BUFFER_SIZE) || 
                    (bulkNum >= MAX_BULK_DOCS)) {
                    first = true;
                    bulkNum = 0;
                    builder.append(" ]\n}\n");
                    sender.sendDocuments(builder.toString());
                    builder.setLength(0);
                    builder.append("{\n \"docs\": [\n");
                }
            }
        }
        if (builder.length() > 0) {
            builder.append(" ]\n}\n");
            sender.sendDocuments(builder.toString());
        }

        System.out.println("total time = " + ts.getTime());
        mst.close();
    }        
}

class SendJsonDocuments {
    private final URL url;
    private final char[] buffer;
    private final StringBuilder builder;
    private final Matcher matcher;
    private final String baseUrl;

    SendJsonDocuments(final String couchHost,
                      final String couchPort,
                      final String couchUser,
                      final String couchPswd,
                      final String couchDbName,
                      final boolean append) throws MalformedURLException, 
                                                   IOException {                 
        assert couchHost != null;
        assert couchPort != null;
        assert couchDbName != null;

        final String userPswd = ((couchUser == null) || (couchPswd == null)) 
                                      ? "" : couchUser + ":" + couchPswd + "@";
        baseUrl = "http://" + userPswd + couchHost + ":" + couchPort + "/"; 
        url = new URL(baseUrl + couchDbName + "/_bulk_docs");
        buffer = new char[1024];
        builder = new StringBuilder();
        matcher = Pattern.compile("\"id\"\\:\"\\d+\",\"error\"\\:" +
                "\"[^\"]+\",\"reason\"\\:\"[^\"]+\"").matcher("");
        
        openDatabase(couchDbName, append);
    }

    private void openDatabase(final String couchDbName,
                              final boolean append) throws IOException {
        assert couchDbName != null;
        
        final URL dbasesUrl = new URL(baseUrl + "_all_dbs");
        final URL ourl = new URL(baseUrl + couchDbName);
        final HttpURLConnection hconn = 
                                  (HttpURLConnection)dbasesUrl.openConnection();                        
        hconn.setRequestMethod("GET");            
        hconn.connect();
        final BufferedReader reader = new BufferedReader(
                             new InputStreamReader(hconn.getInputStream()));
        boolean exists = false;
        
        while (!exists) {
            final String line = reader.readLine();
            if (line == null) {
                break;
            }
            exists = line.contains("\"" + couchDbName + "\"");
        }
        reader.close();
                
        if (exists) {
            if (!append) { // Reset database if required
                final HttpURLConnection conn2 = 
                                       (HttpURLConnection)ourl.openConnection();                        
                conn2.setRequestMethod("DELETE");            
                conn2.connect();
                new InputStreamReader(conn2.getInputStream()).close();
                final HttpURLConnection conn3 = 
                                       (HttpURLConnection)ourl.openConnection();                       
                conn3.setRequestMethod("PUT");
                conn3.connect();   
                new InputStreamReader(conn3.getInputStream()).close();
            }
        } else {  // Create database        
            final HttpURLConnection conn3 = 
                                       (HttpURLConnection)ourl.openConnection();                       
            conn3.setRequestMethod("PUT");
            conn3.connect();   
            new InputStreamReader(conn3.getInputStream()).close();
        }
    }
    
    List<String> getBadDocuments(final Reader reader) throws IOException {
        assert reader != null;
        
        final List<String> msgs = new ArrayList<String>();
            
        builder.setLength(0);
        while (true) {        
            final int read = reader.read(buffer);
            if (read == -1) {
                break;
            }
            builder.append(buffer, 0, read);
        }
        matcher.reset(builder);
        while (matcher.find()) {
            msgs.add(matcher.group(0));
        }
        return msgs;
    }
    
    void sendDocuments(final String docs) throws IOException  {
        assert docs != null;

        final URLConnection connection = url.openConnection();
        connection.setRequestProperty("CONTENT-TYPE", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.connect();

        final OutputStreamWriter out = new OutputStreamWriter(
                                                  connection.getOutputStream());
        //final String udocs = URLEncoder.encode(docs, "UTF-8");

        out.write(docs);
        out.close();

        final InputStreamReader reader = new InputStreamReader(
                                                   connection.getInputStream());         
        final List<String> msgs = getBadDocuments(reader);

        for (String msg: msgs) {
            System.err.println("write error: " + msg);
        }
        reader.close();
    }
}

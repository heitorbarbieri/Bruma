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
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Heitor Barbieri
 */
public class Isis2Couch {
    public static final String DEFAULT_COUCH_HOST = "localhost";
    public static final int DEFAULT_COUCH_PORT = 5984;
    //private static final int MAX_BUFFER_SIZE = 53137200; // 50 mbytes
    private static final int MAX_BUFFER_SIZE = 4194304; // 4 mbytes

    private static void usage() {
        System.err.println("usage: Isis2Couch -isisMaster=<name> -couchDbName=<name>\n"
                + "[-encod=<encoding>] [-from=<num>] [-to=<num>] [-idTag=<num>]"
                + "\n[-couchHost=<host>] [-couchPort=<port>] "
                + "\n[-threads=<num>] [-tell=<num>] [--useFDT]");
        System.exit(1);
    }

    public static void main(final String[] args) throws Exception {
        if (args.length < 2) {
            usage();
        }

        System.out.println("\n== Isis2Couch ==\n\n");
        
        String isisMaster = null;
        String encoding = Master.GUESS_ISO_IBM_ENCODING;
        int from = 1;
        int to = Integer.MAX_VALUE;
        String couchHost = DEFAULT_COUCH_HOST;
        int couchPort = DEFAULT_COUCH_PORT;
        String couchDbName = null;
        int tell = Integer.MAX_VALUE;
        boolean allOrNothing = false;
        boolean useFDT = false;
        Record rec;
        int idTag = 0;
        int threads = SendJsonDocumentsPool.DEFAULT_THREADS;

        for (int index = 0; index < args.length; index++) {
            if (args[index].startsWith("-isisMaster=")) {
                isisMaster = args[index].substring(12);
            } else if (args[index].startsWith("-encod=")) {
                encoding = args[index].substring(7);
            } else if (args[index].startsWith("-from=")) {
                from = Integer.parseInt(args[index].substring(6));
            } else if (args[index].startsWith("-to=")) {
                to = Integer.parseInt(args[index].substring(4));
            } else if (args[index].startsWith("-couchHost=")) {
                couchHost = args[index].substring(11);
            } else if (args[index].startsWith("-couchPort=")) {
                couchPort = Integer.parseInt(args[index].substring(11));
            } else if (args[index].startsWith("-couchDbName=")) {
                couchDbName = args[index].substring(13);
            } else if (args[index].startsWith("-threads=")) {
                threads = Integer.parseInt(args[index].substring(9));
            } else if (args[index].startsWith("-tell=")) {
                tell = Integer.parseInt(args[index].substring(6));
            } else if (args[index].startsWith("-idTag=")) {
                idTag = Integer.parseInt(args[index].substring(7));
            } else if (args[index].equals("--AllOrNothing")) {
                allOrNothing = true;
            } else if (args[index].equals("--useFDT")) {
                useFDT = true;
            } else {
                System.err.println("unknown: " + args[index] + "\n");
                usage();
            }
        }

        if (isisMaster == null) {
            throw new IllegalArgumentException("null isisMaster");
        }
        if (to < from) {
            throw new IllegalArgumentException("to < from");
        }
        if (threads < 1) {
            throw new IllegalArgumentException("threads < 1");
        }
        if (couchDbName == null) {
            throw new IllegalArgumentException("null couchDbName");
        }

        final Master mst = MasterFactory.getInstance(isisMaster)
                                      .setEncoding(encoding)                                      
                                      .open();
        final int xto = Math.min(to, mst.getControlRecord().getNxtmfn() - 1);
        final String fileName =
                             Util.changeFileExtension(mst.getMasterName(), "fdt");
        final boolean hasFdt = new File(fileName).isFile();
        final Map<Integer,String> tags = (useFDT && hasFdt) ?
             new ZeFDT().fromFile(mst.getMasterName()).getFieldDescriptionMap()
                                                            : null;
        final StringBuilder builder = new StringBuilder("{\n");
        final TimeString ts = new TimeString();
        final SendJsonDocumentsPool pThreads = new SendJsonDocumentsPool(
                   couchHost, Integer.toString(couchPort), couchDbName, threads);

        if (allOrNothing) {
            builder.append(" \"all_or_nothing\": true,\n");
        }
        builder.append(" \"docs\": [\n");
                
        ts.start();
        for (int mfn = from; mfn <= xto; mfn++) {
            if (mfn % tell == 0) {
                System.out.println("+++" + mfn + " - " + ts.getTime());
            }
            rec = mst.getRecord(mfn);
            if (rec.getStatus() == Record.Status.ACTIVE) {
                builder.append(rec.toJSON3(idTag));
                if (rec.getMfn() == xto) {
                    builder.append(",");
                }
                builder.append("\n");
                if (builder.length() >= MAX_BUFFER_SIZE) {
                    builder.append(" ]\n}\n");
                    pThreads.sendDocuments(builder.toString());
                    builder.setLength(0);
                    builder.append("{\n \"docs\": [\n");
                }
            }
        }
        if (builder.length() > 13) {
            builder.append(" ]\n}\n");
            pThreads.sendDocuments(builder.toString());
        }
        pThreads.finishSending();

        System.out.println("total time = " + ts.getTime());
        mst.close();
    }
}

class SendJsonDocuments extends Thread {
    private final String couchHost;
    private final String couchPort;
    private final String couchDbName;
    private final Integer lock;
    private boolean finished;
    private String docs;

    SendJsonDocuments(final String couchHost,
                      final String couchPort,
                      final String couchDbName) {
        assert couchHost != null;
        assert couchDbName != null;

        this.couchHost = couchHost;
        this.couchPort = couchPort;
        this.couchDbName = couchDbName;
        this.lock = new Integer(0);
        docs = null;
        finished = false;
    }

    void sendDocuments(final String docs) throws Exception {
        assert docs != null;
        
        synchronized(lock) {
            this.docs = docs;
            lock.notify();
        }
    }

    void finishSending() {
        finished = true;
         synchronized(lock) {
            lock.notifyAll();
         }
    }

    boolean isFree() {
        return docs == null;
    }

    /*
     @Override
    public void run() {
        try {
            final URL url = new URL("http://" + couchHost + ":" + couchPort
                               + "/" + couchDbName + "/_bulk_docs");
            final URLConnection connection = url.openConnection();

            connection.setRequestProperty("CONTENT-TYPE",
                                                       "application/json");
            connection.setDoOutput(true);
            connection.setConnectTimeout(10 * 60 * 1000);

            final OutputStreamWriter out = new OutputStreamWriter(
                                             connection.getOutputStream());
            final StringBuilder builder = new StringBuilder();

            while (true) {
                synchronized(lock) {
                    lock.wait();
                }                
                if (finished) {
                    break;
                }
                assert docs != null;
                                               
                //final String udocs = URLEncoder.encode(docs, "UTF-8");

                System.out.println("sending [" + docs.length() + " bytes] ...");
                out.write(docs);
                out.close();

                final char[] buffer = new char[100];
                final InputStreamReader reader = new InputStreamReader(
                                                  connection.getInputStream());

                if (reader.read(buffer) == -1) {
                    throw new IOException("read error");
                }
                builder.append(buffer);
                builder.append(" ...");                
                reader.close();
                System.out.println("... data sent: OK => " + builder.toString());
            } catch (Exception ex) {
                Logger.getLogger(SendJsonDocuments.class.getName())
                                                  .log(Level.SEVERE, null, ex);
            }
            docs = null;
        }
    }
    */

    @Override
    public void run() {        
        while (true) {

            try {
                synchronized(lock) {
                    lock.wait();
                }
                if (finished) {
                    break;
                }
                assert docs != null;

                /*final URL url = new URL("http://" + couchHost + ":" + couchPort
                               + "/" + couchDbName + "/");*/
                final URL url = new URL("http://" + couchHost + ":" + couchPort
                               + "/" + couchDbName + "/_bulk_docs");
                //System.out.println("Connecting to " + url.toString());
                final URLConnection connection = url.openConnection();
                //System.out.println("step 1");
                connection.setRequestProperty("CONTENT-TYPE",
                                                           "application/json");
                connection.setDoOutput(true);
                //connection.setConnectTimeout(10 * 60 * 1000);
                //System.out.println("step 2");
                //connection.connect();
                //System.out.println("step 3");

                final OutputStreamWriter out = new OutputStreamWriter(
                                                 connection.getOutputStream());
                //System.out.println("step 4");
                final StringBuilder builder = new StringBuilder();
                //final String udocs = URLEncoder.encode(docs, "UTF-8");

                System.out.println("sending [" + docs.length() + " bytes] ...");
                //System.out.println(docs.substring(0, 4000));
                out.write(docs);
                out.flush();
                //System.out.println("step 5");
                out.close();
                //System.out.println("step 6");

                final char[] buffer = new char[100];
                final InputStreamReader reader = new InputStreamReader(
                                                  connection.getInputStream());

                if (reader.read(buffer) == -1) {
                    throw new IOException("read error");
                }
                builder.append(buffer);
                builder.append(" ...");
                reader.close();
                //out.close();
                System.out.println("... data sent: OK => " + builder.toString());
            } catch (Exception ex) {
                Logger.getLogger(SendJsonDocuments.class.getName())
                                                  .log(Level.SEVERE, null, ex);
            }
            docs = null;
        }
    }
}

class SendJsonDocumentsPool {
    static final int DEFAULT_THREADS = 1;

    private final String couchHost;
    private final String couchPort;
    private final String couchDbName;
    private final int threadNum;
    private final SendJsonDocuments[] threads;
    private int curIndex;

    SendJsonDocumentsPool(final String couchHost,
                          final String couchPort,
                          final String couchDbName,
                          final int threadNum) {
        assert couchHost != null;
        assert couchDbName != null;
        assert threadNum > 0;

        this.couchHost = couchHost;
        this.couchPort = couchPort;
        this.couchDbName = couchDbName;
        this.threadNum = threadNum;
        this.threads = new SendJsonDocuments[threadNum];
        this.curIndex = 0;

        for (int counter = 0; counter < threadNum; counter++) {
            threads[counter] =
                      new SendJsonDocuments(couchHost, couchPort, couchDbName);
            threads[counter].start();
        }
    }

    void sendDocuments(final String docs) throws Exception {
        assert docs != null;

        int auxIndex = curIndex;

        while (true) {
            if (threads[auxIndex].isFree()) {
                threads[auxIndex].sendDocuments(docs);
                curIndex = auxIndex;
                break;
            } else {
                if (++auxIndex >= threadNum) {
                    auxIndex = 0;
                }
                if (auxIndex == curIndex) {
                    Thread.sleep(1000);
                }
            }
        }
    }

    void finishSending() {
        int total;

        while (true) {
            total = 0;

            for (int index = 0; index < threadNum; index++) {
                if (threads[index].isFree()) {
                    threads[index].finishSending();
                    total++;
                }
            }
            if (total == threadNum) {
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                Logger.getLogger(SendJsonDocumentsPool.class.getName())
                                                  .log(Level.SEVERE, null, ex);
            }
        }
    }
}

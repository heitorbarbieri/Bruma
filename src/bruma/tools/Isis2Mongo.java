/*=========================================================================

    Copyright © 2012 BIREME/PAHO/WHO

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
import bruma.master.Field;
import bruma.master.Master;
import bruma.master.MasterFactory;
import bruma.master.Record;
import bruma.master.Subfield;
import bruma.utils.Util;
import bruma.utils.ZeFDT;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 *
 * @author Heitor Barbieri
 * @date 20120528
 */
public class Isis2Mongo {
    public static final int DEFAULT_PORT = 27017;
    public static final int DEFAULT_BUFFER_SIZE = 1000;

    private static void usage() {
        System.err.println("usage: Isis2Mongo -isisMaster=<name>"
          + " -mongoDbName=<name> -collection=<name>\n"
          + "[-mongoHost=<host>] [-mongoPort=<port>]\n"
          + "[-user=<str> -password=<str>]\n"
          + "[-encod=<encoding>] [-from=<num>] [-to=<num>] [-idTag=<num>]\n"
          + "[-tell=<num>] [--useFDT] [--useOnlyFDT] [--clearCollection]");
        System.exit(1);
    }

    public static void main(final String[] args) {
        if (args.length < 3) {
            usage();
        }

        String isisMaster = null;
        String mongoDbName = null;
        String mongoCollection = null;
        String host = "localhost";
        int port = DEFAULT_PORT;
        String user = null;
        String password = null;
        String encoding = Master.GUESS_ISO_IBM_ENCODING;
        int from = 1;
        int to = Integer.MAX_VALUE;
        int idTag = 0;
        int tell = Integer.MAX_VALUE;
        boolean useFDT = false;
        boolean useOnlyFDT = false;
        boolean clearCollection = false;

        for (String arg : args) {
            if (arg.startsWith("-isisMaster=")) {
                isisMaster = arg.substring(12);
            } else if (arg.startsWith("-mongoDbName=")) {
                mongoDbName = arg.substring(13);
            } else if (arg.startsWith("-collection=")) {
                mongoCollection = arg.substring(12);
            } else if (arg.startsWith("-mongoHost=")) {
                host = arg.substring(11);
            } else if (arg.startsWith("-mongoPort=")) {
                port = Integer.parseInt(arg.substring(11));
            } else if (arg.startsWith("-user=")) {
                user = arg.substring(6);
            } else if (arg.startsWith("-password=")) {
                password = arg.substring(10);
            } else if (arg.startsWith("-encod=")) {
                encoding = arg.substring(7);
            } else if (arg.startsWith("-from=")) {
                from = Integer.parseInt(arg.substring(6));
            } else if (arg.startsWith("-to=")) {
                to = Integer.parseInt(arg.substring(4));
            } else if (arg.startsWith("-idTag=")) {
                idTag = Integer.parseInt(arg.substring(7));
            } else if (arg.startsWith("-tell=")) {
                tell = Integer.parseInt(arg.substring(6));
            } else if (arg.equals("--useFDT")) {
                useFDT = true;
            } else if (arg.equals("--useOnlyFDT")) {
                useOnlyFDT = true;
            } else if (arg.equals("--clearCollection")) {
                clearCollection = true;
            } else {
                usage();
            }
        }

        final Logger log = Logger.getLogger("Isis2Mongo");
        final List<DBObject> buffer = new ArrayList<DBObject>();

        try {
            final DB database = getDatabase(host, port, user,
                                                         password, mongoDbName);
            final DBCollection collection =
                                        database.getCollection(mongoCollection);
            final Master mst = MasterFactory.getInstance(isisMaster)
                                            .setEncoding(encoding).open();
            final boolean hasFdt = new File(Util.changeFileExtension(
                                 isisMaster, ZeFDT.DEFAULT_EXTENSION)).isFile();
            final ZeFDT fdt = ((useFDT || useOnlyFDT) && hasFdt)
                                      ? new ZeFDT().fromFile(isisMaster) : null;
            final Map<Integer,ZeFDT.ZeFDTField> mfdt = (fdt == null) ? null :
                                                 fdt.getFieldDescriptionMapEx();

            to = Math.min(to, mst.getControlRecord().getNxtmfn() - 1);
            int cur = 0;

            if (clearCollection) {
                collection.drop();
            }

            for (int mfn = from; mfn <= to; mfn++) {
                final Record rec = mst.getRecord(mfn);

                if (rec.getStatus() == Record.Status.ACTIVE) {
                    buffer.add(createDocument(rec, idTag, mfdt, useOnlyFDT));
                    if (buffer.size() == DEFAULT_BUFFER_SIZE) {
                        final String errMess =
                                           collection.insert(buffer).getError();
                        if (errMess != null) {
                            log.warning(errMess);
                        }
                        buffer.clear();
                    }
                }

                if (++cur == tell) {
                    cur = 0;
                    log.info("+++ " + Integer.toString(mfn));
                }
            }
            if (!buffer.isEmpty()) {
                final String errMess = collection.insert(buffer).getError();
                if (errMess != null) {
                    log.warning(errMess);
                }
            }

            mst.close();
            log.info("Importing ");
            log.info(args[0]);
            log.info(" finished.");
            log.info("Documents imported: ");
            log.info(Integer.toString(cur));
            log.info("Total documents : ");
            log.info(Long.toString(collection.count()));
        } catch(Exception ex) {
            log.severe(ex.getMessage());
        }
    }

    private static DB getDatabase(final String host,
                                  final int port,
                                  final String user,
                                  final String password,
                                  final String mongoDb) throws IOException {
        final MongoClient mongo = new MongoClient(host, port);
        final DB db = mongo.getDB(mongoDb);

        if (user != null) {
            if (!db.authenticate(user, password.toCharArray())) {
                throw new IOException("database authentication failed");
            }
        }
        return db;
    }

    private static BasicDBObject createDocument(final Record rec,
                                       final int idTag,
                                       final Map<Integer,ZeFDT.ZeFDTField> mfdt,
                                       final boolean useOnlyFDT)
                                                         throws BrumaException {
        assert rec != null;
        assert idTag > 0;

        final BasicDBObject doc = new BasicDBObject();
        final Map<Integer, List<Field>> flds =
                                        new TreeMap<Integer,List<Field>> ();
        if (idTag <= 0) {
            doc.put("_id", rec.getMfn());
        } else {
            final ZeFDT.ZeFDTField fdt = (mfdt == null)? null : mfdt.get(idTag);
            final boolean isNumeric = (fdt == null) ? false : fdt.isNumeric();
            final String id = rec.getField(idTag, 1).getContent();

            if (isNumeric) {
                doc.put("_id", Integer.valueOf(id));
            } else {
                doc.put("_id", id);
            }
        }

        doc.put("nvf", rec.getNvf());

        for (Field fld: rec) {
            List<Field> fldLst = flds.get(fld.getId());
            if (fldLst == null) {
                fldLst = new ArrayList<Field>();
                flds.put(fld.getId(), fldLst);
            }
            fldLst.add(fld);
        }
        for (Map.Entry<Integer, List<Field>> entry : flds.entrySet()) {
            final int tag = entry.getKey();
            final ZeFDT.ZeFDTField fdt = (mfdt == null) ? null : mfdt.get(tag);
            final String auxId = (fdt == null) ? null : fdt.getDescription();
            final boolean isNumeric = (fdt == null) ? false : fdt.isNumeric();
            final String fid = (auxId == null) ? (useOnlyFDT ? null : "v" + tag)
                                               : auxId;

            if (fid != null) {
                final List<Field> fields = entry.getValue();
                final int size = fields.size();

                if (size == 1) {
                    if (isNumeric) {
                        doc.put(fid, getNumSubfields(fields.get(0)));
                    } else {
                        doc.put(fid, getStrSubfields(fields.get(0)));
                    }
                } else if (size > 1) {
                    final BasicDBList objL = new BasicDBList();
                    for (Field fld : fields) {
                        if (isNumeric) {
                            objL.add(getNumSubfields(fld));
                        } else {
                            objL.add(getStrSubfields(fld));
                        }
                    }
                    doc.put(fid, objL);
                }
            }
        }

        return doc;
    }

    private static BasicDBObject getStrSubfields(final Field fld) {
        final BasicDBObject obj = new BasicDBObject();
        final Map<Character, List<String>> map =
                                         new HashMap<Character, List<String>>();

        for (Subfield sub: fld) {
            final char id = sub.getId();
            final List<String> lst;

            if (map.containsKey(id)) {
                lst = map.get(id);
            } else {
                lst = new ArrayList<String>();
                map.put(id, lst);
            }
            lst.add(sub.getContent());
        }
        for (Map.Entry<Character, List<String>> entry : map.entrySet()) {
            final String id = entry.getKey().toString();
            final List<String> lst = entry.getValue();

            if (lst.size() == 1) {
                obj.put(id, lst.get(0));
            } else {
                final BasicDBList bobj = new BasicDBList();
                for (String elem : lst) {
                    bobj.add(elem);
                }
                obj.put(id, bobj);
            }
        }

        return obj;
    }

    private static BasicDBObject getNumSubfields(final Field fld) {
        final BasicDBObject obj = new BasicDBObject();
        final Map<Character, List<Integer>> map =
                                        new HashMap<Character, List<Integer>>();

        for (Subfield sub: fld) {
            final char id = sub.getId();
            final List<Integer> lst;

            if (map.containsKey(id)) {
                lst = map.get(id);
            } else {
                lst = new ArrayList<Integer>();
                map.put(id, lst);
            }
            lst.add(Integer.valueOf(sub.getContent()));
        }
        for (Map.Entry<Character, List<Integer>> entry : map.entrySet()) {
            final String id = entry.getKey().toString();
            final List<Integer> lst = entry.getValue();

            if (lst.size() == 1) {
                obj.put(id, lst.get(0));
            } else {
                final BasicDBList bobj = new BasicDBList();
                for (Integer elem : lst) {
                    bobj.add(elem);
                }
                obj.put(id, bobj);
            }
        }

        return obj;
    }
}

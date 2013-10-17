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
import bruma.master.*;
import bruma.utils.ZeFDT;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Heitor Barbieri
 */
public class Statistics {
    private enum text_type {ALPHANUM, ALPHA, NUMERIC, UNKNOWN }

    private class Profile {
        int total = 0;         // numero total de vezes que apareceu
        int presenceTimes = 0; // numero de registros/campos que apareceu
        int minTimes = Integer.MAX_VALUE; // numero min de vezes que apareceu em um registro/campo
        int maxTimes = 0;      // numero max de vezes que apareceu em um registro/campo
        int minLen = Integer.MAX_VALUE;   // comprimento minimo
        int maxLen = 0;        // comprimento maximo
        long totLen = 0;       // soma dos comprimentos
        text_type type = text_type.UNKNOWN; // tipo do campo/subcampo
        Map<Character,Profile> sub = null;  // perfil dos subcampos
    }

    private int active = 0;
    private int deleted = 0;
    private int minrl = Integer.MAX_VALUE;   // min rec size
    private int maxrl = 0;                   // max rec size
    private long totrl = 0;                  // tot rec size

    /**
     * Tabula o conteudo de um determinado campo da base de dados
     * @param dbname nome da base de dados
     * @param encoding codificação dos caracteres da base de dados
     * @param from mfn inicial
     * @param to mfn final
     * @param tag identificador do campo a ser tabulado
     * @param tell poe aviso na saida a cada tell registros
     * @return String contendo o resultado da tabulacao
     * @throws BrumaException
     */
    public String tab(final String dbname,
                      final String encoding,
                      final int from,
                      final int to,
                      final int tag,
                      final int tell) throws BrumaException {
        if (dbname == null) {
            throw new BrumaException("null dbname");
        }
        if (encoding == null) {
            throw new BrumaException("null encoding");
        }
        if (from < 0) {
            throw new BrumaException("from < 0");
        }
        if (to < from) {
            throw new BrumaException("to < from");
        }
        if (tag < 0) {
            throw new BrumaException("tag < 0");
        }
        if (tell < 0) {
            throw new BrumaException("tell < 0");
        }

        final Master mst = MasterFactory.getInstance(dbname)
                                        .setEncoding(encoding)
                                        .open();
        final StringBuilder sb = new StringBuilder();
        final Map<String,Integer> map = new TreeMap<String,Integer>();
        final Control control = mst.getControlRecord();
        final int first = Math.min(from, control.getNxtmfn() - 1);
        final int last = Math.min(to, control.getNxtmfn() - 1);
        Record rec;
        int ntell = (tell == 0) ? Integer.MAX_VALUE : tell;
        int cur = 0;
        String field;
        Integer total;

        for (int mfn = first; mfn <= last; mfn++) {
            rec = mst.getRecord(mfn);
            if (++cur % ntell == 0) {
                Logger.getGlobal().info("++" + cur);
            }
            if (rec.getStatus() == Record.Status.ACTIVE) {
                for (Field fld : rec.getFieldList(tag)) {
                  field = fld.getContent().trim();
                  total = map.get(field);
                  if (total == null) {
                      total = 0;
                  }
                  map.put(field, ++total);
                }
            }
        }

        for (Map.Entry<String,Integer> entry : map.entrySet()) {
            sb.append("[" + entry.getValue()  + "] : " + entry.getKey() + "\n");
        }

        mst.close();

        return sb.toString();
    }

    /**
     * Faz a estatistica da base de dados, seus (sub)campos e caracteres.
     * @param dbname nome da base de dados
     * @param encoding codificação dos caracteres da base de dados
     * @param from mfn inicial
     * @param to mfn final
     * @param tell poe aviso na saida a cada tell registros
     * @return String contendo o resultado da tabulacao
     * @throws BrumaException
     */
    public String stat(final String dbname,
                       final String encoding,
                       final int from,
                       final int to,
                       final int tell) throws BrumaException {
        if (dbname == null) {
            throw new BrumaException("null dbname");
        }
        if (encoding == null) {
            throw new BrumaException("null encoding");
        }
        if (from < 0) {
            throw new BrumaException("from < 0");
        }
        if (to < from) {
            throw new BrumaException("to < from");
        }
        if (tell < 0) {
            throw new BrumaException("tell < 0");
        }

        final Master mst = MasterFactory.getInstance(dbname)
                                        .setEncoding(encoding)
                                        .open();
        final Map<Integer,Profile> fldProf = new TreeMap<Integer,Profile>();
        final Map<Integer,Integer> fieldsInRec = new HashMap<Integer,Integer>();
        final Map<Character,Integer> characters =
                                              new TreeMap<Character,Integer> ();
        final Control control = mst.getControlRecord();
        final int first = Math.min(from, control.getNxtmfn() - 1);
        final int last = Math.min(to, control.getNxtmfn() - 1);
        Record rec;
        int ntell = (tell == 0) ? Integer.MAX_VALUE : tell;
        int cur = 0;
        int tag;
        int len;
        Integer amount;
        String content;
        String ret;
        Profile prof;

        active = 0;
        deleted = 0;
        maxrl = 0;
        minrl = Integer.MAX_VALUE;
        totrl = 0;

        for (int mfn = first; mfn <= last; mfn++) {
            rec = mst.getRecord(mfn);

            if (++cur % ntell == 0) {
                Logger.getLogger(Statistics.class.getName()).log(Level.INFO,"++"
                                                                         + cur);
            }
            if (rec.getStatus() == Record.Status.ACTIVE) {
                active++;
                len = rec.getRecordLength(mst.getEncoding(), mst.isFFI());
                maxrl = Math.max(maxrl, len);
                minrl = Math.min(minrl, len);
                totrl += len;
                fieldsInRec.clear();

                for (Field field : rec) {
                    tag = field.getId();
                    content = field.getContent();
                    amount = content.length();

                    prof = fldProf.get(tag);
                    if (prof == null) {
                        prof = new Profile();
                        fldProf.put(tag, prof);
                    }
                    prof.total++;
                    prof.minLen = Math.min(prof.minLen, amount);
                    prof.maxLen = Math.max(prof.maxLen, amount);
                    prof.totLen += amount;
                    if (prof.sub == null) {
                        prof.sub = new TreeMap<Character,Profile>();
                    }

                    parseSubFields(field, characters, prof.sub);

                    if (prof.type != text_type.ALPHANUM) {
                        for (Profile subProf : prof.sub.values()) {
                            if (subProf.type == text_type.NUMERIC) {
                                if (prof.type == text_type.ALPHA) {
                                    prof.type = text_type.ALPHANUM;
                                    break;
                                } else {
                                    prof.type = text_type.NUMERIC;
                                }
                            } else if (subProf.type == text_type.ALPHA) {
                                if (prof.type == text_type.NUMERIC) {
                                    prof.type = text_type.ALPHANUM;
                                    break;
                                } else {
                                    prof.type = text_type.ALPHA;
                                }
                            } else if (subProf.type == text_type.ALPHANUM) {
                                prof.type = text_type.ALPHANUM;
                                break;
                            }
                        }
                    }

                    amount = fieldsInRec.get(tag);
                    if (amount == null) {
                        amount = 0;
                        prof.presenceTimes++;
                    }
                    fieldsInRec.put(tag, amount+1);
                }

                for (Map.Entry<Integer,Integer> entry : fieldsInRec.entrySet()){
                    prof = fldProf.get(entry.getKey());
                    prof.minTimes = Math.min(prof.minTimes, entry.getValue());
                    prof.maxTimes = Math.max(prof.maxTimes, entry.getValue());
                }
            } else {
                deleted++;
            }
        }
        ret = showResults(mst, control, characters, fldProf);

        mst.close();

        return ret;
    }

    private void parseSubFields(final Field field,
                                final Map<Character,Integer> characters,
                                final Map<Character,Profile> subProf) {
        assert field != null;
        assert characters != null;
        assert subProf != null;

        final Map<Character,Integer> tot = new HashMap<Character,Integer>();
        Character ch;
        Profile prof;
        String content;
        Integer amount;
        int length;

        for (Subfield sub : field) {
            ch = Character.toLowerCase(sub.getId());
            content = sub.getContent();
            length = content.length();

            prof = subProf.get(ch);
            if (prof == null) {
                prof = new Profile();
                subProf.put(ch, prof);
            }
            prof.total++;
            prof.minLen = Math.min(prof.minLen, length);
            prof.maxLen = Math.max(prof.maxLen, length);
            prof.totLen += length;

            amount = tot.get(ch);
            if (amount == null) {
                amount = 0;
                prof.presenceTimes++;
            }
            tot.put(ch, ++amount);

            for (Character ch2 : content.toCharArray()) {
                amount = characters.get(ch2);
                if (amount == null) {
                    amount = 0;
                }
                amount++;
                characters.put(ch2, amount);
                if (prof.type != text_type.ALPHANUM) {
                    if (Character.isDigit(ch2)) {
                        if (prof.type == text_type.ALPHA) {
                            prof.type = text_type.ALPHANUM;
                        } else {
                            prof.type = text_type.NUMERIC;
                        }
                    } else {
                        if (prof.type == text_type.NUMERIC) {
                            prof.type = text_type.ALPHANUM;
                        } else {
                            prof.type = text_type.ALPHA;
                        }
                    }
                }
            }
        }
        for (Map.Entry<Character,Integer> entry : tot.entrySet()) {
            prof = subProf.get(entry.getKey());
            prof.minTimes = Math.min(prof.minTimes, entry.getValue());
            prof.maxTimes = Math.max(prof.maxTimes, entry.getValue());
        }
    }

    private String alignResult(final List<String> elems,
                               final int colSize,
                               final boolean alignRight) {
        assert elems != null;
        assert colSize > 0;

        final String whites = "                                               ";
        final StringBuilder sb = new StringBuilder();
        String filler;
        int size;

        for (String elem : elems) {
            size = elem.length();
            if (size < colSize) {
                filler = whites.substring(0, colSize - size);
                if (alignRight) {
                    sb.append(filler);
                    sb.append(elem);
                } else {
                    sb.append(elem);
                    sb.append(filler);
                }
            } else {
                sb.append(elem);
            }
        }

        return sb.toString();
    }

    private String paddingWhites(final String in,
                                 final int outSize) {
        assert (in != null) : "paddingWhites/null in";
        assert (outSize > 0) : "paddingWhites/outSize <= 0";

        String ret = in;

        if (in.length() < outSize) {
            StringBuilder out = new StringBuilder(in);
            int rem = outSize - in.length();

            if (rem > 0) {
                char[] whites = new char[rem];

                Arrays.fill(whites, ' ');
                out.append(whites);
            }
            ret = out.toString();
        }

        return ret;
    }

    private String showResults(final Master mst,
                               final Control control,
                               final Map<Character,Integer> characters,
                               final Map<Integer,Profile> profiles)
                                                        throws BrumaException {
        class Elem implements Comparable<Elem> {
            Integer tot;
            Character val;

            public Elem(Integer tot, Character val) {
                this.tot = tot;
                this.val = val;
            }

            @Override
            public int compareTo(Elem o)  {
                if (o == null) {
                    throw new NullPointerException("null Elem");
                }
                return (tot == o.tot) ?  val.compareTo(o.val) : tot - o.tot;
            }
        }

        assert mst != null;
        assert control != null;
        assert characters != null;
        assert profiles != null;

        final StringBuilder sb = new StringBuilder();
        final List<String> elems = new ArrayList<String>();
        final TreeSet<Elem> chts = new TreeSet<Elem>();
        final String encoding = mst.getEncoding();
        final ZeFDT fdt = new ZeFDT();
        final File mstFile = new File(mst.getMasterName());
        Map<Integer,String> fnames;
        String fname;
        String subfields;
        int totalRec = active + deleted;
        int total;
        Profile prof;
        int presence;
        int colSize = 10;
        int size = mst.getGigaSize();
        int totChar = 0;
        boolean presenceAll = (active == totalRec);

        try {
            fdt.fromFile(mst.getMasterName());
            fnames = new HashMap<Integer,String>();
            for (ZeFDT.ZeFDTField fdtField : fdt.getFieldDescriptions()) {
                fnames.put(fdtField.getTag(), fdtField.getDescription());
            }
        } catch(Exception ex) {
            fnames = null;
        }

        sb.append("\nAnalysis Date: " + new Date() + "\n");

        sb.append("\n\nDatabase:\n");
        sb.append("   dbname = " + mstFile.getAbsolutePath() + "\n");
        sb.append("   platform = " + (mst.getDataAlignment() == 0 ? "Windows"
                                                            : "Linux") + "\n");
        sb.append("   encoding = " + encoding + "\n");
        sb.append("   ffi = " + mst.isFFI() + "\n");
        sb.append("   swapped = " + mst.isSwapped() + "\n");
        sb.append("   max size = ");
        if (size == 0) {
            sb.append("512 megabytes\n");
        } else {
            sb.append(mst.getGigaSize() + " gigabytes\n");
        }
        sb.append("   next mfn = " + control.getNxtmfn() + "\n");
        sb.append("   data entry lock (DEL) = " + control.getMfcxx2() + "\n");
        sb.append("   exclusive write lock (EWL) = " + control.getMfcxx3()
                                                                        + "\n");

        sb.append("\n\nRecords:\n");
        sb.append(String.format("   active records = "
                + (presenceAll ? "(A) " : "") + "%1$d (%2$.2f%%)",
                           active, ((float)active/totalRec) * 100) + "\n");
        sb.append(String.format("   deleted records = %1$d (%2$.2f%%)",
                           deleted, ((float)deleted/totalRec) * 100) + "\n");
        sb.append("   max record size = " + maxrl + "\n");
        sb.append("   min record size = " + minrl + "\n");
        sb.append("   avg record size = " + (totrl/active) + "\n");

        sb.append("\n\nFields:\n");
        elems.clear();
        elems.add("name");
        elems.add("tag");
        elems.add("presence");
        elems.add("presence%");
        elems.add("ausence");
        elems.add("ausence%");
        elems.add("occ");
        elems.add("minTimes");
        elems.add("maxTimes");
        elems.add("minLen");
        elems.add("maxLen");
        elems.add("avgLen");
        elems.add("type");

        sb.append(alignResult(elems, colSize, true));
        sb.append("\n\n");

        for (Map.Entry<Integer,Profile> entry : profiles.entrySet()) {
            prof = entry.getValue();
            presence = prof.presenceTimes;
            //total = prof.total;
            presenceAll = (presence == totalRec);

            elems.clear();
            fname = (fnames == null ? null
                    : fnames.get(entry.getKey()));
            fname = (fname == null) ? "" : 
                      (fname.length() > colSize) 
                                      ? fname.substring(0, colSize) : fname;
            elems.add(fname);
            elems.add(entry.getKey().toString());
            elems.add((presenceAll ? "(A) " : "") + Integer.toString(presence));
            elems.add(String.format(
                     "%1$4.2f%%", (100 * ((float)(presence)/totalRec))));
            elems.add(Integer.toString(totalRec-presence));
            elems.add(String.format("%1$4.2f%%",
                              (100 * ((float)(totalRec - presence)/totalRec))));
            elems.add(Integer.toString(prof.total));
            elems.add(Integer.toString(prof.minTimes));
            elems.add(Integer.toString(prof.maxTimes));
            elems.add(Integer.toString(prof.minLen));
            elems.add(Integer.toString(prof.maxLen));
            elems.add(Long.toString(prof.totLen/prof.total));
            elems.add(prof.type.toString());

            sb.append(alignResult(elems, colSize, true));
            sb.append("\n");
        }

        sb.append("\n\nSubfields:\n");
        elems.clear();
        elems.add("fld tag");
        elems.add("subf id");
        elems.add("presence");
        elems.add("presence%");
        elems.add("ausence");
        elems.add("ausence%");
        elems.add("occ");
        elems.add("minTimes");
        elems.add("maxTimes");
        elems.add("minLen");
        elems.add("maxLen");
        elems.add("avgLen");
        elems.add("type");

        sb.append(alignResult(elems, colSize, true));
        sb.append("\n\n");

        for (Map.Entry<Integer,Profile> entry : profiles.entrySet()) {
            prof = entry.getValue();
            int tag = entry.getKey();
            total = prof.total;            

            for (Map.Entry<Character,Profile> sentry : prof.sub.entrySet()) {
                prof = sentry.getValue();
                presence = prof.presenceTimes;
                presenceAll = (presence == total);

                elems.clear();
                elems.add(Integer.toString(tag));
                elems.add(sentry.getKey().toString());
                elems.add((presenceAll ? "(A) " : "")
                                                 + Integer.toString(presence));
                elems.add(String.format(
                         "%1$4.2f%%", (100 * ((float)(presence)/total))));
                elems.add(Integer.toString(total-presence));
                elems.add(String.format("%1$4.2f%%",
                                   (100 * ((float)(total - presence)/total))));
                elems.add(Integer.toString(prof.total));
                elems.add(Integer.toString(prof.minTimes));
                elems.add(Integer.toString(prof.maxTimes));
                elems.add(Integer.toString(prof.minLen));
                elems.add(Integer.toString(prof.maxLen));
                elems.add(Long.toString(prof.totLen/prof.total));
                elems.add(prof.type.toString());

                sb.append(alignResult(elems, colSize, true));
                sb.append("\n");
            }
        }

        sb.append("\n\nCharacters:\n");

        elems.clear();
        elems.add("char");
        elems.add("val(Unicode)");
        elems.add("val(" + encoding + ")");
        elems.add("times");
        elems.add("times%");
        sb.append(alignResult(elems, colSize + 7, true));
        sb.append("\n");

        for (Map.Entry<Character,Integer> entry : characters.entrySet()) {
            chts.add(new Elem(entry.getValue(), entry.getKey()));
            totChar += entry.getValue();
        }
        for (Elem elem : chts) {
            Character ch = elem.val;
            String sch = ch.toString();
            int val = ch.charValue();

            elems.clear();
            elems.add(ch.toString());
            elems.add(Integer.toString(val));
            try {
                elems.add(Integer.toString(
                (0x000000FF & (sch.getBytes(encoding)[0]))));
            } catch (UnsupportedEncodingException ex) {
                elems.add("");
            }
            elems.add(elem.tot.toString());
            elems.add(String.format("%1$4.2f%%",
                                            (100 * ((float)elem.tot/totChar))));
            sb.append(alignResult(elems, colSize + 7, true));
            sb.append("\n");
        }

        fname = mstFile.getName();
        fname = (fname.length() > 3) ? fname.substring(0, 3) : fname;
        sb.append("\n\nField Definition Table (FDT):\n\n");
        sb.append("W:" + fname + "   \n");
        sb.append("F:" + fname + "   \n");
        sb.append("S:" + fname + "   \n");
        sb.append("***\n");
        for (Map.Entry<Integer,Profile> entry : profiles.entrySet()) {
            fname = (fnames == null ? null
                    : fnames.get(entry.getKey()));
            fname = (fname == null) ? "field_" + entry.getKey().toString() :
                      (fname.length() > 30)
                                      ? fname.substring(0, colSize) : fname;
            sb.append(paddingWhites(fname, 30)); // nome campo
            prof = entry.getValue();
            subfields = "";
            for (Character subId : prof.sub.keySet()) {
                if (subId != '*') {
                    subfields += subId;
                }
            }
            sb.append(paddingWhites(subfields, 20)); // subcampos
            sb.append(Integer.toString(entry.getKey()) + " "); //tag
            sb.append(Integer.toString(prof.maxLen) + " "); // max size
            sb.append(Integer.toString(prof.type.ordinal()) + " "); //type
            sb.append(prof.maxTimes > 1  ? "1" : "0"); // repeatable
            sb.append("\n");
        }

        return sb.toString();
    }

    private static void usage() {
        System.err.println("usage: Statistics <dbname> [encoding=<encoding>] " +
            "[from=<from>][to=<to>][tab=<tag>][tell=<tell>]");
        System.exit(1);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws BrumaException {
        if (args.length < 1) {
            usage();
        }
        final Statistics sta = new Statistics();
        final String out;
        String encoding = Master.GUESS_ISO_IBM_ENCODING;
        int from = 1;
        int to = Integer.MAX_VALUE;
        int tag = 0;
        int tell = Integer.MAX_VALUE;

        for (int counter = 2; counter < args.length; counter++) {
            if (args[counter].startsWith("encoding=")) {
                encoding = args[counter].substring(9);
            } else if (args[counter].startsWith("from=")) {
                from = Integer.parseInt(args[counter].substring(5));
            } else if (args[counter].startsWith("to=")) {
                to = Integer.parseInt(args[counter].substring(3));
            } else if (args[counter].startsWith("tab=")) {
                tag = Integer.parseInt(args[counter].substring(4));
            } else if (args[counter].startsWith("tell=")) {
                tell = Integer.parseInt(args[counter].substring(5));
            } else {
                usage();
            }
        }

        if (tag == 0) {
            out = sta.stat(args[0], encoding, from, to, tell);
        } else {
            out = sta.tab(args[0], encoding, from, to, tag, tell);
        }

        System.out.println(out);
    }
}

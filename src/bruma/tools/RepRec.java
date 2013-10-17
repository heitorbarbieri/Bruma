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
import bruma.master.Field;
import bruma.master.Master;
import bruma.master.MasterFactory;
import bruma.master.Record;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Classe que checa em uma base de dados quais sao os possiveis candidatos a
 * registros repetidos. A verificacao é baseada na comparacao de strings
 * unicas para cada registro formadas da concatenacao das strings
 * geradas pelos metodos MD5 e SHA-1 de seus campos.
 * @author Heitor Barbieri
 */
public class RepRec { // Repeated Record
    final Master mst;
    final Map<String,Integer> map;
    final Writer out;

    /**
     *
     * @param mstName nome da base de dados de entrada
     * @param encoding encoding da base de dados
     * @param outFile nome do arquivo no qual serão escritos os mfns dos
     *                registros repetidos
     * @throws BrumaException
     */
    public RepRec(final String mstName,
                  final String encoding,
                  final String outFile) throws BrumaException {
        if (mstName == null) {
            throw new BrumaException("null mstName");
        }
        if (encoding == null) {
            throw new BrumaException("null encoding");
        }
        if (outFile == null) {
            throw new BrumaException("null outFile");
        }
        mst = MasterFactory.getInstance(mstName)
                           .setEncoding(encoding).open();
        map = new HashMap<String,Integer>();
        try {
            out = new BufferedWriter(new FileWriter(outFile));
        } catch(IOException ioe) {
            throw new BrumaException(ioe);
        }
    }

    public void close() throws BrumaException {
        mst.close();
        try {
            out.close();
        } catch(IOException ioe) {
            throw new BrumaException(ioe);
        }
    }

    /**
     * Checa se existem registros repetidos na base de dados.
     * @param tell  de quanto em quanto imprime-se o mfn do registro
     * @throws BrumaException
     */
    public void check(int tell) throws BrumaException {
        assert tell >= 0;

        final int tell2 = (tell == 0) ? Integer.MAX_VALUE : tell;

        for (Record rec : mst) {
            final int mfn = rec.getMfn();

            if (mfn % tell2 == 0) {
                System.err.println("++" + mfn);
            }
            if (rec.getStatus() == Record.Status.ACTIVE) {
                final Record recOut = sortFields(stripWhites(rec));
                final String fields = recOut.getFieldsWithTags();
                final String md5 = geraHashString(fields, "MD5");
                final String sha1 = geraHashString(fields, "SHA-1");

                putHashString(mfn, md5+sha1);
            }
        }
    }

    /**
     *  Retira os espaços em branco, tag e quebra de linha de cada campo do
     *  registro.
     * @param in registro de entrada
     * @return registro transformado
     * @throws BrumaException
     */
    private Record stripWhites(final Record in) throws BrumaException {
        final Record ret = new Record();
        String content;

        for (Field fld : in) {
            content = fld.getContent().replaceAll("\\s+", "").toUpperCase();
            ret.addField(fld.getId(), content);
        }

        return ret;
    }

    /**
     * Ordena os campos por tag e conteudo.
     * @param in registro a ser ordenado
     * @return registro com campos ordenados
     * @throws BrumaException
     */
    private Record sortFields(final Record in) throws BrumaException {
        final Record ret = new Record();
        final TreeSet<Integer> tags = new TreeSet<Integer>();

        for (Integer tag : in.getRecordTags()) {
            tags.add(tag);
        }
        for (Integer tag : tags) {
            final List<Field> fields = in.getFieldList(tag);
            final List<String> contents = new ArrayList<String>();

            for (Field fld : fields) {
                contents.add(fld.getContent());
            }
            Collections.sort(contents, String.CASE_INSENSITIVE_ORDER);
            for (String content: contents) {
                ret.addField(tag, content);
            }
        }

        return ret;
    }

    /**
     * Gera uma string formada pelo numero hash gerado a partir da string dos
     * campos do registro.
     * @param in string formada pelos campos do registro
     * @param algorithm nome do algoritmo de hash
     * @return String formada pelo numero de hash gerado
     * @throws BrumaException
     */
    private String geraHashString(final String in,
                                  final String algorithm) throws BrumaException {
        assert in != null;
        assert algorithm != null;

        final String ret;

        try {
            final MessageDigest md = MessageDigest.getInstance(algorithm);
            final BigInteger hash = new BigInteger(1, md.digest(in.getBytes()));

            ret = hash.toString(16);
        } catch (Exception ex) {
            throw new BrumaException(ex);
        }

        return ret;
    }

    /**
     * Armazena no map o registro ou se encontrar igual informa no arquivo de
     * saida.
     * @param mfn mfn do registro a ser armazenado
     * @param in String composta da concatenacao das strings formadas pela
     *                  funcao geraHashString().
     * @throws BrumaException
     */
    private void putHashString(final int mfn,
                               final String in) throws BrumaException {
        assert mfn > 0;
        assert in != null;

        final Integer smfn = map.get(in);

        if (smfn == null) {
            map.put(in, mfn);
        } else {
            try {
                out.write("records " + mfn + " and " + smfn
                                            + " seems to be the same\n");
            } catch(Exception ex) {
                throw new BrumaException(ex);
            }
        }
    }

    private static void usage() {
        System.err.println(
                       "usage: RepRec <dbname> <encoding> <outfile> [<tell>]");
        System.exit(1);
    }

    public static void main(final String[] args) throws BrumaException {
        if (args.length < 3) {
            usage();
        }
        final RepRec rr = new RepRec(args[0], args[1], args[2]);
        final int tell = (args.length > 3) ? Integer.parseInt(args[3]) : 0;

        rr.check(tell);
        rr.close();
    }
}

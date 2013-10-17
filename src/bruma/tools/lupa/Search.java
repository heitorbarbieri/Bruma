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

package bruma.tools.lupa;

import bruma.BrumaException;
import bruma.master.Field;
import bruma.master.Master;
import bruma.master.MasterFactory;
import bruma.master.Record;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Heitor Barbieri
 */
public class Search {
    public static final int MAX_HITS = 1000;

    private final Master mst;
    private final List<Integer> hits;
    private int max;
    private int current;
    private Matcher mat;

    public Search(final Master mst) throws BrumaException {
        if (mst == null) {
            throw new BrumaException("null master");
        }
        this.mst = mst;
        hits = new ArrayList<Integer>();
        clear();
    }

   /* public void close() throws BrumaException {
        mst.close();
    }*/

    private void clear() {
        max = 0;
        current = -1;
        mat = null;
        hits.clear();
    }

    public int searchRegExp(final String exp,
                            final Set<Integer>tags,
                            final boolean ignoreCase) throws BrumaException {
        if (exp == null) {
            throw new BrumaException("null expression");
        }
        clear();
        mat = Pattern.compile(exp, (ignoreCase ? Pattern.CASE_INSENSITIVE : 0))
                                                                   .matcher("");

        //mst.open();
        for (Record rec: mst) {
            if (rec.getStatus() == Record.Status.ACTIVE) {
                for (Field fld : rec) {
                    if ((tags == null) || (tags.contains(fld.getId()))) {
                        mat.reset(fld.getContent());
                        if (mat.find() && (max < MAX_HITS)) {
                            hits.add(rec.getMfn());
                            max++;
                            break;
                        }
                    }
                }
            }
            if (max == MAX_HITS) {
                break;
            }
        }
        
        return hits.size();
    }

    public int search(final String exp,
                      final Set<Integer>tags,
                      final boolean ignoreCase) throws BrumaException {
        return searchRegExp(Pattern.quote(exp), tags, ignoreCase);
    }

    public int numOfHits() {
        return hits.size();
    }

    public int getCurrent() {
        return current;
    }

    public boolean hasNext() {
        return current < max - 1;
    }

    public boolean hasPrevious() {
        return current > 0;
    }

    public int getNext() {
        int ret = -1;

        if (hasNext()) {
            ret = hits.get(++current);
        }

        return ret;
    }

    public int getPrevious() {
        int ret = -1;

        if (hasPrevious()) {
            ret = hits.get(--current);
        }

        return ret;
    }
    
    private static void usage() {
        System.err.println("usage: Search <dbname> <regExp> [encoding=<encod>] " 
                                 + "[tags=<tag1,tag2,..,tagn>] [-ignoreCase]");
        System.exit(1);
    }
    
    public static void main(String[] args) throws BrumaException {
        final int len = args.length;
        String encoding = Master.GUESS_ISO_IBM_ENCODING;
        Set<Integer> tags = null;
        boolean ignoreCase = false;
        
        if (len < 2) {
            usage();
        }
        for (int index = 2; index < len; index++) {
            if (args[index].startsWith("encoding=")) {
                encoding = args[index].substring(9);
            } else if (args[index].startsWith("tags=")) {
                final String[] split = args[3].trim().split("[\\s\\,\\;]+");
                
                tags = new HashSet<Integer>();
                for (String elem : split) {
                    tags.add(Integer.getInteger(elem));
                }                
            } else if (args[index].equals("-ignoreCase")) {
               ignoreCase = true;
            }
        }
        final Master mst = MasterFactory.getInstance(args[0])
                                        .setEncoding(encoding)
                                        .open();
        final Search src = new Search(mst);
        int hits = src.searchRegExp(args[1], tags, ignoreCase);
        
        System.out.println("number of hits: " + hits);
        if (hits > 0) {
            System.out.println("hits: \n");
            while (src.hasNext()) {
                System.out.print(src.getNext() + " ");
                if ((src.getCurrent()+1) % 10 == 0) {
                    System.out.println();
                }
            }
            System.out.println();
        }                
        
        mst.close();
    }
}

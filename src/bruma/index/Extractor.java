package bruma.index;

import bruma.BrumaException;
import bruma.master.Record;

/**
 *
 * @author Heitor Barbieri
 * @date 08/07/2011
 */
public interface Extractor {
    String extract(final Record rec) throws BrumaException;
}

package bruma.index;

import bruma.BrumaException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.lucene.analysis.Analyzer;

/**
 *
 * @author Heitor Barbieri
 * @date 08/07/2011
 */
public class FieldIndexTable {
    class FieldIndexElement {
        String fieldName;
        Analyzer techName;
        Extractor extractor;
        
        FieldIndexElement(String fieldName, 
                          Analyzer techName, 
                          Extractor extractor) {
            this.fieldName = fieldName;
            this.techName = techName;
            this.extractor = extractor;
        }        
    }
    
    private final List<FieldIndexElement> elements;
    
    public FieldIndexTable(final String fitFile) throws BrumaException {
        if (fitFile == null) {
            throw new BrumaException("null fit file");
        }
        try {
            elements = parseFitFile(fitFile);
        } catch(Exception ex) {
            throw new BrumaException(ex);
        }
    }
    
    public List<FieldIndexElement> getElements() {
        return elements;
    }
    
    private List<FieldIndexElement> parseFitFile(final String fitFile) 
                                                            throws Exception {
        assert fitFile != null;
        
        final List<FieldIndexElement> ret = new ArrayList<FieldIndexElement>();
        final Map<String,Extractor> map = new HashMap<String,Extractor>();
        final BufferedReader reader = new BufferedReader(
                                                       new FileReader(fitFile));
        
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            line = line.trim();
            if (line.isEmpty() || line.charAt(0)=='#') continue;
            
            final String[] split = line.split("\\s+", 3);
            if (split.length != 3) {
                throw new IOException("invalid line format:" + line);
            }

            Extractor extr = map.get(split[2]);            
            if (extr == null) {
                extr = (Extractor)Class.forName(split[2]).newInstance();
                map.put(split[2], extr);
            }
                               
            ret.add(new FieldIndexElement(split[0],parseTech(split[1]), extr));                              
        }
        
        reader.close();
        
        return ret;
    }
    
    private Analyzer parseTech(final String techName) {
        assert techName != null;
        
        final Analyzer analyzer = null;
        
        
        
        return analyzer;
    }
}

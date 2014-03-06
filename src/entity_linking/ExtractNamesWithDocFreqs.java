package entity_linking;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TreeMap;

import entity_linking.input_data_pipeline.*;

// Extract a set with all 1-ngrams and 2-ngrams from the Wikilinks corpus (*.data file)
// together with their doc frequencies. Just names appearing in Crosswikis dict are considered.
// This was used to compute p(name) and to evaluate
// p(exist ent | n) = p(n|e)/p(e|n,exist e)  * p(e)/p(n)
public class ExtractNamesWithDocFreqs {
    static int nrLinesDictFile = 140811861;
    // dict: P(e|n)
    private static HashSet<String> LoadNamesFromCrosswikisDict(String dictFilename, int i, int numOutputShards) throws IOException{
        // <string><tab><cprob><space><url>     
        System.err.println("[INFO] Loading names from dict P(e|n) ...");

        HashSet<String> dictNames =  new HashSet<String>(nrLinesDictFile/numOutputShards);
        BufferedReader in = new BufferedReader(new FileReader(dictFilename));
        String line = "";
        int nr_line = 0;
        String lastName = "";
        
        int start = (nrLinesDictFile / numOutputShards) * (i-1);
        int end = Math.min(nrLinesDictFile,  (nrLinesDictFile / numOutputShards) * i);
            
        while (true) {
            line = in.readLine();
            nr_line ++;

            if (line == null || line.length() < 1) break;
            if (nr_line % 20000000 == 0) {
                System.err.println("loaded " + nr_line);
            }

            if (nr_line < start) continue;
            if (nr_line > end) break;
            
            StringTokenizer st = new StringTokenizer(line, "\t");

            if (!st.hasMoreTokens()) {
                continue;
            }
            String name = st.nextToken();

            if (!st.hasMoreTokens()) {
                continue;
            }
            
            if (lastName.compareTo(name) != 0)
                dictNames.add(name);
            lastName = name;
        }
        in.close();     

        System.err.println("[INFO] Done. Size = " + dictNames.size());
        return dictNames;
    }   

    
    public static void fromFile(String filename, HashSet<String> dictNames, HashMap<String, Integer> freqMap) throws IOException {    
        System.err.println("Processing file " + filename);
        WikilinksParser p = new WikilinksParser(filename);

        int doc_index = 0;
        while (p.hasNext()) {
            doc_index++;
            WikilinksSinglePage doc = p.next();

            HashSet<String> hs = new HashSet<String>();

            String rawText = doc.getRawText();
            for (int i = 0; i < rawText.length(); i++) {
                if (Utils.isWordSeparator(rawText.charAt(i))) continue;
                if (i > 0 && !Utils.isWordSeparator(rawText.charAt(i-1))) continue;
                int j = i;
                while (j < rawText.length() && !Utils.isWordSeparator(rawText.charAt(j))) {
                    j++;
                }
                
                // j is now the end of the word
                String tkspan = rawText.substring(i, j).trim();
                if (dictNames.contains(tkspan)) hs.add(tkspan);
                
                if (j == rawText.length() || rawText.charAt(j) == '\n') continue;
               
                j++; // j is now the first char after the first Word Separator
                if (j == rawText.length() || Utils.isWordSeparator(rawText.charAt(j))) {
                    tkspan = rawText.substring(i, j).trim();
                    if (dictNames.contains(tkspan)) hs.add(tkspan);
                }
                if (j == rawText.length()) continue;
                boolean hasEOF = false;
                while (j < rawText.length() && Utils.isWordSeparator(rawText.charAt(j))) {
                    if (rawText.charAt(j) == '\n') {
                        hasEOF = true;
                        j++;
                        break;
                    }
                    j++;
                }
                
                if (hasEOF || j == rawText.length()) continue;
                
                while (j < rawText.length() && !Utils.isWordSeparator(rawText.charAt(j))) {
                    j++;
                }
                // j is now the end of the second word
                tkspan = rawText.substring(i, j).trim();
                if (dictNames.contains(tkspan)) hs.add(tkspan);
                if (j == rawText.length() || rawText.charAt(j) == '\n') continue;
                
                j++; // j is now the first char after the first Word Separator
                if (j == rawText.length() || Utils.isWordSeparator(rawText.charAt(j))) {
                    tkspan = rawText.substring(i, j).trim();
                    if (dictNames.contains(tkspan)) hs.add(tkspan);
                }              
            }
            
            
            Iterator<String> it = hs.iterator();
            while(it.hasNext()) {
                String name = it.next();
                if (!freqMap.containsKey(name)) {
                    freqMap.put(name, 0);
                }
                freqMap.put(name, freqMap.get(name) + 1);
            }
        }
    }   
    

    // Output: files with (name, doc freq)
    // Input: directory that contains all *._namesfreq_shard files
    public static void fromDir(String dir_file, String dictFilename, String out_file, int numOutputShards) throws IOException {
        if (!dir_file.endsWith("/")) {
            dir_file += "/";
        }

        for (int i = 1; i <= numOutputShards; ++i) {
            System.err.println("[INFO] .... Computing output shard " + i);
            HashSet<String> dictNames = LoadNamesFromCrosswikisDict(dictFilename, i, numOutputShards);

            HashMap<String, Integer> freqMap = new HashMap<String, Integer>(nrLinesDictFile/numOutputShards);

            File dir = new File(dir_file);
            if(dir.isDirectory()==false) {
                System.out.println("Directory does not exists : " + dir_file);
                return;
            }
            String[] list = dir.list();
            for (String filename : list) {
                if (!filename.endsWith(".data")) {
                    continue;       
                }
                fromFile(dir_file + filename, dictNames, freqMap);
            }


            PrintWriter writer = new PrintWriter(out_file + ".shard_" + i, "UTF-8");
            for (String e : freqMap.keySet()) {
                writer.println(e + "\t" + freqMap.get(e));
            }
            writer.flush();
            writer.close();     
        }
    }
}

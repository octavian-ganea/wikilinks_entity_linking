package entity_linking;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.StringTokenizer;


class Prune {
    private static HashSet<String> knownEntities = new HashSet<String>();
    
    private static void LoadKnownEntities(String entities_file) throws IOException {
        if (!new File(entities_file).exists()) {
            System.out.println("File " + entities_file + " does not exists");
            System.exit(1);
        }
        BufferedReader in = new BufferedReader(new FileReader(entities_file));
        String next_line = in.readLine();
        while (!next_line.startsWith("NR DOCS:")) {
            StringTokenizer st = new StringTokenizer(next_line, "\t");
            String url = st.nextToken();
            knownEntities.add(url);
            next_line = in.readLine();
        }       
    }
    
    // P(e|n)
    public static void _3_prune_dict(String filename, String entities_file, String out_file) throws IOException, InterruptedException {
        LoadKnownEntities(entities_file);
        
        BufferedReader in = new BufferedReader(new FileReader(filename));
        PrintWriter writer = new PrintWriter(out_file, "UTF-8");    
        
        in.readLine();
        String line = in.readLine();

        int nr_line = 0;
        while (line != null && line.length() > 3) {
            nr_line ++;
            StringTokenizer st = new StringTokenizer(line, "\t");
            
            if (!st.hasMoreTokens()) {
                line = in.readLine();
                continue;
            }
            String mention = st.nextToken();
            
            if (!st.hasMoreTokens()) {
                line = in.readLine();
                continue;
            }

            StringTokenizer st2 = new StringTokenizer(st.nextToken(), " ");
            double cprob = Double.parseDouble(st2.nextToken());             
            String url = st2.nextToken();
            url = Utils.pruneURL(url);
            
            if (!knownEntities.contains(Utils.pruneURL(url)) || cprob < 0.0001) {
                line = in.readLine();
                continue;
            }
            writer.println(mention + "\t" + cprob + " " + url);
            line = in.readLine();
        }
        in.close();
        writer.flush();
        writer.close();
    }
    
    // P(n|e)
    public static void _3_prune_invdict(String filename, String entities_file, String out_file) throws IOException, InterruptedException {
        // <url><tab><cprob><space><string>[<tab><score>[<space><score>]*]
        LoadKnownEntities(entities_file);
        
        BufferedReader in = new BufferedReader(new FileReader(filename));
        PrintWriter writer = new PrintWriter(out_file, "UTF-8");    
        
        in.readLine();
        String line = in.readLine();

        int nr_line = 0;
        while (line != null && line.length() > 3) {
            nr_line ++;
            StringTokenizer st = new StringTokenizer(line, "\t");
            
            if (!st.hasMoreTokens()) {
                line = in.readLine();
                continue;
            }
            String url = st.nextToken();
            url = Utils.pruneURL(url);
            
            if (!st.hasMoreTokens()) {
                line = in.readLine();
                continue;
            }

            String left = st.nextToken();
            double cprob = Double.parseDouble(left.substring(0,left.indexOf(" ")));             
            String mention = left.substring(left.indexOf(" ") + 1);         
            
            if (!knownEntities.contains(Utils.pruneURL(url)) || cprob < 0.0001) {
                line = in.readLine();
                continue;
            }
            writer.println(url + "\t" + cprob + " " + mention);
            line = in.readLine();
        }
        in.close();     
        writer.flush();
        writer.close();
    }   
}


public class AuxCode {
    public static void prune_index_main(String[] args) throws IOException, InterruptedException {  
        // Code to prune the dictionary index of P(e|n) or the inv.dict of P(n|e).
        // INPUT: args[0] = file that contains dict or invdict;
        //        args[1] = file with all entities obtained by running [ExtractEntsWithFreq]
        //        args[3] = [prune_dict] or [prune_invdict]
        // OUTPUT: args[2]
        if (args.length == 4 && args[3].compareTo("[prune_dict]") == 0) {
            Utils.loadWikiRedirects("wikiRedirects/wikipedia_redirect.txt");    
            Prune._3_prune_dict(args[0], args[1], args[2]);
        }   
        if (args.length == 4 && args[3].compareTo("[prune_invdict]") == 0) {
            Utils.loadWikiRedirects("wikiRedirects/wikipedia_redirect.txt");    
            Prune._3_prune_invdict(args[0], args[1], args[2]);
        }   
    }
}
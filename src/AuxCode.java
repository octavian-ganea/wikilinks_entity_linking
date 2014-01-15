import java.io.IOException;

class AuxCode {
    public static void prune_index_main(String[] args) throws IOException, InterruptedException {  
        // Code to prune the dictionary index of P(e|n) or the inv.dict of P(n|e).
        // INPUT: args[0] = file that contains dict or invdict;
        //        args[1] = file with all entities obtained by running [file_ents]
        //        args[3] = [prune_dict] or [prune_invdict]
        // OUTPUT: args[2]
        if (args.length == 4 && args[3].compareTo("[prune_dict]") == 0) {
            Utils.loadWikiRedirects("wikiRedirects/wikipedia_redirect.txt");    
            Part3._3_prune_dict(args[0], args[1], args[2]);
        }   
        if (args.length == 4 && args[3].compareTo("[prune_invdict]") == 0) {
            Utils.loadWikiRedirects("wikiRedirects/wikipedia_redirect.txt");    
            Part3._3_prune_invdict(args[0], args[1], args[2]);
        }   
    }
    
    
    //////// Old simple matching approach (no longer of interest) ///////////////////////
    public static void thrift_main(String[] args) throws IOException {      
        boolean create_index_from_termdocid_pairs = false;
        if (create_index_from_termdocid_pairs) {
            InvertedIndex.createDistributedIndexFromTermDocidPairs();
            return;
        }

        
        // Number of mentions coming with each WikiLinkItem object. 
        int total_num_raw_mentions = 0;

        boolean annotate_from_current_page = false;     
        boolean annotate_from_all_pages_index = true;
        
        WikilinksParser p = new WikilinksParser(args[0]);

        boolean write_term_docids = false;
        if (write_term_docids) {
            InvertedIndex.outputTermDocidPairs(p);
            return;
        }

        AnnotateSentences annotator = null;
        if (annotate_from_current_page) {
            annotator = new AnnotateSentFromCurrentPage();
        }
        if (annotate_from_all_pages_index) {
            annotator = new AnnotateSentFromAllPagesIndex("inverted_index_v2/index_shards/");
        }
        
        // ---------------- Parsing input data ----------------------
        while (p.hasMoreItems()) {
            WikiLinkItem i = p.nextItem();
            total_num_raw_mentions += i.mentions.size();
            
            annotator.annotateSentences(i);
        }
        // ----------------------------------------------------------

        System.out.println("Num raw mentions: " + total_num_raw_mentions);

        if (annotate_from_current_page) {
            System.out.println("Total num local matched entities: ");
        }
        if (annotate_from_all_pages_index) {
            System.out.println("Total num mentions from inverted index: ");
        }
        System.out.println(annotator.getTotalNumAnnotations());
    }

}
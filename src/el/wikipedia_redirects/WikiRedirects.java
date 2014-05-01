package el.wikipedia_redirects;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Comparator;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;

import el.TokenSpan;
import el.utils.Utils;


public class WikiRedirects {
    private static HashMap<String,String> wikiRedirects = null;

    public static void loadWikiRedirects(String filename) throws IOException {
        System.err.println("[INFO] Loading Wikipedia redirects...");

        wikiRedirects = new HashMap<String,String>(3000000);

        if (new java.io.File(filename).exists() == false) {
            System.err.println("[FATAL] Wikipedia Redirects file does not exist.");
            System.exit(1);
        }

        BufferedReader in = new BufferedReader(new FileReader(filename));
        String line = in.readLine();
        while (line != null) {
            StringTokenizer st = new StringTokenizer(line, "\t");
            wikiRedirects.put(pruneURLWithRedirect(st.nextToken(), false), pruneURLWithRedirect(st.nextToken(), false));
            line = in.readLine();
        }

        System.err.println("[INFO] Done loading Wikipedia redirects.");
    }


    public static String pruneURL(String s) {
        return pruneURLWithRedirect(s, true);
    }
    
    public static String pruneURLWithRedirect(String s, boolean useRedirects) {
        
        if (useRedirects && wikiRedirects == null) {
            System.err.println("[FATAL] The Wikipedia redirects file was not loaded.");
            System.exit(1);
        }
        String url = s.trim();
        if (url.contains("wiki/")) {
            url = url.substring(url.lastIndexOf("wiki/") + "wiki/".length());
        }
        if (url.contains("wikipedia.org")) {
            url = url.substring(url.indexOf("wikipedia.org") + "wikipedia.org".length());
        }
        String final_url = url;
        if (url.length() == 0) return "";

        if (Character.isLowerCase(url.charAt(0))) {
            url = url.substring(0, 1).toUpperCase() + url.substring(1);
        }

        try {
            final_url = URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
        } catch (java.lang.IllegalArgumentException e) {
        }   

        final_url = final_url.replace("%29", ")");
        final_url = final_url.replace("%28", "(");

        final_url = final_url.replace(' ', '_');

        if (useRedirects && wikiRedirects.containsKey(final_url)) {
            final_url = wikiRedirects.get(final_url);
        }
        
        StringBuilder sb = new StringBuilder();
        for (char c : final_url.toCharArray()) {
            if (!Character.isSpace(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}

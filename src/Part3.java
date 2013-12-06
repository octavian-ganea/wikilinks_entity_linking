import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.StringTokenizer;


public class Part3 {
	private static HashSet<String> knownEntities = new HashSet<String>();
	
	private static void LoadKnownEntities() throws IOException {
		if (!new File("_2_merge").exists()) {
			System.out.println("File _2_merge does not exists");
			System.exit(1);
		}
		BufferedReader in = new BufferedReader(new FileReader("_2_merge"));
		String next_line = in.readLine();
		while (!next_line.startsWith("NR DOCS:")) {
			StringTokenizer st = new StringTokenizer(next_line, "\t");
			String url = st.nextToken();
			knownEntities.add(url);
			next_line = in.readLine();
		}		
	}
	
	// P(e|n)
	public static void _3_prune_dict(String filename) throws IOException, InterruptedException {
		LoadKnownEntities();
		
		BufferedReader in = new BufferedReader(new FileReader(filename));
		PrintWriter writer = new PrintWriter("prunned_" + filename, "UTF-8");	
		
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
			
			if (!knownEntities.contains(Utils.pruneURL(url)) || cprob < 0.0001) {
				line = in.readLine();
				continue;
			}
			//InvertedIndexEntry iie = new InvertedIndexEntry(mention, url, cprob);
			writer.println(line);
			line = in.readLine();
		}
		in.close();
		writer.flush();
		writer.close();
	}
	
	// half size of the dict: P(n|e)
	public static void _3_prune_invdict(String filename) throws IOException, InterruptedException {
		// <url><tab><cprob><space><string>[<tab><score>[<space><score>]*]
		LoadKnownEntities();
		
		BufferedReader in = new BufferedReader(new FileReader(filename));
		PrintWriter writer = new PrintWriter("prunned_" + filename, "UTF-8");	
		
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
			//InvertedIndexEntry iie = new InvertedIndexEntry(mention, url, cprob);
			writer.println(line);
			line = in.readLine();
		}
		in.close();		
		writer.flush();
		writer.close();
	}	
}

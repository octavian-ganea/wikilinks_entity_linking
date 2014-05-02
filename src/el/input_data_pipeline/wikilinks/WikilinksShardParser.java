package el.input_data_pipeline.wikilinks;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import el.input_data_pipeline.GenericPagesIterator;
import el.input_data_pipeline.TruthMention;

public class WikilinksShardParser implements GenericPagesIterator {
	private BufferedReader in;
	private String nextLine;
	
	private String filename;
	
	public WikilinksShardParser(String filename) throws IOException {
	    this.filename = filename;
		in = new BufferedReader(new FileReader(filename));
		nextLine = in.readLine();
	}

	public WikilinksSinglePage next(){
	    WikilinksSinglePage rez = new WikilinksSinglePage();
		if (!nextLine.startsWith("------------")) {
			System.err.println("Error parsing the current file!!! " + nextLine);
			System.exit(1);
		}
		try {
            nextLine = in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
		while (!nextLine.startsWith("--- Page num:")) {
			try {
                nextLine = in.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }			
		}
		
		rez.pageName= Integer.toString(Integer.parseInt(nextLine.substring(nextLine.indexOf(":") + 2)));
		try {
            nextLine = in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
		rez.docId = Integer.parseInt(nextLine.substring(nextLine.indexOf(":") + 2));
		try {
            nextLine = in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
		rez.pageUrl = nextLine.substring(nextLine.indexOf(": ") + 2);

		try {
	        nextLine = in.readLine();
            nextLine = in.readLine(); // --- Mentions:
        } catch (IOException e) {
            e.printStackTrace();
        } 

		while(!nextLine.startsWith("--- ")) {
			rez.truthMentions.add(new TruthMention(nextLine));
			try {
                nextLine = in.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }	
			if (nextLine == null) {
				System.err.println("Not proper formatted item.");
				System.exit(1);
			}
		}	
		try {
            nextLine = in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }			

		
		StringBuilder allTextSb = new StringBuilder();
		while (nextLine != null && nextLine.compareTo("-------------------------------------") != 0) {
			allTextSb.append(nextLine + "\n");
			try {
                nextLine = in.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }			
		}
		rez.setAnnotatedText(allTextSb.toString());
				
		return rez;
	}
	
	public boolean hasNext() {
		return nextLine != null && nextLine.startsWith("-----------");
	}
	
	public void remove() {
	}

    @Override
    public GenericPagesIterator hardCopy() {
        try {
            return new WikilinksShardParser(filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}

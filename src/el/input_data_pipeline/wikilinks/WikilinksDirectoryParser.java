package el.input_data_pipeline.wikilinks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;

import el.input_data_pipeline.GenericPagesIterator;
import el.input_data_pipeline.GenericSinglePage;

public class WikilinksDirectoryParser implements GenericPagesIterator {
    private String WikilinksDir;
    private String[] shardsFilenamesList;
    private int nrFile;
    private GenericPagesIterator currentIterator = null;


    public WikilinksDirectoryParser(String WikilinksDir) throws IOException {
        this.WikilinksDir = WikilinksDir;
        File dir = new File(WikilinksDir);
        if (!WikilinksDir.endsWith("/")) {
            WikilinksDir = WikilinksDir + "/";
        }
        if(dir.isDirectory()==false) {
            System.err.println("Directory does not exists : " + WikilinksDir);
            return;
        }
        this.shardsFilenamesList = dir.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".data");
            }
        });
        
        nrFile = 0;
        if (nrFile >= shardsFilenamesList.length) {
            return;
        }
        
        System.err.println("[INFO] Analyzing shard file " + nrFile + " with name: " + shardsFilenamesList[nrFile]);
        currentIterator = new WikilinksShardParser(WikilinksDir + shardsFilenamesList[nrFile]);
    }

    @Override
    public boolean hasNext() {
        if (nrFile >= shardsFilenamesList.length) {
            return false;
        }

        if (currentIterator.hasNext()) return true;

        nrFile++;
        if (nrFile >= shardsFilenamesList.length) {
            return false;
        }
        
        System.err.println("[INFO] Analyzing shard file " + nrFile + " with name: " + shardsFilenamesList[nrFile]);
        try {
            currentIterator = new WikilinksShardParser(WikilinksDir + shardsFilenamesList[nrFile]);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public GenericSinglePage next() {
        if (nrFile >= shardsFilenamesList.length) {
            return null;
        }
        
        if (currentIterator.hasNext()) return currentIterator.next();

        nrFile++;
        if (nrFile >= shardsFilenamesList.length) {
            return null;
        }
        
        System.err.println("[INFO] Analyzing shard file " + nrFile + " with name: " + shardsFilenamesList[nrFile]);
        try {
            currentIterator = new WikilinksShardParser(WikilinksDir + shardsFilenamesList[nrFile]);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return currentIterator.next();
    }

    @Override
    public void remove() {
    }

    @Override
    public GenericPagesIterator hardCopy() {
        try {
            return new WikilinksDirectoryParser(WikilinksDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}

package entity_linking.input_data_pipeline;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.File;
import java.nio.charset.Charset;


class Annotation {
    public String docName;
    public String wikiName;
    public int offset;
    public int length;
    public Annotation(String docName, String wikiName, int offset, int length) {
        this.docName = docName;
        this.wikiName = wikiName;
        this.offset = offset;
        this.length = length;
    }
}

// Docs iterator for the IITB evaluation set from here: http://soumen.cse.iitb.ac.in/~soumen/doc/CSAW/Annot/
public class IITBPagesIterator implements GenericPagesIterator {
    private Vector<IITBSinglePage> allDocs;
    private int currentCounter;
    
    private String groundTruthAnnotationsFilename;
    private String auxGroundTruthAnnotationsFilename;
    private String pagesDir;
    
    
    // Parses the XML groundTruthAnnotationsFilename and inserts all the annotations in the annotationsTree.
    private void insertGroundTruthAnnotations(String groundTruthAnnotationsFilename, TreeMap<String, Vector<Annotation>> annotationsTree) throws SAXException, IOException, ParserConfigurationException {
        // Parse the XML file and extract all <docName> names.
        File fXmlFile = new File(groundTruthAnnotationsFilename);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(fXmlFile);
        //optional, but recommended
        //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
        doc.getDocumentElement().normalize();
        
        NodeList nList = doc.getElementsByTagName("annotation");
        for (int temp = 0; temp < nList.getLength(); temp++) {
            Node nNode = nList.item(temp);     
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                Element eElement = (Element) nNode;
                String docName = eElement.getElementsByTagName("docName").item(0).getTextContent();        
                if (!annotationsTree.containsKey(docName)) {
                    annotationsTree.put(docName, new Vector<Annotation>());
                }
                
                String wikiName = eElement.getElementsByTagName("wikiName").item(0).getTextContent();
                if (wikiName.length() == 0) continue;

                int offset = Integer.parseInt(eElement.getElementsByTagName("offset").item(0).getTextContent());
                int length = Integer.parseInt(eElement.getElementsByTagName("length").item(0).getTextContent());
                annotationsTree.get(docName).add(new Annotation(docName, wikiName, offset, length));
            }
        }
    }
    
    // Constructor that builds a PagesIterator over the IITB data.
    public IITBPagesIterator(String groundTruthAnnotationsFilename, String auxGroundTruthAnnotationsFilename, String pagesDir) throws SAXException, IOException, ParserConfigurationException {
        if (!pagesDir.endsWith("/")) {
            pagesDir += "/";
        }
        
        this.groundTruthAnnotationsFilename = groundTruthAnnotationsFilename;
        this.auxGroundTruthAnnotationsFilename = auxGroundTruthAnnotationsFilename;
        this.pagesDir = pagesDir;
        
        // Map from the file name to a map of <offset, truth_mention> containing all the truth mentions from that page.
        TreeMap<String, Vector<Annotation>> annotations = new TreeMap<String, Vector<Annotation>>();
        
        insertGroundTruthAnnotations(groundTruthAnnotationsFilename, annotations);
        
        if (auxGroundTruthAnnotationsFilename != null) {
            insertGroundTruthAnnotations(auxGroundTruthAnnotationsFilename, annotations);
        }
        
        // Create an IITBSinglePage object for each of the documents found in the list uniqueFileNames.
        allDocs = new Vector<IITBSinglePage>();

        for (String docName : annotations.keySet()) {
            try {
               // BufferedReader in =  new BufferedReader(new FileReader(pagesDir + docName));
                String text = FileUtils.readFileToString(new File(pagesDir + docName), Charset.defaultCharset());
                
                Vector<TruthMention> truthMentions = new Vector<TruthMention>();
                
                for (Annotation annotation : annotations.get(docName)) {
                    TruthMention truthMention = new TruthMention(annotation.wikiName, null,
                            text.substring(annotation.offset, annotation.offset + annotation.length), annotation.offset);
                    truthMentions.add(truthMention);
                }
                
                IITBSinglePage iitbPage = new IITBSinglePage(docName, text, truthMentions);
                allDocs.add(iitbPage);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }  
        }
        
        currentCounter = 0;
    }
    
    @Override
    public boolean hasNext() {
        return currentCounter < allDocs.size();
    }

    @Override
    public GenericSinglePage next() {
        currentCounter++;
        return allDocs.get(currentCounter - 1);
    }

    @Override
    public void remove() {        
    }

    @Override
    public GenericPagesIterator hardCopy() {
        try {
            return new IITBPagesIterator(groundTruthAnnotationsFilename, auxGroundTruthAnnotationsFilename, pagesDir);
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        return null;
    }

}

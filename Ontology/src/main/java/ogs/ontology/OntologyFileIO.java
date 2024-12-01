package ogs.ontology;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RiotException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class OntologyFileIO {

    public static void loadFromRDF(Model ontModel, String filePath) throws RiotException, IOException {

        InputStream in = RDFDataMgr.open(filePath);
        ontModel.read(in, null);
        in.close();
    }

    public static void saveTo(Model model, String filePath) throws IOException {

        OutputStream out = new FileOutputStream(filePath);
        model.write(out);
        out.close();
    }
}
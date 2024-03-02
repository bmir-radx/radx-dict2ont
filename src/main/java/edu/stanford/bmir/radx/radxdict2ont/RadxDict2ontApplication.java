package edu.stanford.bmir.radx.radxdict2ont;

import edu.stanford.bmir.radx.datadictionary.lib.ParseMode;
import edu.stanford.bmir.radx.datadictionary.lib.RADxDataDictionaryParser;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Lazy;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
@ComponentScan(basePackages = {
        "edu.stanford.bmir.radx.datadictionary.lib",
        "edu.stanford.bmir.radx.radxdict2ont"
})
public class RadxDict2ontApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(RadxDict2ontApplication.class, args);
    }


    @Lazy
    @Bean
    OntCsvCache getOntCsvCache() throws IOException {
        var cache = new OntCsvCache();
        cache.load(new BufferedInputStream(RadxDict2ontApplication.class.getResourceAsStream("/MONDO.csv.gz")));
        cache.load(new BufferedInputStream(RadxDict2ontApplication.class.getResourceAsStream("/NCIT.csv.gz")));
        cache.load(new BufferedInputStream(RadxDict2ontApplication.class.getResourceAsStream("/IAO.csv.gz")));
        cache.load(new BufferedInputStream(RadxDict2ontApplication.class.getResourceAsStream("/HP.csv.gz")));
        cache.load(new BufferedInputStream(RadxDict2ontApplication.class.getResourceAsStream("/PATO.csv.gz")));
        cache.load(new BufferedInputStream(RadxDict2ontApplication.class.getResourceAsStream("/GSSO.csv.gz")));
        cache.load(new BufferedInputStream(RadxDict2ontApplication.class.getResourceAsStream("/NBO.csv.gz")));
        cache.load(new BufferedInputStream(RadxDict2ontApplication.class.getResourceAsStream("/SYMP.csv.gz")));
        return cache;
    }

    @Bean
    OWLDataFactory dataFactory() {
        return new OWLDataFactoryImpl();
    }

    @Autowired
    private ApplicationContext context;

    @Override
    public void run(String... args) throws Exception {
        var filePath = new URL(args[0]);
        var createAxioms = context.getBean(CreateAxioms.class);
        var parser = context.getBean(RADxDataDictionaryParser.class);
        var dataDictionary = parser.parse(filePath.openStream(),
                     ParseMode.LAX);
        var axioms = createAxioms.createAxioms(dataDictionary);
        var manager = OWLManager.createOWLOntologyManager();
        var ontology = manager.createOntology(axioms, IRI.create("https://bmir-radx.github.io/"));
        ontology.saveOntology(Files.newOutputStream(Path.of("/tmp/ont.owl")));
    }
}

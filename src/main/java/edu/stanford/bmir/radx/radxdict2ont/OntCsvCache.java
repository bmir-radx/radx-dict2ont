package edu.stanford.bmir.radx.radxdict2ont;

import edu.stanford.bmir.radx.datadictionary.lib.TermIdentifier;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.semanticweb.owlapi.model.IRI;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A class that represents a cache for ontology information in CSV format.  The cache can be used
 * to return synonyms, classes and parents for ontology terms.
 */
public class OntCsvCache {


    private final Map<String, String> id2Label = new HashMap<>();

    private final Map<String, List<String>> id2Synonyms = new HashMap<>();

    private final Map<String, List<String>> id2Definitions = new HashMap<>();

    private final Map<String, List<String>> id2Parents = new HashMap<>();

    /**
     * Retrieves the synonyms for a given OBO identifier.
     *
     * @param oboId The OBO identifier for which synonyms are to be retrieved.
     * @return A list of synonyms for the specified OBO identifier. If no synonyms are found, an empty list is returned.
     */
    public List<String> getSynonyms(String oboId) {
        return Optional.ofNullable(id2Synonyms.get(oboId)).orElse(List.of());
    }

    /**
     * Retrieves the definition(s) associated with a given OBO identifier.
     *
     * @param oboId The OBO identifier for which definitions are to be retrieved.
     * @return A list of definitions for the specified OBO identifier. If no definitions are found, an empty list is returned.
     */
    public List<String> getDefinition(String oboId) {
        return Optional.ofNullable(id2Definitions.get(oboId)).orElse(List.of());
    }

    /**
     * Retrieves the parent classes for a given OBO identifier.
     *
     * @param oboId The OBO identifier for which parent classes are to be retrieved.
     * @return A list of parent classes for the specified OBO identifier. If no parent classes are found, an empty list is returned.
     */
    public List<String> getParents(String oboId) {
        return Optional.ofNullable(id2Parents.get(oboId)).orElse(List.of());
    }

    public void load(InputStream inputStream) throws IOException {
            var gzipStream = new GzipCompressorInputStream(inputStream);
            var br = new BufferedReader(new InputStreamReader(gzipStream));
            var csvParser = new CSVParser(br, CSVFormat.DEFAULT);

            var records = csvParser.getRecords();
            var headerMap = new HashMap<String, Integer>();
            var header = records.get(0);
            for(int i = 0; i < header.size(); i++) {
                headerMap.put(header.get(i), i);
            }

            records.forEach(r -> {
                var id = r.get(headerMap.get(OntCsvHeader.IRI));
                var label = r.get(headerMap.get(OntCsvHeader.PREFERRED_LABEL));
                var syns = r.get(headerMap.get(OntCsvHeader.SYNONYMS));
                var split = syns.split("\\|");
                var synonyms = Stream.of(split)
                        .map(String::trim)
                      .filter(s -> !s.isEmpty())
                        .toList();
                id2Synonyms.put(id, synonyms);
                id2Label.put(id, label);

                var parentsIndex = headerMap.get(OntCsvHeader.PARENTS);
                if(parentsIndex != null) {
                    var parents = r.get(parentsIndex);
                    if(parents != null) {
                        var parentsList = Stream.of(parents.split("\\|"))
                                .map(p -> p.trim())
                                .filter(p -> !p.isEmpty())
                                .toList();
                        if(!parentsList.isEmpty()) {
                            id2Parents.put(id, parentsList);
                        }
                    }
                }

                var i = headerMap.get(OntCsvHeader.DEFINITION);
                if (i != null) {
                    var def = r.get(i);
                    if(def != null && !def.isEmpty()) {
                        id2Definitions.put(id, List.of(def));
                    }
                }
            });
    }

    public Optional<String> getLabel(IRI iri) {
        return Optional.ofNullable(id2Label.get(iri.toString()));
    }
}

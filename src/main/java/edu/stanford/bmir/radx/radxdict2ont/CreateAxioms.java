package edu.stanford.bmir.radx.radxdict2ont;

import edu.stanford.bmir.radx.datadictionary.lib.RADxDataDictionary;
import edu.stanford.bmir.radx.datadictionary.lib.TermIdentifier;
import org.apache.commons.text.CaseUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import org.semanticweb.owlapi.vocab.SKOSVocabulary;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.util.HashSet;
import java.util.Set;

import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.*;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2023-11-14
 */
@Component
public class CreateAxioms {

    private final OWLDataFactory dataFactory;

    private final OntCsvCache termsCache;

    public CreateAxioms(OWLDataFactory dataFactory, OntCsvCache termsCache) {
        this.dataFactory = dataFactory;
        this.termsCache = termsCache;
    }

    private final String PREFIX = "https://bmir-radx.github.io/";

    public Set<OWLAxiom> createAxioms(RADxDataDictionary dataDictionary) {
        var axioms = new HashSet<OWLAxiom>();
        var addedTerms = new HashSet<TermIdentifier>();
        var dataElementRootCls = dataFactory.getOWLClass(IRI.create(PREFIX, "DataElement"));
        axioms.add(dataFactory.getOWLDeclarationAxiom(dataElementRootCls));

        dataDictionary.records()
                .forEach(record -> {
                    var id = record.id();
                    var iri = IRI.create(PREFIX + id);
                    var cls = dataFactory.getOWLClass(iri);
                    axioms.add(dataFactory.getOWLDeclarationAxiom(cls));

                    axioms.add(dataFactory.getOWLAnnotationAssertionAxiom(
                            dataFactory.getOWLAnnotationProperty(IRI.create("https://schema.org/identifier")),
                            iri,
                            dataFactory.getOWLLiteral(id)
                    ));

                    // Under Section
                    var section = record.section();
                    if(!section.isEmpty()) {
                        var sectionIri = getSectionIri(section);
                        var sectionCls = dataFactory.getOWLClass(sectionIri);
                        var sectionClsRoot = dataFactory.getOWLClass(IRI.create(PREFIX + "DataElementSection"));
                        axioms.add(dataFactory.getOWLSubClassOfAxiom(sectionCls, sectionClsRoot));
//                        axioms.add(dataFactory.getOWLSubClassOfAxiom(sectionCls, dataElementRootCls));
                        axioms.add(dataFactory.getOWLSubClassOfAxiom(cls, dataElementRootCls));
                        axioms.add(dataFactory.getOWLAnnotationAssertionAxiom(
                                dataFactory.getRDFSLabel(),
                                sectionIri,
                                dataFactory.getOWLLiteral(section + " Section")
                        ));
                        axioms.add(dataFactory.getOWLAnnotationAssertionAxiom(
                                dataFactory.getOWLAnnotationProperty(IRI.create(PREFIX + "isInSection")),
                                cls.getIRI(),
                                sectionIri
                        ));
                    }
                    else {
                        axioms.add(dataFactory.getOWLSubClassOfAxiom(cls, dataElementRootCls));
                    }

                    // Label
                    if (!record.label().isEmpty()) {
                        axioms.add(dataFactory.getOWLAnnotationAssertionAxiom(
                                dataFactory.getRDFSLabel(),
                                iri,
                                dataFactory.getOWLLiteral(id + " Data Element")
                        ));
                        axioms.add(dataFactory.getOWLAnnotationAssertionAxiom(
                                getTextProperty(),
                                iri,
                                dataFactory.getOWLLiteral(record.label())
                        ));
                    }

                    // Description
                    if(!record.description().isEmpty()) {
                        axioms.add(dataFactory.getOWLAnnotationAssertionAxiom(
                                dataFactory.getOWLAnnotationProperty(SKOSVocabulary.DEFINITION.getIRI()),
                                iri,
                                dataFactory.getOWLLiteral(record.description())
                        ));
                    }

                    // Datatype


                    // Enumeration
                    if(record.enumeration() != null) {
                        var enumeration = record.enumeration();
                        enumeration.choices()
                                .forEach(choice -> {
                                    var choiceLabel = choice.label();
                                    var choiceValue = choice.value();
                                    var choiceIri = IRI.create(PREFIX + id + "_" + choiceValue);
                                    var choiceCls = dataFactory.getOWLClass(choiceIri);
                                    axioms.add(dataFactory.getOWLDeclarationAxiom(choiceCls));
                                    axioms.add(dataFactory.getOWLAnnotationAssertionAxiom(getTextProperty(),
                                                                                          choiceIri,
                                                                                          dataFactory.getOWLLiteral(choiceLabel)
                                    ));
                                    axioms.add(dataFactory.getOWLAnnotationAssertionAxiom(dataFactory.getRDFSLabel(),
                                                                                          choiceIri,
                                                                                          dataFactory.getOWLLiteral(id + " Value, " + choiceValue + " (" + choiceLabel + ")")
                                    ));

                                    axioms.add(dataFactory.getOWLAnnotationAssertionAxiom(dataFactory.getOWLAnnotationProperty(IRI.create("https://schema.org/value")),
                                                                                          choiceIri,
                                                                                          dataFactory.getOWLLiteral(Integer.parseInt(choiceValue))));
                                    axioms.add(dataFactory.getOWLAnnotationAssertionAxiom(
                                            dataFactory.getRDFSSeeAlso(),
                                            choiceIri,
                                            iri
                                    ));


                                    var choiceRootValueIri = IRI.create(PREFIX + id + "_value");
                                    var choiceRootValueCls = dataFactory.getOWLClass(choiceRootValueIri);
                                    axioms.add(dataFactory.getOWLDeclarationAxiom(choiceRootValueCls));
                                    axioms.add(dataFactory.getOWLSubClassOfAxiom(choiceCls, choiceRootValueCls));
                                    axioms.add(dataFactory.getOWLAnnotationAssertionAxiom(
                                            dataFactory.getRDFSLabel(),
                                            choiceRootValueIri,
                                            dataFactory.getOWLLiteral(id + " value")
                                    ));
                                    axioms.add(dataFactory.getOWLAnnotationAssertionAxiom(
                                            dataFactory.getRDFSSeeAlso(),
                                            choiceRootValueIri,
                                            iri
                                    ));

                                    var valueRootIri = IRI.create(PREFIX + "DataElementValue");
                                    var valueRootCls = dataFactory.getOWLClass(valueRootIri);
                                    axioms.add(dataFactory.getOWLSubClassOfAxiom(choiceRootValueCls, valueRootCls));

                                    axioms.add(dataFactory.getOWLAnnotationAssertionAxiom(
                                            dataFactory.getRDFSSeeAlso(),
                                            iri,
                                            choiceIri
                                    ));

                                });

                    }


                    // SeeAlso
                    if(record.seeAlso() != null) {
                        axioms.add(dataFactory.getOWLAnnotationAssertionAxiom(
                                dataFactory.getRDFSSeeAlso(),
                                iri,
                                IRI.create(record.seeAlso())
                        ));
                    }

                    record.terms()
                            .forEach(term -> {
                                if (!term.identifier().isEmpty()) {
                                    var termCls = addExternalTerm(term, axioms, Set.of(iri), addedTerms, 0);
                                    axioms.add(SubClassOf(
                                        cls,
                                        ObjectSomeValuesFrom(
                                                ObjectProperty(IRI.create(PREFIX + "isAssociatedWith")),
                                                termCls
                                        )
                                    ));
                                }
                            });
                });

        try {
            var definitionsOnt = OWLManager.createOWLOntologyManager()
                    .loadOntologyFromOntologyDocument(
                            new BufferedInputStream(
                                    CreateAxioms.class.getResourceAsStream("/definitions.ofn")
                            )
                    );
            axioms.addAll(definitionsOnt.getAxioms());
        } catch (OWLOntologyCreationException e) {
            throw new RuntimeException(e);
        }

        return axioms;
    }

    private OWLClass addExternalTerm(TermIdentifier term,
                                     HashSet<OWLAxiom> axioms,
                                     Set<IRI> dataElementIris,
                                     Set<TermIdentifier> addedTerms,
                                     int level) {
//        if(addedTerms.contains(term)) {
//            return;
//        }

        addedTerms.add(term);
        var resolved = resolve(term);

        var termCls = dataFactory.getOWLClass(IRI.create(resolved));
        axioms.add(dataFactory.getOWLDeclarationAxiom(termCls));
        dataElementIris.forEach(deIri -> {
            if (level == 0) {
                axioms.add(dataFactory.getOWLAnnotationAssertionAxiom(
                        dataFactory.getRDFSSeeAlso(), deIri,
                        termCls.getIRI()
                ));
            }

            axioms.add(dataFactory.getOWLAnnotationAssertionAxiom(
                        dataFactory.getRDFSSeeAlso(),
                        termCls.getIRI(), deIri
                ));

        });
        termsCache.getLabel(termCls.getIRI())
                .ifPresent(l -> {
                    axioms.add(dataFactory.getOWLAnnotationAssertionAxiom(
                            dataFactory.getRDFSLabel(),
                            termCls.getIRI(),
                            dataFactory.getOWLLiteral(l)
                    ));
                });

        var syns = termsCache.getSynonyms(resolved);
        for (var syn : syns) {
            if(level == 0) {
                dataElementIris.forEach(deIri -> {
                    axioms.add(dataFactory.getOWLAnnotationAssertionAxiom(
                            dataFactory.getOWLAnnotationProperty(SKOSVocabulary.ALTLABEL.getIRI()),
                            deIri,
                            dataFactory.getOWLLiteral(syn)
                    ));
                });
            }
            axioms.add(dataFactory.getOWLAnnotationAssertionAxiom(
                    dataFactory.getOWLAnnotationProperty(SKOSVocabulary.ALTLABEL.getIRI()),
                    termCls.getIRI(),
                    dataFactory.getOWLLiteral(syn)
            ));
        }
        var defs = termsCache.getDefinition(resolved);
        for(var def : defs) {
            axioms.add(dataFactory.getOWLAnnotationAssertionAxiom(
                    dataFactory.getOWLAnnotationProperty(SKOSVocabulary.DEFINITION.getIRI()),
                    termCls.getIRI(),
                    dataFactory.getOWLLiteral(def)
            ));
        }
        var parents = termsCache.getParents(resolved)
                .stream()
                .filter(p -> !p.equals(OWLRDFVocabulary.OWL_THING.getIRI().toString()))
                .toList();
        parents.forEach(parent -> {
            var parentCls = dataFactory.getOWLClass(IRI.create(parent));
            axioms.add(dataFactory.getOWLSubClassOfAxiom(termCls, parentCls));
            addExternalTerm(new TermIdentifier(parent), axioms, dataElementIris, addedTerms, level + 1);
        });
        if(parents.isEmpty()) {
            axioms.add(dataFactory.getOWLSubClassOfAxiom(termCls, dataFactory.getOWLClass(IRI.create(PREFIX + "OtherThing"))));
        }
        return termCls;
    }

    private String resolve(TermIdentifier termIdentifier) {
        var identifier = termIdentifier.identifier();
        if(identifier.startsWith("http")) {
            return identifier;
        }
        else if(identifier.startsWith("NCIT")) {
            return "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#" + identifier.substring(identifier.indexOf(":") + 1);
        }
        else {
            int prefixEnd = identifier.indexOf(":");
            if (prefixEnd != -1) {
                var prefix = identifier.substring(0, prefixEnd);
                return "http://purl.obolibrary.org/obo/" + prefix + "_" + identifier.substring(prefixEnd + 1);
            }
        }
        System.err.println(identifier);
        return identifier;

    }

    private OWLAnnotationProperty getTextProperty() {
        return dataFactory.getOWLAnnotationProperty(IRI.create("https://schema.org/text"));
    }

    private IRI getSectionIri(String section) {
        var sectionCamelCase = CaseUtils.toCamelCase(section, true) + "DataElement";
        return IRI.create(PREFIX + sectionCamelCase);
    }
}

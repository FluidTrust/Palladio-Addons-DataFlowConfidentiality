package org.palladiosimulator.dataflow.confidentiality.pcm.workflow.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Stack;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.EObject;
import org.palladiosimulator.dataflow.confidentiality.pcm.transformation.pcm2dfd.trace.DFDTraceElement;
import org.palladiosimulator.dataflow.confidentiality.pcm.transformation.pcm2dfd.trace.PCM2DFDTransformationTrace;
import org.palladiosimulator.dataflow.confidentiality.pcm.transformation.pcm2dfd.trace.PCMSingleTraceElement;
import org.palladiosimulator.dataflow.confidentiality.pcm.transformation.pcm2dfd.trace.PCMTraceElement;
import org.palladiosimulator.dataflow.confidentiality.pcm.workflow.TransitiveTransformationTrace;
import org.palladiosimulator.dataflow.confidentiality.transformation.prolog.DFD2PrologTransformationTrace;
import org.palladiosimulator.dataflow.diagram.DataFlowDiagram.Component;
import org.palladiosimulator.dataflow.diagram.DataFlowDiagram.DataFlowDiagram;
import org.palladiosimulator.dataflow.diagram.DataFlowDiagram.Entity;
import org.palladiosimulator.dataflow.dictionary.characterized.DataDictionaryCharacterized.CharacteristicType;
import org.palladiosimulator.dataflow.dictionary.characterized.DataDictionaryCharacterized.Literal;

import de.uka.ipd.sdq.identifier.Identifier;

public class TransitiveTransformationTraceImpl implements TransitiveTransformationTrace {

    private final PCM2DFDTransformationTrace pcm2dfdTrace;
    private final DFD2PrologTransformationTrace dfd2prologTrace;

    public TransitiveTransformationTraceImpl(PCM2DFDTransformationTrace pcm2dfdTrace,
            DFD2PrologTransformationTrace dfd2prologTrace) {
        this.pcm2dfdTrace = pcm2dfdTrace;
        this.dfd2prologTrace = dfd2prologTrace;
    }

    @Override
    public Collection<String> getFactIds(EObject pcmElement) {
        var dfdElements = this.pcm2dfdTrace.getDFDEntries(pcmElement);
        return getFactIds(dfdElements);
    }

    @Override
    public Collection<String> getFactIds(EObject pcmElement, Stack<Identifier> pcmElementContext) {
        var dfdElements = this.pcm2dfdTrace.getDFDEntries(pcmElement, pcmElementContext);
        return getFactIds(dfdElements);
    }

    protected Collection<String> getFactIds(Collection<DFDTraceElement> dfdElements) {
        var elements = dfdElements.stream()
            .map(DFDTraceElement::getElement)
            .collect(Collectors.toList());
        var dfdStream = elements.stream()
            .filter(Entity.class::isInstance)
            .map(Entity.class::cast)
            .map(this.dfd2prologTrace::getFactId);
        var ddStream = elements.stream()
            .filter(org.palladiosimulator.dataflow.dictionary.characterized.DataDictionaryCharacterized.Entity.class::isInstance)
            .map(org.palladiosimulator.dataflow.dictionary.characterized.DataDictionaryCharacterized.Entity.class::cast)
            .map(this.dfd2prologTrace::getFactId);
        return Stream.concat(dfdStream, ddStream)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }

    @Override
    public Collection<PCMTraceElement> getPCMEntries(String factId) {
        Optional<String> dfdId = this.dfd2prologTrace.getDfdId(factId);
        if (dfdId.isEmpty()) {
            return Collections.emptyList();
        }
        Optional<Identifier> dfdElement = dfdId.flatMap(id -> this.dfd2prologTrace.resolveDfdElement(id, Identifier.class));

        return dfdElement.map(this.pcm2dfdTrace::getPCMEntries)
            .orElse(Collections.emptyList())
            .stream()
            .map(PCMTraceElement.class::cast)
            .collect(Collectors.toList());
    }

    @Override
    public DataFlowDiagram getDfd() {
        return this.dfd2prologTrace.getDfd();
    }

    @Override
    public Collection<String> getLiteralFactIds(EObject pcmElement) {
        var dfdElements = this.pcm2dfdTrace.getDFDEntries(pcmElement);
        return getLiteralFactIDsFromDFD(dfdElements);
    }

    private Collection<String> getLiteralFactIDsFromDFD(Collection<DFDTraceElement> dfdElements) {
        Collection<Optional<String>> result = new ArrayList<>();
        for (var dfdElement : dfdElements) {
            var realElement = dfdElement.getElement();
            if (realElement instanceof Component) {
                result.add(this.dfd2prologTrace.getFactId((Component) realElement));
            } else if (realElement instanceof Literal) {
                result.add(this.dfd2prologTrace.getFactId((Literal) realElement));
            }
        }
        return result.stream()
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }

    @Override
    public Collection<String> getFactId(Predicate<CharacteristicType> predicate) {
        Predicate<PCMTraceElement> convertedPredicate = p -> {
            if (p instanceof PCMSingleTraceElement) {
                PCMSingleTraceElement pSingleElement = (PCMSingleTraceElement) p;
                if (pSingleElement.getElement() instanceof CharacteristicType) {
                    CharacteristicType ct = (CharacteristicType) pSingleElement.getElement();
                    return predicate.test(ct);
                }
            }
            return false;
        };

        Collection<String> foundIds = this.pcm2dfdTrace.getDFDEntries(convertedPredicate)
            .stream()
            .map(DFDTraceElement::getElement)
            .filter(CharacteristicType.class::isInstance)
            .map(CharacteristicType.class::cast)
            .map(this.dfd2prologTrace::getFactId)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());

        return foundIds;
    }

    @Override
    public Collection<String> getLiteralFactIdsBySemantic(EObject pcmElement) {
        var dfdElements = this.pcm2dfdTrace.getDFDEntriesBySemanticEquality(pcmElement);
        return getLiteralFactIDsFromDFD(dfdElements);
    }

}

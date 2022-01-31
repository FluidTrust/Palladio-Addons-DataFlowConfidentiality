package org.palladiosimulator.dataflow.confidentiality.pcm.transformation.pcm2dfd.trace.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.palladiosimulator.dataflow.confidentiality.pcm.transformation.pcm2dfd.trace.DFDTraceElement;
import org.palladiosimulator.dataflow.confidentiality.pcm.transformation.pcm2dfd.trace.PCM2DFDTransformationTrace;
import org.palladiosimulator.dataflow.confidentiality.pcm.transformation.pcm2dfd.trace.PCMContextHavingTraceElement;
import org.palladiosimulator.dataflow.confidentiality.pcm.transformation.pcm2dfd.trace.PCMSingleTraceElement;
import org.palladiosimulator.dataflow.confidentiality.pcm.transformation.pcm2dfd.trace.PCMTraceElement;
import org.palladiosimulator.dataflow.confidentiality.pcm.transformation.pcm2dfd.trace.TraceEntry;
import org.palladiosimulator.dataflow.dictionary.characterized.DataDictionaryCharacterized.EnumCharacteristicType;
import org.palladiosimulator.dataflow.dictionary.characterized.DataDictionaryCharacterized.Literal;

import de.uka.ipd.sdq.identifier.Identifier;

public class PCM2DFDTransformationTraceImpl implements PCM2DFDTransformationTrace, TransformationTraceModifier {

    private final Collection<TraceEntry> traceEntries = new ArrayList<>();
    private final Map<Object, EnumCharacteristicType> annotationCtEntry = new HashMap<>();
    private final Map<Object, Literal> annotationLiteralEntry = new HashMap<>();

    public PCM2DFDTransformationTraceImpl() {
        // intentionally left blank
    }

    @Override
    public void addTraceEntry(TraceEntry entry) {
        this.traceEntries.add(entry);
    }

    @Override
    public Collection<DFDTraceElement> getDFDEntries(EObject pcmElement) {
        return this.traceEntries.stream()
            .filter(entry -> entry.getPCMEntry() instanceof PCMSingleTraceElement)
            .filter(entry -> ((PCMSingleTraceElement) entry.getPCMEntry()).getElement()
                .equals(pcmElement))
            .map(TraceEntry::getDFDEntry)
            .collect(Collectors.toList());
    }

    @Override
    public Collection<DFDTraceElement> getDFDEntries(EObject pcmElement, Stack<Identifier> pcmElementContext) {
        return this.traceEntries.stream()
            .filter(entry -> entry.getPCMEntry() instanceof PCMContextHavingTraceElement)
            .filter(entry -> ((PCMContextHavingTraceElement) entry.getPCMEntry()).getContext()
                .equals(pcmElementContext))
            .map(TraceEntry::getDFDEntry)
            .collect(Collectors.toList());
    }

    @Override
    public Collection<PCMTraceElement> getPCMEntries(Identifier dfdElement) {
        return this.traceEntries.stream()
            .filter(entry -> entry.getDFDEntry()
                .getElement()
                .equals(dfdElement))
            .map(TraceEntry::getPCMEntry)
            .collect(Collectors.toList());
    }

    @Override
    public Collection<DFDTraceElement> getDFDEntries(Predicate<PCMTraceElement> predicate) {
        return this.traceEntries.stream()
            .filter(e -> predicate.test(e.getPCMEntry()))
            .map(TraceEntry::getDFDEntry)
            .collect(Collectors.toList());
    }

    @Override
    public void addAnnotationEntry(Object annotation, EnumCharacteristicType characteristicType) {
        this.annotationCtEntry.put(annotation, characteristicType);
    }

    @Override
    public void addAnnotationEntry(Object annotation, Literal literal) {
        this.annotationLiteralEntry.put(annotation, literal);
    }

    @Override
    public EnumCharacteristicType getCharacteristicTypeFromAnnotation(Object annotation) {
        return this.annotationCtEntry.get(annotation);
    }

    @Override
    public Literal getLiteralFromAnnotation(Object annotation) {
        return this.annotationLiteralEntry.get(annotation);
    }

    @Override
    public Collection<DFDTraceElement> getDFDEntriesBySemanticEquality(EObject pcmElement) {
        return this.traceEntries.stream()
                .filter(entry -> entry.getPCMEntry() instanceof PCMSingleTraceElement)
            .filter(entry -> EcoreUtil.equals(((PCMSingleTraceElement) entry.getPCMEntry())
                .getElement()
                    , pcmElement))
                .map(TraceEntry::getDFDEntry)
                .collect(Collectors.toList());
    }

}

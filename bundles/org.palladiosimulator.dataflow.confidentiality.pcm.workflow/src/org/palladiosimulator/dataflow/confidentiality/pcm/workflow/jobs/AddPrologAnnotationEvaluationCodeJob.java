package org.palladiosimulator.dataflow.confidentiality.pcm.workflow.jobs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.palladiosimulator.dataflow.confidentiality.pcm.transformation.pcm2dfd.PcmAnnotations.PCMActionType;
import org.palladiosimulator.dataflow.confidentiality.pcm.transformation.pcm2dfd.PcmAnnotations.PCMContainingType;
import org.palladiosimulator.dataflow.confidentiality.pcm.transformation.pcm2dfd.trace.PCM2DFDTransformationTrace;
import org.palladiosimulator.dataflow.confidentiality.pcm.workflow.TransitiveTransformationTrace;
import org.palladiosimulator.dataflow.confidentiality.transformation.workflow.blackboards.KeyValueMDSDBlackboard;
import org.palladiosimulator.supporting.prolog.model.prolog.Clause;
import org.palladiosimulator.supporting.prolog.model.prolog.Comment;
import org.palladiosimulator.supporting.prolog.model.prolog.Program;
import org.palladiosimulator.supporting.prolog.model.prolog.PrologFactory;
import org.palladiosimulator.supporting.prolog.model.prolog.Rule;

import de.uka.ipd.sdq.workflow.jobs.AbstractBlackboardInteractingJob;
import de.uka.ipd.sdq.workflow.jobs.CleanupFailedException;
import de.uka.ipd.sdq.workflow.jobs.JobFailedException;
import de.uka.ipd.sdq.workflow.jobs.UserCanceledException;
import de.uka.ipd.sdq.workflow.mdsd.blackboard.ModelLocation;

public class AddPrologAnnotationEvaluationCodeJob extends AbstractBlackboardInteractingJob<KeyValueMDSDBlackboard> {

    private final ModelLocation programLocation;
    private final String pcmTraceKey;
    private final String transitiveTraceKey;

    public AddPrologAnnotationEvaluationCodeJob(ModelLocation programLocation, String pcmTraceKey,
            String transitiveTraceKey) {
        this.programLocation = programLocation;
        this.pcmTraceKey = pcmTraceKey;
        this.transitiveTraceKey = transitiveTraceKey;
    }

    @Override
    public void execute(IProgressMonitor arg0) throws JobFailedException, UserCanceledException {
        PCM2DFDTransformationTrace pcmTrace = getBlackboard().get(pcmTraceKey)
            .filter(PCM2DFDTransformationTrace.class::isInstance)
            .map(PCM2DFDTransformationTrace.class::cast)
            .orElseThrow(() -> new JobFailedException("There is no PCM2DFD transformation trace available."));

        TransitiveTransformationTrace transitiveTrace = getBlackboard().get(transitiveTraceKey)
            .filter(TransitiveTransformationTrace.class::isInstance)
            .map(TransitiveTransformationTrace.class::cast)
            .orElseThrow(() -> new JobFailedException("There is no transitive transformation trace available."));

        Program prologProgram = getBlackboard().getContents(programLocation)
            .stream()
            .filter(Program.class::isInstance)
            .map(Program.class::cast)
            .findFirst()
            .orElseThrow(() -> new JobFailedException("There is no Prolog program available."));

        extendPrologProgram(prologProgram, pcmTrace, transitiveTrace);
    }

    protected void extendPrologProgram(Program prologProgram, PCM2DFDTransformationTrace pcmTrace,
            TransitiveTransformationTrace transitiveTrace) {

        prologProgram.getClauses().addAll(createComment(
              "==================================\n"
            + "HELPER: query PCM meta information\n"
            + "=================================="
        ));
        
        prologProgram.getClauses()
            .addAll(createNodeCharacteristicRules("containedIn", PCMContainingType.values(), pcmTrace,
                    transitiveTrace));
        prologProgram.getClauses()
            .addAll(createNodeCharacteristicRules("isA", PCMActionType.values(), pcmTrace, transitiveTrace));
    }
    
    protected List<Clause> createComment(String text) {
        return Arrays.asList(text.split("\r?\n")).stream().map(line -> {
            Comment comment = PrologFactory.eINSTANCE.createComment();
            comment.setValue(line);
            return comment;
        }).collect(Collectors.toList());
    }

    protected Collection<Clause> createNodeCharacteristicRules(String rulePrefix, Object[] annotations, PCM2DFDTransformationTrace pcmTrace,
            TransitiveTransformationTrace transitiveTrace) {
        Collection<Clause> additionalRules = new ArrayList<>();
        for (Object annotation : annotations) {
            String ruleName = rulePrefix + annotation.toString();
            createNodeCharacteristicRule(ruleName, annotation, pcmTrace, transitiveTrace).ifPresent(additionalRules::add);
        }
        return additionalRules;
    }

    protected Optional<Rule> createNodeCharacteristicRule(String ruleName, Object annotation, PCM2DFDTransformationTrace pcmTrace,
            TransitiveTransformationTrace transitiveTrace) {
        var ct = pcmTrace.getCharacteristicTypeFromAnnotation(annotation);
        var ctIds = transitiveTrace.getFactIds(ct);
        var literal = pcmTrace.getLiteralFromAnnotation(annotation);
        var literalIds = transitiveTrace.getLiteralFactIds(literal);
        if (ctIds.size() != 1 || literalIds.size() != 1) {
            return Optional.empty();
        }
        var ctId = ctIds.iterator().next();
        var literalId = literalIds.iterator().next();
        
        var rule = PrologFactory.eINSTANCE.createRule();

        var head = PrologFactory.eINSTANCE.createCompoundTerm();
        head.setValue(ruleName);
        rule.setHead(head);

        var nodeVariable = PrologFactory.eINSTANCE.createCompoundTerm();
        nodeVariable.setValue("N");
        head.getArguments().add(nodeVariable);

        var body = PrologFactory.eINSTANCE.createCompoundTerm();
        body.setValue("nodeCharacteristic");
        rule.setBody(body);
        
        body.getArguments().add(EcoreUtil.copy(nodeVariable));
        
        var ctString = PrologFactory.eINSTANCE.createAtomicQuotedString();
        ctString.setValue(ctId);
        body.getArguments().add(ctString);

        var literalString = PrologFactory.eINSTANCE.createAtomicQuotedString();
        literalString.setValue(literalId);
        body.getArguments().add(literalString);
        
        return Optional.of(rule);
    }

    @Override
    public String getName() {
        return "Add additional Prolog code";
    }

    @Override
    public void cleanup(IProgressMonitor arg0) throws CleanupFailedException {
        // nothing to do here
    }

}

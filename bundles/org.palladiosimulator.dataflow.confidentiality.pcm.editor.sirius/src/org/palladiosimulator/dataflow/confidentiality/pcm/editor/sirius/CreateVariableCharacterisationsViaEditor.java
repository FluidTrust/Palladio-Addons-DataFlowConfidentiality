package org.palladiosimulator.dataflow.confidentiality.pcm.editor.sirius;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.ComposedSwitch;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.jface.window.Window;
import org.eclipse.sirius.tools.api.ui.IExternalJavaAction;
import org.eclipse.ui.PlatformUI;
import org.palladiosimulator.dataflow.confidentiality.pcm.editor.sirius.assignments.AssignmentsEditorFactoryProvider;
import org.palladiosimulator.dataflow.confidentiality.pcm.editor.sirius.assignments.AssignmentsEditorImpl;
import org.palladiosimulator.dataflow.confidentiality.pcm.model.confidentiality.ConfidentialityVariableCharacterisation;
import org.palladiosimulator.indirections.actions.ConsumeDataAction;
import org.palladiosimulator.indirections.actions.CreateDateAction;
import org.palladiosimulator.indirections.actions.util.ActionsSwitch;
import org.palladiosimulator.pcm.parameter.VariableUsage;
import org.palladiosimulator.pcm.repository.OperationSignature;
import org.palladiosimulator.pcm.repository.Parameter;
import org.palladiosimulator.pcm.seff.AbstractAction;
import org.palladiosimulator.pcm.seff.AbstractBranchTransition;
import org.palladiosimulator.pcm.seff.AbstractLoopAction;
import org.palladiosimulator.pcm.seff.BranchAction;
import org.palladiosimulator.pcm.seff.ExternalCallAction;
import org.palladiosimulator.pcm.seff.ForkAction;
import org.palladiosimulator.pcm.seff.ForkedBehaviour;
import org.palladiosimulator.pcm.seff.ResourceDemandingBehaviour;
import org.palladiosimulator.pcm.seff.ResourceDemandingSEFF;
import org.palladiosimulator.pcm.seff.SeffPackage;
import org.palladiosimulator.pcm.seff.SetVariableAction;
import org.palladiosimulator.pcm.seff.StartAction;
import org.palladiosimulator.pcm.seff.StopAction;
import org.palladiosimulator.pcm.seff.util.SeffSwitch;
import org.palladiosimulator.pcm.usagemodel.AbstractUserAction;
import org.palladiosimulator.pcm.usagemodel.Branch;
import org.palladiosimulator.pcm.usagemodel.BranchTransition;
import org.palladiosimulator.pcm.usagemodel.EntryLevelSystemCall;
import org.palladiosimulator.pcm.usagemodel.Loop;
import org.palladiosimulator.pcm.usagemodel.ScenarioBehaviour;
import org.palladiosimulator.pcm.usagemodel.Start;
import org.palladiosimulator.pcm.usagemodel.Stop;
import org.palladiosimulator.pcm.usagemodel.UsagemodelPackage;
import org.palladiosimulator.pcm.usagemodel.util.UsagemodelSwitch;

import de.uka.ipd.sdq.stoex.AbstractNamedReference;
import de.uka.ipd.sdq.stoex.VariableReference;

public class CreateVariableCharacterisationsViaEditor implements IExternalJavaAction {

    @Override
    public boolean canExecute(Collection<? extends EObject> context) {
        var isVariableUsage = context.stream()
            .anyMatch(VariableUsage.class::isInstance);
        var isConfidentialityVariableCharacterisation = context.stream()
            .anyMatch(ConfidentialityVariableCharacterisation.class::isInstance);
        return isVariableUsage || isConfidentialityVariableCharacterisation;
    }

    @Override
    public void execute(Collection<? extends EObject> context, Map<String, Object> parameters) {
        var contextParents = context.stream()
            .filter(ConfidentialityVariableCharacterisation.class::isInstance)
            .map(EObject::eContainer);
        var variableUsage = Stream.concat(contextParents, context.stream())
            .filter(VariableUsage.class::isInstance)
            .map(VariableUsage.class::cast)
            .findFirst()
            .get();
        adjustCharacterisations(variableUsage);
    }

    protected void adjustCharacterisations(VariableUsage variableUsage) {
        AssignmentsEditorImpl editor = buildTextualEditor(variableUsage);
        if (editor.open() != Window.OK) {
            return;
        }
        replaceVariableCharacterisations(variableUsage, editor.getResult());
    }

    protected AssignmentsEditorImpl buildTextualEditor(VariableUsage variableUsage) {
        // find shell
        var shell = PlatformUI.getWorkbench()
            .getActiveWorkbenchWindow()
            .getShell();

        // find dictionaries
        var dictionaries = QueryHelpers.findCharacteristicTypeDictionariesInSemanticResources(variableUsage);

        // find output variable name
        var outputName = variableUsage.getNamedReference__VariableUsage()
            .getReferenceName();

        // find input variable names
        var inputNames = findInputs(variableUsage);

        // build editor
        return AssignmentsEditorFactoryProvider.getInstance()
            .create(shell, outputName, inputNames, dictionaries, variableUsage);
    }

    protected Collection<String> findInputs(VariableUsage variableUsage) {
        var inputs = new ArrayList<String>();

        // RDSEFF
        inputs.addAll(findParentOfType(variableUsage, ResourceDemandingSEFF.class)
            .map(seff -> findInputs(variableUsage, seff))
            .orElse(Collections.emptyList()));

        // UsageModel
        inputs.addAll(findParentOfType(variableUsage, ScenarioBehaviour.class)
            .map(behaviour -> findInputs(variableUsage, behaviour))
            .orElse(Collections.emptyList()));

        return inputs;
    }

    protected Collection<String> findInputs(VariableUsage variableUsage, ScenarioBehaviour behaviour) {
        var inputs = new ArrayList<String>();

        // add variables exposed by previous actions
        var initialAction = findParentOfType(variableUsage, AbstractUserAction.class).get();
        var predecessorIter = new AbstractUserActionPredecessorIterator(initialAction);
        var variablesExtractor = new AbstractUserActionOutputVariablesSwitch();
        while (predecessorIter.hasNext()) {
            var action = predecessorIter.next();
            inputs.addAll(variablesExtractor.doSwitch(action));
        }

        // add result for output characterisations of ELSCs
        var calledSignatureHasReturn = Optional.of(initialAction)
            .filter(EntryLevelSystemCall.class::isInstance)
            .map(EntryLevelSystemCall.class::cast)
            .map(EntryLevelSystemCall::getOperationSignature__EntryLevelSystemCall)
            .map(OperationSignature::getReturnType__OperationSignature)
            .map(Objects::nonNull)
            .orElse(false);
        if (calledSignatureHasReturn && variableUsage
            .eContainmentFeature() == UsagemodelPackage.Literals.ENTRY_LEVEL_SYSTEM_CALL__OUTPUT_PARAMETER_USAGES_ENTRY_LEVEL_SYSTEM_CALL) {
            inputs.add("RETURN");
        }

        return inputs;
    }

    protected Collection<String> findInputs(VariableUsage variableUsage, ResourceDemandingSEFF seff) {
        var inputs = new ArrayList<String>();

        // add parameters
        if (seff.getDescribedService__SEFF() instanceof OperationSignature) {
            var signature = (OperationSignature) seff.getDescribedService__SEFF();
            signature.getParameters__OperationSignature()
                .stream()
                .map(Parameter::getParameterName)
                .forEach(inputs::add);
        }

        // add variables exposed by previous actions
        var initialAction = findParentOfType(variableUsage, AbstractAction.class).get();
        var predecessorIter = new AbstractActionPredecessorIterator(initialAction);
        var variablesExtractor = new AbstractActionOutputVariablesSwitch();
        while (predecessorIter.hasNext()) {
            var action = predecessorIter.next();
            inputs.addAll(variablesExtractor.doSwitch(action));
        }

        // add result for output characterisations of ECAs
        var calledSignatureHasReturn = Optional.of(initialAction)
            .filter(ExternalCallAction.class::isInstance)
            .map(ExternalCallAction.class::cast)
            .map(ExternalCallAction::getCalledService_ExternalService)
            .map(OperationSignature::getReturnType__OperationSignature)
            .map(Objects::nonNull)
            .orElse(false);
        if (calledSignatureHasReturn && variableUsage
            .eContainmentFeature() == SeffPackage.Literals.CALL_RETURN_ACTION__RETURN_VARIABLE_USAGE_CALL_RETURN_ACTION) {
            inputs.add("RETURN");
        }

        return inputs;
    }

    protected void replaceVariableCharacterisations(VariableUsage variableUsage,
            Collection<VariableUsage> definedUsages) {
        // extract characterisations
        var characterisations = definedUsages.stream()
            .filter(du -> variableUsage.getNamedReference__VariableUsage()
                .getReferenceName()
                .equals(du.getNamedReference__VariableUsage()
                    .getReferenceName()))
            .map(VariableUsage::getVariableCharacterisation_VariableUsage)
            .flatMap(Collection::stream)
            .filter(ConfidentialityVariableCharacterisation.class::isInstance)
            .map(ConfidentialityVariableCharacterisation.class::cast)
            .map(EcoreUtil::copy)
            .collect(Collectors.toList());

        // replace characterisations in variable usage
        variableUsage.getVariableCharacterisation_VariableUsage()
            .removeIf(ConfidentialityVariableCharacterisation.class::isInstance);
        variableUsage.getVariableCharacterisation_VariableUsage()
            .addAll(characterisations);
    }

    protected static class AbstractUserActionPredecessorIterator implements Iterator<AbstractUserAction> {

        protected static class PredecessorSwitch extends UsagemodelSwitch<Collection<AbstractUserAction>> {

            @Override
            public Collection<AbstractUserAction> caseAbstractUserAction(AbstractUserAction object) {
                return Arrays.asList(object.getPredecessor());
            }

            @Override
            public Collection<AbstractUserAction> caseBranch(Branch object) {
                var result = new ArrayList<AbstractUserAction>();
                result.add(object.getPredecessor());
                object.getBranchTransitions_Branch()
                    .stream()
                    .map(BranchTransition::getBranchedBehaviour_BranchTransition)
                    .map(ScenarioBehaviour::getActions_ScenarioBehaviour)
                    .map(PredecessorSwitch::findStop)
                    .forEach(result::add);
                return result;
            }

            @Override
            public Collection<AbstractUserAction> caseLoop(Loop object) {
                var result = new ArrayList<AbstractUserAction>();
                result.add(object.getPredecessor());
                result.add(findStop(object.getBodyBehaviour_Loop()
                    .getActions_ScenarioBehaviour()));
                return result;
            }

            @Override
            public Collection<AbstractUserAction> caseStart(Start object) {
                return Collections.emptyList();
            }

            @Override
            public Collection<AbstractUserAction> defaultCase(EObject object) {
                return Collections.emptyList();
            }

            protected static Stop findStop(Collection<AbstractUserAction> steps) {
                return steps.stream()
                    .filter(Stop.class::isInstance)
                    .map(Stop.class::cast)
                    .findFirst()
                    .get();
            }

            @Override
            public Collection<AbstractUserAction> doSwitch(EObject eObject) {
                var result = new ArrayList<>(Optional.ofNullable(super.doSwitch(eObject))
                    .orElse(Collections.emptyList()));
                result.removeIf(Objects::isNull);
                return result;
            }

        }

        protected static final PredecessorSwitch PREDECESSOR_SWITCH = new PredecessorSwitch();
        protected final Queue<AbstractUserAction> queue = new LinkedList<>();

        public AbstractUserActionPredecessorIterator(AbstractUserAction startingPoint) {
            queue.add(startingPoint);
            next(); // skip initial action
        }

        @Override
        public boolean hasNext() {
            return !queue.isEmpty();
        }

        @Override
        public AbstractUserAction next() {
            var next = queue.poll();
            queue.addAll(PREDECESSOR_SWITCH.doSwitch(next));
            return next;
        }

    }

    protected static class AbstractActionPredecessorIterator implements Iterator<AbstractAction> {

        protected static class PredecessorSwitch extends SeffSwitch<Collection<AbstractAction>> {

            @Override
            public Collection<AbstractAction> caseAbstractAction(AbstractAction object) {
                return Arrays.asList(object.getPredecessor_AbstractAction());
            }

            @Override
            public Collection<AbstractAction> caseAbstractLoopAction(AbstractLoopAction object) {
                var innerStopAction = findStopAction(object.getBodyBehaviour_Loop()
                    .getSteps_Behaviour());
                return Arrays.asList(object.getPredecessor_AbstractAction(), innerStopAction);
            }

            @Override
            public Collection<AbstractAction> caseBranchAction(BranchAction object) {
                var result = new ArrayList<AbstractAction>();
                result.add(object.getPredecessor_AbstractAction());
                object.getBranches_Branch()
                    .stream()
                    .map(AbstractBranchTransition::getBranchBehaviour_BranchTransition)
                    .map(ResourceDemandingBehaviour::getSteps_Behaviour)
                    .map(PredecessorSwitch::findStopAction)
                    .forEach(result::add);
                return result;
            }

            @Override
            public Collection<AbstractAction> caseForkAction(ForkAction object) {
                var result = new ArrayList<AbstractAction>();
                result.add(object.getPredecessor_AbstractAction());
                object.getAsynchronousForkedBehaviours_ForkAction()
                    .stream()
                    .map(ForkedBehaviour::getSteps_Behaviour)
                    .map(PredecessorSwitch::findStopAction)
                    .forEach(result::add);
                return result;
            }

            @Override
            public Collection<AbstractAction> caseStartAction(StartAction object) {
                return Collections.emptyList();
            }

            protected static StopAction findStopAction(Collection<AbstractAction> steps) {
                return steps.stream()
                    .filter(StopAction.class::isInstance)
                    .map(StopAction.class::cast)
                    .findFirst()
                    .get();
            }

            @Override
            public Collection<AbstractAction> doSwitch(EObject eObject) {
                var result = new ArrayList<>(Optional.ofNullable(super.doSwitch(eObject))
                    .orElse(Collections.emptyList()));
                result.removeIf(Objects::isNull);
                return result;
            }

        }

        protected static final PredecessorSwitch PREDECESSOR_SWITCH = new PredecessorSwitch();
        protected final Queue<AbstractAction> queue = new LinkedList<>();

        public AbstractActionPredecessorIterator(AbstractAction startingPoint) {
            queue.add(startingPoint);
            next(); // skip first action
        }

        @Override
        public boolean hasNext() {
            return !queue.isEmpty();
        }

        @Override
        public AbstractAction next() {
            var next = queue.poll();
            queue.addAll(PREDECESSOR_SWITCH.doSwitch(next));
            return next;
        }
    }

    protected static class AbstractActionOutputVariablesSwitch extends ComposedSwitch<Collection<String>> {

        public AbstractActionOutputVariablesSwitch() {
            super(Arrays.asList(new AbstractActionOutputVariablesSwitchPCM(),
                    new AbstractActionOutputVariablesSwitchIndirections()));
        }

        @Override
        public Collection<String> defaultCase(EObject eObject) {
            return Collections.emptyList();
        }

    }

    protected static class AbstractActionOutputVariablesSwitchPCM extends SeffSwitch<Collection<String>> {

        @Override
        public Collection<String> caseExternalCallAction(ExternalCallAction object) {
            return getNames(object.getReturnVariableUsage__CallReturnAction());
        }

        @Override
        public Collection<String> caseSetVariableAction(SetVariableAction object) {
            return getNames(object.getLocalVariableUsages_SetVariableAction());
        }

    }

    protected static class AbstractActionOutputVariablesSwitchIndirections extends ActionsSwitch<Collection<String>> {

        @Override
        public Collection<String> caseConsumeDataAction(ConsumeDataAction object) {
            return Optional.ofNullable(object.getVariableReference())
                .map(VariableReference::getReferenceName)
                .map(Arrays::asList)
                .orElse(Collections.emptyList());
        }

        @Override
        public Collection<String> caseCreateDateAction(CreateDateAction object) {
            return getNames(object.getVariableUsages());
        }

    }

    protected static class AbstractUserActionOutputVariablesSwitch extends UsagemodelSwitch<Collection<String>> {

        @Override
        public Collection<String> caseEntryLevelSystemCall(EntryLevelSystemCall object) {
            return getNames(object.getOutputParameterUsages_EntryLevelSystemCall());
        }

        @Override
        public Collection<String> defaultCase(EObject object) {
            return Collections.emptyList();
        }

    }

    protected static Collection<String> getNames(Collection<VariableUsage> usages) {
        return usages.stream()
            .map(VariableUsage::getNamedReference__VariableUsage)
            .map(AbstractNamedReference::getReferenceName)
            .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    protected static <T extends EObject> Optional<T> findParentOfType(EObject object, Class<T> clz) {
        var currentObject = object;
        while (currentObject != null && !clz.isInstance(currentObject)) {
            currentObject = currentObject.eContainer();
        }
        return Optional.ofNullable((T) currentObject);
    }

}

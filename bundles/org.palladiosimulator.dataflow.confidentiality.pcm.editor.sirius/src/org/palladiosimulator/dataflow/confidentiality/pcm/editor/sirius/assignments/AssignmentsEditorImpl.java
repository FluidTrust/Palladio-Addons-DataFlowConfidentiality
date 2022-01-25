package org.palladiosimulator.dataflow.confidentiality.pcm.editor.sirius.assignments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.xtext.ui.editor.embedded.EmbeddedEditorFactory;
import org.eclipse.xtext.ui.editor.embedded.EmbeddedEditorModelAccess;
import org.eclipse.xtext.ui.editor.embedded.IEditedResourceProvider;
import org.eclipse.xtext.validation.IResourceValidator;
import org.eclipse.xtext.validation.Issue;
import org.palladiosimulator.dataflow.confidentiality.pcm.model.confidentiality.ConfidentialityVariableCharacterisation;
import org.palladiosimulator.dataflow.confidentiality.pcm.model.confidentiality.behaviour.ReusableBehaviour;
import org.palladiosimulator.dataflow.confidentiality.pcm.model.confidentiality.characteristics.CharacteristicTypeDictionary;
import org.palladiosimulator.dataflow.confidentiality.pcm.model.confidentiality.dictionary.PCMDataDictionary;
import org.palladiosimulator.pcm.parameter.VariableUsage;

import com.google.inject.Inject;

@SuppressWarnings("restriction")
public class AssignmentsEditorImpl extends TitleAreaDialog {

    @Inject
    private ViewerConfiguration configuration;

    @Inject
    private EmbeddedEditorFactory embeddedEditorFactory;

    @Inject
    private IResourceValidator originalValidator;

    @Inject
    private IEditedResourceProvider editedResourceProvider;

    private final String outputName;
    private final Collection<String> usableInputs;
    private final Collection<CharacteristicTypeDictionary> dictionaries;
    private final VariableUsage existingUsage;

    private EmbeddedEditorModelAccess modelAccess;

    private Collection<VariableUsage> result;

    private SerializationHelper serializationHelper;

    public AssignmentsEditorImpl(Shell parentShell, String outputName, Collection<String> usableInputs,
            Collection<CharacteristicTypeDictionary> dictionaries, VariableUsage existingUsage) {
        super(parentShell);
        this.outputName = outputName;
        this.usableInputs = usableInputs;
        this.dictionaries = dictionaries;
        this.existingUsage = existingUsage;
    }

    public Collection<VariableUsage> getResult() {
        return result;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        serializationHelper = new SerializationHelper(editedResourceProvider);

        final var composite = (Composite) super.createDialogArea(parent);
        final var observableValidator = new ObservableResourceValidator(originalValidator);
        final var embeddedEditor = embeddedEditorFactory.newEditor(editedResourceProvider)
            .showErrorAndWarningAnnotations()
            .withResourceValidator(observableValidator)
            .withParent(composite);

        try {
            var prefix = getPrefix();
            var content = getContent();
            var postfix = getPostfix();
            modelAccess = embeddedEditor.createPartialEditor(prefix, content, postfix, true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        configuration.getHighlightingHelper()
            .install(embeddedEditor.getConfiguration(), embeddedEditor.getViewer());
        embeddedEditor.getViewer()
            .getTextWidget()
            .setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));
        observableValidator.addValidationListener(this::processValidationResult);
        composite.pack();
        return composite;
    }

    @Override
    protected Control createContents(Composite parent) {
        Control control = super.createContents(parent);
        getShell().setText("Edit assignments");
        setTitle("Edit assignments");
        return control;
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected void okPressed() {
        setResultVariables();
        super.okPressed();
    }

    protected PCMDataDictionary buildSerializationModel(Consumer<ReusableBehaviour> behaviourBuilder) {
        return serializationHelper.buildSerializationModel(behaviourBuilder, outputName, usableInputs, dictionaries);
    }

    protected String getPrefix() throws IOException {
        var dict = buildSerializationModel(behaviour -> {
        });
        var serializedDict = serializationHelper.serializeDict(dict, dictionaries);
        return serializedDict.substring(0, serializedDict.lastIndexOf("}") - 1);
    }

    protected String getContent() throws IOException {
        if (existingUsage == null || !existingUsage.getVariableCharacterisation_VariableUsage()
            .stream()
            .anyMatch(ConfidentialityVariableCharacterisation.class::isInstance)) {
            return "";
        }

        var dict = buildSerializationModel(behaviour -> {
            var variableUsageToEdit = EcoreUtil.copy(existingUsage);
            behaviour.getVariableUsages()
                .add(variableUsageToEdit);
        });
        var serializedDict = serializationHelper.serializeDict(dict, Arrays.asList(dict));

        var behaviorIndex = serializedDict.indexOf("behavior ");
        var behaviorText = serializedDict.substring(behaviorIndex, serializedDict.length())
            .trim();
        var behaviorBlockOpenIndex = behaviorText.indexOf("{");
        var behaviorContentText = behaviorText.substring(behaviorBlockOpenIndex + 1, behaviorText.length() - 1);

        var assignments = behaviorContentText.replaceAll("\\s*((input)|(output))\\s+[^\\s]+", "")
            .trim();
        var assignmentLines = Arrays.asList(assignments.split("\r?\n"));
        return assignmentLines.stream()
            .map(line -> {
                if (line.startsWith("\t")) {
                    return line.substring(1);
                }
                return line;
            })
            .collect(Collectors.joining(System.lineSeparator()));
    }

    protected String getPostfix() {
        return "}";
    }

    protected void processValidationResult(final Collection<Issue> issues) {
        Display.getDefault()
            .asyncExec(() -> {
                final var okButton = getButton(IDialogConstants.OK_ID);
                if (issues.isEmpty()) {
                    okButton.setEnabled(true);
                } else {
                    okButton.setEnabled(false);
                }
                createAndSetErrorMessage(issues);
            });
    }

    protected void createAndSetErrorMessage(Collection<Issue> issues) {
        setErrorMessage(null);

        var errorMessages = new ArrayList<String>();
        for (var issue : issues) {
            if (issue.getMessage()
                .contains("input '}'")) {
                continue;
            } else if (issue.getMessage()
                .contains("The required feature")
                    && issue.getMessage()
                        .contains("must be set")) {
                errorMessages.add(issue.getMessage()
                    .replaceFirst(" of '[^']+'", ""));
            } else {
                errorMessages.add(
                        issue.getMessage() + " (line " + issue.getLineNumber() + ", column " + issue.getColumn() + ")");
            }
        }

        if (!errorMessages.isEmpty()) {
            var fullMessage = errorMessages.stream()
                .collect(Collectors.joining(System.lineSeparator()));
            setErrorMessage(fullMessage);
        }
    }

    protected void setResultVariables() {
        var resultText = modelAccess.getSerializedModel();
        try {
            var dict = serializationHelper.parseDict(resultText, dictionaries);
            result = Collections.unmodifiableCollection(dict.getReusableBehaviours()
                .get(0)
                .getVariableUsages());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}

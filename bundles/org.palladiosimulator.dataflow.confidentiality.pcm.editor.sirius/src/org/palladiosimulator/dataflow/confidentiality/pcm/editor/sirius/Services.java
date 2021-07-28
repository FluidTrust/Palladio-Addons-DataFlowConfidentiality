package org.palladiosimulator.dataflow.confidentiality.pcm.editor.sirius;

import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.ui.editor.embedded.IEditedResourceProvider;
import org.palladiosimulator.dataflow.confidentiality.pcm.dddsl.ui.internal.DddslActivator;
import org.palladiosimulator.dataflow.confidentiality.pcm.editor.sirius.assignments.SerializationHelper;
import org.palladiosimulator.dataflow.confidentiality.pcm.model.confidentiality.ConfidentialityVariableCharacterisation;
import org.palladiosimulator.pcm.parameter.ParameterFactory;

import com.google.inject.Injector;

/**
 * The services class used by VSM.
 */
@SuppressWarnings("restriction")
public class Services {

    protected static final SerializationHelper SERIALIZATION_HELPER = createSerializationHelper();

    public String getConfidentialityVariableCharacterisationLabel(EObject self) throws IOException {
        if (!(self instanceof ConfidentialityVariableCharacterisation)) {
            return "invalid";
        }
        var characterisation = (ConfidentialityVariableCharacterisation) self;
        return serialize(characterisation);
    }

    protected static String serialize(ConfidentialityVariableCharacterisation characterisation) throws IOException {
        var dictionaries = QueryHelpers.findDictionariesInSemanticResources(characterisation);

        // build virtual dictionary
        var dict = SERIALIZATION_HELPER.buildSerializationModel(behaviour -> {
            var usage = ParameterFactory.eINSTANCE.createVariableUsage();
            behaviour.getVariableUsages()
                .add(usage);

            behaviour.getInputVariables()
                .clear();
            behaviour.getOutputVariables()
                .clear();
            usage.setNamedReference__VariableUsage(
                    EcoreUtil.copy(characterisation.getVariableUsage_VariableCharacterisation()
                        .getNamedReference__VariableUsage()));
            var copy = EcoreUtil.copy(characterisation);
            usage.getVariableCharacterisation_VariableUsage()
                .add(copy);
        }, "", Arrays.asList(""), dictionaries);

        // serialize dictionary
        var dictString = SERIALIZATION_HELPER.serializeDict(dict, Arrays.asList(dict));

        // extract relevant information
        var blockPattern = Pattern.compile("\\{([^}]+)\\}");
        var matcher = blockPattern.matcher(dictString);
        var blocks = matcher.results()
            .collect(Collectors.toList());
        var lastBlock = blocks.get(blocks.size() - 1);
        var lastBlockContent = lastBlock.group(1);

        // return information
        return lastBlockContent.trim();
    }

    protected static SerializationHelper createSerializationHelper() {
        var editedResourceProvider = getInjector().getInstance(IEditedResourceProvider.class);
        return new SerializationHelper(editedResourceProvider);
    }

    protected static Injector getInjector() {
        return DddslActivator.getInstance()
            .getInjector(DddslActivator.ORG_PALLADIOSIMULATOR_DATAFLOW_CONFIDENTIALITY_PCM_DDDSL_DDDSL);
    }

}

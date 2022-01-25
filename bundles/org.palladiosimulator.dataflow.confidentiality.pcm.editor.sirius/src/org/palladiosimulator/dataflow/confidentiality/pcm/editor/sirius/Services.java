package org.palladiosimulator.dataflow.confidentiality.pcm.editor.sirius;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.sirius.business.api.session.SessionManager;
import org.eclipse.xtext.ui.editor.embedded.IEditedResourceProvider;
import org.palladiosimulator.dataflow.confidentiality.pcm.dddsl.ui.internal.DddslActivator;
import org.palladiosimulator.dataflow.confidentiality.pcm.editor.sirius.assignments.SerializationHelper;
import org.palladiosimulator.dataflow.confidentiality.pcm.model.confidentiality.ConfidentialityVariableCharacterisation;
import org.palladiosimulator.dataflow.confidentiality.pcm.model.confidentiality.characteristics.CharacteristicTypeDictionary;
import org.palladiosimulator.dataflow.confidentiality.pcm.model.confidentiality.characteristics.Characteristics;
import org.palladiosimulator.dataflow.confidentiality.pcm.model.confidentiality.characteristics.CharacteristicsFactory;
import org.palladiosimulator.dataflow.confidentiality.pcm.model.confidentiality.characteristics.EnumCharacteristic;
import org.palladiosimulator.dataflow.confidentiality.pcm.model.profile.ProfileConstants;
import org.palladiosimulator.dataflow.dictionary.characterized.DataDictionaryCharacterized.EnumCharacteristicType;
import org.palladiosimulator.dataflow.dictionary.characterized.DataDictionaryCharacterized.Enumeration;
import org.palladiosimulator.dataflow.dictionary.characterized.DataDictionaryCharacterized.Literal;
import org.palladiosimulator.mdsdprofiles.api.StereotypeAPI;
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

    public Collection<EnumCharacteristic> findAppliedEnumCharacteristics(EObject self) {
        Collection<?> taggedValue = StereotypeAPI
            .getTaggedValueSafe(self, ProfileConstants.characterisable.getValue(),
                    ProfileConstants.characterisable.getStereotype())
            .filter(Collection.class::isInstance)
            .map(Collection.class::cast)
            .orElse(Collections.emptyList());
        Collection<EnumCharacteristic> enumCharacteristics = taggedValue.stream()
            .filter(EnumCharacteristic.class::isInstance)
            .map(EnumCharacteristic.class::cast)
            .collect(Collectors.toList());
        return enumCharacteristics;
    }

    public Collection<EnumCharacteristicType> findAllEnumCharacteristicTypes(EObject self) {
        return QueryHelpers.findCharacteristicTypeDictionariesInSemanticResources(self)
            .stream()
            .map(CharacteristicTypeDictionary::getCharacteristicTypes)
            .flatMap(Collection::stream)
            .filter(EnumCharacteristicType.class::isInstance)
            .map(EnumCharacteristicType.class::cast)
            .collect(Collectors.toList());
    }

    public Characteristics getCharacteristicsContainer(EObject self) throws IOException {
        var modelResourceURI = self.eResource()
            .getURI();
        var modelFileName = modelResourceURI.lastSegment();
        var containerFileName = modelFileName + ".characteristics";
        var containerResourceURI = modelResourceURI.trimSegments(1)
            .appendSegment(containerFileName);

        var session = SessionManager.INSTANCE.getSession(self);
        var containerResource = session.getSemanticResources()
            .stream()
            .filter(r -> r.getURI()
                .equals(containerResourceURI))
            .findFirst();
        if (containerResource.isEmpty()) {
            var rs = session.getTransactionalEditingDomain().getResourceSet();
            if (!rs.getURIConverter()
                .exists(containerResourceURI, Collections.emptyMap())) {
                var r = rs.createResource(containerResourceURI);
                r.getContents()
                    .add(CharacteristicsFactory.eINSTANCE.createCharacteristics());
                r.save(Collections.emptyMap());
            }

            session.addSemanticResource(containerResourceURI, new NullProgressMonitor());
            containerResource = session.getSemanticResources()
                .stream()
                .filter(r -> r.getURI()
                    .equals(containerResourceURI))
                .findFirst();
        }

        return containerResource.map(Resource::getContents)
            .map(Collection::iterator)
            .map(Iterator::next)
            .filter(Characteristics.class::isInstance)
            .map(Characteristics.class::cast)
            .orElse(null);
    }

    public String getLabelForEnumCharacteristic(EObject self) {
        if (self instanceof EnumCharacteristic) {
            var characteristic = (EnumCharacteristic) self;
            var typeName = Optional.ofNullable(characteristic.getType()).map(EnumCharacteristicType::getName).orElse("n/a");
            var values = characteristic.getValues()
                .stream()
                .map(Literal::getName)
                .collect(Collectors.joining(", "));
            return String.format("%s: %s", typeName, values);
        }
        return null;
    }

    public Collection<Literal> getLiteralsFromEnumCharacteristic(EObject self) {
        return Optional.ofNullable(self)
            .filter(EnumCharacteristic.class::isInstance)
            .map(EnumCharacteristic.class::cast)
            .map(EnumCharacteristic::getType)
            .map(EnumCharacteristicType::getType)
            .map(Enumeration::getLiterals)
            .orElse(new BasicEList<>());
    }

    protected static String serialize(ConfidentialityVariableCharacterisation characterisation) throws IOException {
        var dictionaries = QueryHelpers.findCharacteristicTypeDictionariesInSemanticResources(characterisation);

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

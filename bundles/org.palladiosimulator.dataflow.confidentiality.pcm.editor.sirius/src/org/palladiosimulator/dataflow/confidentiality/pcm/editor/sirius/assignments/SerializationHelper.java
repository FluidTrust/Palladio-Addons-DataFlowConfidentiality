package org.palladiosimulator.dataflow.confidentiality.pcm.editor.sirius.assignments;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.ui.editor.embedded.IEditedResourceProvider;
import org.eclipse.xtext.util.StringInputStream;
import org.palladiosimulator.dataflow.confidentiality.pcm.model.confidentiality.behaviour.BehaviourFactory;
import org.palladiosimulator.dataflow.confidentiality.pcm.model.confidentiality.behaviour.ReusableBehaviour;
import org.palladiosimulator.dataflow.confidentiality.pcm.model.confidentiality.characteristics.CharacteristicTypeDictionary;
import org.palladiosimulator.dataflow.confidentiality.pcm.model.confidentiality.dictionary.DictionaryFactory;
import org.palladiosimulator.dataflow.confidentiality.pcm.model.confidentiality.dictionary.PCMDataDictionary;
import org.palladiosimulator.dataflow.dictionary.characterized.DataDictionaryCharacterized.CharacteristicType;
import org.palladiosimulator.dataflow.dictionary.characterized.DataDictionaryCharacterized.EnumCharacteristicType;
import org.palladiosimulator.dataflow.dictionary.characterized.DataDictionaryCharacterized.expressions.EnumCharacteristicReference;
import org.palladiosimulator.pcm.parameter.VariableUsage;

import de.uka.ipd.sdq.identifier.Identifier;
import de.uka.ipd.sdq.stoex.StoexFactory;
import de.uka.ipd.sdq.stoex.VariableReference;

@SuppressWarnings("restriction")
public final class SerializationHelper {

    private final IEditedResourceProvider editedResourceProvider;

    public SerializationHelper(IEditedResourceProvider editedResourceProvider) {
        this.editedResourceProvider = editedResourceProvider;
    }

    public PCMDataDictionary buildSerializationModel(Consumer<ReusableBehaviour> behaviorBuilder, String lhsName,
            Collection<String> inputs, Collection<CharacteristicTypeDictionary> dictionaries) {
        var dict = DictionaryFactory.eINSTANCE.createPCMDataDictionary();

        // create characteristic types
        for (var dictionary : dictionaries) {
            var copy = EcoreUtil.copy(dictionary);
            dict.getCharacteristicEnumerations()
                .addAll(copy.getCharacteristicEnumerations());
            dict.getCharacteristicTypes()
                .addAll(copy.getCharacteristicTypes());
        }

        // create behavior
        var behaviour = BehaviourFactory.eINSTANCE.createReusableBehaviour();
        dict.getReusableBehaviours()
            .add(behaviour);
        behaviour.setEntityName("foo");
        behaviour.getOutputVariables()
            .add(createVariableReferenceFromString(lhsName));
        inputs.stream()
            .map(SerializationHelper::createVariableReferenceFromString)
            .forEach(behaviour.getInputVariables()::add);
        behaviorBuilder.accept(behaviour);

        // return dict
        return dict;
    }

    public PCMDataDictionary parseDict(String text, Collection<CharacteristicTypeDictionary> dictionaries) throws IOException {
        var parsedDict = parseDict(text);
        var parsedVariableUsages = findVariableUsages(parsedDict);
        parsedVariableUsages.forEach(usage -> replaceCharacteristicTypes(usage, dictionaries));
        return parsedDict;
    }

    public String serializeDict(PCMDataDictionary dict, Collection<CharacteristicTypeDictionary> dictionaries) throws IOException {
        var copy = EcoreUtil.copy(dict);
        var dictionariestoUse = new ArrayList<>(dictionaries);
        if (dictionariestoUse.contains(dict)) {
            dictionariestoUse.remove(dict);
            dictionariestoUse.add(copy);
        }
        var variableUsages = findVariableUsages(copy);
        variableUsages.forEach(usage -> replaceCharacteristicTypes(usage, dictionariestoUse));
        return serializeDict(copy);
    }

    protected PCMDataDictionary parseDict(String text) throws IOException {
        var r = editedResourceProvider.createResource();
        try (var is = new StringInputStream(text)) {
            r.load(is, Collections.emptyMap());
        }
        return (PCMDataDictionary) r.getContents()
            .get(0);
    }

    protected String serializeDict(PCMDataDictionary dict) throws IOException {
        var r = editedResourceProvider.createResource();
        r.getContents()
            .add(dict);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            r.save(baos, Collections.emptyMap());
            return baos.toString();
        }
    }

    protected static void replaceCharacteristicTypes(VariableUsage variableUsage,
            Collection<CharacteristicTypeDictionary> dictionaries) {

        // fix references to data dictionary
        eAllContentsStream(variableUsage).filter(EnumCharacteristicReference.class::isInstance)
            .map(EnumCharacteristicReference.class::cast)
            .forEach(reference -> {
                if (reference.getCharacteristicType() != null) {
                    var foundCharacteristicType = findCharacteristicTypeInDicts(reference.getCharacteristicType()
                        .getName(), dictionaries).filter(EnumCharacteristicType.class::isInstance)
                            .map(EnumCharacteristicType.class::cast);
                    assert (foundCharacteristicType.isPresent());
                    foundCharacteristicType.ifPresent(reference::setCharacteristicType);

                    if (foundCharacteristicType.isPresent() && reference.getLiteral() != null) {
                        var foundLiteral = foundCharacteristicType.get()
                            .getType()
                            .getLiterals()
                            .stream()
                            .filter(l -> reference.getLiteral()
                                .getName()
                                .equals(l.getName()))
                            .findFirst();
                        assert (foundLiteral.isPresent());
                        foundLiteral.ifPresent(reference::setLiteral);
                    }
                }
            });

        // assign new ids to all elements
        eAllContentsStream(variableUsage).filter(Identifier.class::isInstance)
            .map(Identifier.class::cast)
            .forEach(i -> i.setId(EcoreUtil.generateUUID()));
    }

    protected static Optional<CharacteristicType> findCharacteristicTypeInDicts(String name,
            Collection<CharacteristicTypeDictionary> dictionaries) {
        return dictionaries.stream()
            .map(dict -> findCharacteristicTypeInDict(name, dict))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst();
    }

    protected static Optional<CharacteristicType> findCharacteristicTypeInDict(String name, CharacteristicTypeDictionary dict) {
        return dict.getCharacteristicTypes()
            .stream()
            .filter(ct -> name.equals(ct.getName()))
            .findFirst();
    }

    protected static Collection<VariableUsage> findVariableUsages(PCMDataDictionary dictionary) {
        return eAllContentsStream(dictionary).filter(VariableUsage.class::isInstance)
            .map(VariableUsage.class::cast)
            .collect(Collectors.toList());
    }

    protected static Stream<EObject> eAllContentsStream(EObject object) {
        var iter = object.eAllContents();
        var spliterator = Spliterators.spliteratorUnknownSize(iter, Spliterator.ORDERED);
        return StreamSupport.stream(spliterator, false);
    }

    protected static VariableReference createVariableReferenceFromString(String name) {
        var varRef = StoexFactory.eINSTANCE.createVariableReference();
        varRef.setReferenceName(name);
        return varRef;
    }

}

package org.palladiosimulator.dataflow.confidentiality.pcm.model.confidentiality.characteristics.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;

import org.eclipse.emf.edit.provider.IItemPropertyDescriptor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.palladiosimulator.dataflow.confidentiality.pcm.model.confidentiality.characteristics.CharacteristicsFactory;
import org.palladiosimulator.dataflow.confidentiality.pcm.model.confidentiality.characteristics.CharacteristicsPackage;
import org.palladiosimulator.dataflow.dictionary.characterized.DataDictionaryCharacterized.EnumCharacteristicType;
import org.palladiosimulator.dataflow.dictionary.characterized.DataDictionaryCharacterized.Enumeration;

import tools.mdsd.library.standalone.initialization.StandaloneInitializationException;
import tools.mdsd.library.standalone.initialization.StandaloneInitializerBuilder;

public class EnumCharacteristicItemProviderTest {

    private static final CharacteristicsFactory CHARACTERISTCS_FACTORY = CharacteristicsFactory.eINSTANCE;
    private static CharacteristicsItemProviderAdapterFactory ADAPTER_FACTORY;
    private EnumCharacteristicItemProvider subject;

    @BeforeAll
    public static void init() throws StandaloneInitializationException {
        // init EMF for standalone use
        StandaloneInitializerBuilder.builder()
            .build()
            .init();
        // create adapter factory of corresponding EPackage
        ADAPTER_FACTORY = new CharacteristicsItemProviderAdapterFactory();
    }

    @BeforeEach
    public void setup() {
        // create item provider for EClass
        subject = new EnumCharacteristicItemProvider(ADAPTER_FACTORY);
    }

    @Test
    public void testChoiceOfValuesLiteralsWithoutSelectedType() {
        // create models
        var dd = EnumCharacteristicItemProviderTestModelBuilder.createDataDictionary();
        var container = EnumCharacteristicItemProviderTestModelBuilder.createCharacteristicsContainer(dd);
        var enumCharacteristic = CHARACTERISTCS_FACTORY.createEnumCharacteristic();
        container.getCharacteristics()
            .add(enumCharacteristic);

        // define expected result
        var expected = dd.getEnumerations()
            .stream()
            .map(Enumeration::getLiterals)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

        // ask subject for result
        IItemPropertyDescriptor descriptor = subject.getPropertyDescriptor(enumCharacteristic,
                CharacteristicsPackage.Literals.ENUM_CHARACTERISTIC__VALUES.getName());
        assertNotNull(descriptor);
        var actual = descriptor.getChoiceOfValues(enumCharacteristic);

        // verify result
        assertEquals(expected.size(), actual.size());
        assertTrue(expected.containsAll(actual));
    }

    @Test
    public void testChoiceOfValuesLiteralsWithSelectedType() {
        // create models
        var dd = EnumCharacteristicItemProviderTestModelBuilder.createDataDictionary();
        var container = EnumCharacteristicItemProviderTestModelBuilder.createCharacteristicsContainer(dd);
        var enumCharacteristicType = (EnumCharacteristicType) dd.getCharacteristicTypes()
            .get(0);
        var enumCharacteristic = CHARACTERISTCS_FACTORY.createEnumCharacteristic();
        enumCharacteristic.setType(enumCharacteristicType);
        container.getCharacteristics()
            .add(enumCharacteristic);

        // define expected result
        var expected = new HashSet<>();
        expected.addAll(enumCharacteristicType.getType()
            .getLiterals());

        // ask subject for result
        IItemPropertyDescriptor descriptor = subject.getPropertyDescriptor(enumCharacteristic,
                CharacteristicsPackage.Literals.ENUM_CHARACTERISTIC__VALUES.getName());
        assertNotNull(descriptor);
        var actual = descriptor.getChoiceOfValues(enumCharacteristic);

        // verify result
        assertEquals(expected.size(), actual.size());
        assertTrue(expected.containsAll(actual));
    }

}

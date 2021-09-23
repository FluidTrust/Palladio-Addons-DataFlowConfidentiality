package org.palladiosimulator.dataflow.confidentiality.pcm.model.confidentiality.characteristics.provider

import java.util.UUID
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.resource.ResourceSet
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl
import org.palladiosimulator.dataflow.confidentiality.pcm.model.confidentiality.characteristics.CharacteristicsFactory
import org.palladiosimulator.dataflow.dictionary.characterized.DataDictionaryCharacterized.DataDictionaryCharacterized
import org.palladiosimulator.dataflow.dictionary.characterized.DataDictionaryCharacterized.DataDictionaryCharacterizedFactory

class EnumCharacteristicItemProviderTestModelBuilder {
	
	static val extension DataDictionaryCharacterizedFactory DD_FACTORY = DataDictionaryCharacterizedFactory.eINSTANCE
	static val extension CharacteristicsFactory C_FACTORY = CharacteristicsFactory.eINSTANCE
	
	static def createDataDictionary() {
		val rs = new ResourceSetImpl
		val r = rs.createRandomResource()
		val dd = createDataDictionaryCharacterized => [
			val firstEnum = createEnumeration => [
				literals += createLiteral
				literals += createLiteral
			]
			val secondEnum = createEnumeration => [
				literals += createLiteral
				literals += createLiteral
			]
			enumerations += firstEnum
			enumerations += secondEnum
			characteristicTypes += createEnumCharacteristicType => [
				type = firstEnum
			]
			characteristicTypes += createEnumCharacteristicType => [
				type = secondEnum
			]
		]
		r.contents += dd
		return dd
	}
	
	static def createCharacteristicsContainer(DataDictionaryCharacterized dd) {
		val rs = dd.eResource.resourceSet
		val r = rs.createRandomResource()
		val container = createCharacteristics
		r.contents += container
		return container
	}
	
	protected static def createRandomResource(ResourceSet rs) {
		return rs.createResource(URI.createURI('''virtual://«UUID.randomUUID.toString».xmi'''));
	}
	
}
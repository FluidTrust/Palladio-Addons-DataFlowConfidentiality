package org.palladiosimulator.dataflow.confidentiality.pcm.transformation.pcm2dfd.impl.devided

import org.palladiosimulator.dataflow.confidentiality.pcm.model.confidentiality.characteristics.DataTypeCharacteristicType
import org.palladiosimulator.dataflow.confidentiality.pcm.transformation.pcm2dfd.impl.DFDFactoryUtilities
import org.palladiosimulator.dataflow.dictionary.characterized.DataDictionaryCharacterized.CharacteristicType
import org.palladiosimulator.dataflow.dictionary.characterized.DataDictionaryCharacterized.DataDictionaryCharacterized
import org.palladiosimulator.dataflow.dictionary.characterized.DataDictionaryCharacterized.EnumCharacteristicType
import org.palladiosimulator.dataflow.dictionary.characterized.DataDictionaryCharacterized.Enumeration
import org.palladiosimulator.dataflow.dictionary.characterized.DataDictionaryCharacterized.Literal
import org.palladiosimulator.pcm.repository.CollectionDataType
import org.palladiosimulator.pcm.repository.CompositeDataType
import org.palladiosimulator.pcm.repository.DataType
import org.palladiosimulator.pcm.repository.PrimitiveDataType
import org.palladiosimulator.pcm.repository.PrimitiveTypeEnum
import org.palladiosimulator.dataflow.confidentiality.pcm.transformation.pcm2dfd.trace.impl.TransformationTraceModifier

class DDEntityCreator {
	
	val extension IdGenerationHelper idGenerationHelper = new IdGenerationHelper
	val extension DFDFactoryUtilities dfdFactoryUtils = new DFDFactoryUtilities
	val extension TransformationTraceModifier traceModifier
	val DataDictionaryCharacterized dd
	
	
	new(DataDictionaryCharacterized dd, TransformationTraceModifier traceModifier) {
		this.dd = dd
		this.traceModifier = traceModifier
	}
	
	def getCharacteristicType(CharacteristicType ct) {
		return ct.getCharacteristicTypeInternal()
	}
	
	protected def dispatch create newCt : createEnumCharacteristicType getCharacteristicTypeInternal(EnumCharacteristicType ct) {
		newCt.name = ct.name
		newCt.type = ct.type.getEnumeration
		newCt.calculatedId = #["ct", ct]
		dd.characteristicTypes += newCt
		addTraceEntry(ct, newCt)
	}
	
	protected def dispatch create newCt : createEnumCharacteristicType getCharacteristicTypeInternal(DataTypeCharacteristicType ct) {
		newCt.name = ct.name
		newCt.type = getDataTypeEnumeration()
		newCt.calculatedId = #["ct", ct]
		dd.characteristicTypes += newCt
		addTraceEntry(ct, newCt)
	}
	
	protected def create newEnum: createEnumeration getDataTypeEnumeration() {
		newEnum.name = "DataTypes"
		newEnum.calculatedId = #["dataTypeEnum"]
		dd.enumerations += newEnum
	}
	
	def getLiteral(DataType dataType) {
		if (dataType instanceof PrimitiveDataType) {
			dataType.type.getLiteralInternal
		} else {
			dataType.literalInternal
		}
	}
	
	protected def create newLiteral: createLiteral getLiteralInternal(PrimitiveTypeEnum primitiveType) {
		newLiteral.name = primitiveType.getName
		newLiteral.calculatedId = #["literal", primitiveType]
		dataTypeEnumeration.literals += newLiteral
		//FIXME trace recorder is only capable of recording Identifier instances
	}
	
	protected def create newLiteral: createLiteral getLiteralInternal(DataType dataType) {
		newLiteral.name = dataType.name
		newLiteral.calculatedId = #["literal", dataType]
		dataTypeEnumeration.literals += newLiteral
		addTraceEntry(dataType, newLiteral)
	}
	
	protected def create newEnum: createEnumeration getEnumeration(Enumeration enumeration) {
		newEnum.name = enumeration.name
		newEnum.literals += enumeration.literals.map[getLiteral]
		newEnum.calculatedId = #["enum", enumeration]
		dd.enumerations += newEnum
		addTraceEntry(enumeration, newEnum)
	}
	
	def create newLiteral: createLiteral getLiteral(Literal literal) {
		newLiteral.name = literal.name
		newLiteral.calculatedId = #["literal", literal]
		addTraceEntry(literal, newLiteral)
	}
	
	protected def dispatch getName(PrimitiveDataType dataType) {
		dataType.type.getName
	}
	
	protected def dispatch getName(CompositeDataType dataType) {
		dataType.entityName
	}
	
	protected def dispatch getName(CollectionDataType dataType) {
		dataType.entityName
	}
	
}
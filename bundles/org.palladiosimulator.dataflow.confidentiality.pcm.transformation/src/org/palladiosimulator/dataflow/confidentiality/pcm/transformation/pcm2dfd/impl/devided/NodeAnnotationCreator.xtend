package org.palladiosimulator.dataflow.confidentiality.pcm.transformation.pcm2dfd.impl.devided

import java.util.HashMap
import java.util.Map
import java.util.Random
import java.util.Set
import java.util.UUID
import org.palladiosimulator.dataflow.confidentiality.pcm.transformation.pcm2dfd.PcmAnnotations
import org.palladiosimulator.dataflow.confidentiality.pcm.transformation.pcm2dfd.PcmAnnotations.PCMActionType
import org.palladiosimulator.dataflow.confidentiality.pcm.transformation.pcm2dfd.PcmAnnotations.PCMContainingType
import org.palladiosimulator.dataflow.confidentiality.pcm.transformation.pcm2dfd.impl.DFDFactoryUtilities
import org.palladiosimulator.dataflow.confidentiality.pcm.transformation.pcm2dfd.trace.impl.TransformationTraceModifier
import org.palladiosimulator.dataflow.diagram.characterized.DataFlowDiagramCharacterized.CharacterizedNode
import org.palladiosimulator.dataflow.dictionary.characterized.DataDictionaryCharacterized.EnumCharacteristicType
import org.palladiosimulator.dataflow.dictionary.characterized.DataDictionaryCharacterized.Literal
import org.palladiosimulator.dataflow.confidentiality.pcm.transformation.pcm2dfd.trace.impl.PCM2DFDTransformationTraceImpl

class NodeAnnotationCreator {

	static val extension DFDFactoryUtilities DFD_FACTORY_UTILS = new DFDFactoryUtilities
	static val Random RANDOM = new Random(42) // predictable random numbers for generation of IDs
	static val Map<Object, Literal> ENUM_VALUE_TO_LITERAL = new HashMap
	static val Set<EnumCharacteristicType> ENUM_CHARACTERISTIC_TYPES = createCharacteristicTypes()
	val DDEntityCreator ddCreator
	val TransformationTraceModifier traceRecorder
	
	new(DDEntityCreator ddCreator, PCM2DFDTransformationTraceImpl traceModifier) {
		this.ddCreator = ddCreator
		traceRecorder = traceModifier
	}

	def createAnnotation(CharacterizedNode node, PCMContainingType annotation) {
		node.createAnnotation(annotation as Object)
	}

	def createAnnotation(CharacterizedNode node, PCMActionType annotation) {
		node.createAnnotation(annotation as Object)
	}
	
	protected def getInternalLiteral(Object annotation) {
		val internalLiteral = NodeAnnotationCreator.ENUM_VALUE_TO_LITERAL.get(annotation)
		traceRecorder.addAnnotationEntry(annotation, internalLiteral)
		internalLiteral
	}
	
	protected def getInternalCharacteristicType(Object annotation) {
		val internalLiteral = annotation.internalLiteral
		val internalCt = NodeAnnotationCreator.ENUM_CHARACTERISTIC_TYPES.findFirst[type | type.type.literals.contains(internalLiteral)]
		traceRecorder.addAnnotationEntry(annotation, internalCt)
		internalCt
	}

	protected def getExternalLiteral(Object annotation) {
		val internalLiteral = annotation.internalLiteral
		ddCreator.getLiteral(internalLiteral)
	}

	protected def getExternalCharacteristicType(Object annotation) {
		val internalCt = annotation.internalCharacteristicType
		ddCreator.getCharacteristicType(internalCt)
	}

	protected def createAnnotation(CharacterizedNode node, Object annotation) {
		val ctLiteral = annotation.externalLiteral
		val ct = annotation.externalCharacteristicType
		node.ownedCharacteristics += createCharacteristic(ct, #[ctLiteral])
	}

	private static def createCharacteristicTypes() {
		val literalTrace = new HashMap
		val characteristicTypes = PcmAnnotations.declaredClasses.filter[isEnum].map [
			createEnumCharacteristicType(literalTrace)
		].toSet
		NodeAnnotationCreator.ENUM_VALUE_TO_LITERAL.putAll(literalTrace)
		characteristicTypes
	}

	private static def EnumCharacteristicType createEnumCharacteristicType(Class<?> enumeration,
		Map<Object, Literal> literalTrace) {
		val ct = createEnumCharacteristicType
		ct.id = generateUUID
		ct.name = enumeration.simpleName
		ct.type = createEnum(enumeration.simpleName)
		val ctEnum = ct.type
		for (literal : enumeration.enumConstants) {
			val ctLiteral = createLiteral
			ctLiteral.id = generateUUID
			literalTrace.put(literal, ctLiteral)
			ctLiteral.name = literal.toString
			ctEnum.literals += ctLiteral
		}
		ct
	}

	private static def generateUUID() {
		val byte[] bytes = newByteArrayOfSize(16)
		RANDOM.nextBytes(bytes)
		UUID.nameUUIDFromBytes(bytes).toString
	}

}

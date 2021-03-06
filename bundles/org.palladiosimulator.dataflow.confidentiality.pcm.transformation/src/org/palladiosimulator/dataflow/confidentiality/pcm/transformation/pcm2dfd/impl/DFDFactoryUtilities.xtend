package org.palladiosimulator.dataflow.confidentiality.pcm.transformation.pcm2dfd.impl

import org.palladiosimulator.dataflow.diagram.DataFlowDiagram.DataFlowDiagramFactory
import org.palladiosimulator.dataflow.diagram.DataFlowDiagram.ExternalActor
import org.palladiosimulator.dataflow.diagram.DataFlowDiagram.Node
import org.palladiosimulator.dataflow.diagram.characterized.DataFlowDiagramCharacterized.Behaving
import org.palladiosimulator.dataflow.diagram.characterized.DataFlowDiagramCharacterized.DataFlowDiagramCharacterizedFactory
import org.palladiosimulator.dataflow.dictionary.DataDictionary.DataDictionaryFactory
import org.palladiosimulator.dataflow.dictionary.DataDictionary.DataType
import org.palladiosimulator.dataflow.dictionary.characterized.DataDictionaryCharacterized.CharacteristicType
import org.palladiosimulator.dataflow.dictionary.characterized.DataDictionaryCharacterized.DataDictionaryCharacterizedFactory
import org.palladiosimulator.dataflow.dictionary.characterized.DataDictionaryCharacterized.Literal
import org.palladiosimulator.dataflow.dictionary.characterized.DataDictionaryCharacterized.Pin
import org.palladiosimulator.dataflow.dictionary.characterized.DataDictionaryCharacterized.expressions.DataCharacteristicReference
import org.palladiosimulator.dataflow.dictionary.characterized.DataDictionaryCharacterized.expressions.ExpressionsFactory
import org.palladiosimulator.dataflow.dictionary.characterized.DataDictionaryCharacterized.expressions.Term

class DFDFactoryUtilities {
	
	extension DataFlowDiagramCharacterizedFactory dfdCharacterizedFactory = DataFlowDiagramCharacterizedFactory.eINSTANCE
	extension DataFlowDiagramFactory dfdFactory = DataFlowDiagramFactory.eINSTANCE
	extension DataDictionaryCharacterizedFactory ddCharacterizedFactory = DataDictionaryCharacterizedFactory.eINSTANCE
	extension DataDictionaryFactory ddFactory = DataDictionaryFactory.eINSTANCE
	extension ExpressionsFactory ddExpressionsFactory = ExpressionsFactory.eINSTANCE

	def createDataFlowDiagram() {
		DataFlowDiagramFactory.eINSTANCE.createDataFlowDiagram
	}
	
	def createDataDictionary() {
		createDataDictionaryCharacterized
	}
	
	def createActor() {
		createCharacterizedExternalActor
	}
	
	def createBehavior() {
		ddCharacterizedFactory.createBehaviorDefinition
	}
	
	def <T extends Behaving> T createBehavior(T behaving) {
		behaving.ownedBehavior = createBehavior
		behaving
	}
	
	def createPin() {
		ddCharacterizedFactory.createPin
	}
	
	def createStore() {
		createCharacterizedStore
	}
	
	def createActorProcess(ExternalActor actor, String entityName) {
		val actorProcess = createCharacterizedActorProcess
		actorProcess.name = entityName
		actorProcess.actor = actor
		actorProcess
	}
	
	def createActorProcess() {
		createCharacterizedActorProcess
	}
	
	def createProcess() {
		createCharacterizedProcess
	}
	
	def createAssignment(Behaving behaving, DataCharacteristicReference lhs, Term rhs) {
		val assignment = ddCharacterizedFactory.createAssignment
		assignment.lhs = lhs
		assignment.rhs = rhs
		behaving.behavior.assignments += assignment
		behaving
	}
	
	def createEnumCharacteristicType() {
		ddCharacterizedFactory.createEnumCharacteristicType
	}
	
	def createEnumeration() {
		ddCharacterizedFactory.createEnumeration
	}
	
	def createLiteral() {
		ddCharacterizedFactory.createLiteral
	}
	
	def createDataCharacteristicReference(Pin pin, CharacteristicType characteristicType, Literal literal) {
		val reference = createDataCharacteristicReference
		reference.pin = pin
		reference.characteristicType = characteristicType
		reference.literal = literal
		reference
	}
	
	def createContainerCharacteristicReference(CharacteristicType characteristicType, Literal literal) {
		val reference = createContainerCharacteristicReference
		reference.characteristicType = characteristicType
		reference.literal = literal
		reference
	}
	
	def createDataFlow() {
		createCharacterizedDataFlow
	}
	
	def createDataFlow(Node source, Pin sourcePin, Node destination, Pin destinationPin, DataType dataType) {
		val flow = createCharacterizedDataFlow
		//TODO calculate and set name
		flow.name = "data"
		flow.source = source
		flow.sourcePin = sourcePin
		flow.target = destination
		flow.targetPin = destinationPin
		flow.data += createData(dataType)
		flow
	}
	
	def createData(DataType dataType) {
		val data = createData
		//TODO calculate and set name
		data.name = "data"
		data.type = dataType
		data
	}
	
	def createCopyAssignment(Behaving behaving, Pin toPin, Pin fromPin) {
		val lhs = toPin.createDataCharacteristicReference(null, null)
		val rhs = fromPin.createDataCharacteristicReference(null, null)
		behaving.createAssignment(lhs, rhs)
	}
	
	def createCharacteristic(CharacteristicType characteristicType, Iterable<Literal> characteristicValue) {
		val characteristic = createEnumCharacteristic
		characteristic.name = characteristicType.name
		characteristic.type = characteristicType
		characteristic.values += characteristicValue
		characteristic
	}
	
	def createPrimitiveDT() {
		createPrimitiveDataType
	}
	
	def createCollectionDT() {
		createCollectionDataType
	}
	
	def createCompositeDT() {
		createCompositeDataType
	}
	
	def createEntry(String name, DataType type) {
		val entry = createEntry
		entry.name = name
		entry.type = type
		entry
	}
	
	def createEnum(String name) {
		val enumeration = createEnumeration
		enumeration.name = name
		enumeration
	}
	
	def createEnumLiteral() {
		val literal = createLiteral
		literal
	}
	
	def createCharacteristicType(String name) {
		val characteristicType = createEnumCharacteristicType
		characteristicType.name = name
		characteristicType
	}
}
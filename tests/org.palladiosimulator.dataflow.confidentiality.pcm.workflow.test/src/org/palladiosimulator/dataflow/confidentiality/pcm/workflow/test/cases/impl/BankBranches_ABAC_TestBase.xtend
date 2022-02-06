package org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.cases.impl

import java.util.function.Consumer
import org.eclipse.emf.ecore.util.EcoreUtil
import org.palladiosimulator.dataflow.confidentiality.pcm.model.confidentiality.characteristics.CharacteristicTypeDictionary
import org.palladiosimulator.dataflow.confidentiality.pcm.workflow.TransformPCMDFDToPrologWorkflowFactory
import org.palladiosimulator.dataflow.confidentiality.pcm.workflow.jobs.TransformPCMDFDToPrologJobBuilder
import org.palladiosimulator.pcm.allocation.Allocation
import org.palladiosimulator.pcm.usagemodel.UsageModel

import static org.junit.jupiter.api.Assertions.*
import static org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.StandaloneUtil.getModelURI

class BankBranches_ABAC_TestBase extends TestBase {
	
	val String folderName

	new(String folderName) {
		this.folderName = folderName;
	}

	protected def runTest(int expectedNumberOfSolutions) {
		runTest(expectedNumberOfSolutions, [])
	}

	protected def runTest(int expectedNumberOfSolutions, Consumer<UsageModel> usageModelModifier) {
		val solution = deriveSolution(usageModelModifier)
		assertNumberOfSolutions(solution, expectedNumberOfSolutions, #["A", "PIN", "S"])
	}

	protected def deriveSolution(Consumer<UsageModel> usageModelModifier) {
		val usageModelURI = getModelURI(folderName + "/newUsageModel.usagemodel")
		val usageModel = rs.getResource(usageModelURI, true).contents.get(0) as UsageModel
		val allocationModelURI = getModelURI(folderName + "/newAllocation.allocation")
		val allocationModel = rs.getResource(allocationModelURI, true).contents.get(0) as Allocation
		EcoreUtil.resolveAll(rs)

		usageModelModifier.accept(usageModel)

		val job = new TransformPCMDFDToPrologJobBuilder().addSerializeModelToString.addUsageModels(usageModel).
			addAllocationModel(allocationModel).build
		val workflow = TransformPCMDFDToPrologWorkflowFactory.createWorkflow(job)
		workflow.run

		val resultingProgram = workflow.prologProgram
		assertTrue(resultingProgram.isPresent)


		val traceWrapper = workflow.trace
		assertTrue(traceWrapper.isPresent)
		val trace = traceWrapper.get
		
		val characteristicTypeDict = rs.resources.findFirst[r | r.URI.lastSegment == "CharacteristicTypes.characteristics"].contents.get(0) as CharacteristicTypeDictionary
		val rolesEnum = characteristicTypeDict.characteristicEnumerations.findFirst[name == "Roles"]
		val statusEnum = characteristicTypeDict.characteristicEnumerations.findFirst[name == "Status"]

		val ctLocation = trace.getFactId([ct|ct.name == "Location"]).findFirst[true]
		val ctOrigin = trace.getFactId([ct|ct.name == "Origin"]).findFirst[true]
		val ctRole = trace.getFactId([ct|ct.name == "Role"]).findFirst[true]
		val ctStatus = trace.getFactId([ct|ct.name == "Status"]).findFirst[true]
		val cClerkRole = trace.getLiteralFactIds(rolesEnum.literals.findFirst[name == "Clerk"]).findFirst[true]
		val cManagerRole = trace.getLiteralFactIds(rolesEnum.literals.findFirst[name == "Manager"]).findFirst[true]
		val cRegularStatus = trace.getLiteralFactIds(statusEnum.literals.findFirst[name == "Regular"]).findFirst[true]

		prover.addTheory(resultingProgram.get)
		prover.loadTheory('''
			matchSubject('Clerk', N) :-
			  nodeCharacteristic(N, '«ctRole»', '«cClerkRole»').
			
			matchSubject('Manager', N) :-
			  nodeCharacteristic(N, '«ctRole»', '«cManagerRole»').
			
			matchObject('Regular', N, PIN, S) :-
			  exactCharacteristicValues(N, PIN, '«ctStatus»', ['«cRegularStatus»'], S).
			
			matchObject('all', _, _, _).
			
			read(N, PIN, S) :-
			  matchSubject('Manager', N),
			  matchObject('all', N, PIN, S).
			
			read(N, PIN, S) :-
			  matchSubject('Clerk', N),
			  matchObject('Regular', N, PIN, S),
			  nodeCharacteristic(N, '«ctLocation»', L),
			  exactCharacteristicValues(N, PIN, '«ctOrigin»', [L], S).
		''')
		var queryString = '''
			(actor(A);actorProcess(A,_)),
			inputPin(A, PIN),
			flowTree(A, PIN, S),
			\+ read(A, PIN, S).
		'''
		var query = prover.query(queryString)
		var solution = query.solve()
		solution
	}
	
}
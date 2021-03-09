package org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.cases.impl

import java.util.function.Consumer
import org.eclipse.emf.ecore.util.EcoreUtil
import org.palladiosimulator.dataflow.confidentiality.pcm.workflow.TransformPCMDFDToPrologWorkflowFactory
import org.palladiosimulator.dataflow.confidentiality.pcm.workflow.jobs.TransformPCMDFDToPrologJobBuilder
import org.palladiosimulator.pcm.allocation.Allocation
import org.palladiosimulator.pcm.usagemodel.UsageModel

import static org.junit.jupiter.api.Assertions.*
import static org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.StandaloneUtil.getModelURI

class ImageSharing_DAC_TestBase extends TestBase {

	val String folderName

	new(String folderName) {
		this.folderName = folderName;
	}

	protected def runTest(int expectedNumberOfSolutions) {
		runTest(expectedNumberOfSolutions, [])
	}

	protected def runTest(int expectedNumberOfSolutions, Consumer<UsageModel> usageModelModifier) {
		val solution = deriveSolution(usageModelModifier)
		assertNumberOfSolutions(solution, expectedNumberOfSolutions, #["A", "STORE", "S"])
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

		val ctIdentity = trace.getFactId([ct|ct.name == "Identity"]).findFirst[true]
		val ctReadPermission = trace.getFactId([ct|ct.name == "ReadPermission"]).findFirst[true]
		val ctWritePermission = trace.getFactId([ct|ct.name == "WritePermission"]).findFirst[true]

		val queryRules = '''
			readViolation(A, STORE, S) :-
				CT_IDENTITY = '«ctIdentity»',
				CT_READ = '«ctReadPermission»',
				store(STORE),
				(actor(A);actorProcess(A,_)),
				nodeCharacteristic(A, CT_IDENTITY, C_IDENTITY),
				\+ nodeCharacteristic(STORE, CT_READ, C_IDENTITY),
				inputPin(A, PIN),
				flowTree(A, PIN, S),
				traversedNode(S, STORE).
				
			writeViolation(A, STORE, S) :-
				CT_IDENTITY = '«ctIdentity»',
				CT_WRITE = '«ctWritePermission»',
				store(STORE),
				(actor(A);actorProcess(A,_)),
				inputPin(STORE, PIN),
				nodeCharacteristic(A, CT_IDENTITY, C_IDENTITY),
				\+ nodeCharacteristic(STORE, CT_WRITE, C_IDENTITY),
				flowTree(STORE, PIN, S),
				traversedNode(S, A).
		'''
		
		var prologProgram = resultingProgram.get + System.lineSeparator + queryRules
		prover.addTheory(prologProgram)
		prover.query('''readViolation(A, STORE, S); writeViolation(A, STORE, S).''').solve
	}
}

package org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.cases.impl

import java.util.function.Consumer
import org.eclipse.emf.ecore.util.EcoreUtil
import org.palladiosimulator.dataflow.confidentiality.pcm.workflow.TransformPCMDFDToPrologWorkflowFactory
import org.palladiosimulator.dataflow.confidentiality.pcm.workflow.jobs.TransformPCMDFDToPrologJobBuilder
import org.palladiosimulator.pcm.allocation.Allocation
import org.palladiosimulator.pcm.usagemodel.UsageModel

import static org.junit.jupiter.api.Assertions.*
import static org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.StandaloneUtil.getModelURI

class HealthRecord_MAC_NTK_TestBase extends TestBase {
	
	val String folderName

	new(String folderName) {
		this.folderName = folderName
	}

	protected def runTest(int expectedNumberOfSolutions) {
		runTest(expectedNumberOfSolutions, [])
	}

	protected def runTest(int expectedNumberOfSolutions, Consumer<UsageModel> usageModelModifier) {
		val solution = deriveSolution(usageModelModifier)
		assertNumberOfSolutions(solution, expectedNumberOfSolutions, #["N", "PIN", "COMP", "NTK", "S"])
	}

	def deriveSolution(Consumer<UsageModel> usageModelModifier) {
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
		val ctCompartments = trace.getFactId([ct|ct.name == "Compartments"]).findFirst[true]
		val ctNeedsToKnow = trace.getFactId([ct|ct.name == "NeedsToKnow"]).findFirst[true]

		prover.addTheory(resultingProgram.get)

		val query = prover.query('''
			(actor(N);actorProcess(N, _)),
			inputPin(N, PIN),
			flowTree(N, PIN, S),
			findall(X, characteristic(N,PIN,?CTCOMPARTMENT,X,S), L_COMP),
			findall(X, nodeCharacteristic(N,?CTNTK,X), L_NTK),
			sort(L_COMP, COMP), sort(L_NTK, NTK), \+ subset(COMP, NTK).
		''')
		query.bind("CTCOMPARTMENT", ctCompartments)
		query.bind("CTNTK", ctNeedsToKnow)
		query.solve()
	}
}

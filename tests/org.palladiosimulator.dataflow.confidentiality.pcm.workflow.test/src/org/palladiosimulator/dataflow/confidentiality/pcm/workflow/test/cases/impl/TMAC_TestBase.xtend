package org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.cases.impl

import java.util.function.Consumer
import org.eclipse.emf.ecore.util.EcoreUtil
import org.palladiosimulator.dataflow.confidentiality.pcm.workflow.TransformPCMDFDToPrologWorkflowFactory
import org.palladiosimulator.dataflow.confidentiality.pcm.workflow.jobs.TransformPCMDFDToPrologJobBuilder
import org.palladiosimulator.pcm.allocation.Allocation
import org.palladiosimulator.pcm.usagemodel.UsageModel

import static org.junit.jupiter.api.Assertions.*
import static org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.StandaloneUtil.getModelURI

class TMAC_TestBase extends TestBase {
	
	val String folderName
	
	new(String folderName) {
		this.folderName = folderName;
	}
	
	protected def runTest(int expectedNumberOfSolutions) {
		runTest(expectedNumberOfSolutions, [])
	}
	
	protected def runTest(int expectedNumberOfSolutions, Consumer<UsageModel> usageModelModifier) {
		val solution = deriveSolution(usageModelModifier)
		assertNumberOfSolutions(solution, expectedNumberOfSolutions, #["P", "PIN", "ROLES", "REQ", "C", "V", "S"])
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
		val ctRights = trace.getFactId([ct | ct.name == "AllowedRoles"]).findFirst[true]
		val ctRoles = trace.getFactId([ct | ct.name == "OwnedRoles"]).findFirst[true]
		val ctValidation = trace.getFactId([ct | ct.name == "ValidationStatus"]).findFirst[true]
		val ctCriticality = trace.getFactId([ct | ct.name == "Criticality"]).findFirst[true]

		prover.addTheory(resultingProgram.get)
		
		val query = prover.query('''
			inputPin(P, PIN), flowTree(P, PIN, S),
			((
				findall(R, nodeCharacteristic(P, ?CTROLES, R), L_ROLES),
				findall(R, characteristic(P, PIN, ?CTRIGHTS, R, S), L_REQ),
				sort(L_ROLES, ROLES), sort(L_REQ, REQ),
				intersection(REQ, ROLES, [])
			) ; (
				CT_CRITICALITY=?CTCRIT,
				CT_VALIDATION=?CTVAL,
				nodeCharacteristic(P, CT_CRITICALITY, C),
				characteristicTypeValue(CT_CRITICALITY, C, NC),
				characteristic(P, PIN, CT_VALIDATION, V, S),
				characteristicTypeValue(CT_VALIDATION, V, NV),
				NV > NC
			)).
		''')
		query.bind("CTROLES", ctRoles)
		query.bind("CTRIGHTS", ctRights)
		query.bind("CTCRIT", ctCriticality)
		query.bind("CTVAL", ctValidation)
		query.solve()
	}
}
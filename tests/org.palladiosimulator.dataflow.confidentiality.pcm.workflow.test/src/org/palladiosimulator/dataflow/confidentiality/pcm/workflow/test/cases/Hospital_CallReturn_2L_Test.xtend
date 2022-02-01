package org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.cases

import de.uka.ipd.sdq.stoex.StoexFactory
import org.eclipse.emf.ecore.util.EcoreUtil
import org.junit.jupiter.api.Test
import org.palladiosimulator.dataflow.confidentiality.pcm.model.confidentiality.ConfidentialityVariableCharacterisation
import org.palladiosimulator.dataflow.confidentiality.pcm.model.confidentiality.expression.NamedEnumCharacteristicReference
import org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.cases.impl.InformationFlowHierarchicalLatices_TestBase
import org.palladiosimulator.pcm.repository.Repository
import org.palladiosimulator.pcm.seff.SetVariableAction

class Hospital_CallReturn_2L_Test extends InformationFlowHierarchicalLatices_TestBase {

	new() {
		super("Hospital_CallReturn_2L")
	}

	@Test
	def void testNoIssueFound() {
		runTest(0)
	}
	
	@Test
	def void testMissingEncryption() {
		runTest(1, [um |
			val rs = um.eResource.resourceSet
			EcoreUtil.resolveAll(rs)
			val repository = rs.resources.findFirst[URI.fileExtension == "repository"].contents.get(0) as Repository
			val actionToExclude = repository.eAllContents.filter(SetVariableAction).findFirst[entityName == "PatientsDBLogic.get.encrypt"]
			val actionSuccessor = actionToExclude.successor_AbstractAction as SetVariableAction
			actionToExclude.predecessor_AbstractAction.successor_AbstractAction = actionSuccessor
			EcoreUtil.delete(actionToExclude)
			val characterization = actionSuccessor.localVariableUsages_SetVariableAction.get(0).variableCharacterisation_VariableUsage.get(0) as ConfidentialityVariableCharacterisation
			val reference = characterization.rhs as NamedEnumCharacteristicReference
			reference.namedReference = StoexFactory.eINSTANCE.createVariableReference => [
				referenceName = "patientList"
			]
		])
	}
}

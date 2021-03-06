package org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.cases

import org.eclipse.emf.ecore.util.EcoreUtil
import org.junit.jupiter.api.Test
import org.palladiosimulator.pcm.repository.Repository
import org.palladiosimulator.pcm.seff.SetVariableAction
import org.palladiosimulator.dataflow.confidentiality.pcm.model.confidentiality.expression.NamedEnumCharacteristicReference
import org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.cases.impl.PrivateTaxi_LG_TestBase

class PrivateTaxi_CallReturn_LG_Test extends PrivateTaxi_LG_TestBase {
	
	new() {
		super("PrivateTaxi_CallReturn_LG")
	}

	@Test
	def void testNoIssueFound() {
		runTest(0)
	}
	
	@Test
	def void testIssue() {
		runTest(10, [um |
			val repository = um.eResource.resourceSet.resources.findFirst[r|r.URI.lastSegment == "newRepository.repository"].contents.get(0) as Repository
			val encryptAction = repository.eAllContents.filter(SetVariableAction).findFirst[entityName == "PrivateDriverLogic.submitRoute.encryptRoute"]
			val callAction = encryptAction.successor_AbstractAction
			callAction.predecessor_AbstractAction = encryptAction.predecessor_AbstractAction
			EcoreUtil.remove(encryptAction)
			callAction.eAllContents.filter(NamedEnumCharacteristicReference).findFirst[true].namedReference.referenceName = "route"
		])
	}

}
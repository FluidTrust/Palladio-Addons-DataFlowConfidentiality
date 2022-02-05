package org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.cases

import org.junit.jupiter.api.Test
import org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.cases.impl.RBAC_TestBase
import org.palladiosimulator.pcm.seff.ExternalCallAction
import org.palladiosimulator.pcm.usagemodel.EntryLevelSystemCall

class ContactSMS_CallReturn_RBAC_Test extends RBAC_TestBase {

	new() {
		super("ContactSMS_CallReturn_RBAC")
	}

	@Test
	def void testNoIssueFound() {
		runTest(0)
	}

	@Test
	def void testIssueFound() {
		runTest(1, [ um |
			val repository = um.usageScenario_UsageModel.map[scenarioBehaviour_UsageScenario].flatMap [
				actions_ScenarioBehaviour
			].filter(EntryLevelSystemCall).findFirst[true].operationSignature__EntryLevelSystemCall.
				interface__OperationSignature.repository__Interface
			val eca = repository.eAllContents.filter(ExternalCallAction).findFirst [
				entityName == "SMSApp.sendSMS.callGateway"
			]
			
			val variableCharacterizations = eca.inputVariableUsages__CallAction.findFirst[vu|vu.namedReference__VariableUsage.string == "number"].variableCharacterisation_VariableUsage
			variableCharacterizations.remove(1)
		])
	}
}

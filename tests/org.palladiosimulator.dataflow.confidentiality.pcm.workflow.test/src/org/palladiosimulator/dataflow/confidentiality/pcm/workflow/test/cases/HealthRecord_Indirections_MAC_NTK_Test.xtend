package org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.cases

import org.junit.jupiter.api.Test
import org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.cases.impl.HealthRecord_MAC_NTK_TestBase
import org.palladiosimulator.pcm.usagemodel.EntryLevelSystemCall

class HealthRecord_Indirections_MAC_NTK_Test extends HealthRecord_MAC_NTK_TestBase {

	new() {
		super("HealthRecord_Indirections_MAC_NTK")
	}

	@Test
	def void testNoIssueFound() {
		runTest(0)
	}

	@Test
	def void testMissingDeclassification() {
		runTest(8, [ um |
			val scenarioBehavior = um.usageScenario_UsageModel.findFirst[entityName == "Physician"].scenarioBehaviour_UsageScenario
			val elsc = scenarioBehavior.actions_ScenarioBehaviour.findFirst[entityName == "Physician.addTreatments"] as EntryLevelSystemCall
			val variableCharacterizations = elsc.inputParameterUsages_EntryLevelSystemCall.get(0).variableCharacterisation_VariableUsage
			variableCharacterizations.remove(1)
		])
	}
}

package org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.cases

import org.eclipse.emf.ecore.util.EcoreUtil
import org.junit.jupiter.api.Test
import org.palladiosimulator.dataflow.confidentiality.pcm.model.confidentiality.behaviour.Behaviours
import org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.cases.impl.InformationFlowHierarchicalLatices_TestBase

class ContactSMS_Indirections_HL_Test extends InformationFlowHierarchicalLatices_TestBase {

	new() {
		super("ContactSMS_Indirections_HL")
	}

	@Test
	def void testNoIssueFound() {
		runTest(0)
	}

	@Test
	def void testIssueFound() {
		runTest(1, [ um |

			val rs = um.eResource.resourceSet
			EcoreUtil.resolveAll(rs)
			val dcBehaviour = rs.resources.flatMap[contents].filter(Behaviours).flatMap[dataChannelBehaviour].findFirst [
				entityName == "SMSApp.extractNumber"
			]
			val variableCharacterizations = dcBehaviour.variableUsages.get(0).variableCharacterisation_VariableUsage
			variableCharacterizations.remove(2)
			variableCharacterizations.remove(1)
		])
	}
}

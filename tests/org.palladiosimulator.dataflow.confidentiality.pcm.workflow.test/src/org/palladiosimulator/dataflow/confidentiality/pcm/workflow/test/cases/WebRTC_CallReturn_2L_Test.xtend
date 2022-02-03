package org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.cases

import de.uka.ipd.sdq.stoex.StoexFactory
import org.eclipse.emf.ecore.util.EcoreUtil
import org.junit.jupiter.api.Test
import org.palladiosimulator.dataflow.confidentiality.pcm.model.confidentiality.ConfidentialityFactory
import org.palladiosimulator.dataflow.confidentiality.pcm.model.confidentiality.expression.ExpressionFactory
import org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.cases.impl.InformationFlowHierarchicalLatices_TestBase
import org.palladiosimulator.pcm.repository.Repository
import org.palladiosimulator.pcm.seff.SetVariableAction

class WebRTC_CallReturn_2L_Test extends InformationFlowHierarchicalLatices_TestBase {

	new() {
		super("WebRTC_CallReturn_2L")
	}

	@Test
	def void testNoIssueFound() {
		runTest(0)
	}

	@Test
	def void testMissingEncryption() {
		runTest(6, [ um |
			val rs = um.eResource.resourceSet
			EcoreUtil.resolveAll(rs)
			val repo = rs.resources.findFirst[URI.fileExtension == "repository"].contents.get(0) as Repository

			val action = repo.eAllContents.filter(SetVariableAction).findFirst[entityName == "BrowserLogic.exchangeSessionData.return"]
			val characterizations = action.localVariableUsages_SetVariableAction.get(0).variableCharacterisation_VariableUsage
			characterizations.clear
			characterizations.add(ConfidentialityFactory.eINSTANCE.createConfidentialityVariableCharacterisation => [
				lhs = ExpressionFactory.eINSTANCE.createLhsEnumCharacteristicReference
				rhs = ExpressionFactory.eINSTANCE.createNamedEnumCharacteristicReference => [
					namedReference = StoexFactory.eINSTANCE.createVariableReference => [
						referenceName = "ownID"
					]
				]
			])
		])
	}
}

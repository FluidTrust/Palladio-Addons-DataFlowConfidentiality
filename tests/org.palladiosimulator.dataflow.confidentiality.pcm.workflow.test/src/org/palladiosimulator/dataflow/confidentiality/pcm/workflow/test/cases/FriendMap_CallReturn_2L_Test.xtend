package org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.cases

import de.uka.ipd.sdq.stoex.StoexFactory
import org.eclipse.emf.ecore.util.EcoreUtil
import org.junit.jupiter.api.Test
import org.palladiosimulator.dataflow.confidentiality.pcm.model.confidentiality.ConfidentialityVariableCharacterisation
import org.palladiosimulator.dataflow.confidentiality.pcm.model.confidentiality.expression.NamedEnumCharacteristicReference
import org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.cases.impl.InformationFlowHierarchicalLatices_TestBase
import org.palladiosimulator.pcm.repository.Repository
import org.palladiosimulator.pcm.seff.ExternalCallAction

class FriendMap_CallReturn_2L_Test extends InformationFlowHierarchicalLatices_TestBase {

	new() {
		super("FriendMap_CallReturn_2L")
	}

	@Test
	def void testNoIssueFound() {
		runTest(0)
	}
	
	@Test
	def void testMissingEncryption() {
		runTest(3, [um |
			val rs = um.eResource.resourceSet
			EcoreUtil.resolveAll(rs)
			val repository = rs.resources.findFirst[URI.fileExtension == "repository"].contents.get(0) as Repository
			val eca = repository.eAllContents.filter(ExternalCallAction).findFirst[entityName == "FriendMapApp.createMap.callGoogle"]
			val characterization = eca.inputVariableUsages__CallAction.get(0).variableCharacterisation_VariableUsage.get(0) as ConfidentialityVariableCharacterisation
			val reference = characterization.rhs as NamedEnumCharacteristicReference
			reference.namedReference = StoexFactory.eINSTANCE.createVariableReference => [
				referenceName = "locations"
			]
		])
	}
}

package org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.cases

import de.uka.ipd.sdq.stoex.StoexFactory
import org.junit.jupiter.api.Test
import org.palladiosimulator.dataflow.confidentiality.pcm.model.confidentiality.ConfidentialityVariableCharacterisation
import org.palladiosimulator.dataflow.confidentiality.pcm.model.confidentiality.expression.NamedEnumCharacteristicReference
import org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.cases.impl.PrivateTaxi_AL_TestBase
import org.palladiosimulator.pcm.seff.ExternalCallAction

class PrivateTaxi_CallReturn_AL_Test extends PrivateTaxi_AL_TestBase {

	new() {
		super("PrivateTaxi_CallReturn_AL")
	}

	@Test
	def void testNoIssueFound() {
		runTest(0)
	}
	
	@Test
	def void testMissingEncryption() {
		runTest(10, [ um |
			val eca = um.eResource.resourceSet.allContents.filter(ExternalCallAction).findFirst [
				entityName == "PrivateDriverLogic.submitRoute.callPrivateTaxi"
			]
			val characterization = eca.inputVariableUsages__CallAction.get(0).variableCharacterisation_VariableUsage.
				get(0) as ConfidentialityVariableCharacterisation
			val reference = characterization.rhs as NamedEnumCharacteristicReference
			reference.namedReference = StoexFactory.eINSTANCE.createVariableReference => [
				referenceName = "route"
			]
		])
	}
}

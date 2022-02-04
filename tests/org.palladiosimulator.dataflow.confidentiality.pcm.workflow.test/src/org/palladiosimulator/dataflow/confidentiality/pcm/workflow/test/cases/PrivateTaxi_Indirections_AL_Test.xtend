package org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.cases

import org.eclipse.emf.ecore.util.EcoreUtil
import org.junit.jupiter.api.Test
import org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.cases.impl.PrivateTaxi_AL_TestBase
import org.palladiosimulator.indirections.composition.AssemblyDataConnector
import org.palladiosimulator.indirections.composition.DataSourceDelegationConnector
import org.palladiosimulator.indirections.repository.DataSourceRole
import org.palladiosimulator.pcm.repository.CompositeComponent

class PrivateTaxi_Indirections_AL_Test extends PrivateTaxi_AL_TestBase {

	new() {
		super("PrivateTaxi_Indirections_AL")
	}

	@Test
	def void testNoIssueFound() {
		runTest(0)
	}

	@Test
	def void testMissingEncryption() {
		runTest(2, [ um |
			val pdComponent = um.eResource.resourceSet.allContents.filter(CompositeComponent).findFirst [
				entityName == "PrivateDriver"
			]

			val pdFacadeAc = pdComponent.assemblyContexts__ComposedStructure.findFirst [ ac |
				ac.entityName == "Assembly_PrivateDriverFacade"
			]
			val pdFacadeSourceRole = pdFacadeAc.encapsulatedComponent__AssemblyContext.
				requiredRoles_InterfaceRequiringEntity.
				findFirst[entityName == "PrivateTaxiLogic.Route.sourceToTaxi"] as DataSourceRole

			val pdToEncryptionConnector = pdComponent.connectors__ComposedStructure.filter(AssemblyDataConnector).
				findFirst[sourceAssemblyContext == pdFacadeAc && dataSourceRole == pdFacadeSourceRole]
			val encryptionAc = pdToEncryptionConnector.sinkAssemblyContext
			val connectorsToDelete = pdComponent.connectors__ComposedStructure.filter(AssemblyDataConnector).filter [
				sourceAssemblyContext == encryptionAc || sinkAssemblyContext == encryptionAc
			].toList
			
			val delegationConnector = pdComponent.connectors__ComposedStructure.filter(DataSourceDelegationConnector).
				findFirst[assemblyContext == encryptionAc]
			delegationConnector.assemblyContext = pdFacadeAc
			delegationConnector.innerDataSourceRole = pdFacadeSourceRole

			connectorsToDelete.forEach[c|EcoreUtil.delete(c)]
			EcoreUtil.delete(encryptionAc)
		])
	}
}

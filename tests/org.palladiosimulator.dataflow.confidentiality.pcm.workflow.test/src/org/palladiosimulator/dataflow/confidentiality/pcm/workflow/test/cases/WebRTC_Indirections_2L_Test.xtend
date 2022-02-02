package org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.cases

import org.eclipse.emf.ecore.util.EcoreUtil
import org.junit.jupiter.api.Test
import org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.cases.impl.InformationFlowHierarchicalLatices_TestBase
import org.palladiosimulator.indirections.composition.AssemblyDataConnector
import org.palladiosimulator.indirections.composition.DataSourceDelegationConnector
import org.palladiosimulator.pcm.repository.CompositeComponent
import org.palladiosimulator.pcm.repository.Repository

class WebRTC_Indirections_2L_Test extends InformationFlowHierarchicalLatices_TestBase {

	new() {
		super("WebRTC_Indirections_2L")
	}

	@Test
	def void testNoIssueFound() {
		runTest(0)
	}

	@Test
	def void testMissingEncryption() {
		runTest(2, [ um |
			val rs = um.eResource.resourceSet
			EcoreUtil.resolveAll(rs)
			val repo = rs.resources.findFirst[URI.fileExtension == "repository"].contents.get(0) as Repository

			val browserComponent = repo.components__Repository.filter(CompositeComponent).findFirst [
				entityName == "Browser"
			]
			val encryptAc = browserComponent.assemblyContexts__ComposedStructure.findFirst [
				entityName == "Assembly_Browser.encryptSession"
			]
			val encryptAcAssemblyConnector = browserComponent.connectors__ComposedStructure.filter(
				AssemblyDataConnector).findFirst[c|c.sinkAssemblyContext == encryptAc]
			val connectorToChange = browserComponent.connectors__ComposedStructure.filter(
				DataSourceDelegationConnector).findFirst[c|c.assemblyContext == encryptAc]

			connectorToChange.assemblyContext = encryptAcAssemblyConnector.sourceAssemblyContext
			connectorToChange.innerDataSourceRole = encryptAcAssemblyConnector.dataSourceRole

			EcoreUtil.delete(encryptAcAssemblyConnector)
			EcoreUtil.delete(encryptAc)
		])
	}
}

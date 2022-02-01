package org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.cases

import org.eclipse.emf.ecore.util.EcoreUtil
import org.junit.jupiter.api.Test
import org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.cases.impl.InformationFlowHierarchicalLatices_TestBase
import org.palladiosimulator.indirections.composition.AssemblyDataConnector
import org.palladiosimulator.indirections.repository.DataSinkRole
import org.palladiosimulator.pcm.system.System

class FriendMap_Indirections_2L_Test extends InformationFlowHierarchicalLatices_TestBase {

	new() {
		super("FriendMap_Indirections_2L")
	}

	@Test
	def void testNoIssueFound() {
		runTest(0)
	}

	@Test
	def void testMissingEncryption() {
		runTest(1, [ um |
			val rs = um.eResource.resourceSet
			EcoreUtil.resolveAll(rs)
			val pcmSystem = rs.resources.flatMap[contents].filter(System).findFirst[true]
			val connectorToChange = pcmSystem.connectors__ComposedStructure.filter(AssemblyDataConnector).findFirst[id == "_BPXbwINwEeyth_Kqe2ur6g"]
			val newTarget = pcmSystem.assemblyContexts__ComposedStructure.findFirst[entityName == "Assembly_Google.createMap"]
			val newTargetSink = newTarget.encapsulatedComponent__AssemblyContext.providedRoles_InterfaceProvidingEntity.get(0) as DataSinkRole
			connectorToChange.sinkAssemblyContext = newTarget
			connectorToChange.dataSinkRole = newTargetSink
		])
	}
}

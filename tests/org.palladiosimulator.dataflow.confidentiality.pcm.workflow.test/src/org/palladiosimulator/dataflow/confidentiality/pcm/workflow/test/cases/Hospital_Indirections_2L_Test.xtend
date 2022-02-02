package org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.cases

import org.eclipse.emf.ecore.util.EcoreUtil
import org.junit.jupiter.api.Test
import org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.cases.impl.InformationFlowHierarchicalLatices_TestBase
import org.palladiosimulator.indirections.composition.AssemblyDataConnector
import org.palladiosimulator.indirections.repository.DataChannel
import org.palladiosimulator.pcm.system.System

class Hospital_Indirections_2L_Test extends InformationFlowHierarchicalLatices_TestBase {

	new() {
		super("Hospital_Indirections_2L")
	}

	@Test
	def void testNoIssueFound() {
		runTest(0)
	}

	@Test
	def void testMissingEncryption() {
		runTest(3, [ um |
			val rs = um.eResource.resourceSet
			EcoreUtil.resolveAll(rs)
			val system = rs.resources.findFirst[URI.fileExtension == "system"].contents.
				get(0) as System
			val acToRemove = system.assemblyContexts__ComposedStructure.findFirst [
				entityName == "Assembly_EncryptPatientsList"
			]
			val newSourceAc = system.assemblyContexts__ComposedStructure.findFirst[entityName == "Assembly_PatientsDB"]
			val newSourceRole = (newSourceAc.encapsulatedComponent__AssemblyContext as DataChannel).dataSourceRoles.
				get(0)
			system.connectors__ComposedStructure.filter(AssemblyDataConnector).filter [ c |
				c.sourceAssemblyContext == acToRemove
			].forEach [ c |
				c.sourceAssemblyContext = newSourceAc
				c.dataSourceRole = newSourceRole
			]
			system.connectors__ComposedStructure.filter(AssemblyDataConnector).filter [ c |
				c.sinkAssemblyContext == acToRemove
			].toList.forEach[c|EcoreUtil.delete(c)]
		])
	}
}

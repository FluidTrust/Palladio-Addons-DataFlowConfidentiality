package org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.cases.impl

import java.util.function.Consumer
import java.util.function.Function
import org.eclipse.emf.ecore.util.EcoreUtil
import org.palladiosimulator.dataflow.confidentiality.pcm.workflow.TransformPCMDFDToPrologWorkflowFactory
import org.palladiosimulator.dataflow.confidentiality.pcm.workflow.TransitiveTransformationTrace
import org.palladiosimulator.dataflow.confidentiality.pcm.workflow.jobs.TransformPCMDFDToPrologJobBuilder
import org.palladiosimulator.dataflow.dictionary.characterized.DataDictionaryCharacterized.Enumeration
import org.palladiosimulator.pcm.allocation.Allocation
import org.palladiosimulator.pcm.usagemodel.UsageModel
import org.prolog4j.Solution

import static org.junit.jupiter.api.Assertions.*
import static org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.StandaloneUtil.getModelURI

class PrivateTaxi_AL_TestBase extends TestBase {
	
	var boolean initialized
	var TransitiveTransformationTrace trace
	val String folderName
	
	new(String folderName) {
		this.folderName = folderName;
	}
	
	protected def runTest(int expectedNumberOfSolutions) {
		runTest(expectedNumberOfSolutions, [])
	}
	
	protected def runTest(int expectedNumberOfSolutions, Consumer<UsageModel> usageModelModifier) {
		var keySolution = findKeyFlaws(usageModelModifier)
		assertNumberOfSolutions(keySolution, 0, #["N", "PIN0", "PIN1", "S0", "S1", "REQ", "PROV"])
		var solution = findFlaws(usageModelModifier)
		assertNumberOfSolutions(solution, expectedNumberOfSolutions, #["N", "PIN", "V_CLEAR", "V_CLASS", "S"])
	}
	
	protected def Solution<Object> findFlaws(Consumer<UsageModel> usageModelModifier) {
		val Function<TransitiveTransformationTrace, String> queryGenerator = [TransitiveTransformationTrace trace|
			val ctClearance = trace.getFactId([ct|ct.name == "Clearance"]).last
			val ctClassification = trace.getFactId([ct|ct.name == "Classification"]).last
			'''
			inputPin(N, PIN),
			nodeCharacteristic(N, '«ctClearance»', V_CLEAR),
			characteristic(N, PIN, '«ctClassification»', V_CLASS, S),
			\+ connected(V_CLASS, V_CLEAR).
			'''
		]
		findFlaws(queryGenerator, usageModelModifier)
	}

	protected def Solution<Object> findKeyFlaws(Consumer<UsageModel> usageModelModifier) {
		val Function<TransitiveTransformationTrace, String> queryGenerator = [TransitiveTransformationTrace trace|
			val ctPrivateKeyOf = trace.getFactId([ct|ct.name == "PrivateKeyOf"]).last
			val ctDecryptableBy = trace.getFactId([ct|ct.name == "DecryptableBy"]).last
			'''
			inputPin(N, PIN0),
			inputPin(N, PIN1),
			PIN0 \== PIN1,
			sub_string(N,_,_,_,'ecrypt'),
			findall(X, characteristic(N, PIN0, '«ctPrivateKeyOf»', X, S0), L_PROV),
			findall(X, characteristic(N, PIN1, '«ctDecryptableBy»', X, S1), L_REQ),
			sort(L_PROV, PROV), sort(L_REQ, REQ),
			REQ \== [],
			intersection(PROV, REQ, []).
			'''
		]
		findFlaws(queryGenerator, usageModelModifier)
	}
	
	protected def Solution<Object> findFlaws(Function<TransitiveTransformationTrace, String> queryGenerator, Consumer<UsageModel> usageModelModifier) {
		if (!initialized) {
			initialized = true
			val usageModelURI = getModelURI(folderName + "/newUsageModel.usagemodel")
			val usageModel = rs.getResource(usageModelURI, true).contents.get(0) as UsageModel
			val allocationModelURI = getModelURI(folderName + "/newAllocation.allocation")
			val allocationModel = rs.getResource(allocationModelURI, true).contents.get(0) as Allocation
			EcoreUtil.resolveAll(rs)
			
			usageModelModifier.accept(usageModel)
			
			val job = new TransformPCMDFDToPrologJobBuilder().addSerializeModelToString.addUsageModels(usageModel).
				addAllocationModel(allocationModel).build
			val workflow = TransformPCMDFDToPrologWorkflowFactory.createWorkflow(job)
			workflow.run
			
			val resultingProgram = workflow.prologProgram
			assertTrue(resultingProgram.isPresent)
			val resultingTrace = workflow.trace
			assertTrue(resultingTrace.isPresent)
			this.trace = resultingTrace.get
			
			val levelsEnum = rs.allContents.filter(Enumeration).findFirst[name == "Levels"]
			val anyLiteral = levelsEnum.literals.findFirst[name == "Any"]
			val contactLiteral = levelsEnum.literals.findFirst[name == "Contact"]
			val routeLiteral = levelsEnum.literals.findFirst[name == "Route"]
			val privateTaxiLiteral = levelsEnum.literals.findFirst[name == "PrivateTaxi"]
			val driverLiteral = levelsEnum.literals.findFirst[name == "Driver"]
			val riderLiteral = levelsEnum.literals.findFirst[name == "Rider"]
			val calcDistanceLiteral = levelsEnum.literals.findFirst[name == "CalcDistance"]
			
			val anyLiteralId = trace.getLiteralFactIds(anyLiteral).last
			val contactLiteralId = trace.getLiteralFactIds(contactLiteral).last
			val routeLiteralId = trace.getLiteralFactIds(routeLiteral).last
			val privateTaxiLiteralId = trace.getLiteralFactIds(privateTaxiLiteral).last
			val driverLiteralId = trace.getLiteralFactIds(driverLiteral).last
			val riderLiteralId = trace.getLiteralFactIds(riderLiteral).last
			val calcDistanceLiteralId = trace.getLiteralFactIds(calcDistanceLiteral).last
	
			prover.loadTheory(resultingProgram.get())
			prover.addTheory('''
				edge('«anyLiteralId»', '«contactLiteralId»').
				edge('«anyLiteralId»', '«routeLiteralId»').
				edge('«contactLiteralId»', '«privateTaxiLiteralId»').
				edge('«contactLiteralId»', '«driverLiteralId»').
				edge('«contactLiteralId»', '«riderLiteralId»').
				edge('«routeLiteralId»', '«calcDistanceLiteralId»').
				edge('«routeLiteralId»', '«driverLiteralId»').
				edge('«routeLiteralId»', '«riderLiteralId»').
				connected(X, X).
				connected(SRC, DST) :-
					edge(SRC, X),
					connected(X, DST).
			''')
		}
		val queryString = queryGenerator.apply(trace)
		var query = prover.query(queryString.toString)
		var solution = query.solve()
		solution
	}
	
}
package org.palladiosimulator.dataflow.confidentiality.pcm.dddsl.tests

import com.google.inject.Inject
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.eclipse.xtext.testing.formatter.FormatterTestHelper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.^extension.ExtendWith

@ExtendWith(InjectionExtension)
@InjectWith(DDDslInjectorProvider)
class DDDslFormatterTest {

	@Inject
	FormatterTestHelper formatterHelper;

	@Test
	def void testDictionary() {
		'''
		dictionary id "123456"

    	'''.assertFormatting
	}

	@Test
    def testEnumerations() {
    	'''
    	dictionary id "123456"
    	
    	enum FirstEnum {
    		A
    		B
    		C
    	}
    	enum "First Enum" {
    		A
    		"B B"
    		C
    	}'''.assertFormatting
    }
    
	@Test
    def testEnumCharacteristicType() {
    	'''
    	dictionary id "123456"
    	
    	enum FirstEnum {
    		A
    	}
    	enumCharacteristicType Type1 using FirstEnum
    	enumCharacteristicType "Type 1" using FirstEnum'''.assertFormatting
    }
    
	@Test
    def testEnumCharacteristic() {
    	'''
    	dictionary id "123456"
    	
    	enum FirstEnum {
    		A
    		B
    		C
    	}
    	enumCharacteristicType Type1 using FirstEnum
    	enumCharacteristic Char1 using Type1 {
    		B
    	}
    	enumCharacteristic "Char 2" using Type1 {
    		A
    		B
    		C
    	}'''.assertFormatting
    }
    
    
    @Test
    def testBehavior() {
    	val expected = '''
    	dictionary id "123456"
    	
    	behavior Behavior1 {
    		input in1
    		input in2
    		output out1
    		output out2
    	
    		out1.*.* := in1.*.*
    		out2.*.* := in2.*.*
    	}'''
    	
    	val toBeFormatted = '''
    	dictionary id "123456"
    	
    	behavior   Behavior1  {
    		  input   in1
    		input   in2
    		  output  out1
    		output   out2
    	
    		  out1  .  *  .  *  :=  in1  .  *  .  *  
    		  out2  .   *  .  *  :=  in2  .  *  .   *  
    	}'''
    	assertFormatting(expected, toBeFormatted)
    }
    
    @Test
    def testTerms() {
    	val expected = '''
    	dictionary id "123456"
    	
    	enum FirstEnum {
    		A
    	}
    	enumCharacteristicType Type1 using FirstEnum
    	
    	behavior Behavior1 {
    		input in1
    		input in2
    		output out1
    		output out2
    	
    		out1.Type1.* := in1.Type1.*
    		out1. {
    			Type1.A := in2.Type1.A
    			*.* := in1.*.*
    		}
    		out2.Type1.A := in2.Type1.A
    		out1.*.* := true
    		out2.*.* := !(in1.*.* & in2.*.*)
    	}'''
    	
    	val toBeFormatted = '''
    	dictionary id "123456"
    	
    	enum FirstEnum {
    		A
    	}
    	enumCharacteristicType Type1 using FirstEnum
    	behavior   Behavior1  {
    		  input   in1
    		input   in2
    		  output  out1
    		output   out2
    	
    		  out1  .  Type1  .  *  :=  in1  .  Type1  .  *  
    		  out1  .   {
    		     Type1  .  A  :=  in2  .  Type1  .   A    
    		     *     .   * :=   in1   .  *  .  *  
    		  }
    		    out2  .   Type1  .  A  :=  in2  .  Type1  .   A
    		   out1 . * . * :=  true  
    		   out2 . * . * :=   !  (  in1   .  *  .  *   &   in2  .  *  .  *  )  
    	}'''
    	assertFormatting(expected, toBeFormatted)
    }


	protected def assertFormatting(CharSequence expected) {
		expected.assertFormatting(false)
	}

	protected def assertFormatting(CharSequence expected, boolean trim) {
		val toBeFormatted = expected.toString.replaceAll("\\s(?=([^\"']*[\"'][^\"']*[\"'])*[^\"']*$)", "  ")
		val newExpected = trim ? expected.toString.trim : expected.toString
		assertFormatting(newExpected, toBeFormatted)
	}

	protected def assertFormatting(CharSequence expected, CharSequence toBeFormatted) {
		formatterHelper.assertFormatted([ request |
			request.toBeFormatted = toBeFormatted
			request.expectation = expected
		])
	}
}

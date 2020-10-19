/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestCheckForbiddenHelper {
	@Test void addForbiddenFunction() {
		var alg = new CheckForbiddenLanguage();
		CheckForbiddenHelper.addForbiddenFunction(alg, "forEach", "Because");
		CheckForbiddenHelper.addForbiddenFunction(alg, "pool", "Because");

		assertTrue(alg.process("void main(){\n\tfoo.moo();\n\n"));
		assertTrue(alg.process("void main(){\n\tfoo.AorEach();\n\n"));
		assertTrue(alg.process("void main(){\n\tforEach();\n\n"));

		assertFalse(alg.process("void main(){\n\tforEach;foo.forEach();\n\n"));
		assertFalse(alg.process("void main(){\n\tfoo.forEach();\n\n"));
		assertFalse(alg.process("foo.forEach();"));
		assertFalse(alg.process("void main(){\n\ta = b;foo.forEach();\n\n"));
		assertFalse(alg.process("void main(){\n\tfoo \t. forEach();\n\n"));
		assertEquals(3, alg.lineNumber);
		assertEquals(1, alg.failures.size());
		CheckForbiddenLanguage.Failure f = alg.getFailures().get(0);
		assertEquals(1, f.line);
		assertEquals("forEach", f.check.keyword);
	}

	@Test void addVarMustBeExplicit() {
		var alg = new CheckForbiddenLanguage();
		CheckForbiddenHelper.addVarMustBeExplicit(alg);

		assertTrue(alg.process("var a = new Foo()"));
		assertTrue(alg.process("var a =new Foo()"));
		assertTrue(alg.process("var a= new Foo()"));
		assertTrue(alg.process("var a=new Foo()"));
		assertTrue(alg.process("void main(){\n\tvar a=new Foo();\n} \n"));
		assertTrue(alg.process("void main(){\n\tb = s;var a=new Foo();\n} \n"));
		assertTrue(alg.process("var a = \nmoo.foo()")); // known false negative

		// Known false failures
		assertFalse(alg.process("var a = value ? new Foo() : new Foo()"));

		// Failures
		assertFalse(alg.process("var a = moo.foo()"));
		assertFalse(alg.process("var a= moo.foo()"));
		assertFalse(alg.process("var a =moo.foo()"));
		assertFalse(alg.process("var a=moo.foo()"));
		assertFalse(alg.process("var a=new Moo();var a=moo.foo()"));
		assertFalse(alg.process("dude = moo;var a = moo.foo()"));
		assertFalse(alg.process("var b=new Moo();var a = moo.foo()"));
		assertEquals(1, alg.getFailures().size());
		assertEquals(0, alg.getFailures().get(0).line);
		assertEquals("var", alg.getFailures().get(0).check.keyword);

		assertFalse(alg.process("var b=new \n\nMoo();var a = moo.foo()\n\n"));
		assertEquals(1, alg.getFailures().size());
		assertEquals(2, alg.getFailures().get(0).line);
		assertEquals("var", alg.getFailures().get(0).check.keyword);
	}
}

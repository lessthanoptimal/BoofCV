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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestJavaLineTokenizer {
	@Test void wordTokens() {
		var alg = new JavaLineTokenizer();

		alg.parse("int hi = there ;");
		checkTokens(alg.stringTokens, "int", "hi", "=", "there", ";");

		alg.parse("int hi=there;");
		checkTokens(alg.stringTokens, "int", "hi", "=", "there", ";");

		alg.parse("for( var i : list ) {");
		checkTokens(alg.stringTokens, "for", "(", "var", "i", ":", "list", ")", "{");

		alg.parse("Moo foo=new Boo()");
		checkTokens(alg.stringTokens, "Moo", "foo", "=", "new", "Boo", "()");

		alg.parse("Moo foo_moo");
		checkTokens(alg.stringTokens, "Moo", "foo_moo");

		alg.parse("\"for this is\"");
		checkTokens(alg.stringTokens, "\"for this is\"");
	}

	@Test void stringTokenEscape() {
		var alg = new JavaLineTokenizer();
		alg.parse("\"for \\\"this is\"");
		checkTokens(alg.stringTokens, "\"for \"this is\"");
		alg.parse("\"for \\\\\"\\\"this is\"");
		checkTokens(alg.stringTokens, "\"for \\\"", "\\", "\"this is\"");
	}

	void checkTokens( List<String> found, String... expected ) {
		assertEquals(expected.length, found.size());
		for (int i = 0; i < expected.length; i++) {
			assertEquals(expected[i], found.get(i));
		}
	}
}

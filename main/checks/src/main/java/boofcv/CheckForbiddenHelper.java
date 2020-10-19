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

/**
 * Helper functions for adding rules
 *
 * @author Peter Abeles
 */
public class CheckForbiddenHelper {
	/**
	 * Checks to see if a call function has been made. It works by searching for the name then seeing if the
	 * previous non-whitespace character is a ".".
	 */
	public static void addForbiddenFunction( CheckForbiddenLanguage checker,
											 String functionName, String reason ) {
		CheckForbiddenLanguage.ConditionalRule rule = line -> {
			String[] words = line.trim().split("\\s+");
			for (int wordIdx = 0; wordIdx < words.length; wordIdx++) {
				String w = words[wordIdx];
				int lastLoc = 0;
				while (lastLoc < w.length()) {
					int loc = w.indexOf(functionName, lastLoc);
					if (loc < 0)
						break;
					if (loc > 0) {
						if (w.charAt(loc - 1) == '.')
							return false;
					} else if (wordIdx > 0 && words[wordIdx - 1].endsWith(".")) {
						return false;
					}
					lastLoc = loc + functionName.length();
				}
			}
			return true;
		};
		checker.addConditional("function_" + functionName, functionName, reason, rule);
	}

	/**
	 * A simple check that sees if var is only used when the type is shown via "new". There are ways to trick this
	 * function into thinking the type is explicitly shown when it is not.
	 */
	public static void addVarMustBeExplicit( CheckForbiddenLanguage checker ) {
		CheckForbiddenLanguage.ConditionalRule rule = line -> {
			String[] words = line.trim().split("\\s+");
			for (int i = 0; i < words.length; i++) {
				String w = words[i];
				if (!w.endsWith("var"))
					continue;

				if (words.length <= i + 1)
					return true;

				String wn = words[i + 1];

				// var a= new
				if (wn.charAt(wn.length() - 1) == '=') {
					if (words.length > i + 2) {
						if (words[i + 2].equals("new")) {
							i += 2;
							continue;
						} else {
							return false;
						}
					} else {
						continue;
					}
				}

				// var a=new
				if (wn.endsWith("=new")) {
					i++;
					continue;
				} else if (wn.contains("="))
					return false;

				if (words.length <= i + 2)
					continue;

				// var a =new
				if (words[i + 2].length() > 1 && words[i + 2].charAt(0) == '=') {
					if (words[i + 2].endsWith("new")) {
						i += 2;
						continue;
					} else {
						return false;
					}
				}

				// var a = new
				if (words.length <= i + 3)
					continue;
				// var foo = new Foo()
				// 0    1  2  3   4
				return words[i + 3].equals("new");
				// var foo = new Foo().sneaky() will trick this approach
				// var foo \n = Factory.foo() will also get by
			}
			return true;
		};
		checker.addConditional("explicit_var", "var",
				"Auto type inference with var reduces code maintainability and can hide mistakes during refactoring", rule);
	}
}

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

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Searches code for forbidden language features and functions. Typically these are convenience functions that
 * allocate memory that will cause the GC to run more often or in the case of "var" without explicit type, make
 * the code less readable.
 *
 * @author Peter Abeles
 */
public class CheckForbiddenLanguage {
	// Default identifier that it searches for in single line comments to mean there's a command
	public static final String DEFAULT_IDENTIFIER = " lint:forbidden";
	// Command for ignoring the current line
	public static final String IGNORE_LINE = "ignore_line";
	// Command for ignoring the code below
	public static final String IGNORE_BELOW = "ignore_below";

	// Once this line is encountered it will abort processing the file
	public static final String DISABLE_ALL = "disable_all";

	// Once this line is encountered it will ignore the specified check for the remainder of the file
	public static final String DISABLE_CHECK = "disable_check";

	// If there's a special command for ignoring lint errors and there are no errors then that's an error
	static final Check NOTHING_IGNORED =
			new Check("nothing_ignored", "N/A", "The ignore request does noting.", null);
	static final Check MALFORMED_COMMAND =
			new Check("malformed_command", "N/A", "Single line comment command is malformed", null);

	/** Specifies the text used to identify special lint commands in single line comments */
	public String commentIdentifier = DEFAULT_IDENTIFIER;

	// list of all the checks it will apply
	List<Check> allChecks = new ArrayList<>();
	// List of checks which are active for this file
	List<Check> activeChecks = new ArrayList<>();

	// List of all the problems found
	@Getter List<Failure> failures = new ArrayList<>();

	// Parser state
	int lineNumber;
	// If more than zero it will keep on skipping over lines
	int ignoreLines;
	boolean exceptionWasIgnored;

	// if the test has been disabled or not
	boolean disabled;

	// The last line of code which was extracted. It's not processed until the new line is encountered so that
	// a skip command can be processed.
	@Nullable String lineOfCode = "";

	JavaLineTokenizer tokenizer = new JavaLineTokenizer();

	/**
	 * Searches the document for a line with the keyword then applies the conditional rule to see if the activity
	 * is forbidden or not.
	 *
	 * @param ruleName ID for the rule.
	 * @param keyword The keyword that is searched for
	 * @param reason Reason this is forbidden
	 * @param rule Lambda with the rule
	 */
	public void addConditional( String ruleName, String keyword, String reason, ConditionalRule rule ) {
		Check c = new Check();
		c.ruleName = ruleName;
		c.keyword = keyword;
		c.reason = reason;
		c.rule = rule;
		allChecks.add(c);
	}

	/**
	 * Processes the source code contained in the string and looks for exceptions to the passed in rules
	 *
	 * @param sourceCode Source code
	 * @return true If the file contains no invalid code
	 */
	public boolean process( String sourceCode ) {
		// If it doesn't end in a new line add a new line. This greatly simplifies processing of the final line
		if (!isNewLine(sourceCode.charAt(sourceCode.length() - 1)))
			sourceCode = sourceCode + '\n';

		activeChecks.clear();
		activeChecks.addAll(allChecks);

		failures.clear();
		// beginning and end index of code on the same line
		int codeIdx0 = 0;
		int codeIdx1 = 0;

		// Initialize the state
		disabled = false;
		lineOfCode = null;
		lineNumber = 1; // most IDEs seem to start counting at 1
		Mode mode = Mode.CODE;

		// Go through the string one character at a time looking for comments and new lines
		// Extract code which is on a single line and check that for forbidden operations
		char prev = Character.MIN_VALUE;
		for (int idx = 0; idx < sourceCode.length(); idx++) {
			boolean incrementLine = false;
			char c = sourceCode.charAt(idx);

			switch (mode) {
				case CODE -> {
					// Find all the code until the end of the line or a comment is encountered
					if (c == '*' && prev == '/') {
						mode = Mode.MULTI_LINE_COMMENT;
						codeIdx1 = idx - 1;
					} else if (c == '/' && prev == '/') {
						mode = Mode.LINE_COMMENT;
						codeIdx1 = idx - 1;
					} else if (isNewLine(c)) {
						incrementLine = shouldIncrementLine(prev, c);
						codeIdx1 = idx - 1;
					}
					if (codeIdx1 > codeIdx0) {
						updateLineOfCode(sourceCode, codeIdx0, codeIdx1);
						codeIdx0 = idx + 1;
					}
				}

				case LINE_COMMENT -> {
					if (isNewLine(c)) {
						checkCommentForCommands(sourceCode, codeIdx1, idx);
						if (disabled)
							return true;
						incrementLine = shouldIncrementLine(prev, c);
						codeIdx0 = idx + 1;
						mode = Mode.CODE;
					}
				}

				case MULTI_LINE_COMMENT -> {
					if (isNewLine(c)) {
						incrementLine = shouldIncrementLine(prev, c);
					} else if (c == '/' && prev == '*') {
						codeIdx0 = idx + 1;
						mode = Mode.CODE;
					}
				}
			}

			if (incrementLine) {
				// Now that the end of the line has been reached we can process the line
				if (lineOfCode != null) {
					processLine(lineOfCode);
					lineOfCode = null;
				}
				updateIgnoreLines();
				lineNumber++;
			}
			prev = c;
		}

		return failures.isEmpty();
	}

	/**
	 * See if it's still ignoring lines. If so decrement the counter. If nothing was ever ignored then that is
	 * an error too.
	 */
	private void updateIgnoreLines() {
		if (ignoreLines > 0) {
			ignoreLines--;
			if (ignoreLines == 0 && !exceptionWasIgnored) {
				// If nothing was ignored then the comment can be removed
				addFailure("nothing_ignored", NOTHING_IGNORED);
			}
		}
	}

	private void updateLineOfCode( String sourceCode, int codeIdx0, int codeIdx1 ) {
		if (lineOfCode == null)
			lineOfCode = sourceCode.substring(codeIdx0, codeIdx1);
		else
			// there must have been a /**/ comment in the middle. Join together and skip
			lineOfCode = lineOfCode + " " + sourceCode.substring(codeIdx0, codeIdx1);
	}

	private boolean shouldIncrementLine( char prev, char c ) {
		return !(c == '\r' && prev == '\n');
	}

	private boolean isNewLine( char c ) {
		return c == '\n' || c == '\r';
	}

	/**
	 * Examines the line of code and sees if any of the Checks are triggered by it
	 */
	private void processLine( String line ) {
		List<String> tokens = null;
		for (int i = 0; i < activeChecks.size(); i++) {
			Check c = activeChecks.get(i);
			if (!line.contains(c.keyword))
				continue;
			// don't tokenize unless necessary
			if (tokens == null) {
				tokens = tokenizer.parse(line).stringTokens;
			}
			if (c.rule.matches(line, tokens)) {
				continue;
			}
			// See if it's ignoring exceptions and if it was, note that it did ignore at least one exception
			if (ignoreLines > 0) {
				exceptionWasIgnored = true;
				return;
			}
			addFailure(line, c);
		}
	}

	/**
	 * Checks to see if this is a special single line comment that changes the behavior of the link check
	 */
	private void checkCommentForCommands( String code, int idx0, int idx1 ) {
		// See if it has expected prefix
		String substring = code.substring(idx0 + 2, idx1);
		if (!substring.startsWith(commentIdentifier))
			return;

		// Look for specific commands and parse them
		int idx = substring.indexOf(IGNORE_BELOW);
		if (idx >= 0) {
			substring = substring.substring(idx + IGNORE_BELOW.length() + 1).trim();
			ignoreLines = Integer.parseInt(substring) + 1;
			// + 1 since it will be decremented for this line
			exceptionWasIgnored = false;
			return;
		}

		if (substring.contains(IGNORE_LINE)) {
			// the current line has not been processed yet, so this will ignore it when it is processed
			ignoreLines = 1;
			exceptionWasIgnored = false;
			return;
		}

		if (substring.contains(DISABLE_ALL)) {
			disabled = true;
			return;
		}

		if (substring.contains(DISABLE_CHECK)) {
			String[] words = substring.trim().split("\\s+");
			if (words.length < 3) {
				addFailure(substring, MALFORMED_COMMAND);
				return;
			}

			String target = words[2];

			for (int i = 0; i < activeChecks.size(); i++) {
				if (activeChecks.get(i).ruleName.equals(target)) {
					activeChecks.remove(i);
					return;
				}
			}

			addFailure("No check with the name: " + target, MALFORMED_COMMAND);
		}
	}

	private void addFailure( String substring, Check check ) {
		Failure f = new Failure();
		f.code = substring;
		f.line = lineNumber;
		f.check = check;
		failures.add(f);
	}

	@FunctionalInterface
	public interface ConditionalRule {
		/**
		 * @return true if the code is valid by this rule or false if not
		 */
		boolean matches( String line, List<String> tokens );
	}

	static class Check {
		String ruleName;
		String keyword;
		String reason;
		ConditionalRule rule = ( s, tokens ) -> false;

		public Check() {}

		public Check( String ruleName, String keyword, String reason, ConditionalRule rule ) {
			this.ruleName = ruleName;
			this.keyword = keyword;
			this.reason = reason;
			this.rule = rule;
		}
	}

	static class Failure {
		int line;
		String code;
		Check check;
	}

	enum Mode {
		// processing code that needs to be inspected
		CODE,
		// Inside a line comment
		LINE_COMMENT,
		// Inside a multi line comment
		MULTI_LINE_COMMENT
	}
}

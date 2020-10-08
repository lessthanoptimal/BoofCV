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

package boofcv.testing;

import boofcv.BoofTesting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Adds tests to enforce standards, such as no printing to stdout or stderr, unless it's an error.
 *
 * @author Peter Abeles
 */
public class BoofStandardJUnit {
	// Always provide a random number generator since it's needed so often
	protected final Random rand = BoofTesting.createRandom(234);

	// Override output streams to keep log spam to a minimum
	protected final ByteArrayOutputStream out = new ByteArrayOutputStream();
	protected final ByteArrayOutputStream err = new ByteArrayOutputStream();
	protected final PrintStream systemOut = System.out;
	protected final PrintStream systemErr = System.err;

	@BeforeEach
	public void captureStreams() {
		System.setOut(new PrintStream(out));
		System.setErr(new PrintStream(err));
	}

	@AfterEach
	public void revertStreams() {
		boolean nonEmptyOut = out.size() > 0;
		boolean nonEmptyErr = err.size() > 0;
		systemOut.print(out.toString());
		systemErr.print(err.toString());
		assertFalse(nonEmptyOut,"stdout was written to which is forbidden by default");
		assertFalse(nonEmptyErr,"stderr was written to which is forbidden by default");
		System.setOut(systemOut);
		System.setErr(systemErr);
	}
}

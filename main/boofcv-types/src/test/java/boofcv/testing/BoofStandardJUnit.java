/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import java.io.IOException;
import java.io.OutputStream;
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
	protected final MirrorStream out = new MirrorStream(System.out);
	protected final MirrorStream err = new MirrorStream(System.err);
	protected final PrintStream systemOut = System.out;
	protected final PrintStream systemErr = System.err;

	@BeforeEach
	public void captureStreams() {
		System.setOut(new PrintStream(out));
		System.setErr(new PrintStream(err));
	}

	@AfterEach
	public void revertStreams() {
		assertFalse(out.used,"stdout was written to which is forbidden by default");
		assertFalse(err.used,"stderr was written to which is forbidden by default");
		System.setOut(systemOut);
		System.setErr(systemErr);
	}

	public static class MirrorStream extends OutputStream {

		public PrintStream out;
		public boolean used = false;

		public MirrorStream( PrintStream out ) {
			this.out = out;
		}

		@Override public void write( int b ) throws IOException {
			used = true;
			out.write(b);
		}

		@Override public void write( byte[] b, int off, int len ) throws IOException {
			used = true;
			out.write(b, off, len);
		}

		@Override public void flush() throws IOException {
			out.flush();
		}

		@Override public void close() throws IOException {
			out.close();
		}
	}
}

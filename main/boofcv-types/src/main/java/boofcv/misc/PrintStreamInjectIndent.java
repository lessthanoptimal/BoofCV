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

package boofcv.misc;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Injects spaces after new line print printing. Useful for verbose print when children are printing
 *
 * @author Peter Abeles
 */
public class PrintStreamInjectIndent extends PrintStream {
	public PrintStreamInjectIndent( String prefix, int numIndent, PrintStream out ) {
		super(new Injector(out, prefix, numIndent*2));
	}

	public int getIndentSpaces() {
		return ((Injector)out).numSpaces;
	}

	public int getIndentCount() {
		return ((Injector)out).numSpaces/2;
	}

	public PrintStream getOriginalStream() {
		return ((Injector)out).out;
	}

	public static class Injector extends OutputStream {
		PrintStream out;
		int numSpaces;
		String prefix;
		boolean newLine = true;

		public Injector( PrintStream out, String prefix, int numSpaces ) {
			this.out = out;
			this.prefix = prefix;
			this.numSpaces = numSpaces;
		}

		@Override public void write( int b ) throws IOException {
			if (newLine) {
				for (int i = 0; i < prefix.length(); i++) {
					out.write(prefix.charAt(i));
				}
				for (int i = 0; i < numSpaces; i++) {
					out.write(' ');
				}
				newLine = false;
			}
			out.write(b);
			// inject tab at the new line
			if (b == '\n') {
				newLine = true;
			}
		}
	}
}

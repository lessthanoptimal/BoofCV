/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.io;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads one or more lines of pure numbers while skipping over lines which begin with the
 * comment character.  Intended for use with simple config files.
 *
 * @author Peter Abeles
 */
public class SimpleNumberSequenceReader {

	public SimpleNumberSequenceReader(char commentChar) {
		this.commentChar = commentChar;
	}

	char commentChar;
	Reader input;

	List<Double> sequence;

	char buffer[] = new char[1024];
	
	public List<Double> read( Reader input ) throws IOException {
		this.input = input;
		sequence = new ArrayList<>();
		int v = input.read();

		while( v >= 0 ) {
			if( v == commentChar ) {
				skipLine();
			} else {
				parseLine(v);
			}
			v = input.read();
		}

		this.input = null;
		return sequence;
	}
	
	private void parseLine( int v ) throws IOException {
		int size = 0;

		while( v >= 0 && v != '\n' ) {
			if( Character.isWhitespace(v) ) {
				if( size > 0 ) {
					String s = new String(buffer,0,size);
					sequence.add( Double.parseDouble(s) );
					size = 0;
				}
			} else {
				buffer[size++] = (char)v;
			}
			v = input.read();
		}
		if( size > 0 ) {
			String s = new String(buffer,0,size);
			sequence.add( Double.parseDouble(s) );
		}
	}
	
	private void skipLine() throws IOException {
		
		int v = input.read();
	
		while( v >= 0 && v != '\n' ) {
			v = input.read();
		}
	}

	public char getCommentChar() {
		return commentChar;
	}
}

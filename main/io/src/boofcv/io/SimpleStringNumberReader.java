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
public class SimpleStringNumberReader {

	char commentChar;
	Reader input;

	char buffer[] = new char[1024];
	List<Object> sequence;

	int where;
	
	Object current;

	public SimpleStringNumberReader(char commentChar) {
		this.commentChar = commentChar;
	}
	
	public boolean read( Reader input )  {
		this.input = input;
		try {
			sequence = new ArrayList<>();
			where = 0;
			int v = input.read();

			while( v >= 0 ) {
				if( v == commentChar ) {
					skipLine();
				} else {
					parseLine(v);
				}
				v = input.read();
			}
		} catch( IOException e ) {
			return false;
		}

		this.input = null;
		
		return true;
	}
	
	public int remainingTokens() {
		return sequence.size()-where;
	}
	
	public boolean next() {
		if( sequence.size() > where ) {
			current = sequence.get(where++);
			return true;
		}
		return false;
	}
	
	public boolean isString() {
		return current instanceof String;
	}
	
	public String getString() {
		return (String)current;
	}

	public double getDouble() {
		return (Double)current;
	}

	public String nextString() {
		if( !next() )
			throw new RuntimeException("There is no next token!");
		if( !isString())
			throw new RuntimeException("The token is a double not a string. "+current);
		return (String)current;
	}

	public double nextDouble() {
		if( !next() )
			throw new RuntimeException("There is no next token!");
		if( isString())
			throw new RuntimeException("The token is a string not a double. "+current);
		return (Double)current;
	}
	
	private void parseLine( int v ) throws IOException {
		int size = 0;

		while( v >= 0 && v != '\n' ) {
			if( Character.isWhitespace(v) ) {
				if( size > 0 ) {
					addString(new String(buffer,0,size));
					size = 0;
				}
			} else {
				buffer[size++] = (char)v;
			}
			v = input.read();
		}
		if( size > 0 ) {
			addString(new String(buffer,0,size));
		}
	}
	
	private void skipLine() throws IOException {
		
		int v = input.read();
	
		while( v >= 0 && v != '\n' ) {
			v = input.read();
		}
	}
	
	private void addString( String s ) {
		try {
			sequence.add( Double.parseDouble(s) );
		} catch( NumberFormatException e ) {
			sequence.add(s);
		}
	}

	public char getCommentChar() {
		return commentChar;
	}
}

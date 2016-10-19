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

package boofcv;

import java.io.*;


/**
 * Converts a file written for 64bit numbers into 32bit numbers by replacing keywords.
 *
 * @author Peter Abeles
 */
public class ConvertFile32From64 {

	InputStream in;
	PrintStream out;

	public ConvertFile32From64( File inputFile ) throws FileNotFoundException {
		in = new FileInputStream( inputFile );

		String inputName = inputFile.getAbsolutePath();
		String outputFileName = inputName.substring( 0, inputName.length() - 8 ) + "F32.java";

		out = new PrintStream( outputFileName );
	}

	public void process() throws IOException {
		int n;
		StringBuffer s = new StringBuffer( 1024 );
		boolean prevChar = false;

		// copyright is a special case and don't wont it turning Apache 2.0 info 2.0f
		copyTheCopyRight(s);

		while( ( n = in.read() ) != -1 ) {
			if( Character.isWhitespace( (char) n ) ) {
				if( prevChar ) {
					handleToken( s.toString() );
					s.delete( 0, s.length() );
					prevChar = false;
				}
				out.write( n );
			} else {
				prevChar = true;
				s.append( (char) n );
			}
		}

		if( prevChar ) {
			handleToken( s.toString() );
		}

		out.close();
		in.close();
	}

	private void copyTheCopyRight( StringBuffer s ) throws IOException {
		int n;

		while( ( n = in.read() ) != -1 ) {
			char c = (char)n;
			s.append( c );

			if( c == '\n') {
				out.print( s );
				boolean finished = s.length() == 4 && s.charAt(2) == '/';
				s.delete(0, s.length());
				if( finished )
					return;
			}

		}
	}

	private void handleToken( String s ) {
		// handle overrides where double should stay the same
		s = s.replaceAll( "/\\*\\*/double", "FIXED_DOUBLE" );
		s = s.replaceAll( "double", "float" );
		s = s.replaceAll( "Double", "Float" );
		s = s.replaceAll( "_F64", "_F32" );
		s = s.replaceAll( "DOUBLE_TEST_TOL", "FLOAT_TEST_TOL" );
		s = s.replaceAll( "DCONV_TOL_", "FCONV_TOL_" );
		s = s.replaceAll( "GrlConstants.PI", "GrlConstants.F_PI" );
//		s = s.replaceAll( "GrlConstants.PI2", "GrlConstants.F_PI2" );
//		s = s.replaceAll( "GrlConstants.PId2", "GrlConstants.F_PId2" );
		s = s.replaceAll( "GrlConstants.EPS", "GrlConstants.F_EPS" );
		s = replaceStartString( s, "Math.", "(float)Math." );
		s = replaceStartString( s, "-Math.", "(float)-Math." );
		s = replaceStartString( s, "rand.nextGaussian", "(float)rand.nextGaussian" );
		s = handleFloats( s );

		// put the doubles back in
		s = s.replaceAll( "FIXED_DOUBLE","/\\*\\*/double" );

		out.print( s );
	}

	/**
	 * Looks for a floating point constant number and tacks on a 'f' to the end
	 * to make it into a float and not a double.
	 */
	private String handleFloats( String input ) {
		String regex = "\\d+\\.+\\d+([eE][-+]?\\d+)?";

		return input.replaceAll( regex, "$0f" );
	}

	private String replaceStartString( String input, String from, String to ) {

		if( input.startsWith( from ) ) {
			return to + input.substring( from.length() );
		} else {
			return input;
		}
	}
}

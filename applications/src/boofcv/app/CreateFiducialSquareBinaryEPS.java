/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.app;

import org.ddogleg.struct.GrowQueue_I32;

import java.io.IOException;
import java.io.PrintStream;

/**
 * Outputs an EPS document describing a binary square fiducial that encodes the specified number
 *
 * @author Peter Abeles
 */
public class CreateFiducialSquareBinaryEPS  extends BaseFiducialSquareEPS {

	// list of the fiducial ID's it will print
	GrowQueue_I32 numbers = new GrowQueue_I32();

	@Override
	protected void printPatternDefinitions() {

		out.print(
				"  /sl "+(innerWidth/4)+" def\n" +
				"  /w0 0 def\n" +
				"  /w1 { w0 sl add} def\n" +
				"  /w2 { w1 sl add} def\n" +
				"  /w3 { w2 sl add} def\n");
		out.print("  /box {newpath moveto sl 0 rlineto 0 sl rlineto sl neg 0 rlineto closepath fill} def\n");

		for( int i = 0; i < numbers.size(); i++ ) {
			int patternNumber = numbers.get(i);

			out.print("  /"+getPatternPrintDef(i)+" {\n"+
					"% Block corner used to identify orientation\n" +
					"  0 0 box\n");
			for (int j = 0; j < 12; j++) {
				if( (patternNumber & (1<<j)) != 0 ) {
					box(out,j);
				}
			}
			out.print("} def\n");
		}
	}

	@Override
	protected int totalPatterns() {
		return numbers.size();
	}

	@Override
	protected void addPattern(String name) {
		int value = Integer.parseInt(name);
		if( value < 0 || value > 4095 )
			throw new IllegalArgumentException("Values must be tween 0 and 4095, inclusive");
		numbers.add( Integer.parseInt(name));
	}

	@Override
	protected String getPatternName(int num) {
		return ""+numbers.get(num);
	}

	@Override
	public String defaultOutputFileName() {
		if( numbers.size() == 1 )
			return "Fiducial"+numbers.get(0)+".eps";
		else
			return "BinaryFiducials.eps";
	}

	@Override
	public String selectEpsName() {
		if( numbers.size() == 1 )
			return ""+numbers.get(0);
		else
			return numbers.get(0)+" and more";
	}

	private static void box( PrintStream out , int bit ) {
		if( bit < 2 )
			bit++;
		else if( bit < 10 )
			bit += 2;
		else if( bit < 12 )
			bit += 3;
		else
			throw new RuntimeException("Bit must be between 0 and 11");

		int x = bit%4;
		int y = bit/4;

		String wx = "w"+x;
		String wy = "w"+y;

		out.print("  "+wx+" "+wy+" box\n");
	}

	public static void main(String[] args) throws IOException {

		CommandParserFiducialSquare parser = new CommandParserFiducialSquare("number");

		parser.setExampleNames("284","845");
		parser.execute(args,new CreateFiducialSquareBinaryEPS());
	}


}

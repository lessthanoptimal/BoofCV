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

import org.ddogleg.struct.GrowQueue_I64;

import java.io.IOException;
import java.io.PrintStream;

/**
 * Outputs an EPS document describing a binary square fiducial that encodes the specified number
 *
 * @author Peter Abeles
 */
public class CreateFiducialSquareBinary extends BaseFiducialSquare {

	// list of the fiducial ID's it will print
	GrowQueue_I64 numbers = new GrowQueue_I64();

	private int gridWidth = 4;

	@Override
	protected void printPatternDefinitions(final int startPattern, final int numberOfPatterns) {
		// This always gets printed, every page
		out.print("  /sl "+(innerWidth/gridWidth)+" def\n  /w0 0 def\n");
		// Handle different size grids.
		for(int i = 1; i < gridWidth; i++) {
			out.print("  /w" + i + " { w" + (i-1) + " sl add} def\n");
		}
		out.print("  /box {newpath moveto sl 0 rlineto 0 sl rlineto sl neg 0 rlineto closepath fill} def\n");

		// This one, we only print for the patterns specified
		for( int i = startPattern; i < startPattern + numberOfPatterns; i++ ) {
			long patternNumber = numbers.get(i);
			out.print("  /"+getPatternPrintDef(i)+" {\n"+
					"% Block corner used to identify orientation\n" +
					"  0 0 box\n");
			final int bitCount = numberOfElements() - 4;
			for (int j = 0; j < bitCount; j++) {
				if( (patternNumber & (1L<<j)) != 0 ) {
					box(out, j);
				}
			}
			out.print("} def\n");
		}
	}

	protected void printStandardPatternDefinitions() {

	}


	@Override
	protected int totalPatterns() {
		return numbers.size();
	}

	@Override
	protected void addPattern(String name) {
		final long maxValue = (long) Math.pow(2, numberOfElements() - 4) - 1;
		long value = Long.parseLong(name);
		if( value < 0 || value > maxValue)
			throw new IllegalArgumentException("Values must be tween 0 and " +maxValue + ", inclusive");
		numbers.add( value );
	}

	@Override
	protected String getPatternName(int num) {
		return ""+numbers.get(num);
	}

	@Override
	public String defaultOutputFileName() {
		if( numbers.size() == 1 )
			return "Fiducial"+numbers.get(0)+".ps";
		else
			return "BinaryFiducials.ps";
	}

	@Override
	public String selectDocumentName() {
		if( numbers.size() == 1 )
			return ""+numbers.get(0);
		else
			return numbers.get(0)+" and more";
	}

	private void box( PrintStream out , final int bit ) {

		int transitionBit0 = gridWidth-3;
		int transitionBit1 = transitionBit0 + gridWidth*(gridWidth-2);
		int transitionBit2 = transitionBit1 + gridWidth-2;

		final int adjustedBit;
		if( bit <= transitionBit0 )
			adjustedBit = bit + 1;
		else if( bit <= transitionBit1 )
			adjustedBit = bit + 2;
		else if( bit <= transitionBit2 )
			adjustedBit = bit + 3;
		else
			throw new RuntimeException("Bit must be between 0 and " + transitionBit2);

		int x = adjustedBit % gridWidth;
		int y = adjustedBit / gridWidth;
		out.print("  w" + x + " w" + y +" box\n");
	}

	private int numberOfElements() {
		return gridWidth*gridWidth;
	}


	public void setGridSize(int gridWidth) {
		this.gridWidth = gridWidth;
	}

	public static void main(String[] args) throws IOException {

		CommandParserFiducialSquare parser = new CommandParserFiducialSquare("number");
		parser.setIsBinary(true);
		parser.setExampleNames("284","845");
		parser.applicationDescription = "Generates postscript documents for square binary fiducials.";
		parser.execute(args,new CreateFiducialSquareBinary());
	}


}

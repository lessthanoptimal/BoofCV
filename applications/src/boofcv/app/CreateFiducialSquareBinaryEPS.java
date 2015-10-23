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

import boofcv.abst.fiducial.BinaryFiducialGridSize;
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

	private BinaryFiducialGridSize gridSize = BinaryFiducialGridSize.FOUR_BY_FOUR;

	@Override
	protected void printPatternDefinitions() {

		out.print("  /sl "+(innerWidth/gridSize.getWidth())+" def\n  /w0 0 def\n");
		// Handle different size grids.
		for(int i = 1; i < gridSize.getWidth(); i++) {
			out.print("  /w" + i + " { w" + (i-1) + " sl add} def\n");
		}
		out.print("  /box {newpath moveto sl 0 rlineto 0 sl rlineto sl neg 0 rlineto closepath fill} def\n");

		for( int i = 0; i < numbers.size(); i++ ) {
			int patternNumber = numbers.get(i);

			out.print("  /"+getPatternPrintDef(i)+" {\n"+
					"% Block corner used to identify orientation\n" +
					"  0 0 box\n");
			final int bitCount = gridSize.getNumberOfElements() - 4;
			for (int j = 0; j < bitCount; j++) {
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
		final int maxValue = (int) Math.pow(2, gridSize.getNumberOfElements() - 4) - 1;
		int value = Integer.parseInt(name);
		if( value < 0 || value > maxValue)
			throw new IllegalArgumentException("Values must be tween 0 and " +maxValue + ", inclusive");
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

	private void box( PrintStream out , final int bit ) {
		final int transitionBits[];
		switch (gridSize) {
			case THREE_BY_THREE:
				transitionBits = new int[]{ 0 ,3 , 4 };
				break;
			case FOUR_BY_FOUR:
				transitionBits = new int[]{ 1 ,9 , 11 };
				break;
			case FIVE_BY_FIVE:
				transitionBits = new int[]{ 1 ,17 , 20 };
				break;
			default:
				throw new RuntimeException("Unexpected grid size");
		}

		final int adjustedBit;
		if( bit <= transitionBits[0] )
			adjustedBit = bit + 1;
		else if( bit <= transitionBits[1] )
			adjustedBit = bit + 2;
		else if( bit <= transitionBits[2] )
			adjustedBit = bit + 3;
		else
			throw new RuntimeException("Bit must be between 0 and " + transitionBits[2]);

		int x = adjustedBit % gridSize.getWidth();
		int y = adjustedBit / gridSize.getWidth();
		out.print("  w" + x + " w" + y +" box\n");
	}

	@Override
	protected double getFiducialInnerWidth(double fiducialBoxWidth) {
		final double totalWidthInSquares = 2 + gridSize.getWidth() + 2; 	// border + bits + border squares.
		// the whole width of the fiducial (including border), broken into squares,
		// then the number of squares in the grid.
		return fiducialBoxWidth / totalWidthInSquares * gridSize.getWidth();
	}

	@Override
	protected double getBlackBorderWidth(double fiducialBoxWidth) {
		// To work properly with DetectFidualSquareBinary, we need to make
		// sure the border is a multiple of 2 the size of a single square inside
		// the fiducial.
		final double totalWidthInSquares = 2 + gridSize.getWidth() + 2; 	// border + bits + border squares.
		// the whole width of the fiducial, broken into squares, then two of those squares for the border width.
		return fiducialBoxWidth / totalWidthInSquares * 2;
	}

	public void setGridSize(BinaryFiducialGridSize gridSize) {
		this.gridSize = gridSize;
	}

	public static void main(String[] args) throws IOException {

		CommandParserFiducialSquare parser = new CommandParserFiducialSquare("number");
		parser.setIsBinary(true);
		parser.setExampleNames("284","845");
		parser.execute(args,new CreateFiducialSquareBinaryEPS());
	}


}

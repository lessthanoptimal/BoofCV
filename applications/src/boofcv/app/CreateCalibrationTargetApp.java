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

package boofcv.app;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 * Application for generating calibration targets
 *
 * @author Peter Abeles
 */
public class CreateCalibrationTargetApp {
	@Option(name="-t",aliases = {"--Type"}, usage="Type of calibration target.")
	String _type=null;
	PatternType type;

	@Option(name="-r",aliases = {"--Rows"}, usage="Number of rows")
	int rows=-1;

	@Option(name="-c",aliases = {"--Columns"}, usage="Number of columns")
	int columns=-1;

	@Option(name="-u",aliases = {"--Units"}, usage="Name of document units.  default: cm")
	String _unit = Unit.CENTIMETER.abbreviation;
	Unit unit;

	@Option(name="-p",aliases = {"--PaperSize"}, usage="Size of paper used.  See below for predefined document sizes.  "
	+"You can manually specify any size using the following notation. W:H  where W is the width and H is the height.  "
	+"Values of W and H is specified with <number><unit abbreviation>, e.g. 6cm or 6, the unit is optional.  If no unit"
	+"is specified the default document units are used.")
	String _paperSize = PaperSize.LETTER.name;
	PaperSize paperSize;

	@Option(name="-w",aliases = {"--ShapeWidth"}, usage="Width of the shape or diameter if a circle.  In document units.")
	double shapeWidth=-1;

	@Option(name="-s",aliases = {"--Space"}, usage="Spacing between the shapes.  In document units.")
	double shapeSpace=-1;

	@Option(name="-d",aliases = {"--CenterDistance"}, usage="Distance between circle centers.  In document units.")
	double centerDistance=-1;

	@Option(name="-o",aliases = {"--OutputName"}, usage="Name of output file.  E.g. chessboard for chessboard.ps")
	String fileName = "target";

	private static void printHelpExit( CmdLineParser parser ) {
		parser.getProperties().withUsageWidth(120);
		parser.printUsage(System.out);
		System.out.println();
		System.out.println("Target Types");
		for( PatternType p : PatternType.values() ) {
			System.out.println("  "+p);
		}
		System.out.println();
		System.out.println("Document Types");
		for( PaperSize p : PaperSize.values() ) {
			System.out.printf("  %12s  %5.0f %5.0f %s\n",p.getName(),p.width,p.height,p.unit.abbreviation);
		}
		System.out.println();
		System.out.println("Units");
		for( Unit u : Unit.values() ) {
			System.out.printf("  %12s  %3s\n",u,u.abbreviation);
		}

		System.exit(1);
	}

	private static void failExit( String message ) {
		System.err.println(message);
		System.exit(1);
	}

	private void finishParsing(CmdLineParser parser) {
		if( rows <= 0 || columns <= 0 || _type == null )
			printHelpExit(parser);
		for( PatternType p : PatternType.values() ) {
			if( _type.compareToIgnoreCase(p.name() ) == 0 ) {
				type = p;
				break;
			}
		}
		if( type == null ) {
			failExit("Must specify a known document type "+_type);
		}

		unit = Unit.lookup(_unit);
		if( unit == null ) {
			System.err.println("Must specify a valid unit or use default");
			System.exit(1);
		}
		paperSize = PaperSize.lookup(_paperSize);
		if( paperSize == null ) {
			String words[] = _paperSize.split(":");
			if( words.length != 2) failExit("Expected two value+unit separated by a :");
			LengthUnit w = new LengthUnit(words[0]);
			LengthUnit h = new LengthUnit(words[1]);
			if( w.unit != h.unit ) failExit("Same units must be specified for width and height");
			paperSize = new PaperSize(w.length,h.length,w.unit);
		}

		if( rows <= 0 || columns <= 0)
			failExit("Must the number of rows and columns");

		if( shapeWidth <= 0 )
			failExit("Must specify a shape width more than zero");

		switch( type ) {
			case BINARY_GRID:
			case SQUARE_GRID:
				if( centerDistance > 0 )
					failExit("Don't specify center distance for square type targets, use shape space instead");
				if( shapeSpace <= 0 )
					failExit("Must specify a space greater than zero");
				break;

			case CHESSBOARD:
				if( centerDistance > 0 )
					failExit("Don't specify center distance for chessboard targets");
				if( shapeSpace > 0 )
					failExit("Don't specify center distance for chessboard targets");
				break;

			case CIRCLE_ASYMMETRIC_GRID:
				if( shapeSpace > 0 )
					failExit("Don't specify space for circle type targets, use center distance instead");
				if( centerDistance <= 0 )
					failExit("Must specify a center distance greater than zero");
				break;
		}
	}

	public void run() {
		CreateCalibrationTargetGenerator generator =
				new CreateCalibrationTargetGenerator(fileName,paperSize,rows,columns,unit);

		switch( type ) {
			case CHESSBOARD:generator.chessboard(shapeWidth);break;
			case SQUARE_GRID:generator.squareGrid(shapeWidth,shapeSpace);break;
			case BINARY_GRID:generator.binaryGrid(shapeWidth,shapeSpace);break;
			case CIRCLE_ASYMMETRIC_GRID:generator.circleAsymmetric(shapeWidth,shapeSpace);break;
			default: throw new RuntimeException("Unknown target type");
		}

	}

	private enum PatternType {
		CHESSBOARD,
		SQUARE_GRID,
		BINARY_GRID,
		CIRCLE_ASYMMETRIC_GRID
	}

	public static void main(String[] args) {
		CreateCalibrationTargetApp generator = new CreateCalibrationTargetApp();
		CmdLineParser parser = new CmdLineParser(generator);
		try {
			parser.parseArgument(args);
			generator.finishParsing(parser);
			generator.run();
		} catch (CmdLineException e) {
			// handling of wrong arguments
			System.err.println(e.getMessage());
			printHelpExit(parser);
		}
	}

}

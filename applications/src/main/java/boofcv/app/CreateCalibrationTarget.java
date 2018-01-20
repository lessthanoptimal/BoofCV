/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.fiducial.calib.CalibrationPatterns;
import boofcv.misc.LengthUnit;
import boofcv.misc.Unit;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.IOException;

/**
 * Application for generating calibration targets
 *
 * @author Peter Abeles
 */
public class CreateCalibrationTarget {
	@Option(name="-t",aliases = {"--Type"}, usage="Type of calibration target.")
	String _type=null;
	CalibrationPatterns type;

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
	+" are specified the default document units are used.")
	String _paperSize = PaperSize.LETTER.name;
	PaperSize paperSize;

	@Option(name="-w",aliases = {"--ShapeWidth"}, usage="Width of the shape or diameter if a circle.  In document units.")
	float shapeWidth=-1;

	@Option(name="-s",aliases = {"--Space"}, usage="Spacing between the shapes.  In document units.")
	float shapeSpace=-1;

	@Option(name="-d",aliases = {"--CenterDistance"}, usage="Distance between circle centers.  In document units.")
	float centerDistance=-1;

	@Option(name="-o",aliases = {"--OutputName"}, usage="Name of output file.  E.g. chessboard for chessboard.pdf")
	String fileName = "target";

	@Option(name="-i",aliases = {"--DisablePrintInfo"}, usage="Disable printing information about the calibration target")
	boolean disablePrintInfo = false;

	@Option(name="--GUI", usage="Ignore all other command line arguments and switch to GUI mode")
	private boolean guiMode = false;

	private static void printHelpExit( CmdLineParser parser ) {
		parser.getProperties().withUsageWidth(120);
		parser.printUsage(System.out);
		System.out.println();
		System.out.println("Target Types");
		for( CalibrationPatterns p : CalibrationPatterns.values() ) {
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

		System.out.println();
		System.out.println("Examples:");
		System.out.println("-r 24 -c 28 -o target -t CIRCLE_HEXAGONAL -u cm -w 1 -d 1.2 -p LETTER");
		System.out.println("          circle hexagonal, grid 24x28, 1cm diameter, 1.2cm separation, on letter paper");
		System.out.println();
		System.out.println("-r 16 -c 12 -o target -t CIRCLE_GRID -u cm -w 1 -d 1.5 -p LETTER");
		System.out.println("          circle grid, grid 16x12, 1cm diameter, 1.5cm distance, on letter paper");
		System.out.println();
		System.out.println("-r 4 -c 3 -o target -t SQUARE_GRID -u cm -w 3 -s 3 -p LETTER");
		System.out.println("          square grid, grid 4x3, 3cm squares, 3cm space, on letter paper");
		System.out.println();
		System.out.println("-r 7 -c 5 -o target -t CHESSBOARD -u cm -w 3 -p LETTER");
		System.out.println("          chessboard, grid 7x5, 3cm squares, on letter paper");
		System.out.println();

		System.exit(1);
	}

	private static void failExit( String message ) {
		System.err.println(message);
		System.exit(1);
	}

	private void finishParsing(CmdLineParser parser) {
		if( guiMode ) {
			new CreateCalibrationTargetGui();
			return;
		}

		if( rows <= 0 || columns <= 0 || _type == null )
			printHelpExit(parser);
		for( CalibrationPatterns p : CalibrationPatterns.values() ) {
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
					shapeSpace = shapeWidth;
				break;

			case CHESSBOARD:
				if( centerDistance > 0 )
					failExit("Don't specify center distance for chessboard targets");
				if( shapeSpace > 0 )
					failExit("Don't specify center distance for chessboard targets");
				break;

			case CIRCLE_HEXAGONAL:
				if( shapeSpace > 0 )
					failExit("Don't specify space for circle type targets, use center distance instead");
				if( centerDistance <= 0 )
					centerDistance = shapeWidth*2;
				break;

			case CIRCLE_GRID:
				if( shapeSpace > 0 )
					failExit("Don't specify space for circle type targets, use center distance instead");
				if( centerDistance <= 0 )
					centerDistance = shapeWidth*2;
				break;
		}
	}

	public void run() throws IOException {
		String suffix = ".pdf";
		System.out.println("Saving to "+fileName+suffix);
		CreateCalibrationTargetGenerator generator =
				new CreateCalibrationTargetGenerator(fileName+suffix,paperSize,rows,columns,unit);

		generator.setShowInfo(!disablePrintInfo);

		System.out.println("   paper     : "+paperSize);
		System.out.println("   type      : "+type);
		System.out.println("   rows      : "+rows);
		System.out.println("   columns   : "+columns);
		System.out.println("   info      : "+(!disablePrintInfo));


		switch( type ) {
			case CHESSBOARD:generator.chessboard(shapeWidth);break;
			case SQUARE_GRID:generator.squareGrid(shapeWidth,shapeSpace);break;
			case BINARY_GRID:generator.binaryGrid(shapeWidth,shapeSpace);break;
			case CIRCLE_HEXAGONAL:generator.circleHexagonal(shapeWidth,centerDistance);break;
			case CIRCLE_GRID:generator.circleGrid(shapeWidth,centerDistance);break;
			default: throw new RuntimeException("Unknown target type");
		}

	}
	public static void main(String[] args) {
		CreateCalibrationTarget generator = new CreateCalibrationTarget();
		CmdLineParser parser = new CmdLineParser(generator);
		try {
			parser.parseArgument(args);
			generator.finishParsing(parser);
			if( !generator.guiMode )
				generator.run();
		} catch (CmdLineException e) {
			// handling of wrong arguments
			System.err.println(e.getMessage());
			printHelpExit(parser);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}

/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

import java.io.FileNotFoundException;
import java.io.PrintStream;

/**
 * Generates the actual calibration target.
 *
 * @author Peter Abeles
 */
public class CreateCalibrationTargetGenerator {

	PaperSize paper;
	int rows,cols;
	Unit units;

	PrintStream out;

	boolean showInfo = true;

	double patternWidth;
	double patternHeight;

	public double UNIT_TO_POINTS;
	public static final double CM_TO_POINTS = 72.0/2.54;

	public CreateCalibrationTargetGenerator( String documentName , PaperSize paper, int rows , int cols , Unit units ) {
		this.paper = paper;
		this.rows = rows;
		this.cols = cols;
		this.units = units;

		UNIT_TO_POINTS = units.getUnitToMeter()*100.0*CM_TO_POINTS;

		try {
			out = new PrintStream(documentName);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public void chessboard( double squareWidth ) {
		double squarePoints = squareWidth*UNIT_TO_POINTS;
		patternWidth = cols*squarePoints;
		patternHeight = rows*squarePoints;

		printHeader("Chessboard "+rows+"x"+cols+", squares "+squareWidth+" "+units.abbreviation);

		out.println(
				"  /w "+squarePoints+" def\n"+
				"  /ww {w 2 mul} def\n" +
				"  /patternWidth "+(patternWidth-squarePoints)+" def\n" +
				"  /patternHeight "+(patternHeight-squarePoints)+" def\n" +
				"\n" +
				"  % ----- Define procedure for drawing a box\n" +
				"  /box {w 0 rlineto 0 w rlineto -1 w mul 0 rlineto closepath} def\n" +
				"\n" +
				"  % draw all the boxes it can across a single row\n"+
				"  /rowboxes {ww patternWidth {newpath y moveto box fill} for} def\n" +
				"\n" +
				"  % increments the y variable and draws all the rows\n"+
				"  0 ww patternHeight { /y exch def 0 rowboxes } for\n"+
				"  w ww patternHeight { /y exch def w rowboxes } for");

		printTrailer();

	}

	public void squareGrid( double squareWidth , double spacing ) {
		double squarePoints = squareWidth*UNIT_TO_POINTS;
		double spacingPoints = spacing*UNIT_TO_POINTS;
		patternWidth = cols*squarePoints +(cols-1)*spacingPoints;
		patternHeight = rows*squarePoints+(rows-1)*spacingPoints;

		printHeader("Square Grid "+rows+"x"+cols+", squares "+squareWidth+", space "+spacing+" "+units.abbreviation);

		out.println(
				"  /w "+squarePoints+" def\n"+
				"  /s "+spacingPoints+" def\n"+
				"  /ww {w s add} def\n" +
				"  /patternWidth "+(patternWidth-squarePoints)+" def\n" +
				"  /patternHeight "+(patternHeight-squarePoints)+" def\n" +
				"\n" +
				"  % ----- Define procedure for drawing a box\n" +
				"  /box {w 0 rlineto 0 w rlineto -1 w mul 0 rlineto closepath} def\n" +
				"\n" +
				"  % draw all the boxes it can across a single row\n"+
				"  /rowboxes {ww patternWidth {newpath y moveto box fill} for} def\n" +
				"\n" +
				"  % increments the y variable and draws all the rows\n"+
				"  0 ww patternHeight { /y exch def 0 rowboxes } for\n");

		printTrailer();
	}

	public void binaryGrid( double squareWidth , double spacing ) {
		System.out.println("Binary grid not yet supported because the standard isn't fully defined yet");
		System.exit(0);
	}

	public void circleAsymmetric( double diameter , double centerDistance ) {
		double diameterPoints = diameter*UNIT_TO_POINTS;
		double separationPoints = centerDistance*UNIT_TO_POINTS;
		patternWidth = ((cols-1)/2.0)*separationPoints + diameterPoints;
		patternHeight = ((rows-1)/2.0)*separationPoints + diameterPoints;

		printHeader("Asymmetric Circle "+rows+"x"+cols+", diameter "+diameter+", separation "+centerDistance+" "+units.abbreviation);

		out.println(
				"  /w "+separationPoints+" def\n"+
				"  /s "+(separationPoints/2)+" def\n"+
				"  /r "+(diameterPoints/2)+" def\n"+
				"  /colA "+((cols+1)/2-1)+" def\n" +
				"  /rowA "+((rows+1)/2-1)+" def\n" +
				"  /colB "+(cols/2-1)+" def\n" +
				"  /rowB "+(rows/2-1)+" def\n" +

				"\n" +
				"  % ----- Define procedure for drawing a circle\n" +
				"  /circle  {r 0 360 arc closepath} def\n" +
				"\n" +
				"  % draw all the circles it can across a single row\n"+
				"  /rowcirclesA {colA {w mul r add y r add circle fill} for} def\n" +
				"  /rowcirclesB {colB {w mul r add s add y r add circle fill} for} def\n" +

				"\n" +
				"  % increments the y variable and draws all the rows\n"+
				"  0 1 rowA { /y exch w mul def 0 1 rowcirclesA } for\n"+
				"  0 1 rowB { /y exch w mul s add def 0 1 rowcirclesB } for\n");

		printTrailer();
	}

	public void circleGrid( double diameter , double centerDistance ) {
		double diameterPoints = diameter*UNIT_TO_POINTS;
		double separationPoints = centerDistance*UNIT_TO_POINTS;
		patternWidth = (cols-1)*separationPoints+diameterPoints;
		patternHeight = (rows-1)*separationPoints+diameterPoints;

		printHeader("Grid Circle "+rows+"x"+cols+", diameter "+diameter+", separation "+centerDistance+" "+units.abbreviation);

		out.println(
				"  /w "+separationPoints+" def\n"+
				"  /r "+(diameterPoints/2)+" def\n"+
				"  /patternWidth "+(patternWidth)+" def\n" +
				"  /patternHeight "+(patternHeight)+" def\n" +
				"\n" +
				"  % ----- Define procedure for drawing a circle\n" +
				"  /circle  {r 0 360 arc closepath} def\n" +
				"\n" +
				"  % draw all the circles it can across a single row\n"+
				"  /rowcircles {r w patternWidth {y circle fill} for} def\n" +
				"\n" +
				"  % increments the y variable and draws all the rows\n"+
				"  r w patternHeight { /y exch def rowcircles } for\n");

		printTrailer();
	}

	private void printHeader( String documentTitle ) {
		double pageWidth = paper.convertWidth(units)*UNIT_TO_POINTS;
		double pageHeight = paper.convertHeight(units)*UNIT_TO_POINTS;
		out.println("%!PS-Adobe-3.0\n" +
				"%%Creator: BoofCV\n" +
				"%%DocumentMedia: Plain "+pageWidth+" "+pageHeight+" 80 white ( )\n" +
				"%%Title: "+documentTitle+" on "+paper+"\n" +
				"%%DocumentData: Clean7Bit\n" +
				"%%LanguageLevel: 2\n" +
				"%%BeginSetup\n" +
				"  << /PageSize ["+pageWidth+" "+pageHeight+"] /Orientation 0 >> setpagedevice\n" +
				"%%EndSetup\n" +
				"%%EndComments\n" +
				"%%BeginProlog\n" +
				"%%EndProlog\n" +
				"%%Pages: 1\n");

		if( showInfo ) {
			double offX = Math.min(CM_TO_POINTS,(pageWidth-patternWidth)/4);
			double offY = Math.min(CM_TO_POINTS,(pageHeight-patternHeight)/4);

			out.print("  /Times-Roman findfont\n  7 scalefont setfont "+offX+" "+offY +
					String.format(" moveto (BoofCV: %s) show\n\n",documentTitle));
		}

		out.println("% Center the pattern on the page");
		out.println("  /offX "+(pageWidth-patternWidth)/2.0+" def");
		out.println("  /offY "+(pageHeight-patternHeight)/2.0+" def");
		out.println("  offX offY translate");
		out.println();
		out.println("  newpath");
		out.println("% Render the pattern");
	}

	private void printTrailer() {
		out.println("  showpage\n" +
				"%%Trailer\n" +
				"%%EOF\n");
		out.close();
	}

	public void setShowInfo(boolean showInfo) {
		this.showInfo = showInfo;
	}
}

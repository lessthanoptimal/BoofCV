/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial;

import java.io.FileNotFoundException;
import java.io.PrintStream;

/**
 * Outputs an EPS document describing a binary square fiducial that encodes the specified number
 *
 * @author Peter Abeles
 */
public class CreateSquareBinaryPatternEPS {

	public static String fileName = "pattern.eps";
	public static int number = 284;
	public static double width = 10;

	public static double CM_TO_POINTS = 72.0/2.54;

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

		String wx = "w"+(x+2);
		String wy = "w"+(y+2);

		out.print("  "+wx+" "+wy+" box\n");
	}

	public static void main(String[] args) throws FileNotFoundException {

		if( args.length == 2 ) {
			width = Double.parseDouble(args[0]);
			number = Integer.parseInt(args[1]);
		}
		number &= 0x0FFF;

		System.out.println("Target width "+width+" (cm)  number = "+number);

		// print out the selected number in binary for debugging purposes
		for (int i = 0; i < 12; i++) {
			if( (number & (1<<i)) != 0 ) {
				System.out.print("1");
			} else {
				System.out.print("0");
			}
		}
		System.out.println();

		PrintStream out = new PrintStream(fileName);

		double sideLength = width*CM_TO_POINTS;
		double squareLength = sideLength/6;
		double pageLength = sideLength+squareLength*2;

		out.println("%!PS-Adobe-3.0 EPSF-3.0\n" +
				"%%Creator: BoofCV\n" +
				"%%Title: Binary Fiducial #"+number+" w="+width+"cm\n" +
				"%%DocumentData: Clean7Bit\n" +
				"%%Origin: 0 0\n" +
//				"%%BoundingBox: xmin ymin xmax ymax\n" +
				"%%BoundingBox: 0 0 "+pageLength+" "+pageLength+"\n" +
				"%%LanguageLevel: 3\n" +
				"%%Pages: 1\n" +
				"%%Page: 1 1\n" +
				"  /w { 1 "+squareLength+" mul} def\n" +
				"  /w2 { w "+squareLength+" add} def\n" +
				"  /w3 { w2 "+squareLength+" add} def\n" +
				"  /w4 { w3 "+squareLength+" add} def\n" +
				"  /w5 { w4 "+squareLength+" add} def\n" +
				"  /w6 { w5 "+squareLength+" add} def\n" +
				"  /w7 { w6 "+squareLength+" add} def\n" +
				"  /w8 { w7 "+squareLength+" add} def\n" +
				"  /pagewidth "+sideLength+" def\n" +
				"  /box {newpath moveto w 0 rlineto 0 w rlineto w neg 0 rlineto closepath fill} def\n" +
				"% bottom top left right borders..\n" +
				"  newpath w w moveto w7 w lineto w7 w2 lineto w w2 lineto closepath fill\n" +
				"  newpath w w6 moveto w7 w6 lineto w7 w7 lineto w w7 lineto closepath fill\n" +
				"  newpath w w2 moveto w2 w2 lineto w2 w6 lineto w w6 lineto closepath fill\n" +
				"  newpath w6 w2 moveto w7 w2 lineto w7 w6 lineto w6 w6 lineto closepath fill\n" +
				"% Block corner used to identify orientation\n" +
				"  w2 w2 box\n" +
				"% information bits\n");

		for (int i = 0; i < 12; i++) {
			if( (number & (1<<i)) != 0 ) {
				box(out,i);
			}
		}

		// print out encoding information for convenience
		out.print("  /Times-Roman findfont\n" +
				"7 scalefont setfont w "+(pageLength-10)+" moveto (# "+number+"   "+width+" cm) show\n");
		out.print("  showpage\n" +
				"%%EOF\n");

	}
}

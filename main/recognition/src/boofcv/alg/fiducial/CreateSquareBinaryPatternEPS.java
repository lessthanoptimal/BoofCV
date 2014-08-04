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

		String wx = x==0?"w" : "w"+(x+1);
		String wy = y==0?"w" : "w"+(y+1);


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

		out.println("%!PS-Adobe-3.0 EPSF-3.0\n" +
				"%%Creator: BoofCV\n" +
				"%%Title: Binary Fiducial #"+number+" w="+width+"cm\n" +
				"%%DocumentData: Clean7Bit\n" +
				"%%Origin: 0 0\n" +
//				"%%BoundingBox: xmin ymin xmax ymax\n" +
				"%%BoundingBox: 0 0 "+sideLength+" "+sideLength+"\n" +
				"%%LanguageLevel: 3\n" +
				"%%Pages: 1\n" +
				"%%Page: 1 1\n" +
				"  /w "+squareLength+" def\n" +
				"  /w2 { 2 "+squareLength+" mul} def\n" +
				"  /w3 { 3 "+squareLength+" mul} def\n" +
				"  /w4 { 4 "+squareLength+" mul} def\n" +
				"  /w5 { 5 "+squareLength+" mul} def\n" +
				"  /w6 { 6 "+squareLength+" mul} def\n" +
				"  /pagewidth "+sideLength+" def\n" +
				"  /box {newpath moveto w 0 rlineto 0 w rlineto -1 w mul 0 rlineto closepath fill} def\n" +
				"% bottom top left right borders..\n" +
				"  newpath 0 0 moveto w6 0 lineto w6 w lineto 0 w lineto closepath fill\n" +
				"  newpath 0 w5 moveto w6 w5 lineto w6 w6 lineto 0 w6 lineto closepath fill\n" +
				"  newpath 0 w moveto w w lineto w w5 lineto 0 w5 lineto closepath fill\n" +
				"  newpath w5 w moveto w6 w lineto w6 w5 lineto w5 w5 lineto closepath fill\n" +
				"% Block corner used to identify orientation\n" +
				"  w w box\n" +
				"% information bits\n");

		for (int i = 0; i < 12; i++) {
			if( (number & (1<<i)) != 0 ) {
				box(out,i);
			}
		}

		out.print("  showpage\n" +
				"%%EOF\n");

	}
}

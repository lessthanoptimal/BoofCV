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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class which parses command-line arguments for generating fiducials and then configures the generator
 *
 * @author Peter Abeles
 */
public class CommandParserFiducialSquare {

	// what it should refer to a pattern as
	protected String nameOfPatterns;

	public String outputName = null;
	public int gridX = 1, gridY = 1;
	public double pageBorderX = Double.NaN, pageBorderY = Double.NaN;
	public double blackBorder = 0.25;
	public double offsetX = 0, offsetY = 0;
	public boolean autoCenter = true;
	public double whiteBorder = -1;
	public Unit units = Unit.CENTIMETER;
	public PaperSize paper = null;
	public boolean printInfo = false;
	public boolean printGrid = false;
	public boolean noBoundaryHack = false;
	public double fiducialWidth = -1;
	private boolean isBinary = false;

	// binary specific flags
	private int binaryGridSize = 4;

	public String applicationDescription;

	public List<String> patternNames = new ArrayList<String>();

	public List<String> exampleNames = new ArrayList<String>();

	public CommandParserFiducialSquare(String nameOfPatterns) {
		this.nameOfPatterns = nameOfPatterns;
	}

	public void printHelp() {

		String n0 = exampleNames.get(0);
		String n1 = exampleNames.get(1);

		System.out.println(applicationDescription);
		System.out.println();
		System.out.println();
		System.out.println("./application <optional flags> <fiducial width>  <"+nameOfPatterns+" 0> ... <"+nameOfPatterns+" N-1>");
		System.out.println();
		System.out.println("Optional Flags");
		System.out.println("-OutputFile=<name>    Specify name of output file.  Default is input file + ps");
		System.out.println("-Grid=fill            Automatically fill the paper with fiducials");
		System.out.println("-Grid=<rows>,<cols>   Create a grid of fiducials with the specified number of rows and columns");
		System.out.println("-WhiteBorder=<val>    Size of the white border around the fiducial.");
		System.out.println("-BlackBorder=<frac>   Specifies the fractional width of the fiducial's black border. default = 0.25");
		System.out.println("-PrintInfo            Will print the size and name of each fiducial.");
		System.out.println("-PrintGrid            Will draw a light gray grid around the fiducials");
		System.out.println("-NoBoundaryHack       By default an invisible rectangle around the document border is added.");
		System.out.println("                      The invisible border prevents some smart printers from cropping the document.");
		System.out.println("                      Using this flag will turn off this option.");
		System.out.println("-PageBorder=<x>,<y>   Specifies the border of the page in which it can't print. default = 1cm");
		System.out.println("-Offsets=<x>,<y>      Shift the fiducial/grid");
		System.out.println("                      Turns off auto centering");
		System.out.println("-Units=<unit>         Specify units used: mm, cm, m, inch, foot, yard");
		System.out.println("                      example: -Units=cm");
		System.out.println("-PageSize=<type>      Specify the page: A0, A1, A2, A3, A4, legal, letter");
		System.out.println("                      example: -PageSize=letter");
		if(isBinary) {
			System.out.println("-BinaryGridWidth=<n> Specify the width of the grid used to encode binary numbers. Valid values are 3 to 8");
			System.out.println("                     Maximum distinct fiducials: 3 is 32, 4 is 4096, 5 is 2,097,152. Default is 4");
			System.out.println("                     example: -BinaryGridSize=4");
		}
		System.out.println();
		System.out.println("Examples:");
		System.out.println("./application -PrintInfo -OutputFile=fiducial.ps 10 "+n0);
		System.out.println("         10cm fiducial using '"+n0+"' as the pattern with it's size and info");
		System.out.println("./application -Grid=fill -Units=inch -PageSize=letter 2.5 "+n0);
		System.out.println("         2.5 inch fiducial, filling letter sized paper with grid, '"+n0+"' as the pattern");
		System.out.println("./application -Grid=fill -Units=inch -PageSize=letter 2.5 "+n0+" "+n1);
		System.out.println("         same as the previous, but alternates between "+n0+" and "+n1+" patterns");
	}

	public int parseFlags( String []args) {
		int index = 0;

		for( ; index < args.length; index++ ) {
			String arg = args[index];
			if( arg.charAt(0) == '-' ) {
				String label = label(arg);

				if( "OutputFile".compareToIgnoreCase(label) == 0 ) {
					outputName = param(arg);
				} else if( "Grid".compareToIgnoreCase(label) == 0 ) {
					String right = param(arg);
					if( "fill".compareToIgnoreCase(right) == 0 ) {
						gridX = -1; gridY = -1;
					} else {
						String words[] = split(right);
						gridX = Integer.parseInt(words[0]);
						gridY = Integer.parseInt(words[1]);
					}
				} else if( "WhiteBorder".compareToIgnoreCase(label) == 0 ) {
					whiteBorder = Double.parseDouble(param(arg));
				} else if( "PrintInfo".compareToIgnoreCase(label) == 0 ) {
					printInfo = true;
				} else if( "PrintGrid".compareToIgnoreCase(label) == 0 ) {
					printGrid = true;
				} else if( "NoBoundaryHack".compareToIgnoreCase(label) == 0 ) {
					noBoundaryHack = true;
				} else if( "PageBorder".compareToIgnoreCase(label) == 0 ) {
					String words[] = split(param(arg));
					pageBorderX = Integer.parseInt(words[0]);
					pageBorderY = Integer.parseInt(words[1]);
				} else if( "BlackBorder".compareToIgnoreCase(label) == 0 ) {
					blackBorder = Double.parseDouble(param(arg));
					if( blackBorder <= 0 || blackBorder >= 0.5 ) {
						System.err.println("black border should be 0 < border < 0.5");
						System.exit(1);
					}
				} else if( "Offsets".compareToIgnoreCase(label) == 0 ) {
					String words[] = split(param(arg));
					offsetX = Integer.parseInt(words[0]);
					offsetY = Integer.parseInt(words[1]);
					autoCenter = false;
				} else if( "Units".compareToIgnoreCase(label) == 0 ) {
					units = Unit.lookup(param(arg));
				} else if( "PageSize".compareToIgnoreCase(label) == 0 ) {
					paper = PaperSize.lookup(param(arg));
				} else if( "BinaryGridWidth".compareToIgnoreCase(label) == 0 ) {
					binaryGridSize = Integer.parseInt(param(arg));
					if( binaryGridSize < 3 || binaryGridSize > 8 ) {
						System.err.println("binary grid must be >= 3 and <= 8");
						System.exit(1);
					}
				} else {
					throw new IllegalArgumentException("Unknown: "+label);
				}
			} else {
				break;
			}
		}
		return index;
	}



	public void execute( String []args , BaseFiducialSquare app) throws IOException {
		try {
			parseArguments(args);
		} catch( IllegalArgumentException e ) {
			printHelp();
			System.out.println();
			System.out.println(e.getMessage());
			System.exit(-1);
		}

		if( gridX < 0 && paper == null ) {
			printHelp();
			System.out.println();
			System.err.println("If grid is set to fill then the paper size must be specified");
			System.exit(-1);
		}

		System.out.println("################### Configuration");
		System.out.println("Output               "+outputName);
		System.out.println("Units                "+units);
		System.out.println("Fiducial Width       "+fiducialWidth);
		if( whiteBorder > -1 )
			System.out.println("White Border         "+whiteBorder);
		System.out.println("Black Border         "+blackBorder);
		System.out.println("Print Info           "+printInfo);
		System.out.println("Print Grid           "+printGrid);
		System.out.println("Boundary Hack        "+(!noBoundaryHack));
		if(isBinary) System.out.println("Binary Grid Size     "+ binaryGridSize + "x" + binaryGridSize);
		if( paper != null )	System.out.println("Paper Size           "+paper);
		if( gridX < 0)
			System.out.println("Grid                 automatic");
		else if( gridX > 1 && gridY > 1)
			System.out.printf("Grid                  rows = %2d cols = %2d",gridY,gridX);
		if( autoCenter)
			System.out.println("Auto centering");
		else
			System.out.printf("Offset                x = %f y = %f",offsetX,offsetY);
		if( !Double.isNaN(pageBorderX))
			System.out.printf("Page Border           x = %f y = %f", pageBorderX, pageBorderY);
		System.out.println();
		System.out.println("Patterns");
		for( String p : patternNames ) {
			System.out.println("  "+p);
		}

		System.out.println("################### Generating");

		if(app instanceof CreateFiducialSquareBinary) {
			((CreateFiducialSquareBinary) app).
					setGridSize(binaryGridSize);
		}

		for( String path : patternNames ) {
			app.addPattern(path);
		}

		app.setOutputFileName(outputName);

		app.setPrintInfo(printInfo);
		app.setPrintGrid(printGrid);
		app.setBoundaryHack(!noBoundaryHack);
		app.setUnit(units);
		if( !autoCenter ) {
			app.setCentering(false);
			app.setOffset(offsetX, offsetY, units);
		}
		if( !Double.isNaN(pageBorderX)) {
			app.setPageBorder(pageBorderX, pageBorderY, units);
		} else {
			app.setPageBorder(0,0, units);
		}
		app.setBlackBorderFractionalWidth(blackBorder);
		app.generateGrid(fiducialWidth,whiteBorder,gridX,gridY,paper);

		System.out.println("################### Finished");
	}

	public void parseArguments( String []args ) {
		int where = parseFlags(args);
		if( args.length-where < 2 )
			throw new IllegalArgumentException("Expected size followed by image list");

		fiducialWidth = Double.parseDouble(args[where++]);

		while( where < args.length ) {
			patternNames.add(args[where++]);
		}
	}

	public static String label( String arg ) {
		int end = 1;
		while( end < arg.length() ) {
			if( arg.charAt(end) == '=' ) {
				break;
			}
			end++;
		}
		return arg.substring(1,end);
	}

	public static String param( String arg ) {
		int where = 0;
		while( where < arg.length() ) {
			if( arg.charAt(where++) == '=' ) {
				return arg.substring(where,arg.length());
			}
		}
		throw new IllegalArgumentException("Couldn't find '=' in "+arg);
	}

	public static String[] split( String arg ) {
		int where = 0;
		while( where < arg.length() ) {
			if( arg.charAt(where++) == ',' ) {
				String left = arg.substring(0, where-1);
				String right = arg.substring(where,arg.length());
				return new String[]{left,right};
			}
		}
		throw new IllegalArgumentException("Couldn't find ',' in "+arg);
	}

	public void setExampleNames( String name0 , String name1 ) {
		exampleNames.add(name0);
		exampleNames.add(name1);
	}

	public void setIsBinary(boolean isBinary) {
		this.isBinary = isBinary;
	}
}

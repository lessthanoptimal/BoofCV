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

import boofcv.misc.LengthUnit;
import boofcv.misc.Unit;

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
	public LengthUnit pageBorderX = null, pageBorderY = null;
	public float blackBorder = 0.25f;
	public float offsetX = 0, offsetY = 0;
	public boolean autoCenter = true;
	public LengthUnit whiteBorder = null;
	public Unit units = Unit.CENTIMETER;
	public PaperSize paper = null;
	public boolean printInfo = false;
	public boolean printGrid = false;
	public LengthUnit fiducialWidth = null;
	private boolean isBinary = false;

	// binary specific flags
	private int binaryGridSize = 4;

	public String applicationDescription;

	public List<String> patternNames = new ArrayList<>();

	public List<String> exampleNames = new ArrayList<>();

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
		System.out.println("-OutputFile=<name>    Specify name of output file.  Default is <name>.pdf");
		System.out.println("-Grid=fill            Automatically fill the paper with fiducials");
		System.out.println("-Grid=<rows>,<cols>   Create a grid of fiducials with the specified number of rows and columns");
		System.out.println("-WhiteBorder=<val>    Size of the white border around the fiducial.");
		System.out.println("-BlackBorder=<frac>   Specifies the fractional width of the fiducial's black border. default = 0.25");
		System.out.println("-PrintInfo            Will print the size and name of each fiducial.");
		System.out.println("-PrintGrid            Will draw a light gray grid around the fiducials");
		System.out.println("-PageBorder=<x>,<y>   Specifies the border of the page in which it can't print. Default = 1cm");
		System.out.println("-Offsets=<x>,<y>      Shift the fiducial/grid");
		System.out.println("                      Turns off auto centering");
		System.out.println("-Units=<unit>         Specify units used: mm, cm, m, inch, foot, yard.  Default = cm");
		System.out.println("                      example: -Units=cm");
		System.out.println("-PageSize=<type>      Specify the page: A0, A1, A2, A3, A4, legal, letter");
		System.out.println("                      example: -PageSize=letter");
		System.out.println("-PageSize=<w>,<h>     Specify width and height manually.  Units allowed.  No units document default");
		System.out.println("                      example: -PageSize=10,20");
		System.out.println("                      example: -PageSize=10in,20in");
		if(isBinary) {
			System.out.println("-BinaryGridWidth=<n> Specify the width of the grid used to encode binary numbers. Valid values are 3 to 8");
			System.out.println("                     Maximum distinct fiducials: 3 is 32, 4 is 4096, 5 is 2,097,152. Default is 4");
			System.out.println("                     example: -BinaryGridSize=4");
		}
		System.out.println();
		System.out.println("Examples:");
		System.out.println("./application -PrintInfo -OutputFile=fiducial.pdf 10 "+n0);
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
					whiteBorder = new LengthUnit(param(arg));
				} else if( "PrintInfo".compareToIgnoreCase(label) == 0 ) {
					printInfo = true;
				} else if( "PrintGrid".compareToIgnoreCase(label) == 0 ) {
					printGrid = true;
				} else if( "PageBorder".compareToIgnoreCase(label) == 0 ) {
					String words[] = split(param(arg));
					pageBorderX = new LengthUnit(words[0]);
					pageBorderY = new LengthUnit(words[1]);
				} else if( "BlackBorder".compareToIgnoreCase(label) == 0 ) {
					blackBorder = Float.parseFloat(param(arg));
					if( blackBorder <= 0 || blackBorder >= 0.5 ) {
						System.err.println("black border should be 0 < border < 0.5");
						System.exit(1);
					}
				} else if( "Offsets".compareToIgnoreCase(label) == 0 ) {
					String words[] = split(param(arg));
					offsetX = Float.parseFloat(words[0]);
					offsetY = Float.parseFloat(words[1]);
					autoCenter = false;
				} else if( "Units".compareToIgnoreCase(label) == 0 ) {
					units = Unit.lookup(param(arg));
				} else if( "PageSize".compareToIgnoreCase(label) == 0 ) {
					paper = PaperSize.lookup(param(arg));
					if( paper == null ) {
						String words[] = split(param(arg));
						LengthUnit width = new LengthUnit(words[0]);
						LengthUnit height = new LengthUnit(words[1]);
						if( width.unit != height.unit )
							throw new IllegalArgumentException("Paper width and height must have the same unit");
						paper = new PaperSize(width.length,height.length,width.unit);
					}
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
		if( whiteBorder != null )
			System.out.println("White Border         "+whiteBorder);
		System.out.println("Black Border         "+blackBorder);
		System.out.println("Print Info           "+printInfo);
		System.out.println("Print Grid           "+printGrid);
		if(isBinary) System.out.println("Binary Grid Size     "+ binaryGridSize + "x" + binaryGridSize);
		if( paper != null )	System.out.println("Paper Size           "+paper);
		if( gridX < 0)
			System.out.println("Grid                 automatic");
		else if( gridX > 1 && gridY > 1)
			System.out.printf("Grid                 rows = %2d cols = %2d\n",gridY,gridX);
		if( autoCenter)
			System.out.println("Auto centering enabled");
		else
			System.out.printf("Offset                x = %f y = %f\n",offsetX,offsetY);
		if( pageBorderX != null)
			System.out.printf("Page Border           x = %s y = %s\n", pageBorderX, pageBorderY);
		System.out.println();
		System.out.println("Patterns");
		// compactly print out the patterns
		int maxPatternNameLength = 0;
		for( String p : patternNames ) {
			maxPatternNameLength = Math.max(maxPatternNameLength, p.length());
		}
		int l = 0;
		for( String p : patternNames ) {
			System.out.printf(" %"+maxPatternNameLength+"s",p);
			l += maxPatternNameLength;
			if( l > 80 ) {
				System.out.println();
			}
		}
		System.out.println();

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
		app.setUnit(units);
		if( !autoCenter ) {
			app.setCentering(false);
			app.setOffset(offsetX, offsetY, units);
		}
		if( pageBorderX != null ) {
			app.setPageBorder((float)pageBorderX.convert(units), (float)pageBorderY.convert(units), units);
		} else {
			app.setPageBorder(0,0, units);
		}
		double whiteBorderValue = whiteBorder==null?-1:whiteBorder.convert(units);
		app.setBlackBorderFractionalWidth(blackBorder);
		app.generateGrid((float)fiducialWidth.convert(units),(float)whiteBorderValue,gridX,gridY,paper);

		System.out.println("################### Finished");
	}

	public void parseArguments( String []args ) {
		int where = parseFlags(args);
		if( args.length-where < 2 )
			throw new IllegalArgumentException("Expected size followed by image list");

		fiducialWidth = new LengthUnit(args[where++]);

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

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

import boofcv.abst.distort.FDistort;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayU8;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Outputs an EPS document describing a binary square fiducial that encodes the specified number
 *
 * @author Peter Abeles
 */
public class CreateFiducialSquareImage extends BaseFiducialSquare {

	// Paths to image files containing fiducial patterns
	List<String> imagePaths = new ArrayList<>();

	@Override
	protected void printPatternDefinitions() {
		for( int i = 0; i < imagePaths.size(); i++ ) {
			String imageName = new File(imagePaths.get(i)).getName();
			GrayU8 image = UtilImageIO.loadImage(imagePaths.get(i), GrayU8.class);

			if( image == null ) {
				System.err.println("Can't read image.  Path = "+ imagePaths.get(i));
				System.exit(1);
			} else {
				System.out.println("  loaded "+imageName);
			}

			// make sure the image is square and divisible by 8
			int s = image.width - (image.width%8);
			if( image.width != s || image.height != s ) {
				GrayU8 tmp = new GrayU8(s, s);
				new FDistort(image, tmp).scaleExt().apply();
				image = tmp;
			}

			double scale = image.width/innerWidth;
			GrayU8 binary = ThresholdImageOps.threshold(image, null, threshold, false);
			if( showPreview )
				ShowImages.showWindow(VisualizeBinaryData.renderBinary(binary, false, null), "Binary Image");

			out.println();
			out.print("  /"+getPatternPrintDef(i)+" {\n" +
					"  "+binary.width+" " + binary.height + " 1 [" + scale + " 0 0 " + scale + " 0 0]\n" +
					"  {<"+binaryToHex(binary)+">} image\n" +
					"} def\n");
			out.println();
		}
	}

	@Override
	protected int totalPatterns() {
		return imagePaths.size();
	}

	@Override
	protected void addPattern(String name) {
		if( !new File(name).exists() ) {
			System.err.println("Image file does not exist.  "+name);
			System.exit(1);
		}
		this.imagePaths.add(name);
	}

	@Override
	protected String getPatternName(int num) {
		String n = new File(imagePaths.get(num)).getName();
		return n.substring(0,n.length()-4);
	}

	@Override
	public String defaultOutputFileName() {
		String inputPath = imagePaths.get(0);
		File dir = new File(inputPath).getParentFile();
		String outputName = new File(inputPath).getName();
		outputName = outputName.substring(0,outputName.length()-3) + "ps";
		try {
			outputName = new File(dir,outputName).getCanonicalPath();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return outputName;
	}

	@Override
	public String selectDocumentName() {
		if( imagePaths.size() == 1 ) {
			return new File(imagePaths.get(0)).getName();
		} else {
			return "Multiple Patterns";
		}
	}

	public static void main(String[] args) throws IOException {

		CommandParserFiducialSquare parser = new CommandParserFiducialSquare("image path");

		parser.applicationDescription = "Generates postscript documents for square image fiducials.";
		parser.setExampleNames("ke.png","chicken.png");
		parser.execute(args,new CreateFiducialSquareImage());
	}
}

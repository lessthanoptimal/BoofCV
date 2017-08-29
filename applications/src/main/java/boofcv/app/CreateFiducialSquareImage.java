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
import boofcv.alg.misc.PixelMath;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayU8;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.image.BufferedImage;
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
	protected void drawPattern(int patternID) throws IOException {
		String imageName = new File(imagePaths.get(patternID)).getName();
		GrayU8 image = UtilImageIO.loadImage(imagePaths.get(patternID), GrayU8.class);

		if( image == null ) {
			System.err.println("Can't read image.  Path = "+ imagePaths.get(patternID));
			System.exit(1);
//		} else {
//			System.out.println("  loaded "+imageName);
		}

		// make sure the image is square and divisible by 8
		int s = image.width - (image.width%8);
		if( image.width != s || image.height != s ) {
			GrayU8 tmp = new GrayU8(s, s);
			new FDistort(image, tmp).scaleExt().apply();
			image = tmp;
		}

		GrayU8 binary = ThresholdImageOps.threshold(image, null, threshold, false);
		if( showPreview )
			ShowImages.showWindow(VisualizeBinaryData.renderBinary(binary, false, null), "Binary Image");

		PixelMath.multiply(binary,255,binary);

		BufferedImage buffered = new BufferedImage(binary.width,binary.height,BufferedImage.TYPE_BYTE_GRAY);
		ConvertBufferedImage.convertTo(binary,buffered,true);

		PDImageXObject pdImage = JPEGFactory.createFromImage(document,buffered);
		pcs.drawImage(pdImage, 0, 0,innerWidth,innerWidth);
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
		outputName = outputName.substring(0,outputName.length()-3) + "pdf";
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

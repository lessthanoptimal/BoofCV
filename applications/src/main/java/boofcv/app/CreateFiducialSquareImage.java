/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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
import boofcv.app.fiducials.CreateFiducialDocumentImage;
import boofcv.app.fiducials.CreateFiducialDocumentPDF;
import boofcv.app.fiducials.CreateSquareFiducialDocumentImage;
import boofcv.app.fiducials.CreateSquareFiducialDocumentPDF;
import boofcv.gui.BoofSwingUtil;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayU8;
import org.apache.commons.io.FilenameUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.StringArrayOptionHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Outputs an PDF document describing a binary square fiducial that encodes the specified number
 *
 * @author Peter Abeles
 */
public class CreateFiducialSquareImage extends BaseFiducialSquareBorder {

	// Paths to image files containing fiducial patterns
	@Option(name = "-i", aliases = {"--Images"}, usage = "Path to images", handler = StringArrayOptionHandler.class)
	List<String> imagePaths = new ArrayList<>();

	@Option(name = "--Threshold", usage = "Threshold used to convert images into binary")
	int threshold = 128;

	@Override
	protected void callRenderPdf( CreateFiducialDocumentPDF renderer ) throws IOException {
		((CreateSquareFiducialDocumentPDF)renderer).render(getNames(), loadPatterns());
	}

	@Override
	protected void callRenderImage( CreateFiducialDocumentImage renderer ) {
		((CreateSquareFiducialDocumentImage)renderer).render(getNames(), loadPatterns());
	}

	private List<GrayU8> loadPatterns() {
		List<GrayU8> images = new ArrayList<>();
		for (int i = 0; i < imagePaths.size(); i++) {
			images.add(loadPattern(i));
		}
		return images;
	}

	private List<String> getNames() {
		List<String> names = new ArrayList<>();

		for (int i = 0; i < this.imagePaths.size(); i++) {
			names.add(FilenameUtils.getName(imagePaths.get(i)));
		}
		return names;
	}

	protected GrayU8 loadPattern( int patternID ) {
		GrayU8 image = UtilImageIO.loadImage(imagePaths.get(patternID), GrayU8.class);

		if (image == null) {
			System.err.println("Can't read image. Path = " + imagePaths.get(patternID));
			System.exit(1);
			throw new RuntimeException("Stupid compiler");
		}

		// If it is larger than 240x240 resize it
		if (image.width > 240 || image.height > 240) {
			GrayU8 tmp = new GrayU8(Math.min(image.width, 240), Math.min(image.height, 240));
			new FDistort(image, tmp).scaleExt().apply();
			image = tmp;
		}

		// it will threshold again later on, but here we can use the user selected threshold
		GrayU8 binary = ThresholdImageOps.threshold(image, null, threshold, false);
		PixelMath.multiply(binary, 255, binary);

		return binary;
	}

	@Override
	protected void printHelp( CmdLineParser parser ) {
		super.printHelp(parser);

		System.out.println("Creates two images in PNG format 220x220 pixels, 20 pixel white border");
		System.out.println("-i patternA.png -i patternB.png -w 200 -s 20 -o binary.png");
		System.out.println();
		System.out.println("Creates a PDF document the fills in a grid from these two fiducials");
		System.out.println("5cm with 2cm space between fiducials.");
		System.out.println("-i patternA.png -i patternB.png -u cm -w 5 -s 2 --GridFill -o binary.pdf");
		System.out.println();
		System.out.println("Opens a GUI");
		System.out.println("--GUI");


		System.exit(-1);
	}

	public static void main( String[] args ) throws IOException {
		CreateFiducialSquareImage generator = new CreateFiducialSquareImage();
		CmdLineParser parser = new CmdLineParser(generator);

		if (args.length == 0) {
			generator.printHelp(parser);
		}

		try {
			parser.parseArgument(args);
			if (generator.guiMode) {
				BoofSwingUtil.invokeNowOrLater(CreateFiducialSquareImageGui::new);
			} else {
				if (generator.imagePaths.isEmpty()) {
					System.err.println("Must specify at least one image");
					System.exit(1);
				}
				generator.finishParsing();
				generator.run();
			}
		} catch (CmdLineException e) {
			// handling of wrong arguments
			generator.printHelp(parser);
			System.err.println(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

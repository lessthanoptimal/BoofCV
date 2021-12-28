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

import boofcv.alg.filter.misc.AverageDownSampleOps;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/**
 * Loads a set of images, resizes them to a smaller size using an intelligent algorithm then saves them.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class BatchDownsizeImage {

	@Option(name = "-i", aliases = {"--Input"}, usage = "Directory or glob pattern or regex pattern.\n" +
			"Glob example: 'glob:data/**/left*.jpg'\n" +
			"Regex example: 'regex:data/\\w+/left\\d+.jpg'\n" +
			"If not a pattern then it's assumed to be a path. All files with known image extensions in their name as added, e.g. jpg, png")
	String inputPattern;
	@Option(name = "-o", aliases = {"--Output"}, usage = "Path to output directory")
	String outputPath;
	@Option(name = "--Rename", usage = "Rename files")
	boolean rename;
	@Option(name = "--GUI", usage = "Ignore all other command line arguments and switch to GUI mode")
	private boolean guiMode = false;
	@Option(name = "-w", aliases = {"--Width"}, usage = "Sets output width. If zero then aspect is matched with height")
	int width = 0;
	@Option(name = "-h", aliases = {"--Height"}, usage = "Sets output height. If zero then aspect is matched with width")
	int height = 0;
	@Option(name = "--MaxLength", usage = "Indicates that if only one dimension is set then that's the size of the largest side")
	boolean maxLength = false;
	@Option(name = "--PixelCount", usage = "Indicates it will attempt to match the number of pixels in both images")
	boolean pixelCount = false;

	Listener listener;
	boolean cancel;

	public static void printHelpExit( CmdLineParser parser ) {
		parser.getProperties().withUsageWidth(120);
		parser.printUsage(System.out);

		System.out.println();
		System.out.println("Examples:");
		System.out.println();
		System.exit(1);
	}

	public void process() {
		cancel = false;
		if (width == 0 && height == 0) {
			throw new RuntimeException("Need to specify at least a width or height");
		}

		System.out.println("width          = " + width);
		System.out.println("height         = " + height);
		System.out.println("max length     = " + maxLength);
		System.out.println("pixel count    = " + pixelCount);
		System.out.println("rename         = " + rename);
		System.out.println("input pattern  = " + inputPattern);
		System.out.println("output dir     = " + outputPath);

		List<String> paths = UtilIO.listSmartImages(inputPattern, true);

		if (paths.isEmpty())
			System.out.println("No inputs found. Bad path or pattern? " + inputPattern);

		// Create the output directory if it doesn't exist
		if (!new File(outputPath).exists()) {
			BoofMiscOps.checkTrue(new File(outputPath).mkdirs());
		}

		Planar<GrayU8> planar = new Planar<>(GrayU8.class, 1, 1, 1);
		Planar<GrayU8> small = new Planar<>(GrayU8.class, 1, 1, 1);
		int numDigits = BoofMiscOps.numDigits(paths.size() - 1);
		String format = "%0" + numDigits + "d";
		for (int i = 0; i < paths.size(); i++) {
			File file = new File(paths.get(i));
			System.out.print("processing " + file.getName());
			BufferedImage orig = UtilImageIO.loadImage(file.getAbsolutePath());
			if (orig == null) {
				throw new RuntimeException("Can't load file: " + file.getAbsolutePath());
			}

			int smallWidth, smallHeight;

			if (pixelCount) {
				int desired = width*height;
				if (desired <= 0)
					desired = Math.max(width, height);

				double scale = Math.sqrt(desired)/Math.sqrt(orig.getWidth()*orig.getHeight());

				// make sure it won't enlarge the image
				scale = Math.min(1.0, scale);

				smallWidth = (int)Math.round(scale*orig.getWidth());
				smallHeight = (int)Math.round(scale*orig.getHeight());
			} else if (maxLength && (width == 0 || height == 0)) {
				int largestSide = Math.max(orig.getWidth(), orig.getHeight());
				int desired = Math.max(width, height);

				smallWidth = orig.getWidth()*desired/largestSide;
				smallHeight = orig.getHeight()*desired/largestSide;
			} else {
				if (width == 0) {
					smallWidth = orig.getWidth()*height/orig.getHeight();
				} else {
					smallWidth = width;
				}
				if (height == 0) {
					smallHeight = orig.getHeight()*width/orig.getWidth();
				} else {
					smallHeight = height;
				}
			}
			System.out.println("   " + smallWidth + " x " + smallHeight);

			if (smallWidth > orig.getWidth() || smallHeight > orig.getHeight()) {
				System.out.println("Skipping " + file.getName() + " because it is too small");
			}

			if (listener != null)
				listener.loadedImage(orig, file.getName());

			String nameOut;
			if (rename) {
				nameOut = String.format("image" + format + ".png", i);
			} else {
				nameOut = file.getName().split("\\.")[0] + "_small.png";
			}

			planar.reshape(orig.getWidth(), orig.getHeight());
			ConvertBufferedImage.convertFrom(orig, planar, true);

			small.reshape(smallWidth, smallHeight, planar.getNumBands());

			if (small.width < planar.width && small.height < planar.height) {
				AverageDownSampleOps.down(planar, small);
			} else {
				small.setTo(planar);
			}

			BufferedImage output = ConvertBufferedImage.convertTo(small, null, true);

			UtilImageIO.saveImage(output, new File(outputPath, nameOut).getAbsolutePath());

			if (cancel) {
				break;
			}
		}
		if (listener != null)
			listener.finishedConverting();
	}

	public interface Listener {
		void loadedImage( BufferedImage image, String name );

		void finishedConverting();
	}

	public static void main( String[] args ) {
		BatchDownsizeImage generator = new BatchDownsizeImage();
		CmdLineParser parser = new CmdLineParser(generator);

		if (args.length == 0) {
			printHelpExit(parser);
		}

		try {
			parser.parseArgument(args);
			if (generator.guiMode) {
				new BatchDownsizeImageGui();
			} else {
				generator.process();
			}
		} catch (CmdLineException e) {
			// handling of wrong arguments
			System.err.println(e.getMessage());
			printHelpExit(parser);
		}
	}
}

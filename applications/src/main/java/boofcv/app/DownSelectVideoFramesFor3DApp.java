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
import boofcv.alg.video.SelectFramesForReconstruction3D;
import boofcv.core.image.ConvertImage;
import boofcv.factory.structure.ConfigSelectFrames3D;
import boofcv.factory.structure.FactorySceneReconstruction;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.image.UtilImageIO;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.io.wrapper.images.LoadFileImageSequence;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import org.ddogleg.struct.DogArray_I32;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Selects only frames from an image sequence which contribute 3D information. This is used to reduce file size
 * and potentially improve 3D reconstruction. It will also reduce the image resolution.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class DownSelectVideoFramesFor3DApp {
	ConfigSelectFrames3D config = new ConfigSelectFrames3D();

	@Option(name = "-i", aliases = {"--Input"}, usage = "Path to input directory or file")
	String pathInput;

	@Option(name = "-o", aliases = {"--Output"}, usage = "Path to output directory.")
	String pathOutput = "output";

	@Option(name = "--GUI", usage = "Ignore all other command line arguments and switch to GUI mode")
	boolean guiMode = false;

	@Option(name = "-w", aliases = {"--Width"}, usage = "Sets output width. If zero then aspect is matched with height")
	int width = 0;
	@Option(name = "-h", aliases = {"--Height"}, usage = "Sets output height. If zero then aspect is matched with width")
	int height = 0;
	@Option(name = "--MaxLength", usage = "Indicates that if only one dimension is set then that's the size of the largest side")
	boolean maxLength = false;

	@Option(name = "--Motion", usage = "Simple test used to see if scene is static or clearly not 3D. Pixels.")
	double motionPx = config.motionInlierPx;

	@Option(name = "--MinMotion", usage = "Minimum motion before a keyframe is allowed. Ratio.")
	double minMotion = config.minTranslation.fraction;

	@Option(name = "--MaxMotion", usage = "Maximum motion before a keyframe is forced. Ratio.")
	double maxMotion = config.maxTranslation.fraction;

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
		System.out.println("input path     = " + pathInput);
		System.out.println("output dir     = " + pathOutput);

		SimpleImageSequence<Planar<GrayU8>> sequence;

		int numImages = -1;
		if (new File(pathInput).isFile()) {
			sequence = DefaultMediaManager.INSTANCE.openVideo(pathInput, ImageType.PL_U8);
			if (sequence == null) {
				System.err.println("Failed to load video: " + pathInput);
				System.exit(1);
				throw new RuntimeException("Stupid null check");
			}
		} else {
			var fileSequence = new LoadFileImageSequence<>(ImageType.PL_U8, pathInput, null);
			numImages = fileSequence.getTotalImages();
			sequence = fileSequence;
		}

		// Create the output directory if it doesn't exist
		if (!new File(pathOutput).exists()) {
			new File(pathOutput).mkdirs();
		}

		Planar<GrayU8> scaled = new Planar<>(GrayU8.class, 1, 1, 1);
		int numDigits = numImages > 0 ? BoofMiscOps.numDigits(numImages - 1) : 4;
		String format = "%0" + numDigits + "d";

		int smallWidth, smallHeight;

		if (maxLength && (width == 0 || height == 0)) {
			int largestSide = Math.max(sequence.getWidth(), sequence.getHeight());
			int desired = Math.max(width, height);
			double scale = desired/(double)largestSide;

			if (scale < 1.0) {
				smallWidth = (int)(sequence.getWidth()*scale + 0.5);
				smallHeight = (int)(sequence.getHeight()*scale + 0.5);
			} else {
				smallWidth = sequence.getWidth();
				smallHeight = sequence.getHeight();
			}
		} else {
			// Impossible for both width and height to be zero
			double scale = 1.0;
			if (width == 0) {
				scale = Math.min(scale, height/(double)sequence.getHeight());
			}
			if (height == 0) {
				scale = Math.min(scale, width/(double)sequence.getWidth());
			}
			smallWidth = (int)(scale*sequence.getWidth() + 0.5);
			smallHeight = (int)(scale*sequence.getHeight() + 0.5);
		}

		config.motionInlierPx = motionPx;
		config.minTranslation.setRelative(minMotion, 0);
		config.maxTranslation.setRelative(maxMotion, 20);

		SelectFramesForReconstruction3D<GrayU8> selector =
				FactorySceneReconstruction.frameSelector3D(config, ImageType.SB_U8);
		selector.initialize(smallWidth, smallHeight);
		selector.setVerbose(System.out, null);

		GrayU8 gray = new GrayU8(1, 1);
		while (sequence.hasNext()) {
			Planar<GrayU8> original = sequence.next();
			System.out.println("Selector " + sequence.getFrameNumber());

			if (smallWidth == original.getWidth() && smallHeight == original.getHeight()) {
				scaled.setTo(original);
			} else {
				scaled.reshape(smallWidth, smallHeight, original.getNumBands());
				AverageDownSampleOps.down(original, scaled);
			}

			ConvertImage.average(scaled, gray);

			selector.next(gray);

			if (cancel) {
				break;
			}
		}

		DogArray_I32 selected = selector.getSelectedFrames();
		int selectedIndex = 0;
		sequence.reset();

		while (sequence.hasNext() && selectedIndex < selected.size) {
			Planar<GrayU8> original = sequence.next();
			System.out.println("Reading " + sequence.getFrameNumber());

			if (selected.get(selectedIndex) != sequence.getFrameNumber())
				continue;

			System.out.println("  saving.... " + selectedIndex);

			if (smallWidth > original.getWidth() || smallHeight > original.getHeight()) {
				scaled.setTo(original);
			} else {
				scaled.reshape(smallWidth, smallHeight, original.getNumBands());
				AverageDownSampleOps.down(original, scaled);
			}

			String nameOut = String.format("image" + format + ".png", selectedIndex);

			BufferedImage output = ConvertBufferedImage.convertTo(scaled, null, true);

			UtilImageIO.saveImage(output, new File(pathOutput, nameOut).getAbsolutePath());

			selectedIndex++;

			if (cancel) {
				break;
			}
		}
	}

	public static void main( String[] args ) {
		var generator = new DownSelectVideoFramesFor3DApp();
		var parser = new CmdLineParser(generator);

		if (args.length == 0) {
			printHelpExit(parser);
		}

		try {
			parser.parseArgument(args);
			if (generator.guiMode) {
				throw new RuntimeException("Implement GUI");
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

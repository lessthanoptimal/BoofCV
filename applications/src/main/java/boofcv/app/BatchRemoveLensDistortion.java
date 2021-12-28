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

import boofcv.alg.distort.AdjustmentType;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.LensDistortionOps;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.border.BorderType;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/**
 * Removes lens distortion from all the images in a directory
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class BatchRemoveLensDistortion {

	@Option(name = "-c", aliases = {"--Camera"},
			usage = "Path to camera intrinsics yaml")
	String pathIntrinsic;
	@Option(name = "-i", aliases = {"--Input"}, usage = "Directory or glob pattern or regex pattern.\n" +
			"Glob example: 'glob:data/**/left*.jpg'\n" +
			"Regex example: 'regex:data/\\w+/left\\d+.jpg'\n" +
			"If not a pattern then it's assumed to be a path. All files with known image extensions in their name as added, e.g. jpg, png")
	String inputPattern;
	@Option(name = "-o", aliases = {"--Output"}, usage = "Path to output directory")
	String outputPath;
	@Option(name = "--Rename", usage = "Rename files")
	boolean rename;
	@Option(name = "-a", aliases = {"--Adjustment"}, usage = "none, expand, full_view")
	String adjustmentName;
	AdjustmentType adjustmentType;
	@Option(name = "--GUI", usage = "Ignore all other command line arguments and switch to GUI mode")
	private boolean guiMode = false;

	boolean cancel = false;
	Listener listener;

	public BatchRemoveLensDistortion() {
	}

	public BatchRemoveLensDistortion( String pathIntrinsic, String inputPattern, String outputPath,
									  boolean rename, AdjustmentType adjustmentType,
									  Listener listener ) {
		this.pathIntrinsic = pathIntrinsic;
		this.inputPattern = inputPattern;
		this.outputPath = outputPath;
		this.rename = rename;
		this.adjustmentType = adjustmentType;
		this.listener = listener;
	}

	private static void printHelpExit( CmdLineParser parser ) {
		parser.getProperties().withUsageWidth(120);
		parser.printUsage(System.out);

		System.out.println();
		System.out.println("Examples:");
		System.out.println();
		System.out.println("-c /path/to/intrinsic.yaml -i ~/path/to/input/ -o /path/to/output -a full_view -r .*\\.jpg");

		System.exit(1);
	}

	public void finishParsing() {
		if (adjustmentName.compareToIgnoreCase("none") == 0) {
			adjustmentType = AdjustmentType.NONE;
		} else if (adjustmentName.compareToIgnoreCase("expand") == 0) {
			adjustmentType = AdjustmentType.EXPAND;
		} else if (adjustmentName.compareToIgnoreCase("full_view") == 0) {
			adjustmentType = AdjustmentType.FULL_VIEW;
		} else {
			throw new RuntimeException("Unknown adjustment " + adjustmentName);
		}
	}

	public void process() {
		cancel = false;
		System.out.println("AdjustmentType = " + adjustmentType);
		System.out.println("rename         = " + rename);
		System.out.println("input pattern  = " + inputPattern);
		System.out.println("output dir     = " + outputPath);


		File fileOutputDir = new File(outputPath);
		if (!fileOutputDir.exists()) {
			if (!fileOutputDir.mkdirs()) {
				throw new RuntimeException("Output directory did not exist and failed to create it");
			} else {
				System.out.println("  created output directory");
			}
		}

		CameraPinholeBrown param = CalibrationIO.load(pathIntrinsic);
		CameraPinholeBrown paramAdj = new CameraPinholeBrown();

		List<String> paths = UtilIO.listSmartImages(inputPattern, false);

		if (paths.isEmpty())
			System.out.println("No inputs found. Bad path or pattern? " + inputPattern);

		System.out.println("Found a total of " + paths.size() + " matching files");

		Planar<GrayF32> distoredImg = new Planar<>(GrayF32.class, param.width, param.height, 3);
		Planar<GrayF32> undistoredImg = new Planar<>(GrayF32.class, param.width, param.height, 3);

		ImageDistort distort = LensDistortionOps.changeCameraModel(adjustmentType, BorderType.ZERO, param,
				new CameraPinhole(param), paramAdj, (ImageType)distoredImg.getImageType());
		CalibrationIO.save(paramAdj, new File(outputPath, "intrinsicUndistorted.yaml").getAbsolutePath());

		BufferedImage out = new BufferedImage(param.width, param.height, BufferedImage.TYPE_INT_RGB);

		int numDigits = BoofMiscOps.numDigits(paths.size() - 1);
		String format = "%0" + numDigits + "d";
		for (int i = 0; i < paths.size(); i++) {
			File file = new File(paths.get(i));
			System.out.println("processing " + file.getName());
			BufferedImage orig = UtilImageIO.loadImage(file.getAbsolutePath());
			if (orig == null) {
				throw new RuntimeException("Can't load file: " + file.getAbsolutePath());
			}

			if (orig.getWidth() != param.width || orig.getHeight() != param.height) {
				System.err.println("intrinsic parameters and image size do not match!");
				System.exit(-1);
			}

			if (listener != null)
				listener.loadedImage(orig, file.getName());

			ConvertBufferedImage.convertFromPlanar(orig, distoredImg, true, GrayF32.class);
			distort.apply(distoredImg, undistoredImg);
			ConvertBufferedImage.convertTo(undistoredImg, out, true);

			String nameOut;
			if (rename) {
				nameOut = String.format("image" + format + ".png", i);
			} else {
				nameOut = file.getName().split("\\.")[0] + "_undistorted.png";
			}

			UtilImageIO.saveImage(out, new File(outputPath, nameOut).getAbsolutePath());

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
		BatchRemoveLensDistortion generator = new BatchRemoveLensDistortion();
		CmdLineParser parser = new CmdLineParser(generator);

		if (args.length == 0) {
			printHelpExit(parser);
		}

		try {
			parser.parseArgument(args);
			if (generator.guiMode) {
				new BatchRemoveLensDistortionGui();
			} else {
				generator.finishParsing();
				generator.process();
			}
		} catch (CmdLineException e) {
			// handling of wrong arguments
			System.err.println(e.getMessage());
			printHelpExit(parser);
		}
	}
}

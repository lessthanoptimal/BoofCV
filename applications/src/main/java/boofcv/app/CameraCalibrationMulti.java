/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.fiducial.calib.CalibrationPatterns;
import boofcv.abst.fiducial.calib.ConfigECoCheckMarkers;
import boofcv.abst.geo.calibration.CalibrateMultiPlanar;
import boofcv.abst.geo.calibration.DetectSingleFiducialCalibration;
import boofcv.alg.fiducial.calib.ConfigCalibrationTarget;
import boofcv.alg.geo.calibration.SynchronizedCalObs;
import boofcv.factory.fiducial.FactoryFiducialCalibration;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.MultiCameraCalibParams;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageDimension;
import boofcv.struct.image.ImageType;
import org.ddogleg.struct.DogArray_I32;
import org.jetbrains.annotations.NotNull;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Camera calibration for an arbitrary number of cameras
 *
 * @author Peter Abeles
 */
public class CameraCalibrationMulti {
	public final ConfigCalibrationTarget configTarget = new ConfigCalibrationTarget();

	@Option(name = "-i", aliases = {"--Input"}, usage = "Directory that contains camera sub-directories.")
	String inputDirectory = "";

	@Option(name = "-o", aliases = {"--Output"}, usage = "Directory it saves calibration results to")
	String outputDirectory = "CalibrationResults";

	@Option(name = "--ShapeSize", usage = "Length of a square's side or diameter of a circle, depending on target type.")
	double shapeSize = -1;

	@Option(name = "--Pattern", usage = "Which calibration pattern it should use. chessboard, echocheck, square_grid, circle_hexagonal, circle_grid, hamming_chessboard, hamming_grid")
	String targetType = "";

	@Option(name = "--EcoCheck", usage = "Abbreviated EcoCheck target description. If using ECoCheck this is all you need to specify. E.g. 9x7e3n1")
	String ecocheck = "";

	@Option(name = "--Grid", usage = "Number of rows and column in target. 9x7 for 9 rows and 7 columns")
	String gridText = "";

	@Option(name = "--Verbose", usage = "Prints out verbose debugging information")
	boolean verbose = false;

	@Option(name = "--CamRadial", usage = "Number of radial distortion terms")
	int numRadial = 3;

	void finishParsing() {
		if (inputDirectory.isEmpty()) {
			System.err.println("You must specify input directory with cameras");
			System.exit(1);
		}

		if (shapeSize <= 0) {
			System.err.println("You must specify shapeSize");
			System.exit(1);
		}
		configTarget.grid.shapeSize = shapeSize;

		if (!ecocheck.isEmpty()) {
			configTarget.type = CalibrationPatterns.ECOCHECK;
			configTarget.ecocheck = ConfigECoCheckMarkers.parse(ecocheck, shapeSize);
		} else if (targetType.isEmpty()) {
			System.err.println("You must specify which type of pattern you wish to detect");
			System.exit(1);
		} else {
			configTarget.type = CalibrationPatterns.valueOf(targetType.toUpperCase());
		}

		if (!gridText.isEmpty()) {
			String[] words = gridText.split("x");
			configTarget.grid.numRows = Integer.parseInt(words[0]);
			configTarget.grid.numCols = Integer.parseInt(words[1]);
		}

		if (configTarget.grid.numRows <= 0 || configTarget.grid.numCols <= 0) {
			System.err.println("You must specify the grid's size");
			System.exit(1);
		}
	}

	/**
	 * Perform the entire calibration process
	 */
	public void process() {
		// Create a list of camera directories
		List<String> cameras = findCameraDirectories();

		DetectSingleFiducialCalibration detector = FactoryFiducialCalibration.genericSingle(configTarget);

		var calibrator = new CalibrateMultiPlanar();
		calibrator.getCalibratorMono().configurePinhole(true, numRadial, false);
		calibrator.initialize(/*num cameras*/cameras.size(), /*num targets*/1);
		calibrator.setTargetLayout(0, detector.getLayout());

		// stores which images are in each camera
		Map<String, Set<String>> cameraImageSets = new HashMap<>();

		// Stores the expected image size for each camera
		Map<String, ImageDimension> cameraToShape = new HashMap<>();

		configureCameraAndSortImages(cameras, calibrator, cameraImageSets, cameraToShape);

		// Create a list of all unique image names
		Set<String> allImages = new HashSet<>();
		for (String cameraName : cameras) {
			allImages.addAll(cameraImageSets.get(cameraName));
		}

		// Find synchronized frames in all the cameras
		createSynchronizedDetections(cameras, detector, calibrator, cameraImageSets, cameraToShape, allImages);

		// perform calibration
		if (verbose) {
			calibrator.getBundleUtils().sba.setVerbose(System.out, null);
		}

		BoofMiscOps.checkTrue(calibrator.process(), "Calibration Failed!");

		printAndSaveResults(cameras, calibrator, allImages);
	}

	private void printAndSaveResults( List<String> cameras, CalibrateMultiPlanar calibrator, Set<String> allImages ) {
		// Print out summary information
		System.out.println("unique synchronized frames: " + allImages.size());
		System.out.println("unique cameras: " + cameras.size());
		System.out.println();

		System.out.println(calibrator.computeQualityText());

		// Make sure output directory exists
		UtilIO.mkdirs(new File(outputDirectory));

		// Save quality summary
		try (var out = new PrintWriter(new File(outputDirectory, "quality.txt"), UTF_8)) {
			out.println(calibrator.computeQualityText());
		} catch (IOException e) {
			System.err.println("Failed to save quality.txt");
		}

		// Save mapping from index to camera name
		System.out.println("\nIndex to Camera Name");
		try (var out = new PrintWriter(new File(outputDirectory, "index_to_camera.txt"), UTF_8)) {
			for (int cameraID = 0; cameraID < cameras.size(); cameraID++) {
				String cameraName = cameras.get(cameraID);
				out.println(cameraID + " " + cameraName);
				System.out.println(cameraID + " " + cameraName);
			}
			System.out.println();
		} catch (IOException e) {
			System.err.println("Failed to save quality.txt");
		}

		MultiCameraCalibParams params = calibrator.getResults();
		CalibrationIO.save(params, new File(outputDirectory, "multi_camera.yaml").getPath());
		System.out.println(params.toStringFormat());
	}

	@NotNull private List<String> findCameraDirectories() {
		List<File> children = UtilIO.listFilesSorted(new File(inputDirectory));
		List<String> cameras = new ArrayList<>();
		for (var f : children) {
			if (!f.isDirectory() || f.isHidden())
				continue;
			cameras.add(f.getName());
		}
		return cameras;
	}

	private void configureCameraAndSortImages( List<String> cameras, CalibrateMultiPlanar calibrator, Map<String, Set<String>> cameraImageSets, Map<String, ImageDimension> cameraToShape ) {
		// Go through each camera and set properties and create a set of images
		for (int cameraID = 0; cameraID < cameras.size(); cameraID++) {
			String cameraName = cameras.get(cameraID);

			String pathToImages = new File(inputDirectory, cameraName).getPath();
			List<String> imageFiles = UtilIO.listSmartImages(pathToImages, true);
			if (imageFiles.isEmpty()) {
				System.err.println("No images found in camera '" + cameraName + "'");
				continue;
			}

			// Determine how large images should be for this camera
			cameraToShape.put(cameraName, setImageSize(calibrator, cameraID, imageFiles.get(0)));

			var imageSet = new HashSet<String>();
			imageFiles.forEach(p -> imageSet.add(new File(p).getName()));

			cameraImageSets.put(cameraName, imageSet);
		}
	}

	private void createSynchronizedDetections( List<String> cameras, DetectSingleFiducialCalibration detector, CalibrateMultiPlanar calibrator, Map<String, Set<String>> cameraImageSets, Map<String, ImageDimension> cameraToShape, Set<String> allImages ) {
		for (String imageName : allImages) {
			// Find all cameras with a synchronized image at this time
			var camerasWithImage = new DogArray_I32();

			for (int cameraID = 0; cameraID < cameras.size(); cameraID++) {
				String cameraName = cameras.get(cameraID);
				if (Objects.requireNonNull(cameraImageSets.get(cameraName)).contains(imageName)) {
					camerasWithImage.add(cameraID);
				}
			}

			System.out.println("Detecting frame: " + imageName);
			// Create a synchronized observation for this image/frame
			var syncObs = new SynchronizedCalObs();
			camerasWithImage.forEach(cameraID -> {
				ImageDimension imageShape = Objects.requireNonNull(cameraToShape.get(cameras.get(cameraID)));
				var imagePath = new File(inputDirectory, cameras.get(cameraID) + "/" + imageName);
				addCameraObservations(cameraID, imagePath, imageShape, detector, syncObs);
			});

			System.out.println();

			calibrator.addObservation(syncObs);
		}
	}

	private static void addCameraObservations( int cameraID, File imagePath,
											   ImageDimension imageShape,
											   DetectSingleFiducialCalibration detector,
											   SynchronizedCalObs dst ) {
		GrayF32 image = UtilImageIO.loadImage(imagePath, true, ImageType.SB_F32);
		Objects.requireNonNull(image);

		// Sanity check to make sure the user isn't mixing up random images in the same directory
		if (!imageShape.isIdentical(image.width, image.height)) {
			System.err.println("Unexpected image size " + imagePath.getPath());
			System.exit(1);
		}

		// Find calibration targets inside the image
		detector.process(image);
		System.out.println(" " + detector.getDetectedPoints().points.size() + " " + imagePath.getParentFile().getName());

		// No need to add empty observations
		if (detector.getDetectedPoints().points.isEmpty())
			return;

		// Find the target which matches the expected target ID
		var set = dst.cameras.grow();
		set.cameraID = cameraID;
		set.targets.grow().setTo(detector.getDetectedPoints());
	}

	private ImageDimension setImageSize( CalibrateMultiPlanar calibrator, int cameraID, String imagePath ) {
		BufferedImage image = UtilImageIO.loadImage(imagePath);
		if (image == null) {
			System.err.println("Unable to load image '" + imagePath + "'");
			System.exit(1);
			throw new RuntimeException();
		}

		calibrator.setCameraProperties(cameraID, image.getWidth(), image.getHeight());
		return new ImageDimension(image.getWidth(), image.getHeight());
	}

	private static void printHelpExit( CmdLineParser parser ) {
		parser.getProperties().withUsageWidth(120);
		parser.printUsage(System.out);

		String help = """
				  	Calibration for an arbitrary number of cameras. Images for each camera are stored in separate directories with
				  	a common parent directory. Images that were collected at the same moment in time will have the same file name.
				  	
				  	Currently only a single pattern is allowed. It's recommended that you use ECoCheck or another self identifying
				  	pattern it can be extremely different to impossible to get the entire pattern in view for all cameras.
				""";

		System.out.println(help);
		System.out.println();
		System.out.println("Examples:");
		System.out.println();


		System.exit(1);
	}

	public static void main( String[] args ) {
		var generator = new CameraCalibrationMulti();
		var parser = new CmdLineParser(generator);

		if (args.length == 0) {
			printHelpExit(parser);
		}

		try {
			parser.parseArgument(args);

			generator.finishParsing();
			try {
				generator.process();
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println();
				System.out.println("Failed! See exception above");
			}
		} catch (CmdLineException e) {
			// handling of wrong arguments
			System.err.println(e.getMessage());
			printHelpExit(parser);
		}
	}
}

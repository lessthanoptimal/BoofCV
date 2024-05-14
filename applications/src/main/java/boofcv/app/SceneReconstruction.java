/*
 * Copyright (c) 2024, Peter Abeles. All Rights Reserved.
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

import boofcv.BoofVerbose;
import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.abst.tracker.PointTrack;
import boofcv.abst.tracker.PointTracker;
import boofcv.alg.cloud.PointCloudReader;
import boofcv.alg.cloud.PointCloudUtils_F64;
import boofcv.alg.geo.bundle.cameras.BundlePinholeSimplified;
import boofcv.alg.geo.rectify.DisparityParameters;
import boofcv.alg.mvs.ColorizeMultiViewStereoResults;
import boofcv.alg.mvs.MultiViewStereoFromKnownSceneStructure;
import boofcv.alg.similar.ConfigSimilarImagesSceneRecognition;
import boofcv.alg.similar.ConfigSimilarImagesTrackThenMatch;
import boofcv.alg.structure.*;
import boofcv.concurrency.BoofConcurrency;
import boofcv.core.image.ConvertImage;
import boofcv.core.image.GConvertImage;
import boofcv.core.image.LookUpColorRgbFormats;
import boofcv.factory.disparity.ConfigDisparity;
import boofcv.factory.disparity.ConfigDisparitySGM;
import boofcv.factory.scene.FactorySceneRecognition;
import boofcv.factory.structure.ConfigGeneratePairwiseImageGraph;
import boofcv.factory.structure.ConfigSparseToDenseCloud;
import boofcv.factory.structure.FactorySceneReconstruction;
import boofcv.factory.tracker.ConfigPointTracker;
import boofcv.factory.tracker.FactoryPointTracker;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.MirrorStream;
import boofcv.io.UtilIO;
import boofcv.io.geo.MultiViewIO;
import boofcv.io.image.LookUpImageFilesByIndex;
import boofcv.io.image.UtilImageIO;
import boofcv.io.points.PointCloudIO;
import boofcv.io.wrapper.images.LoadFileImageSequence2;
import boofcv.misc.BoofMiscOps;
import boofcv.misc.LookUpImages;
import boofcv.struct.Configuration;
import boofcv.struct.Point3dRgbI_F64;
import boofcv.struct.image.*;
import boofcv.visualize.PointCloudViewer;
import boofcv.visualize.VisualizeData;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.so.Rodrigues_F64;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.ddogleg.DDoglegConcurrency;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.jetbrains.annotations.NotNull;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static boofcv.misc.BoofMiscOps.checkTrue;

/**
 * Command line application for doing a 3D reconstruction
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class SceneReconstruction {
	// TODO save and load vocabulary used in similar

	ConfigPointTracker configTracker = FactorySceneRecognition.createDefaultTrackerConfig();
	ConfigSimilarImagesTrackThenMatch configSimilarTracker = new ConfigSimilarImagesTrackThenMatch();
	ConfigSimilarImagesSceneRecognition configSimilarUnordered = new ConfigSimilarImagesSceneRecognition();

	ConfigGeneratePairwiseImageGraph configPairwise = new ConfigGeneratePairwiseImageGraph();
	ConfigSparseToDenseCloud configSparseToDense = new ConfigSparseToDenseCloud();

	@Option(name = "-i", aliases = {"--Input"}, usage = """
			Directory or glob pattern or regex pattern.
			Glob example: 'glob:data/**/left*.jpg'
			Regex example: 'regex:data/\\w+/left\\d+.jpg'
			If not a pattern then it's assumed to be a path. All files with known image extensions in their name as added, e.g. jpg, png""")
	String inputPattern;

	@Option(name = "-o", aliases = {"--Output"}, usage = "Path to output directory.")
	String outputPath = "output";

	@Option(name = "--ConfigPath", usage = "Path to directory containing configuration files it should use. " +
			"This will override the defaults. Be prepared to read source code if you want to understand everything.")
	String configPath = "";

	@Option(name = "--GUI", usage = "Ignore all other command line arguments and switch to GUI mode")
	boolean guiMode = false;

	@Option(name = "--MaxPixels", usage = "Maximum number of images in an image before its down sampled. E.g. 800*600=480000")
	int maxPixels = 800*600;

	@Option(name = "--DisparityMin", usage = "Minimum disparity. Points less than this are filtered. Can be used to remove noisy distant points.")
	double disparityMin = 0.0;

	@Option(name = "--DisparityRange", usage = "Number of disparity values considered.")
	int disparityRange = 0;

	@Option(name = "--Ordered", usage = "Images are assumed to be in sequential order and a feature tracker can be used")
	boolean ordered = false;

	@Option(name = "--TryHarder", usage = "Slower but has a greater chance of doing a good reconstruction. " +
			"Lower thresholds and more iterations to remove outliers.")
	boolean tryHarder = false;

	@Option(name = "--Verbose", usage = "Prints lots of debugging information to stdout. This is always saved to disk too.")
	boolean verbose = false;

	@Option(name = "--ShowCloud", usage = "Show the final point cloud after processing each scene")
	boolean showCloud = false;

	@Option(name = "--AllScenes", usage = "If true, a dense reconstruction will be done for all scenes. Not just the largest")
	boolean allScenes = false;

	@Option(name = "--SaveFusedDisparity", usage = "If true, it will save the fused disparity images")
	boolean saveFusedDisparity = false;

	@Option(name = "--DeleteOutput", usage = "If true, it will recursively delete the output directory if it already exists")
	boolean deleteOutput = false;

	@Option(name = "--RerunDense", usage = "Re-runs dense reconstruction using previously saved results")
	boolean rerunDense = false;

	@Option(name = "--RerunSparse", usage = "Re-runs sparse reconstruction using previously saved results")
	boolean rerunSparse = false;

	@Option(name = "--WindowSize", usage = "Specifies the display window's width and height.")
	String windowSizeString = "800:600";

	@Option(name = "--Threads", usage = "Overrides default number of threads. 0 means system default.")
	int numThreads = 0;

	// Indicates what it should load from disk
	boolean loadSimilar = false;
	boolean loadPairwise = false;
	boolean loadSparseScene = false;

	// Storage for intermediate results
	PairwiseImageGraph pairwise;
	LookUpSimilarImages dbSimilar;
	LookUpCameraInfo dbCams = new LookUpCameraInfo();
	SceneStructureMetric scene;
	SceneObservations observations;
	SparseSceneToDenseCloud<GrayU8> sparseToDense;

	// List of all the independent scenes it was able to construct
	List<SceneWorkingGraph> listScenes = new ArrayList<>();

	// Used to load images and resize them. Also storage for image dimensions needed later on
	LoadFileImageSequence2<Planar<GrayU8>> images;
	List<ImageDimension> listDimensions = new ArrayList<>();

	// How big the view will be
	ImageDimension windowSize = new ImageDimension();

	// All the input images
	List<String> imagePaths;

	PrintStream out;

	public void process() {
		long time0 = System.currentTimeMillis();
		System.out.println("ordered        = " + ordered);
		System.out.println("max pixels     = " + maxPixels + " , sqrt=" + Math.sqrt(maxPixels));
		System.out.println("input pattern  = " + inputPattern);
		System.out.println("output dir     = " + outputPath);

		BoofMiscOps.parseDimension(windowSizeString, windowSize);

		if (rerunSparse) {
			loadSimilar = true;
			loadPairwise = true;
		} else if (rerunDense) {
			loadSimilar = true;
			loadPairwise = true;
			loadSparseScene = true;
		}

		imagePaths = UtilIO.listSmartImages(inputPattern, true);

		if (imagePaths.isEmpty()) {
			System.err.println("No inputs found. Bad path or pattern? " + inputPattern);
			System.exit(-1);
		}

		// See if the user overrode the number of threads
		if (numThreads > 0) {
			BoofConcurrency.setMaxThreads(numThreads);
		}

		// This will make SBA concurrent
		DDoglegConcurrency.USE_CONCURRENT = BoofConcurrency.isUseConcurrent();

		// Create the output directory if it doesn't exist
		UtilIO.mkdirs(new File(outputPath), deleteOutput);

		System.out.println("Total images: " + imagePaths.size());
		saveIndexToImageTable(imagePaths);

		// See if the user
		if (configPath.isEmpty()) {
			if (tryHarder)
				configureHarder();
			else
				configureDefault();
		} else {
			loadConfigurations();
		}
		saveConfigurations();

		try (PrintStream fileOut = new PrintStream(new File(outputPath, "verbose.txt"))) {
			// If verbose print the stream to stdout, otherwise just log it to the file
			if (verbose) {
				out = new PrintStream(new MirrorStream(System.out, fileOut));
			} else {
				out = fileOut;
			}

			images = new LoadFileImageSequence2<>(imagePaths, ImageType.PL_U8);
			images.setTargetPixels(maxPixels);
			listDimensions.clear();

			if (loadSimilar) {
				System.out.println("Loading similar from disk");
				loadImageSizes();
				dbSimilar = MultiViewIO.loadSimilarImages(new File(outputPath, "similar.yaml").getPath());
			} else {
				if (ordered)
					findSimilarImagesSequence();
				else
					findSimilarImagesUnsorted();
				MultiViewIO.save(dbSimilar, new File(outputPath, "similar.yaml").getPath());
			}

			if (loadPairwise) {
				System.out.println("Loading pairwise from disk");
				pairwise = MultiViewIO.load(new File(outputPath, "pairwise.yaml").getPath(), (PairwiseImageGraph)null);
			} else {
				computePairwise();
			}

			if (loadSparseScene) {
				if (loadScenesFromDisk()) return;
			} else {
				computeMetric();
			}

			// Finish constructing the largest or all the scenes
			if (allScenes) {
				// Number of digits needed to contain all the scenes
				int numDigits = BoofMiscOps.numDigits(listScenes.size());
				for (int sceneIndex = 0; sceneIndex < listScenes.size(); sceneIndex++) {
					reconstructScene(imagePaths, listScenes.get(sceneIndex), new File(outputPath, String.format("scene%0" + numDigits + "d", sceneIndex)));
				}
			} else {
				reconstructScene(imagePaths, listScenes.get(0), new File(outputPath, "scene"));
			}

			// Print total processing time
			System.out.println("Total Time: " + BoofMiscOps.milliToHuman(System.currentTimeMillis() - time0));
			out.flush();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println("Finished!");
	}

	private boolean loadScenesFromDisk() {
		// Finding all the scene directories
		File[] children = new File(outputPath).listFiles();
		if (children == null) {
			System.err.println("directory doesn't exist. path=" + outputPath);
			System.exit(1);
			return true;
		}
		List<String> sceneDirs = new ArrayList<>();
		for (File c : children) {
			if (c.isDirectory() && c.getName().startsWith("scene")) {
				sceneDirs.add(c.getPath());
			}
		}
		Collections.sort(sceneDirs);
		if (sceneDirs.isEmpty()) {
			System.err.println("Can't load sparse scene! No directories found. path=" + outputPath);
			System.exit(1);
			return true;
		}

		// Load all the scnes
		for (String sceneDirectory : sceneDirs) {
			File f = new File(sceneDirectory);
			System.out.println("Loading scene=" + f.getName());
			listScenes.add(MultiViewIO.load(new File(f, "working.yaml").getPath(), pairwise, (SceneWorkingGraph)null));
		}
		return false;
	}

	/**
	 * Save the input image list so that you know what the numeric values of each image represents
	 */
	private void saveIndexToImageTable( List<String> paths ) {
		try (PrintStream out = new PrintStream(new File(outputPath, "index_to_image.txt"))) {
			for (int i = 0; i < paths.size(); i++) {
				out.println(paths.get(i));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private void reconstructScene( List<String> paths, SceneWorkingGraph working, File sceneDirectory ) {
		out.println("----- Building " + sceneDirectory.getName());

		// If not loading from disk, refine the save the sparse scene
		if (!loadSparseScene) {
			UtilIO.mkdirs(sceneDirectory);
			bundleAdjustmentRefine(sceneDirectory, working);
			MultiViewIO.save(working, new File(sceneDirectory, "working.yaml").getPath());
		} else {
			scene = MultiViewIO.load(new File(sceneDirectory, "structure.yaml").getPath(), (SceneStructureMetric)null);
			observations = MultiViewIO.load(new File(sceneDirectory, "observations.yaml").getPath(), (SceneObservations)null);
		}
		printSparseSummary(working);

		computeDense(paths, working, sceneDirectory);
		saveSparseCloudToDisk(sceneDirectory);
		saveCloudToDisk(sceneDirectory);
		if (showCloud)
			visualizeInPointCloud(sparseToDense.getCloud(), sparseToDense.getColorRgb(), scene, sceneDirectory.getName());
	}

	private void configureDefault() {
		configSparseToDense.disparity.approach = ConfigDisparity.Approach.SGM;
		ConfigDisparitySGM configSgm = configSparseToDense.disparity.approachSGM;
		configSgm.validateRtoL = 0;
		configSgm.texture = 0.75;
		configSgm.disparityRange = disparityRange <= 0 ? 250 : disparityRange;
		configSgm.paths = ConfigDisparitySGM.Paths.P4;
		configSgm.configBlockMatch.radiusX = 3;
		configSgm.configBlockMatch.radiusY = 3;
	}

	private void configureHarder() {
		configureDefault();

		configTracker.klt.maximumTracks.setFixed(1200);
		configTracker.detDesc.detectPoint.general.radius = 3;

		configSimilarTracker.minimumSimilar.setRelative(0.15, 50);
		configSimilarTracker.descriptions.radius = 20;
		configSimilarTracker.sequentialSearchRadius = 15;
		configSimilarTracker.limitQuery = 30;
		configSimilarTracker.minimumRecognizeDistance = 0;
		configSimilarTracker.sequentialMinimumCommonTracks.setRelative(0.4, 200);

		configSimilarUnordered.features.detectFastHessian.extract.radius = 1;
		configSimilarUnordered.features.detectFastHessian.maxFeaturesAll = 1200;
		configSimilarUnordered.minimumSimilar.setRelative(0.1, 50);

		configPairwise.score.typeRotation.minimumInliers.setRelative(0.1, 50);
		configPairwise.score.ransacF.iterations = 2000;
	}

	private void saveConfigurations() {
		File configDir = new File(outputPath, "configurations");
		UtilIO.mkdirs(configDir);

		UtilIO.saveConfig(configTracker, new ConfigPointTracker(),
				new File(configDir, "tracker.yaml"));
		UtilIO.saveConfig(configSimilarTracker, new ConfigSimilarImagesTrackThenMatch(),
				new File(configDir, "similar_tracker.yaml"));
		UtilIO.saveConfig(configSimilarUnordered, new ConfigSimilarImagesSceneRecognition(),
				new File(configDir, "similar_unordered.yaml"));
		UtilIO.saveConfig(configPairwise, new ConfigGeneratePairwiseImageGraph(),
				new File(configDir, "pairwise.yaml"));
		UtilIO.saveConfig(configSparseToDense, new ConfigSparseToDenseCloud(),
				new File(configDir, "sparse_to_dense.yaml"));
	}

	private void loadConfigurations() {
		File configDir = new File(outputPath, "configurations");
		configTracker = loadConfiguration(new File(configDir, "tracker.yaml"), configTracker);
		configSimilarTracker = loadConfiguration(new File(configDir, "similar_tracker.yaml"), configSimilarTracker);
		configSimilarUnordered = loadConfiguration(new File(configDir, "similar_unordered.yaml"), configSimilarUnordered);
		configPairwise = loadConfiguration(new File(configDir, "pairwise.yaml"), configPairwise);
		configSparseToDense = loadConfiguration(new File(configDir, "sparse_to_dense.yaml"), configSparseToDense);
	}

	/**
	 * If it can, it will load the configuration and return a new instance. Otherwise, it will return the passed in
	 * instance and print an error message
	 */
	private <T extends Configuration> T loadConfiguration( File file, T config ) {
		if (!file.exists()) {
			System.err.println("Configuration file doesn't exist: " + file.getPath());
			return config;
		}
		try {
			return UtilIO.loadConfig(file);
		} catch (Exception e) {
			System.err.println("Failed to load config: " + file.getPath());
			System.err.println("Message: " + e.getMessage());
			return config;
		}
	}

	private void findSimilarImagesUnsorted() {
		final var similarImages = FactorySceneReconstruction.createSimilarImages(configSimilarUnordered, ImageType.SB_U8);

		similarImages.setVerbose(out, BoofMiscOps.hashSet(BoofVerbose.RECURSIVE));

		// Track features across the entire sequence and save the results
		BoofMiscOps.profile(() -> {
			GrayU8 gray = new GrayU8(images.getWidth(), images.getHeight());
			dbCams.addCameraCanonical(gray.width, gray.height, 60.0);
			while (images.hasNext()) {
				Planar<GrayU8> color = images.next();
				ConvertImage.average(color, gray);
				String viewID = images.getFrameNumber() + "";
				similarImages.addImage(viewID, gray);
				listDimensions.add(new ImageDimension(gray.width, gray.height));

				// Assume all views come from the same camera
				dbCams.addView(viewID, 0);
			}

			similarImages.fixate();
		}, "Finding Similar");

		this.dbSimilar = similarImages;
	}

	/**
	 * Loads each image and saves its size.
	 */
	private void loadImageSizes() {
		dbCams.addCameraCanonical(images.getWidth(), images.getHeight(), 60);
		int frameID = 0;
		while (images.hasNext()) {
			Planar<GrayU8> color = images.next();
			listDimensions.add(new ImageDimension(color.width, color.height));
			dbCams.addView(frameID + "", 0);
			frameID += 1;
		}
	}

	private void findSimilarImagesSequence() {
		PointTracker<GrayU8> tracker = FactoryPointTracker.tracker(configTracker, GrayU8.class, null);
		var activeTracks = new ArrayList<PointTrack>();

		final var similarImages = FactorySceneReconstruction.createTrackThenMatch(configSimilarTracker, ImageType.SB_U8);

		similarImages.setVerbose(out, BoofMiscOps.hashSet(BoofVerbose.RECURSIVE));

		// Track features across the entire sequence and save the results
		BoofMiscOps.profile(() -> {
			GrayU8 gray = new GrayU8(images.getWidth(), images.getHeight());
			similarImages.initialize(images.getWidth(), images.getHeight());
			dbCams.addCameraCanonical(images.getWidth(), images.getHeight(), 60);
			while (images.hasNext()) {
				Planar<GrayU8> color = images.next();
				ConvertImage.average(color, gray);

				tracker.process(gray);
				tracker.spawnTracks();
				tracker.getActiveTracks(activeTracks);
				similarImages.processFrame(gray, activeTracks, tracker.getFrameID());
				dbCams.addView(similarImages.frames.getTail().frameID, 0);

				listDimensions.add(new ImageDimension(gray.width, gray.height));
			}

			similarImages.finishedTracking();
		}, "Finding Similar");

		this.dbSimilar = similarImages;
	}

	private void computePairwise() {
		GeneratePairwiseImageGraph generatePairwise = FactorySceneReconstruction.generatePairwise(configPairwise);
		BoofMiscOps.profile(() -> {
			generatePairwise.setVerbose(out, BoofMiscOps.hashSet(BoofVerbose.RECURSIVE));
			generatePairwise.process(dbSimilar, dbCams);
		}, "Created Pairwise graph");
		pairwise = generatePairwise.getGraph();

		MultiViewIO.save(pairwise, new File(outputPath, "pairwise.yaml").getPath());
	}

	private void computeMetric() {
		var metric = new MetricFromUncalibratedPairwiseGraph();
		metric.setVerbose(out, BoofMiscOps.hashSet(BoofVerbose.RECURSIVE));
		BoofMiscOps.profile(() -> {
			if (!metric.process(dbSimilar, dbCams, pairwise)) {
				System.err.println("Reconstruction failed");
				System.exit(1);
			}
		}, "Metric Reconstruction");

		listScenes.addAll(metric.getScenes().toList());
		Collections.sort(listScenes, ( a, b ) -> Integer.compare(b.listViews.size(), a.listViews.size()));
	}

	public void bundleAdjustmentRefine( File sceneDirectory, SceneWorkingGraph working ) {
		var refine = new RefineMetricWorkingGraph();
		refine.metricSba.getSba().setVerbose(out, null);
		BoofMiscOps.profile(() -> {
			// Bundle adjustment is run twice, with the worse 5% of points discarded in an attempt to reduce noise
			refine.metricSba.keepFraction = 0.95;
			if (!refine.process(dbSimilar, working)) {
				out.println("SBA REFINE FAILED");
			}
		}, "Bundle Adjustment refine");
		scene = refine.metricSba.structure;

		MultiViewIO.save(scene, new File(sceneDirectory, "structure.yaml").getPath());
	}

	private void printSparseSummary( SceneWorkingGraph working ) {
		Rodrigues_F64 rod = new Rodrigues_F64();
		out.println("----------------------------------------------------------------------------");
		for (int workIdx = 0; workIdx < working.listViews.size(); workIdx++) {
			SceneWorkingGraph.View wv = working.listViews.get(workIdx);
			ConvertRotation3D_F64.matrixToRodrigues(wv.world_to_view.R, rod);
			BundlePinholeSimplified intrinsics = working.getViewCamera(wv).intrinsic;
			out.printf("view[%2d]='%2s' f=%6.1f k1=%6.3f k2=%6.3f T={%5.1f,%5.1f,%5.1f} R=%5.3f\n",
					workIdx, wv.pview.id, intrinsics.f, intrinsics.k1, intrinsics.k2,
					wv.world_to_view.T.x, wv.world_to_view.T.y, wv.world_to_view.T.z, rod.theta);
		}
		out.println("   Views used: " + scene.views.size + " / " + pairwise.nodes.size);
	}

	private void computeDense( List<String> paths, SceneWorkingGraph working, File sceneDirectory ) {
		sparseToDense = FactorySceneReconstruction.sparseSceneToDenseCloud(configSparseToDense, ImageType.SB_U8);

		// If requested, save disparity information as it's computed
		File outputFused = new File(sceneDirectory, "fused");
		if (saveFusedDisparity) {
			UtilIO.mkdirs(outputFused);
		}

		sparseToDense.getMultiViewStereo().setListener(new MultiViewStereoFromKnownSceneStructure.Listener<>() {
			@Override
			public void handlePairDisparity( String left, String right, GrayU8 rect0, GrayU8 rect1,
											 GrayF32 disparity, DisparityParameters parameters ) {}

			@Override
			public void handleFused( String name, GrayF32 inverseDepth ) {
				if (saveFusedDisparity) {
					BufferedImage colorized = VisualizeImageData.inverseDepth(inverseDepth, null, 1e6f, 0);
					UtilImageIO.saveImage(colorized, new File(outputFused, "visualized_" + name + ".png").getPath());
					// It's not obvious what format to save the float and binary images in. leaving that for the future
				}
			}
		});

		sparseToDense.getMultiViewStereo().setVerbose(out, BoofMiscOps.hashSet(BoofVerbose.RECURSIVE, BoofVerbose.RUNTIME));

		LookUpImages imageLookup = lookupAndRescale(paths);

		// It needs a look up table to go from SBA view index to image name. It loads images as needed to perform
		// stereo disparity
		var viewToId = new TIntObjectHashMap<String>();
		BoofMiscOps.forIdx(working.listViews, ( workIdxI, wv ) -> viewToId.put(wv.index, wv.pview.id));
		if (!sparseToDense.process(scene, observations, viewToId, imageLookup))
			throw new RuntimeException("Dense reconstruction failed!");
	}

	/**
	 * Loads images by calling internal functions in the image sequence. Uses previously saved dimensions.	 * @return
	 */
	@NotNull private LookUpImages lookupAndRescale( List<String> paths ) {
		return new LookUpImages() {
			@Override public boolean loadShape( String name, ImageDimension shape ) {
				int index = Integer.parseInt(name);
				shape.setTo(listDimensions.get(index));
				return true;
			}

			@Override public <LT extends ImageBase<LT>> boolean loadImage( String name, LT output ) {
				int index = Integer.parseInt(name);
				GConvertImage.convert(images.loadImage(paths.get(index)), output);
//				output.setTo((LT)images.loadImage(paths.get(index)));
				return true;
			}
		};
	}

	private void saveSparseCloudToDisk( File outputDirectory ) {
		checkTrue(scene.isHomogenous());
		List<Point3D_F64> cloudXyz = new ArrayList<>();
		Point4D_F64 world = new Point4D_F64();

		// NOTE: By default the colors found below are not used. Look before to see why and how to turn them on.
		//
		// Colorize the cloud by reprojecting the images. The math is straight forward but there's a lot of book
		// keeping that needs to be done due to the scene data structure. A class is provided to make this process easy
		var imageLookup = new LookUpImageFilesByIndex(imagePaths);
		var colorize = new ColorizeMultiViewStereoResults<>(new LookUpColorRgbFormats.PL_U8(), imageLookup);

		DogArray_I32 colorsRgb = new DogArray_I32();
		colorsRgb.resize(scene.points.size);
		colorize.processScenePoints(scene, observations,
				( viewIdx ) -> viewIdx + "", // String encodes the image's index
				( pointIdx, r, g, b ) -> colorsRgb.set(pointIdx, (r << 16) | (g << 8) | b)); // Assign the RGB color

		// Convert the structure into regular 3D points from homogenous
		for (int i = 0; i < scene.points.size; i++) {
			scene.points.get(i).get(world);
			// If the point is at infinity it's not clear what to do. It would be best to skip it then the color
			// array would be out of sync. Let's just throw it far far away then.
			if (world.w == 0.0)
				cloudXyz.add(new Point3D_F64(0, 0, Double.MAX_VALUE));
			else
				cloudXyz.add(new Point3D_F64(world.x/world.w, world.y/world.w, world.z/world.w));
		}

		// Save everything
		try (FileOutputStream out = new FileOutputStream(new File(outputDirectory, "sparse_cloud.ply"))) {
			PointCloudIO.save3D(PointCloudIO.Format.PLY, PointCloudReader.wrapF64(cloudXyz, colorsRgb.data), true, out);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void saveCloudToDisk( File outputDirectory ) {
		List<Point3D_F64> cloud = sparseToDense.getCloud();
		DogArray_I32 colorsRgb = sparseToDense.getColorRgb();

		// Save everything
		try (FileOutputStream out = new FileOutputStream(new File(outputDirectory, "cloud.ply"))) {
			PointCloudIO.save3D(PointCloudIO.Format.PLY, PointCloudReader.wrapF64(cloud, colorsRgb.data), true, out);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Save the dense point cloud to disk in PLY format
		try (FileOutputStream out = new FileOutputStream(new File(outputDirectory, "cloud_clipped.ply"))) {
			// Filter points which are far away to make it easier to view in 3rd party viewers that auto scale
			// You might need to adjust the threshold for your application if too many points are cut
			double distanceThreshold = 50.0;

			DogArray<Point3dRgbI_F64> filtered = PointCloudUtils_F64.filter(
					( idx, p ) -> p.setTo(cloud.get(idx)), colorsRgb::get, cloud.size(),
					( idx ) -> cloud.get(idx).norm() <= distanceThreshold, null);

			PointCloudIO.save3D(PointCloudIO.Format.PLY, PointCloudReader.wrapF64RGB(filtered.toList()), true, out);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void visualizeInPointCloud( List<Point3D_F64> cloud, DogArray_I32 colorsRgb,
									   SceneStructureMetric structure,
									   String name ) {
		PointCloudViewer viewer = VisualizeData.createPointCloudViewer();
		viewer.setFog(true);
		viewer.setDotSize(1);
		viewer.setTranslationStep(0.15);
		viewer.addCloud(( idx, p ) -> p.setTo(cloud.get(idx)), colorsRgb::get, cloud.size());
		viewer.setCameraHFov(UtilAngle.radian(60));

		SwingUtilities.invokeLater(() -> {
			// Show where the cameras are
			BoofSwingUtil.visualizeCameras(structure, viewer);

			// Display the point cloud
			viewer.getComponent().setPreferredSize(new Dimension(windowSize.width, windowSize.height));
			ShowImages.showWindow(viewer.getComponent(), "Cloud: " + name, true);
		});
	}

	public static void printHelpExit( CmdLineParser parser ) {
		parser.getProperties().withUsageWidth(120);
		parser.printUsage(System.out);

		System.out.println();
		System.out.println("Examples:");
		System.out.println();
		System.exit(1);
	}

	public static void main( String[] args ) {
		var generator = new SceneReconstruction();
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

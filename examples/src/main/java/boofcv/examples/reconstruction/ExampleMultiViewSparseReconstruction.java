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

package boofcv.examples.reconstruction;

import boofcv.BoofVerbose;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.abst.tracker.PointTrack;
import boofcv.abst.tracker.PointTracker;
import boofcv.alg.cloud.PointCloudReader;
import boofcv.alg.geo.bundle.cameras.BundlePinholeSimplified;
import boofcv.alg.mvs.ColorizeMultiViewStereoResults;
import boofcv.alg.similar.ConfigSimilarImagesSceneRecognition;
import boofcv.alg.similar.ConfigSimilarImagesTrackThenMatch;
import boofcv.alg.structure.*;
import boofcv.core.image.LookUpColorRgbFormats;
import boofcv.factory.scene.FactorySceneRecognition;
import boofcv.factory.structure.ConfigGeneratePairwiseImageGraph;
import boofcv.factory.structure.FactorySceneReconstruction;
import boofcv.factory.tracker.ConfigPointTracker;
import boofcv.factory.tracker.FactoryPointTracker;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.geo.MultiViewIO;
import boofcv.io.image.LookUpImageFilesByIndex;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.image.UtilImageIO;
import boofcv.io.points.PointCloudIO;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.Point3dRgbI_F64;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.InterleavedU8;
import boofcv.visualize.PointCloudViewer;
import boofcv.visualize.TwoAxisRgbPlane;
import boofcv.visualize.VisualizeData;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.so.Rodrigues_F64;
import org.apache.commons.io.FilenameUtils;
import org.ddogleg.DDoglegConcurrency;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static boofcv.misc.BoofMiscOps.checkTrue;

/**
 * Estimate scene parameters using a sparse set of features across uncalibrated images. In this example, a KLT
 * feature tracker will be used due to speed and simplicity even though there are some disadvantages
 * mentioned below. After image features have been tracked across the sequence we will first determine 3D
 * connectivity through two-view geometry, followed my a metric elevation. Then a final refinement
 * using bundle adjustment.
 *
 * This is unusual in that it will estimate intrinsic parameters from scratch with very few assumptions.
 * Most MVS software uses a data base of known camera parameters to provide an initial seed as this can simplify
 * the problem and make it more stable.
 *
 * @author Peter Abeles
 */
public class ExampleMultiViewSparseReconstruction {
	String workDirectory;
	List<String> imageFiles = new ArrayList<>();

	PairwiseImageGraph pairwise = null;
	LookUpSimilarImages dbSimilar;
	LookUpCameraInfo dbCams = new LookUpCameraInfo();
	SceneWorkingGraph working = null;
	SceneStructureMetric scene = null;

	boolean rebuild = false;

	public static void main( String[] args ) {
		var example = new ExampleMultiViewSparseReconstruction();
		example.compute("tree_snow_01.mp4", true);
//		example.compute("ditch_02.mp4", true);
//		example.compute("holiday_display_01.mp4", true);
//		example.compute("log_building_02.mp4", true);
//		example.compute("drone_park_01.mp4", false);
//		example.compute("stone_sign.mp4", true);
		example.visualizeSparseCloud();

		System.out.println("done");
	}

	public void compute( String videoName, boolean sequential ) {
		// Turn on threaded code for bundle adjustment
		DDoglegConcurrency.USE_CONCURRENT = true;

		// Create a directory to store the work space
		String path = UtilIO.pathExample("mvs/" + videoName);
		workDirectory = "mvs_work/" + FilenameUtils.getBaseName(videoName);

		// Attempt to reload intermediate results if previously computed
		if (!rebuild) {
			try {
				pairwise = MultiViewIO.load(new File(workDirectory, "pairwise.yaml").getPath(), (PairwiseImageGraph)null);
			} catch (UncheckedIOException ignore) {}

			try {
				working = MultiViewIO.load(new File(workDirectory, "working.yaml").getPath(), pairwise, null);
			} catch (UncheckedIOException ignore) {}

			try {
				scene = MultiViewIO.load(new File(workDirectory, "structure.yaml").getPath(), (SceneStructureMetric)null);
			} catch (UncheckedIOException ignore) {}
		}

		// Convert the video into an image sequence. Later on we will need to access the images in random order
		var imageDirectory = new File(workDirectory, "images");

		if (imageDirectory.exists()) {
			imageFiles = UtilIO.listSmart(String.format("glob:%s/images/*.png", workDirectory), true, ( f ) -> true);
		} else {
			checkTrue(imageDirectory.mkdirs(), "Failed to image directory");
			SimpleImageSequence<InterleavedU8> sequence = DefaultMediaManager.INSTANCE.openVideo(path, ImageType.IL_U8);
			System.out.println("----------------------------------------------------------------------------");
			System.out.println("### Decoding Video");
			BoofMiscOps.profile(() -> {
				int frame = 0;
				while (sequence.hasNext()) {
					InterleavedU8 image = sequence.next();
					File imageFile = new File(imageDirectory, String.format("frame%04d.png", frame++));
					imageFiles.add(imageFile.getPath());
					// This is commented out for what appears to be a JRE bug.
					// V  [libjvm.so+0xdc4059]  SWPointer::SWPointer(MemNode*, SuperWord*, Node_Stack*, bool)
					UtilImageIO.saveImage(image, imageFile.getPath());
				}
			}, "Video Decoding");
		}

		// Only determine the visual relationship between images if needed
		if (pairwise == null || working == null) {
			if (sequential) {
				similarImagesFromSequence();
			} else {
				similarImagesFromUnsorted();
			}
		}

		if (pairwise == null)
			computePairwiseGraph();
		if (working == null)
			metricFromPairwise();
		if (scene == null)
			bundleAdjustmentRefine();

		Rodrigues_F64 rod = new Rodrigues_F64();
		System.out.println("----------------------------------------------------------------------------");
		for (PairwiseImageGraph.View pv : pairwise.nodes.toList()) {
			var wv = working.lookupView(pv.id);
			if (wv == null)
				continue;
			int order = working.listViews.indexOf(wv);
			ConvertRotation3D_F64.matrixToRodrigues(wv.world_to_view.R, rod);
			BundlePinholeSimplified intrinsics = working.getViewCamera(wv).intrinsic;
			System.out.printf("view[%2d]='%2s' f=%6.1f k1=%6.3f k2=%6.3f T={%5.1f,%5.1f,%5.1f} R=%4.2f\n",
					order, wv.pview.id, intrinsics.f, intrinsics.k1, intrinsics.k2,
					wv.world_to_view.T.x, wv.world_to_view.T.y, wv.world_to_view.T.z, rod.theta);
		}
		System.out.println("   Views used: " + scene.views.size + " / " + pairwise.nodes.size);
	}

	/**
	 * For a pairwise graph to be constructed, image feature relationships between frames are needed. For a video
	 * sequence, KLT is an easy and fast way to do this. However, KLT will not "close the loop", and it will
	 * not realize you're back at the initial location. Typically this results in a noticeable miss alignment.
	 */
	private void similarImagesFromSequence() {
		System.out.println("----------------------------------------------------------------------------");
		System.out.println("### Creating Similar Images from an ordered set of images");

		// Configure the KLT tracker
		ConfigPointTracker configTracker = FactorySceneRecognition.createDefaultTrackerConfig();

		PointTracker<GrayU8> tracker = FactoryPointTracker.tracker(configTracker, GrayU8.class, null);
		var activeTracks = new ArrayList<PointTrack>();

		var config = new ConfigSimilarImagesTrackThenMatch();

		final var dbSimilar = FactorySceneReconstruction.createTrackThenMatch(config, ImageType.SB_U8);
		dbSimilar.setVerbose(System.out, BoofMiscOps.hashSet(BoofVerbose.RECURSIVE));

		// Track features across the entire sequence and save the results
		BoofMiscOps.profile(() -> {
			boolean first = true;
			for (int frameId = 0; frameId < imageFiles.size(); frameId++) {
				String filePath = imageFiles.get(frameId);
				GrayU8 frame = UtilImageIO.loadImage(filePath, GrayU8.class);
				Objects.requireNonNull(frame, "Failed to load image");
				if (first) {
					first = false;
					dbSimilar.initialize(frame.width, frame.height);
					dbCams.addCameraCanonical(frame.width, frame.height, 60.0);
				}

				tracker.process(frame);
				int activeCount = tracker.getTotalActive();
				int droppedCount = tracker.getDroppedTracks(null).size();
				tracker.spawnTracks();
				tracker.getActiveTracks(activeTracks);
				dbSimilar.processFrame(frame, activeTracks, tracker.getFrameID());
				String id = frameId + "";
				System.out.println("frame id = " + id + " active=" + activeCount + " dropped=" + droppedCount);

				// Everything maps to the same camera
				dbCams.addView(id, 0);
			}

			dbSimilar.finishedTracking();
		}, "Finding Similar");

		this.dbSimilar = dbSimilar;
	}

	/**
	 * Assumes that the images are complete unsorted
	 */
	private void similarImagesFromUnsorted() {
		System.out.println("----------------------------------------------------------------------------");
		System.out.println("### Creating Similar Images from unordered images");

		var config = new ConfigSimilarImagesSceneRecognition();

		final var similarImages = FactorySceneReconstruction.createSimilarImages(config, ImageType.SB_U8);
		similarImages.setVerbose(System.out, BoofMiscOps.hashSet(BoofVerbose.RECURSIVE));

		// Track features across the entire sequence and save the results
		BoofMiscOps.profile(() -> {
			for (int frameId = 0; frameId < imageFiles.size(); frameId++) {
				String filePath = imageFiles.get(frameId);
				GrayU8 frame = UtilImageIO.loadImage(filePath, GrayU8.class);
				Objects.requireNonNull(frame, "Failed to load image");

				String viewID = frameId + "";

				similarImages.addImage(viewID, frame);
				// Everything maps to the same camera
				if (frameId == 0)
					dbCams.addCameraCanonical(frame.width, frame.height, 60.0);
				dbCams.addView(viewID, 0);
			}

			similarImages.fixate();
		}, "Finding Similar");

		this.dbSimilar = similarImages;
	}

	/**
	 * This step attempts to determine which views have a 3D (not homographic) relationship with each other and which
	 * features are real and not fake.
	 */
	public void computePairwiseGraph() {
		System.out.println("----------------------------------------------------------------------------");
		System.out.println("### Creating Pairwise");
		var config = new ConfigGeneratePairwiseImageGraph();
		GeneratePairwiseImageGraph generatePairwise = FactorySceneReconstruction.generatePairwise(config);
		BoofMiscOps.profile(() -> {
			generatePairwise.setVerbose(System.out, BoofMiscOps.hashSet(BoofVerbose.RECURSIVE));
			generatePairwise.process(dbSimilar, dbCams);
		}, "Created Pairwise graph");
		pairwise = generatePairwise.getGraph();

		var savePath = new File(workDirectory, "pairwise.yaml");
		MultiViewIO.save(pairwise, savePath.getPath());
		System.out.println("  nodes.size=" + pairwise.nodes.size);
		System.out.println("  edges.size=" + pairwise.edges.size);
	}

	/**
	 * Next a metric reconstruction is attempted using views with a 3D relationship. This is a tricky step
	 * and works by finding clusters of views which are likely to have numerically stable results then expanding
	 * the sparse metric reconstruction.
	 */
	public void metricFromPairwise() {
		System.out.println("----------------------------------------------------------------------------");
		System.out.println("### Metric Reconstruction");

		var metric = new MetricFromUncalibratedPairwiseGraph();
		metric.setVerbose(System.out, BoofMiscOps.hashSet(BoofVerbose.RECURSIVE));
		BoofMiscOps.profile(() -> {
			if (!metric.process(dbSimilar, dbCams, pairwise)) {
				System.err.println("Reconstruction failed");
				System.exit(0);
			}
		}, "Metric Reconstruction");

		working = metric.getLargestScene();

		var savePath = new File(workDirectory, "working.yaml");
		MultiViewIO.save(working, savePath.getPath());
	}

	/**
	 * Here the initial estimate found in the metric reconstruction is refined using Bundle Adjustment, which just
	 * means all parameters (camera, view pose, point location) are optimized all at once.
	 */
	public void bundleAdjustmentRefine() {
		System.out.println("----------------------------------------------------------------------------");
		System.out.println("Refining the scene");

		var refine = new RefineMetricWorkingGraph();
		BoofMiscOps.profile(() -> {
			// Bundle adjustment is run twice, with the worse 5% of points discarded in an attempt to reduce noise
			refine.metricSba.keepFraction = 0.95;
			refine.metricSba.getSba().setVerbose(System.out, null);
			if (!refine.process(dbSimilar, working)) {
				System.out.println("SBA REFINE FAILED");
			}
		}, "Bundle Adjustment refine");
		scene = refine.metricSba.structure;

		var savePath = new File(workDirectory, "structure.yaml");
		MultiViewIO.save(scene, savePath.getPath());
	}

	/**
	 * To visualize the results we will render a sparse point cloud along with the location of each camera in the
	 * scene.
	 */
	public void visualizeSparseCloud() {
		checkTrue(scene.isHomogenous());
		List<Point3D_F64> cloudXyz = new ArrayList<>();
		Point4D_F64 world = new Point4D_F64();

		// NOTE: By default the colors found below are not used. Look before to see why and how to turn them on.
		//
		// Colorize the cloud by reprojecting the images. The math is straight forward but there's a lot of book
		// keeping that needs to be done due to the scene data structure. A class is provided to make this process easy
		var imageLookup = new LookUpImageFilesByIndex(imageFiles);
		var colorize = new ColorizeMultiViewStereoResults<>(new LookUpColorRgbFormats.PL_U8(), imageLookup);

		DogArray_I32 rgb = new DogArray_I32();
		rgb.resize(scene.points.size);
		colorize.processScenePoints(scene,
				( viewIdx ) -> viewIdx + "", // String encodes the image's index
				( pointIdx, r, g, b ) -> rgb.set(pointIdx, (r << 16) | (g << 8) | b)); // Assign the RGB color

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

		PointCloudViewer viewer = VisualizeData.createPointCloudViewer();
		viewer.setFog(true);
		// We just did a bunch of work to look up the true color of points, however for sparse data it's easy to see
		// the structure with psuedo color. Comment out the line below to see the true color.
		viewer.setColorizer(new TwoAxisRgbPlane.Z_XY(1.0).fperiod(40));
		viewer.setDotSize(1);
		viewer.setTranslationStep(0.15);
		viewer.addCloud(( idx, p ) -> p.setTo(cloudXyz.get(idx)), rgb::get, rgb.size);
		viewer.setCameraHFov(UtilAngle.radian(60));

		SwingUtilities.invokeLater(() -> {
			// Show where the cameras are
			BoofSwingUtil.visualizeCameras(scene, viewer);

			// Size the window and show it to the user
			viewer.getComponent().setPreferredSize(new Dimension(600, 600));
			ShowImages.showWindow(viewer.getComponent(), "Refined Scene", true);

			DogArray<Point3dRgbI_F64> copy = new DogArray<>(Point3dRgbI_F64::new);
			viewer.copyCloud(copy);

			try (FileOutputStream out = new FileOutputStream("saved_cloud.ply")) {
				PointCloudIO.save3D(PointCloudIO.Format.PLY, PointCloudReader.wrapF64RGB(copy.toList()), true, out);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}
}

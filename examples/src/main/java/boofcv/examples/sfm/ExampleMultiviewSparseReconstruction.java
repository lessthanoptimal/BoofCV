/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.examples.sfm;

import boofcv.abst.feature.detect.interest.PointDetectorTypes;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.abst.tracker.PointTracker;
import boofcv.alg.mvs.ColorizeMultiViewStereoResults;
import boofcv.alg.scene.PointTrackerToSimilarImages;
import boofcv.alg.sfm.structure.*;
import boofcv.core.image.LookUpColorRgbFormats;
import boofcv.factory.feature.detect.interest.ConfigDetectInterestPoint;
import boofcv.factory.feature.detect.selector.ConfigSelectLimit;
import boofcv.factory.tracker.ConfigPointTracker;
import boofcv.factory.tracker.FactoryPointTracker;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.geo.MultiViewIO;
import boofcv.io.image.LookUpImageFilesByIndex;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.image.UtilImageIO;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.InterleavedU8;
import boofcv.visualize.PointCloudViewer;
import boofcv.visualize.TwoAxisRgbPlane;
import boofcv.visualize.VisualizeData;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import org.apache.commons.io.FilenameUtils;
import org.ddogleg.DDoglegConcurrency;
import org.ddogleg.struct.GrowQueue_I32;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

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
public class ExampleMultiviewSparseReconstruction {

	// Instead of processing all the frames just process the first N frames
	int maxFrames = 30;
	// Recompute everything and ignore previously saved results
	boolean forceRecompute = false;

	String workDirectory;
	List<String> imageFiles = new ArrayList<>();

	PairwiseImageGraph pairwise = null;
	LookupSimilarImages similarImages;
	SceneWorkingGraph working = null;
	SceneStructureMetric structure = null;

	public void compute( String videoName ) {
		String path = UtilIO.pathExample("mvs/" + videoName);
		workDirectory = "mvs_work/" + FilenameUtils.getBaseName(videoName);

		// Convert the video into an image sequence. Later on we will need to access the images in random order
		var imageDirectory = new File(workDirectory, "images");
		if (!imageDirectory.exists())
			checkTrue(imageDirectory.mkdirs(), "Failed to image directory");
		SimpleImageSequence<InterleavedU8> sequence = DefaultMediaManager.INSTANCE.openVideo(path, ImageType.IL_U8);
		System.out.println("----------------------------------------------------------------------------");
		System.out.println("### Decoding Video");
		BoofMiscOps.profile(() -> {
			int frame = 0;
			while (sequence.hasNext()) {
				File imageFile = new File(imageDirectory, String.format("frame%d.png", frame++));
				imageFiles.add(imageFile.getPath());
				if (imageFile.exists())
					continue;
				UtilImageIO.saveImage(sequence.next(), imageFile.getPath());
			}
		}, "Video Decoding");

		computePairwiseGraph();
		metricFromPairwise();
		bundleAdjustmentRefine();
	}

	/**
	 * For a pairwise graph to be constructed, point feature relationships between frame are needed. For a video
	 * sequence an easy and fast way to do this is with a KLT tracker. This however will not "close the loop"
	 * which if trying to create a model by going in a circle around it.
	 */
	private void trackImageFeatures() {
		System.out.println("----------------------------------------------------------------------------");
		System.out.println("### Creating Similar Images");
		int radius = 5;
		var configTracker = new ConfigPointTracker();
		configTracker.typeTracker = ConfigPointTracker.TrackerType.KLT;
		configTracker.klt.pruneClose = true;
		configTracker.klt.toleranceFB = 2;
		configTracker.klt.templateRadius = radius;
		configTracker.klt.maximumTracks = 800;
		configTracker.klt.config.maxIterations = 30;
		configTracker.detDesc.typeDetector = ConfigDetectInterestPoint.DetectorType.POINT;
		configTracker.detDesc.detectPoint.type = PointDetectorTypes.SHI_TOMASI;
		configTracker.detDesc.detectPoint.shiTomasi.radius = 6;
		configTracker.detDesc.detectPoint.general.radius = 4;
//		configTracker.detDesc.detectPoint.general.threshold = 0;
		configTracker.detDesc.detectPoint.general.selector = ConfigSelectLimit.selectUniform(2.0);

		PointTracker<GrayU8> tracker = FactoryPointTracker.tracker(configTracker, GrayU8.class, null);

		var trackerSimilar = new PointTrackerToSimilarImages();

		BoofMiscOps.profile(() -> {
			boolean first = true;
			for (int frameId = 0; frameId < imageFiles.size(); frameId++) {
				String filePath = imageFiles.get(frameId);
				GrayU8 frame = UtilImageIO.loadImage(filePath, GrayU8.class);
				if (first) {
					first = false;
					trackerSimilar.initialize(frame.width, frame.height);
				}
				tracker.process(frame);
				int active = tracker.getTotalActive();
				int dropped = tracker.getDroppedTracks(null).size();
				tracker.spawnTracks();
				trackerSimilar.processFrame(tracker);
				String id = frameId + "";//trackerSimilar.frames.getTail().frameID;
				System.out.println("frame id = " + id + " active=" + active + " dropped=" + dropped);

				// TODO drop tracks which have been viewed for too long to reduce the negative affects of track drift?

				// To keep things manageable only process the first few frames, if configured to do so
				if (frameId >= maxFrames)
					break;
			}
		}, "Tracking Features");

		similarImages = trackerSimilar;
	}

	/**
	 * This step attempts to determine which views have a 3D (not homographic) relationship with each other and which
	 * features are real and not fake.
	 */
	public void computePairwiseGraph() {
		var savePath = new File(workDirectory, "pairwise.yaml");
		if (!forceRecompute)
			try {
				pairwise = MultiViewIO.load(savePath.getPath(), (PairwiseImageGraph)null);
			} catch (UncheckedIOException ignore) {
			}

		if (pairwise == null) {
			trackImageFeatures();
			System.out.println("----------------------------------------------------------------------------");
			System.out.println("### Creating Pairwise");
			var generatePairwise = new GeneratePairwiseImageGraph();
			BoofMiscOps.profile(() -> {
				generatePairwise.setVerbose(System.out, null);
				generatePairwise.process(similarImages);
			}, "Created Pairwise graph");
			pairwise = generatePairwise.getGraph();
			MultiViewIO.save(pairwise, savePath.getPath());
			System.out.println("  nodes.size=" + pairwise.nodes.size);
			System.out.println("  edges.size=" + pairwise.edges.size);
		} else {
			System.out.println("Loaded Pairwise Graph");
		}
	}

	/**
	 * In this step a metric reconstruction is attempted using views with a 3D relationship. This is a tricky step
	 * and works by finding clusters of views which are likely to have numerically stable results then expanding
	 * the sparse metric reconstruction.
	 */
	public void metricFromPairwise() {
		var savePath = new File(workDirectory, "working.yaml");

		if (!forceRecompute)
			try {
				working = MultiViewIO.load(savePath.getPath(), pairwise, null);
			} catch (UncheckedIOException ignore) {}

		if (working == null) {
			System.out.println("----------------------------------------------------------------------------");
			System.out.println("### Metric Reconstruction");

			var metric = new MetricFromUncalibratedPairwiseGraph();
			metric.setVerbose(System.out, null);
			metric.getInitProjective().setVerbose(System.out, null);
			metric.getExpandMetric().setVerbose(System.out, null);
			PairwiseImageGraph _pairwise = pairwise;
			BoofMiscOps.profile(() -> {
				if (!metric.process(similarImages, _pairwise)) {
					System.err.println("Reconstruction failed");
					System.exit(0);
				}
			}, "Computed metric working graph");

			working = metric.getWorkGraph();
			MultiViewIO.save(working, savePath.getPath());
		} else {
			System.out.println("Loaded Metric Reconstruction");
		}
	}

	/**
	 * In this step we will consider all views, cameras, and points at once when optimizing. Unlike the previous
	 * step which was focused on local optimality and stability.
	 */
	public void bundleAdjustmentRefine() {
		var savePath = new File(workDirectory, "structure.yaml");

		if (!forceRecompute)
			try {
				structure = MultiViewIO.load(savePath.getPath(), (SceneStructureMetric)null);
			} catch (UncheckedIOException ignore) {}

		SceneWorkingGraph _working = working;
		if (structure == null) {
			System.out.println("----------------------------------------------------------------------------");
			System.out.println("Refining the scene");

			var refine = new RefineMetricWorkingGraph();
			BoofMiscOps.profile(() -> {
				refine.bundleAdjustment.keepFraction = 0.95;
				refine.bundleAdjustment.getSba().setVerbose(System.out, null);
				if (!refine.process(similarImages, _working)) {
					System.out.println("REFINE FAILED");
				}
			}, "SBA refine");
			structure = refine.bundleAdjustment.structure;
			MultiViewIO.save(structure, savePath.getPath());
		} else {
			System.out.println("Loaded Refined Scene");
		}
	}

	/**
	 * To visualize the results we will render a sparse point cloud along with the location of each camera in the
	 * scene.
	 */
	public void visualizeSparseCloud() {
		checkTrue(structure.isHomogenous());
		List<Point3D_F64> cloudXyz = new ArrayList<>();
		Point4D_F64 world = new Point4D_F64();

		// NOTE: By default the colors found below are not used. Look before to see why and how to turn them on.
		//
		// Colorize the cloud by reprojecting the images. The math is straight forward but there's a lot of book
		// keeping that needs to be done due to the scene data structure. A class is provided to make this process easy
		var imageLookup = new LookUpImageFilesByIndex(imageFiles);
		var colorize = new ColorizeMultiViewStereoResults<>(new LookUpColorRgbFormats.PL_U8(), imageLookup);

		GrowQueue_I32 rgb = new GrowQueue_I32();
		rgb.resize(structure.points.size);
		colorize.processScenePoints(structure,
				( viewIdx ) -> viewIdx + "", // String encodes the image's index
				( pointIdx, r, g, b ) -> rgb.set(pointIdx, (r << 16) | (g << 8) | b)); // Assign the RGB color

		// Convert the structure into regular 3D points from homogenous
		for (int i = 0; i < structure.points.size; i++) {
			structure.points.get(i).get(world);
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
		viewer.addCloud(cloudXyz, rgb.data);
		viewer.setCameraHFov(UtilAngle.radian(60));

		SwingUtilities.invokeLater(() -> {
			// Show where the cameras are
			BoofSwingUtil.visualizeCameras(structure, viewer);

			// Size the window and show it to the user
			viewer.getComponent().setPreferredSize(new Dimension(600, 600));
			ShowImages.showWindow(viewer.getComponent(), "Refined Scene", true);
		});
	}

	public static void main( String[] args ) {
		// Turn on threaded code for bundle adjustment
		DDoglegConcurrency.USE_CONCURRENT = true;

		var example = new ExampleMultiviewSparseReconstruction();
		example.compute("house_01.mp4");
//		example.compute("forest_path_01.mp4");
		example.visualizeSparseCloud();

		System.out.println("done");
	}
}

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
import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.abst.tracker.PointTracker;
import boofcv.alg.scene.PointTrackerToSimilarImages;
import boofcv.alg.sfm.structure.*;
import boofcv.factory.feature.detect.interest.ConfigDetectInterestPoint;
import boofcv.factory.feature.detect.selector.ConfigSelectLimit;
import boofcv.factory.tracker.ConfigPointTracker;
import boofcv.factory.tracker.FactoryPointTracker;
import boofcv.gui.image.ShowImages;
import boofcv.io.MediaManager;
import boofcv.io.geo.MultiViewIO;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import boofcv.visualize.PointCloudViewer;
import boofcv.visualize.VisualizeData;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import org.ddogleg.DDoglegConcurrency;
import org.ddogleg.struct.GrowQueue_I32;

import javax.swing.*;
import java.awt.*;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

import static boofcv.misc.BoofMiscOps.assertBoof;

/**
 * TODO comment
 *
 * @author Peter Abeles
 */
public class ExampleMultiviewSceneReconstruction {
	public static void main( String[] args ) {
		// Turn on threaded code for bundle adjustment
		DDoglegConcurrency.USE_CONCURRENT = true;

		MediaManager media = DefaultMediaManager.INSTANCE;

		boolean forceRecompute = true;

		PairwiseImageGraph pairwise = null;
		LookupSimilarImages similarImages; // TODO save and load similar images too
		SceneWorkingGraph working = null;

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

		String path = "20201020_111045_small.mp4";
//		String path = "20201022_155545_small.mp4";
		SimpleImageSequence<GrayU8> sequence = media.openVideo(path, ImageType.SB_U8);
		trackerSimilar.initialize(sequence.getWidth(), sequence.getHeight());

		BoofMiscOps.profile(()-> {
			while (sequence.hasNext()) {
				GrayU8 frame = sequence.next();
				tracker.process(frame);
				int active = tracker.getTotalActive();
				int dropped = tracker.getDroppedTracks(null).size();
				tracker.spawnTracks();
				trackerSimilar.processFrame(tracker);
				String id = trackerSimilar.frames.getTail().frameID;
				System.out.println("frame id = " + id + " active=" + active + " dropped=" + dropped);

				// Bad: 30 has a large error to start and doesn't converge well
				if (sequence.getFrameNumber() >= 40) // Good: 10,15,20,
					break;
			}
		}, "Tracking Features");
		similarImages = trackerSimilar;

		if (!forceRecompute)
			try {pairwise = MultiViewIO.load("pairwise.yaml", null);} catch (UncheckedIOException ignore) {}

		if (pairwise == null) {
			System.out.println("----------------------------------------------------------------------------");
			System.out.println("### Creating Pairwise");
			var generatePairwise = new GeneratePairwiseImageGraph();
			BoofMiscOps.profile(()-> {
				generatePairwise.setVerbose(System.out, null);
				generatePairwise.process(similarImages);
			},"Created Pairwise graph");
			pairwise = generatePairwise.getGraph();
			MultiViewIO.save(pairwise, "pairwise.yaml");
		}

		System.out.println("  nodes.size=" + pairwise.nodes.size);
		System.out.println("  edges.size=" + pairwise.edges.size);
		int nodesWithNo3D = 0;
		for (int i = 0; i < pairwise.nodes.size; i++) {
			PairwiseImageGraph.View n = pairwise.nodes.get(i);
			boolean found = false;
			for (int j = 0; j < n.connections.size; j++) {
				if (n.connections.get(j).is3D) {
					found = true;
					break;
				}
			}
			if (!found) {
				System.out.println("   no 3D in " + n.id);
				nodesWithNo3D++;
			}
		}
		System.out.println("  nodes with no 3D " + nodesWithNo3D);

		// TODO save / load SceneWorkingGraph
		System.out.println("----------------------------------------------------------------------------");
		System.out.println("### Metric Reconstruction");

		if (!forceRecompute)
			try {working = MultiViewIO.load("working.yaml", pairwise, null);} catch (UncheckedIOException ignore) {}

		if (working == null) {
			var metric = new MetricFromUncalibratedPairwiseGraph();
			metric.setVerbose(System.out, null);
			metric.getInitProjective().setVerbose(System.out, null);
			metric.getExpandMetric().setVerbose(System.out, null);
			PairwiseImageGraph _pairwise = pairwise;
			BoofMiscOps.profile(()-> {
				if (!metric.process(similarImages, _pairwise)) {
					System.err.println("Reconstruction failed");
					System.exit(0);
				}
			}, "Computed metric working graph");

			working = metric.getWorkGraph();
			MultiViewIO.save(working, "working.yaml");
		}

		System.out.println("----------------------------------------------------------------------------");
		System.out.println("Refining the scene");
		var refine = new RefineMetricWorkingGraph();
		SceneWorkingGraph _working = working;
		BoofMiscOps.profile(()->{
			refine.bundleAdjustment.keepFraction = 0.95;
			refine.bundleAdjustment.getSba().setVerbose(System.out, null);
			if (!refine.process(similarImages, _working)) {
				System.out.println("REFINE FAILED");
			}
		},"SBA refine");

		visualizeInPointCloud(path, refine.bundleAdjustment.getStructure(), refine.bundleAdjustment.getObservations());

		System.out.println("----------------------------------------------------------------------------");
		System.out.println("Printing view info");
		for (PairwiseImageGraph.View pv : pairwise.nodes.toList()) {
			var wv = working.lookupView(pv.id);
			if (wv == null)
				continue;
			int order = working.viewList.indexOf(wv);

			System.out.printf("view[%2d]='%2s' f=%6.1f k1=%6.3f k2=%6.3f t={%5.1f, %5.1f, %5.1f}\n", order, wv.pview.id,
					wv.intrinsic.f, wv.intrinsic.k1, wv.intrinsic.k2,
					wv.world_to_view.T.x, wv.world_to_view.T.y, wv.world_to_view.T.z);
		}

		System.out.println("done");
	}

	/**
	 * Looks up the color of each point in the cloud using the first frame it was observed in
	 */
	public static void colorizeCloud( String path , int totalFeatures,
									  SceneObservations observations, GrowQueue_I32 rgb )
	{
		rgb.resize(totalFeatures);
		rgb.fill(0);

		MediaManager media = DefaultMediaManager.INSTANCE;
		SimpleImageSequence<Planar<GrayU8>> sequence = media.openVideo(path, ImageType.PL_U8);

		var pixel = new PointIndex2D_F64();

		int frameID = 0;
		while (sequence.hasNext()) {
			if (frameID >= observations.views.size)
				break;
			Planar<GrayU8> frame = sequence.next();

			double cx = frame.width/2;
			double cy = frame.height/2;

			SceneObservations.View v = observations.getView(frameID);

			for (int i = 0; i < v.size(); i++) {
				v.get(i, pixel);

				// skip if color is already known
				if (rgb.get(pixel.index) != 0)
					continue;

				pixel.x += cx;
				pixel.y += cy;

				rgb.set(pixel.index, frame.get24u8((int)pixel.x, (int)pixel.y));
			}
		}

		sequence.close();
	}

	public static void visualizeInPointCloud( String path, SceneStructureMetric structure,
											  SceneObservations observations ) {

		List<Point3D_F64> cloudXyz = new ArrayList<>();
		Point4D_F64 world = new Point4D_F64();

		assertBoof(structure.isHomogenous());

		// Convert the structure into regular 3D points from homogenous
		for (int i = 0; i < structure.points.size; i++) {
			structure.points.get(i).get(world);
			if (world.w==0.0)
				continue;
			cloudXyz.add(new Point3D_F64(world.x/world.w, world.y/world.w, world.z/world.w));
		}

		GrowQueue_I32 rgb = new GrowQueue_I32();
		colorizeCloud( path, structure.points.size, observations, rgb);

		PointCloudViewer viewer = VisualizeData.createPointCloudViewer();
		viewer.setFog(true);
//		viewer.setColorizer(new SingleAxisRgb.Z().fperiod(20)); // makes it easier to see points without RGB color
		viewer.setDotSize(1);
		viewer.setTranslationStep(0.15);
		viewer.addCloud(cloudXyz, rgb.data);
		viewer.setCameraHFov(UtilAngle.radian(60));

		SwingUtilities.invokeLater(() -> {
			viewer.getComponent().setPreferredSize(new Dimension(600, 600));
			ShowImages.showWindow(viewer.getComponent(), "Refined Scene", true);
		});
	}
}

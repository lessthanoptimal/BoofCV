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
import boofcv.alg.geo.RectifyImageOps;
import boofcv.alg.mvs.*;
import boofcv.alg.scene.PointTrackerToSimilarImages;
import boofcv.alg.sfm.structure.*;
import boofcv.core.image.LookUpColorRgbFormats;
import boofcv.factory.disparity.ConfigDisparityBMBest5;
import boofcv.factory.disparity.FactoryStereoDisparity;
import boofcv.factory.feature.detect.interest.ConfigDetectInterestPoint;
import boofcv.factory.feature.detect.selector.ConfigSelectLimit;
import boofcv.factory.tracker.ConfigPointTracker;
import boofcv.factory.tracker.FactoryPointTracker;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.gui.stereo.RectifiedPairPanel;
import boofcv.io.UtilIO;
import boofcv.io.geo.MultiViewIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.struct.image.*;
import boofcv.visualize.PointCloudViewer;
import boofcv.visualize.SingleAxisRgb;
import boofcv.visualize.VisualizeData;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import org.ddogleg.DDoglegConcurrency;
import org.ddogleg.struct.GrowQueue_I32;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * TODO comment
 *
 * @author Peter Abeles
 */
public class ExampleMultiviewSceneReconstruction {

	static class LookUpImageFiles implements LookUpImages {
		List<String> paths;
		ImageDimension dimension = new ImageDimension();

		public LookUpImageFiles( List<String> paths ) {
			BoofMiscOps.checkTrue(paths.size()>0);
			this.paths = paths;

			if (paths.size()==0)
				return;

			BufferedImage b = UtilImageIO.loadImage(paths.get(0));
			dimension.width = b.getWidth();
			dimension.height = b.getHeight();
		}

		@Override public boolean loadShape( String name, ImageDimension shape ) {
			int index = Integer.parseInt(name);
			if (index<0 || index>=paths.size())
				return false;
			shape.setTo(dimension);
			return true;
		}

		@Override public <LT extends ImageBase<LT>> boolean loadImage( String name, LT output ) {
			int index = Integer.parseInt(name);
			if (index<0 || index>=paths.size())
				return false;

			UtilImageIO.loadImage(paths.get(index), true, output);

			return true;
		}
	}

	public static void main( String[] args ) {
		// Turn on threaded code for bundle adjustment
		DDoglegConcurrency.USE_CONCURRENT = true;

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

		String path = "desert/";
//		String path = "20201022_155545_small.mp4";

		List<String> imageFiles = UtilIO.listAll(path);
		Collections.sort(imageFiles);

//		SimpleImageSequence<GrayU8> sequence = media.openVideo(path, ImageType.SB_U8);

		BoofMiscOps.profile(()-> {
			boolean first = true;
			for (int frameId = 0; frameId < imageFiles.size(); frameId++) {
				String filePath = imageFiles.get(frameId);
				GrayU8 frame = UtilImageIO.loadImage(filePath,GrayU8.class);
				if (first) {
					first = false;
					trackerSimilar.initialize(frame.width, frame.height);
				}
				tracker.process(frame);
				int active = tracker.getTotalActive();
				int dropped = tracker.getDroppedTracks(null).size();
				tracker.spawnTracks();
				trackerSimilar.processFrame(tracker);
				String id = frameId+"";//trackerSimilar.frames.getTail().frameID;
				System.out.println("frame id = " + id + " active=" + active + " dropped=" + dropped);

				// Bad: 30 has a large error to start and doesn't converge well
				if (frameId >= 15) // Good: 10,15,20,
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

//		visualizeInPointCloud(imageFiles, refine.bundleAdjustment.getStructure(), refine.bundleAdjustment.getObservations());

		var configDisparity = new ConfigDisparityBMBest5();
		configDisparity.validateRtoL = 1;
		configDisparity.texture = 0.5;
		configDisparity.regionRadiusX = configDisparity.regionRadiusY = 4;
		configDisparity.disparityRange = 50;

		var imageLookup = new LookUpImageFiles(imageFiles);
		var mvs = new MultiViewStereoFromKnownSceneStructure<>(imageLookup, ImageType.SB_U8);
		mvs.setVerbose(System.out,null);
//		mvs.setMinimumQuality3D(0.75);
		mvs.setStereoDisparity(FactoryStereoDisparity.blockMatchBest5(configDisparity,GrayU8.class,GrayF32.class));
		mvs.setListener(new MultiViewStereoFromKnownSceneStructure.Listener<>() {
			@Override
			public void handlePairDisparity( String left, String right, GrayU8 rect0, GrayU8 rect1, GrayF32 disparity, GrayU8 mask, DisparityParameters parameters ) {
				System.out.println("Paired stereo: "+left+" "+right);

				// remove annoying false points
				RectifyImageOps.applyMask(disparity, mask, 0);

				BufferedImage rbuff0 = ConvertBufferedImage.convertTo(rect0, null);
				BufferedImage rbuff1 = ConvertBufferedImage.convertTo(rect1, null);

				var rectPanel = new RectifiedPairPanel(true,rbuff0,rbuff1);
				rectPanel.setPreferredSize(new Dimension(1000,500));
				BufferedImage colorized = VisualizeImageData.disparity(disparity, null, parameters.disparityRange, 0);
				ShowImages.showWindow(colorized, "left "+left+" right "+right);
				ShowImages.showWindow(rectPanel, "Rectified left "+left+" right "+right);
			}

			@Override
			public void handleFusedDisparity( String name, GrayF32 disparity, GrayU8 mask, DisparityParameters parameters ) {
				System.out.println("Fused center view "+name);
				BufferedImage colorized = VisualizeImageData.disparity(disparity, null, parameters.disparityRange, 0);
				ShowImages.showWindow(colorized, "Center "+name);
			}
		});

		var mvsGraph = new StereoPairGraph();

		PairwiseImageGraph _pairwise = pairwise;
		BoofMiscOps.forIdx(working.viewList,(i,wv)->mvsGraph.addVertex(wv.pview.id, i));
		BoofMiscOps.forIdx(working.viewList,(workIdxI,wv)->{
			var pv = _pairwise.mapNodes.get(wv.pview.id);
			System.out.println("view.id="+wv.pview.id+" indexSba="+workIdxI);
			pv.connections.forIdx(( j, e ) -> {
				PairwiseImageGraph.View po = e.other(pv);
				double ratio = 1.0 - Math.min(1.0, e.countH/(1.0 + e.countF));
				if (ratio <= 0.05)
					return;
				SceneWorkingGraph.View wvo = _working.views.get(po.id);
				int workIdxO = _working.viewList.indexOf(wvo);
				if (workIdxO <=workIdxI)
					return;
				mvsGraph.connect(pv.id, po.id, ratio);
			});
			});


		BoofMiscOps.profile(()-> {
			mvs.process(refine.bundleAdjustment.getStructure(), mvsGraph);
		},"MVS Cloud");

		GrowQueue_I32 colorRgb = new GrowQueue_I32();
		colorRgb.resize(mvs.getCloud().size());
		var colorizeMvs = new ColorizeMultiViewStereoResults<>(new LookUpColorRgbFormats.PL_U8(),imageLookup);
		colorizeMvs.process(refine.bundleAdjustment.getStructure(), mvs, (i,r,g,b)->colorRgb.set(i,(r<<16)|(g<<8)|b));
		visualizeInPointCloud(mvs.getCloud(),colorRgb);

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
	public static void colorizeCloud( List<String> imagePaths , int totalFeatures,
									  SceneObservations observations, GrowQueue_I32 rgb )
	{
		rgb.resize(totalFeatures);
		rgb.fill(0);

		var pixel = new PointIndex2D_F64();

		for (int frameID = 0; frameID < imagePaths.size(); frameID++) {
			if (frameID >= observations.views.size)
				break;

			Planar<GrayU8> frame = UtilImageIO.loadImage(new File(imagePaths.get(frameID)),true,ImageType.PL_U8);

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
	}

	public static void visualizeInPointCloud( List<Point3D_F64> cloud, GrowQueue_I32 colorsRgb ) {
		PointCloudViewer viewer = VisualizeData.createPointCloudViewer();
		viewer.setFog(true);
		viewer.setDotSize(1);
		viewer.setTranslationStep(0.15);
		if (colorsRgb!=null) {
			viewer.addCloud(cloud,colorsRgb.data);
		} else {
			viewer.addCloud(cloud);
			viewer.setColorizer(new SingleAxisRgb.Z().fperiod(30));
		}
		viewer.setCameraHFov(UtilAngle.radian(60));

		SwingUtilities.invokeLater(() -> {
			viewer.getComponent().setPreferredSize(new Dimension(600, 600));
			ShowImages.showWindow(viewer.getComponent(), "Refined Scene", true);
		});
	}

	public static void visualizeInPointCloud( List<String> imageFiles, SceneStructureMetric structure,
											  SceneObservations observations ) {

		List<Point3D_F64> cloudXyz = new ArrayList<>();
		Point4D_F64 world = new Point4D_F64();

		BoofMiscOps.checkTrue(structure.isHomogenous());

		// Convert the structure into regular 3D points from homogenous
		for (int i = 0; i < structure.points.size; i++) {
			structure.points.get(i).get(world);
			if (world.w==0.0)
				continue;
			cloudXyz.add(new Point3D_F64(world.x/world.w, world.y/world.w, world.z/world.w));
		}

		GrowQueue_I32 rgb = new GrowQueue_I32();
		colorizeCloud( imageFiles, structure.points.size, observations, rgb);

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

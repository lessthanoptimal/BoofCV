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

import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.mvs.ColorizeMultiViewStereoResults;
import boofcv.alg.mvs.DisparityParameters;
import boofcv.alg.mvs.MultiViewStereoFromKnownSceneStructure;
import boofcv.alg.mvs.StereoPairGraph;
import boofcv.alg.sfm.structure.PairwiseImageGraph;
import boofcv.alg.sfm.structure.SceneWorkingGraph;
import boofcv.core.image.LookUpColorRgbFormats;
import boofcv.factory.disparity.ConfigDisparitySGM;
import boofcv.factory.disparity.FactoryStereoDisparity;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.image.LookUpImageFilesByIndex;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.visualize.PointCloudViewer;
import boofcv.visualize.VisualizeData;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point3D_F64;
import org.ddogleg.struct.DogArray_I32;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * A dense point cloud is created using a previously computed sparse reconstruction and a basic implementation of
 * multiview stereo (MVS). This approach to MVS works by identifying "center" views which have the best set of
 * neighbors for stereo computations using a heuristic. Then a global point cloud is created from the "center" view
 * disparity images while taking care to avoid adding duplicate points.
 *
 * As you can see there is still a fair amount of noise in the cloud. Additional filtering and processing
 * is typically required at this point.
 *
 * @author Peter Abeles
 */
public class ExampleMultiViewDenseReconstruction {
	public static void main( String[] args ) {
		var example = new ExampleMultiViewSparseReconstruction();
		example.compute("house_01.mp4");
//		example.compute("forest_path_01.mp4");
//		example.compute("rock_01.mp4");

		// SGM is a reasonable trade between quality and speed.
		var configSgm = new ConfigDisparitySGM();
		configSgm.validateRtoL = 0;
		configSgm.texture = 0.75;
		configSgm.disparityRange = 120;
		configSgm.paths = ConfigDisparitySGM.Paths.P4;
		configSgm.configBlockMatch.radiusX = 3;
		configSgm.configBlockMatch.radiusY = 3;

		// Looks up images based on their index in the file list
		var imageLookup = new LookUpImageFilesByIndex(example.imageFiles);

		// Create and configure MVS
		//
		// Note that the stereo disparity algorithm used must output a GrayF32 disparity image as much of the code
		// is hard coded to use it. MVS would not work without sub-pixel enabled.
		var mvs = new MultiViewStereoFromKnownSceneStructure<>(imageLookup, ImageType.SB_U8);
		mvs.setStereoDisparity(FactoryStereoDisparity.sgm(configSgm, GrayU8.class, GrayF32.class));
		// Improve stereo by removing small regions, which tends to be noise. Consider adjusting the region size.
		mvs.getComputeFused().setDisparitySmoother(FactoryStereoDisparity.removeSpeckle(null, GrayF32.class));
		// Print out profiling info from multi baseline stereo
		mvs.getComputeFused().setVerboseProfiling(System.out);

		// Grab intermediate results as they are computed
		mvs.setListener(new MultiViewStereoFromKnownSceneStructure.Listener<>() {
			@Override
			public void handlePairDisparity( String left, String right, GrayU8 rect0, GrayU8 rect1,
											 GrayF32 disparity, GrayU8 mask, DisparityParameters parameters ) {
				// Displaying individual stereo pair results can be very useful for debugging, but this isn't done
				// because of the amount of information it would show
			}

			@Override
			public void handleFusedDisparity( String name,
											  GrayF32 disparity, GrayU8 mask, DisparityParameters parameters ) {
				// Display the disparity for each center view
				BufferedImage colorized = VisualizeImageData.disparity(disparity, null, parameters.disparityRange, 0);
				ShowImages.showWindow(colorized, "Center " + name);
			}
		});

		// MVS stereo needs to know which view pairs have enough 3D information to act as a stereo pair and
		// the quality of that 3D information. This is used to guide which views act as "centers" for accumulating
		// 3D information which is then converted into the point cloud.
		//
		// StereoPairGraph contains this information and we will create it from Pairwise and Working graphs.

		var mvsGraph = new StereoPairGraph();
		PairwiseImageGraph _pairwise = example.pairwise;
		SceneStructureMetric _structure = example.scene;
		// Add a vertex for each view
		BoofMiscOps.forIdx(example.working.viewList, ( i, wv ) -> mvsGraph.addVertex(wv.pview.id, i));
		// Compute the 3D score for each connected view
		BoofMiscOps.forIdx(example.working.viewList, ( workIdxI, wv ) -> {
			var pv = _pairwise.mapNodes.get(wv.pview.id);
			pv.connections.forIdx(( j, e ) -> {
				// Look at the ratio of inliers for a homography and fundamental matrix model
				PairwiseImageGraph.View po = e.other(pv);
				double ratio = 1.0 - Math.min(1.0, e.countH/(1.0 + e.countF));
				if (ratio <= 0.25)
					return;
				// There is sufficient 3D information between these two views
				SceneWorkingGraph.View wvo = example.working.views.get(po.id);
				int workIdxO = example.working.viewList.indexOf(wvo);
				if (workIdxO <= workIdxI)
					return;
				mvsGraph.connect(pv.id, po.id, ratio);
			});
		});

		// Compute the dense 3D point cloud
		BoofMiscOps.profile(() -> mvs.process(_structure, mvsGraph), "MVS Cloud");

		System.out.println("Dense Cloud Size: " + mvs.getCloud().size());

		// Colorize the cloud to make it easier to view. This is done by projecting points back into the
		// first view they were seen in and reading the color
		DogArray_I32 colorRgb = new DogArray_I32();
		colorRgb.resize(mvs.getCloud().size());
		var colorizeMvs = new ColorizeMultiViewStereoResults<>(new LookUpColorRgbFormats.PL_U8(), imageLookup);
		colorizeMvs.processMvsCloud(example.scene, mvs,
				( idx, r, g, b ) -> colorRgb.set(idx, (r << 16) | (g << 8) | b));
		visualizeInPointCloud(mvs.getCloud(), colorRgb, example.scene);
	}

	public static void visualizeInPointCloud( List<Point3D_F64> cloud, DogArray_I32 colorsRgb,
											  SceneStructureMetric structure ) {
		PointCloudViewer viewer = VisualizeData.createPointCloudViewer();
		viewer.setFog(true);
		viewer.setDotSize(1);
		viewer.setTranslationStep(0.15);
		viewer.addCloud(cloud, colorsRgb.data);
//		viewer.setColorizer(new TwoAxisRgbPlane.Z_XY(1.0).fperiod(40));
		viewer.setCameraHFov(UtilAngle.radian(60));

		SwingUtilities.invokeLater(() -> {
			// Show where the cameras are
			BoofSwingUtil.visualizeCameras(structure, viewer);

			// Display the point cloud
			viewer.getComponent().setPreferredSize(new Dimension(600, 600));
			ShowImages.showWindow(viewer.getComponent(), "Dense Reconstruction Cloud", true);
		});
	}
}

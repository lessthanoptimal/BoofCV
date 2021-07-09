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
import boofcv.alg.cloud.PointCloudReader;
import boofcv.alg.cloud.PointCloudUtils_F64;
import boofcv.alg.mvs.DisparityParameters;
import boofcv.alg.mvs.MultiViewStereoFromKnownSceneStructure;
import boofcv.alg.structure.SparseSceneToDenseCloud;
import boofcv.factory.disparity.ConfigDisparity;
import boofcv.factory.disparity.ConfigDisparitySGM;
import boofcv.factory.structure.ConfigSparseToDenseCloud;
import boofcv.factory.structure.FactorySceneReconstruction;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.image.LookUpImageFilesByIndex;
import boofcv.io.points.PointCloudIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.Point3dRgbI_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.visualize.PointCloudViewer;
import boofcv.visualize.VisualizeData;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point3D_F64;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * A dense point cloud is created using a previously computed sparse reconstruction and a basic implementation of
 * multiview stereo (MVS). This approach to MVS works by identifying "center" views which have the best set of
 * neighbors for stereo computations using a heuristic. Then a global point cloud is created from the "center" view
 * disparity images while taking care to avoid adding duplicate points.
 *
 * @author Peter Abeles
 */
public class ExampleMultiViewDenseReconstruction {
	public static void main( String[] args ) {
		var example = new ExampleMultiViewSparseReconstruction();
		example.compute("tree_snow_01.mp4", true);
//		example.compute("ditch_02.mp4", true);
//		example.compute("holiday_display_01.mp4", true);
//		example.compute("log_building_02.mp4", true);
//		example.compute("drone_park_01.mp4", false);
//		example.compute("stone_sign.mp4", true);

		// Looks up images based on their index in the file list
		var imageLookup = new LookUpImageFilesByIndex(example.imageFiles);

		// We will use a high level algorithm that does almost all the work for us. It is highly configurable
		// and just about every parameter can be tweaked using its Config. Internal algorithms can be accessed
		// and customize directly if needed. Specifics for how it work is beyond this example but the code
		// is easily accessible.

		// Let's do some custom configuration for this scenario
		var config = new ConfigSparseToDenseCloud();
		config.disparity.approach = ConfigDisparity.Approach.SGM;
		ConfigDisparitySGM configSgm = config.disparity.approachSGM;
		configSgm.validateRtoL = 0;
		configSgm.texture = 0.75;
		configSgm.disparityRange = 250;
		configSgm.paths = ConfigDisparitySGM.Paths.P4;
		configSgm.configBlockMatch.radiusX = 3;
		configSgm.configBlockMatch.radiusY = 3;

		// Create the sparse to dense reconstruction using a factory
		SparseSceneToDenseCloud<GrayU8> sparseToDense =
				FactorySceneReconstruction.sparseSceneToDenseCloud(config, ImageType.SB_U8);

		// To help make the time go by faster while we wait about 1 to 2 minutes for it to finish, let's print stuff
		sparseToDense.getMultiViewStereo().setVerbose(
				System.out, BoofMiscOps.hashSet(BoofVerbose.RECURSIVE, BoofVerbose.RUNTIME));

		// To visualize intermediate results we will add a listener. This will show fused disparity images
		sparseToDense.getMultiViewStereo().setListener(new MultiViewStereoFromKnownSceneStructure.Listener<>() {
			@Override
			public void handlePairDisparity( String left, String right, GrayU8 rect0, GrayU8 rect1,
											 GrayF32 disparity, GrayU8 mask, DisparityParameters parameters ) {
				// Uncomment to display individual stereo pairs. Commented out by default because it generates
				// a LOT of windows
//				BufferedImage outLeft = ConvertBufferedImage.convertTo(rect0, null);
//				BufferedImage outRight = ConvertBufferedImage.convertTo(rect1, null);
//
//				ShowImages.showWindow(new RectifiedPairPanel(true, outLeft, outRight), "Rectification: "+left+" "+right);
//				BufferedImage colorized = VisualizeImageData.disparity(disparity, null, parameters.disparityRange, 0);
//				ShowImages.showWindow(colorized, "Disparity " + left + " " + right);
			}

			@Override
			public void handleFusedDisparity( String name,
											  GrayF32 disparity, GrayU8 mask, DisparityParameters parameters ) {
				// You can also do custom filtering of the disparity image in this function. If the line below is
				// uncommented then points which are far away will be marked as invalid
//				PixelMath.operator1(disparity, ( v ) -> v >= 20 ? v : parameters.disparityRange, disparity);

				// Display the disparity for each center view
				BufferedImage colorized = VisualizeImageData.disparity(disparity, null, parameters.disparityRange, 0);
				ShowImages.showWindow(colorized, "Center " + name);
			}
		});

		// It needs a look up table to go from SBA view index to image name. It loads images as needed to perform
		// stereo disparity
		var viewToId = new TIntObjectHashMap<String>();
		BoofMiscOps.forIdx(example.working.listViews, ( workIdxI, wv ) -> viewToId.put(wv.index, wv.pview.id));
		if (!sparseToDense.process(example.scene, viewToId, imageLookup))
			throw new RuntimeException("Dense reconstruction failed!");

		saveCloudToDisk(sparseToDense);

		// Display the dense cloud
		visualizeInPointCloud(sparseToDense.getCloud(), sparseToDense.getColorRgb(), example.scene);
	}

	private static void saveCloudToDisk( SparseSceneToDenseCloud<GrayU8> sparseToDense ) {
		// Save the dense point cloud to disk in PLY format
		try (FileOutputStream out = new FileOutputStream("saved_cloud.ply")) {
			// Filter points which are far away to make it easier to view in 3rd party viewers that auto scale
			// You might need to adjust the threshold for your application if too many points are cut
			double distanceThreshold = 50.0;
			List<Point3D_F64> cloud = sparseToDense.getCloud();
			DogArray_I32 colorsRgb = sparseToDense.getColorRgb();

			DogArray<Point3dRgbI_F64> filtered = PointCloudUtils_F64.filter(
					( idx, p ) -> p.setTo(cloud.get(idx)), colorsRgb::get, cloud.size(),
					( idx ) -> cloud.get(idx).norm() <= distanceThreshold, null);

			PointCloudIO.save3D(PointCloudIO.Format.PLY, PointCloudReader.wrapF64RGB(filtered.toList()), true, out);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void visualizeInPointCloud( List<Point3D_F64> cloud, DogArray_I32 colorsRgb,
											  SceneStructureMetric structure ) {
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
			viewer.getComponent().setPreferredSize(new Dimension(600, 600));
			ShowImages.showWindow(viewer.getComponent(), "Dense Reconstruction Cloud", true);
		});
	}
}

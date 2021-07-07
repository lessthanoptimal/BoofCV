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

import boofcv.alg.distort.brown.LensDistortionBrown;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.alg.geo.bundle.BundleAdjustmentOps;
import boofcv.alg.mvs.DisparityParameters;
import boofcv.alg.mvs.MultiBaselineStereoIndependent;
import boofcv.alg.mvs.MultiViewStereoOps;
import boofcv.alg.structure.SceneWorkingGraph;
import boofcv.factory.disparity.ConfigDisparityBMBest5;
import boofcv.factory.disparity.FactoryStereoDisparity;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.gui.stereo.RectifiedPairPanel;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.LookUpImageFilesByIndex;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.distort.PointToPixelTransform_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.InterleavedU8;
import boofcv.visualize.PointCloudViewer;
import boofcv.visualize.VisualizeData;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point3D_F64;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Multi Baseline Stereo (MBS) is a problem where you have one common/center image with multiple paired images that have
 * different baselines from each other and the goal is to output a single combined stereo image. In BoofCV, this
 * combined stereo image will be the original image's pixel as compared to the more standard rectified image. At
 * the time of this writing, BoofCV only contains one MBS algorithm, which is just about the simplest one possible
 * and works by combining independently computed disparity images together using a median filter to select the output.
 *
 * @author Peter Abeles
 */
public class ExampleMultiBaselineStereo {
	public static void main( String[] args ) {
		// Compute a sparse reconstruction. This will give us intrinsic and extrinsic for all views
		var example = new ExampleMultiViewSparseReconstruction();
		// Specifies the "center" frame to use
		int centerViewIdx = 15;
		example.compute("tree_snow_01.mp4", true);
//		example.compute("ditch_02.mp4", true);
//		example.compute("holiday_display_01.mp4"", true);
//		example.compute("log_building_02.mp4"", true);
//		example.compute("drone_park_01.mp4", false);
//		example.compute("stone_sign.mp4", true);

		// We need a way to load images based on their ID. In this particular case the ID encodes the array index.
		var imageLookup = new LookUpImageFilesByIndex(example.imageFiles);

		// Next we tell it which view to use as the "center", which acts as the common view for all disparity images.
		// The process of selecting the best views to use as centers is a problem all it's own. To keep things
		// we just pick a frame.
		SceneWorkingGraph.View center = example.working.getAllViews().get(centerViewIdx);

		// The final scene refined by bundle adjustment is created by the Working graph. However the 3D relationship
		// between views is contained in the pairwise graph. A View in the working graph has a reference to the view
		// in the pairwise graph. Using that we will find all connected views that have a 3D relationship
		DogArray_I32 pairedViewIdxs = new DogArray_I32();
		TIntObjectMap<String> sbaIndexToImageID = new TIntObjectHashMap<>();

		// This relationship between pairwise and working graphs might seem (and is) a bit convoluted. The Pairwise
		// graph is the initial crude sketch of what might be connected. The working graph is an intermediate
		// data structure for computing the metric scene. SBA is a refinement of the working graph.

		// Iterate through all connected views in the pairwise graph and mark their indexes in the working graph
		center.pview.connections.forEach(( m ) -> {
			// if there isn't a 3D relationship just skip it
			if (!m.is3D)
				return;

			String connectedID = m.other(center.pview).id;
			SceneWorkingGraph.View connected = example.working.views.get(connectedID);

			// Make sure the pairwise view exists in the working graph too
			if (connected == null)
				return;

			// Add this view to the index to name/ID lookup table
			sbaIndexToImageID.put(connected.index, connectedID);

			// Note that this view is one which acts as the second image in the stereo pair
			pairedViewIdxs.add(connected.index);
		});

		// Add the center camera image to the ID look up table
		sbaIndexToImageID.put(centerViewIdx, center.pview.id);

		// Configure there stereo disparity algorithm which is used
		var configDisparity = new ConfigDisparityBMBest5();
		configDisparity.validateRtoL = 1;
		configDisparity.texture = 0.5;
		configDisparity.regionRadiusX = configDisparity.regionRadiusY = 4;
		configDisparity.disparityRange = 120;

		// This is the actual MBS algorithm mentioned previously. It selects the best disparity for each pixel
		// in the original image using a median filter.
		var multiBaseline = new MultiBaselineStereoIndependent<>(imageLookup, ImageType.SB_U8);
		multiBaseline.setStereoDisparity(
				FactoryStereoDisparity.blockMatchBest5(configDisparity, GrayU8.class, GrayF32.class));

		// Print out verbose debugging and profile information
		multiBaseline.setVerbose(System.out, null);
		multiBaseline.setVerboseProfiling(System.out);

		// Improve stereo by removing small regions, which tends to be noise. Consider adjusting the region size.
		multiBaseline.setDisparitySmoother(FactoryStereoDisparity.removeSpeckle(null, GrayF32.class));
		// Print out debugging information from the smoother
		//Objects.requireNonNull(multiBaseline.getDisparitySmoother()).setVerbose(System.out,null);

		// Creates a list where you can switch between different images/visualizations
		var listDisplay = new ListDisplayPanel();
		listDisplay.setPreferredSize(new Dimension(1000, 300));
		ShowImages.showWindow(listDisplay, "Intermediate Results", true);

		// We will display intermediate results as they come in
		multiBaseline.setListener(( leftView, rightView, rectLeft, rectRight,
									disparity, mask, parameters, rect ) -> {
			// Visualize the rectified stereo pair. You can interact with this window and verify
			// that the y-axis is  aligned
			var rectified = new RectifiedPairPanel(true);
			rectified.setImages(ConvertBufferedImage.convertTo(rectLeft, null),
					ConvertBufferedImage.convertTo(rectRight, null));

			// Cleans up the disparity image by zeroing out pixels that are outside the original image bounds
			RectifyImageOps.applyMask(disparity, mask, 0);
			// Display the colorized disparity
			BufferedImage colorized = VisualizeImageData.disparity(disparity, null, parameters.disparityRange, 0);

			SwingUtilities.invokeLater(() -> {
				listDisplay.addItem(rectified, "Rectified " + leftView + " " + rightView);
				listDisplay.addImage(colorized, leftView + " " + rightView);
			});
		});

		// Process the images and compute a single combined disparity image
		if (!multiBaseline.process(example.scene, center.index, pairedViewIdxs, sbaIndexToImageID::get)) {
			throw new RuntimeException("Failed to fuse stereo views");
		}

		// Extract the point cloud from the fused disparity image
		GrayF32 fusedDisparity = multiBaseline.getFusedDisparity();
		DisparityParameters fusedParam = multiBaseline.getFusedParam();
		BufferedImage colorizedDisp = VisualizeImageData.disparity(fusedDisparity, null, fusedParam.disparityRange, 0);
		ShowImages.showWindow(colorizedDisp, "Fused Disparity");

		// Now compute the point cloud it represents and the color of each pixel.
		// For the fused image, instead of being in rectified image coordinates it's in the original image coordinates
		// this makes extracting color much easier.
		var cloud = new DogArray<>(Point3D_F64::new);
		var cloudRgb = new DogArray_I32(cloud.size);
		// Load the center image in color
		var colorImage = new InterleavedU8(1, 1, 3);
		imageLookup.loadImage(center.pview.id, colorImage);
		// Since the fused image is in the original (i.e. distorted) pixel coordinates and is not rectified,
		// that needs to be taken in account by undistoring the image to create the point cloud.
		CameraPinholeBrown intrinsic = BundleAdjustmentOps.convert(example.scene.cameras.get(center.index).model,
				colorImage.width, colorImage.height, null);
		Point2Transform2_F64 pixel_to_norm = new LensDistortionBrown(intrinsic).distort_F64(true, false);
		MultiViewStereoOps.disparityToCloud(fusedDisparity, fusedParam, new PointToPixelTransform_F64(pixel_to_norm),
				( pixX, pixY, x, y, z ) -> {
					cloud.grow().setTo(x, y, z);
					cloudRgb.add(colorImage.get24(pixX, pixY));
				});

		// Configure the point cloud viewer
		PointCloudViewer pcv = VisualizeData.createPointCloudViewer();
		pcv.setCameraHFov(UtilAngle.radian(70));
		pcv.setTranslationStep(0.15);
		pcv.addCloud(cloud.toList(), cloudRgb.data);
//		pcv.setColorizer(new SingleAxisRgb.Z().fperiod(30.0));
		JComponent viewer = pcv.getComponent();
		viewer.setPreferredSize(new Dimension(600, 600));
		ShowImages.showWindow(viewer, "Point Cloud", true);

		System.out.println("Done");
	}
}

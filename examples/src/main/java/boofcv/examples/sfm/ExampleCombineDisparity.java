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

import boofcv.alg.distort.brown.LensDistortionBrown;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.alg.geo.bundle.BundleAdjustmentOps;
import boofcv.alg.mvs.ColorizeCloudFromImage;
import boofcv.alg.mvs.DisparityParameters;
import boofcv.alg.mvs.MultiViewStereoOps;
import boofcv.alg.mvs.MultiViewToFusedDisparity;
import boofcv.alg.sfm.structure.SceneWorkingGraph;
import boofcv.core.image.LookUpColorRgbFormats;
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
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.visualize.PointCloudViewer;
import boofcv.visualize.VisualizeData;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * This example shows how multiple disparity images computed with a common "center" image can be combined
 * into a single disparity image that has less noise and simplify processing. This is a common intermediate
 * step in a MVS pipeline.
 *
 * @author Peter Abeles
 */
public class ExampleCombineDisparity {
	public static void main( String[] args ) {
		// Compute a sparse reconstruction. This will give us intrinsic and extrinsic for all views
		var example = new ExampleMultiviewSparseReconstruction();
		example.compute("forest_path_01.mp4");

		// We need a way to load images based on their ID. In this particular case the ID encodes the array index.
		var imageLookup = new LookUpImageFilesByIndex(example.imageFiles);

		// The next task is selecting a view to act as the "center" then we will compute stereo disparity images from
		// all of its neighbors. The process of selecting the best views to use as centers is a problem all it's own
		// instead we will pick a view and just hope everything works out
		SceneWorkingGraph.View center = example.working.getAllViews().get(20);

		// The final scene refined by bundle adjustment is created by the Working graph. However the 3D relationship
		// between views is contained in the pairwise graph. A View in the working graph has a reference to the view
		// in the pairwise graph. Using that we will find all connected views that have a 3D relationship
		GrowQueue_I32 pairs = new GrowQueue_I32();
		TIntObjectMap<GrayU8> images = new TIntObjectHashMap<>();

		// This relationship between pairwise and working graphs might seem (and is) a bit convoluted. The Pairwise
		// graph is the initial crude sketch of what might be connected. The working graph is an intermediate
		// data structure for computing the metric scene. SBA is a refinement of the working graph.

		// Iterate through all connected views in the pairwise graph for this view in the working graph
		center.pview.connections.forEach((m)->{
			// if there isn't a 3D relationship just skip it
			if( !m.is3D )
				return;

			String imageID = m.other(center.pview).id;
			SceneWorkingGraph.View connected = example.working.views.get(imageID);

			// Make sure the view exists in the working graph too
			if (connected==null)
				return;

			// Load the image and record the mapping of SBA index to image
			var image = new GrayU8(1,1);
			imageLookup.loadImage(imageID,image);
			images.put(connected.index, image);
			pairs.add(connected.index);
		});

		// Add the center camera image
		var centerImage = new GrayU8(1,1);
		imageLookup.loadImage(center.pview.id,centerImage);
		images.put(center.index, centerImage);

		// Configure there stereo disparity algorithm which is used
		var configDisparity = new ConfigDisparityBMBest5();
		configDisparity.validateRtoL = 1;
		configDisparity.texture = 0.5;
		configDisparity.regionRadiusX = configDisparity.regionRadiusY = 4;
		configDisparity.disparityRange = 50;

		// This is the code which combines/fuses multiple disparity images together. It employs a very simple
		// algorithm based on voting. See class description for details.
		var combiner = new MultiViewToFusedDisparity<GrayU8>();
		combiner.setVerbose(System.out, null);
		combiner.setStereoDisparity(FactoryStereoDisparity.blockMatchBest5(configDisparity, GrayU8.class, GrayF32.class));
		combiner.initialize(example.scene, images);

		var listDisplay = new ListDisplayPanel();
		listDisplay.setPreferredSize(new Dimension(1000,300));

		// We will display intermediate results
		combiner.setListener(( leftView, rightView, rectLeft, rectRight,
									 disparity, mask, parameters, rect ) -> {
			// Visualize the rectified stereo pair. You can interact with this window and verify
			// that the y-axis is  aligned
			var rectified = new RectifiedPairPanel(true);
			rectified.setImages(ConvertBufferedImage.convertTo(rectLeft,null),
					ConvertBufferedImage.convertTo(rectRight,null));
			listDisplay.addItem(rectified,"Rectified "+leftView+" "+rightView);

			// Cleans up the disparity image by zeroing out pixels that are outside the original image bounds
			RectifyImageOps.applyMask(disparity, mask, 0);
			// Display the colorized disparity
			BufferedImage colorized = VisualizeImageData.disparity(disparity, null, parameters.disparityRange, 0);
			listDisplay.addImage(colorized, leftView+" " +rightView);
		});

		ShowImages.showWindow(listDisplay,"Intermediate Results",true);

		if (!combiner.process(center.index, pairs)) {
			throw new RuntimeException("Failed to fuse stereo views");
		}

		// Extract the point cloud from the combined disparity image
		GrayF32 fusedDisparity = combiner.getFusedDisparity();
		DisparityParameters fusedParam = combiner.getFusedParam();
		BufferedImage colorizedDisp = VisualizeImageData.disparity(fusedDisparity, null, fusedParam.disparityRange, 0);
		ShowImages.showWindow(colorizedDisp, "Fused Disparity");

		// Now the point cloud it represents
		var cloud = new FastQueue<>(Point3D_F64::new);
		// The fused image has no mask since it marks pixels outside of all views as invalid
		var dummyMask = fusedDisparity.createSameShape(GrayU8.class);
		MultiViewStereoOps.disparityToCloud(fusedDisparity,dummyMask, fusedParam, cloud);

		// Extract the color of each point for visualize. We could look up the image as color but will use the gray
		// image we have on hand. Another option would be pseudo color but that doesn't look as good.
		var cloudRgb = new GrowQueue_I32(cloud.size);
		var colorizer = new ColorizeCloudFromImage<>(new LookUpColorRgbFormats.SB_U8());
		// Convert from a bundle adjustment camera model into the standard camera models
		CameraPinholeBrown intrinsic = BundleAdjustmentOps.convert(example.scene.cameras.get(center.index).model,
				centerImage.width, centerImage.height, null);
		// conversion from normalized to pixel coordinates that takes in acount lens distortion
		Point2Transform2_F64 norm_to_pixel = new LensDistortionBrown(intrinsic).distort_F64(false,true);
		colorizer.process3(centerImage, cloud.toList(),0,cloud.size,new Se3_F64(),norm_to_pixel,
				(i,r,g,b)->cloudRgb.set(i,(r<<16)|(g<<8)|b));

		// Configure the point cloud viewer
		PointCloudViewer pcv = VisualizeData.createPointCloudViewer();
		pcv.setCameraHFov(UtilAngle.radian(70));
		pcv.setTranslationStep(0.15);
		pcv.addCloud(cloud.toList(),cloudRgb.data);
//		pcv.setColorizer(new SingleAxisRgb.Z().fperiod(30.0));
		JComponent viewer = pcv.getComponent();
		viewer.setPreferredSize(new Dimension(600, 600));
		ShowImages.showWindow(viewer,"Point Cloud", true);

		System.out.println("Done");
	}
}

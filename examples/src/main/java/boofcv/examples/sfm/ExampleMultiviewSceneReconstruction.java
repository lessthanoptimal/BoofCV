/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.geo.bundle.BundleAdjustmentObservations;
import boofcv.abst.geo.bundle.BundleAdjustmentSceneStructure;
import boofcv.abst.geo.bundle.BundleAdjustmentShur_DSCC;
import boofcv.alg.distort.LensDistortionOps;
import boofcv.alg.sfm.structure.EstimateSceneUnordered;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.image.GrayF32;
import boofcv.visualize.PointCloudViewer;
import boofcv.visualize.VisualizeData;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.struct.GrowQueue_I32;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Demonstration on how to do 3D reconstruction from a set of unordered photos with known intrinsic camera calibration.
 * The code below is still a work in process and is very basic, but still require a solid understanding of
 * structure from motion to understand.  In other words, this is not for beginners and requires good clean set of
 * images to work.
 *
 * TODO Update comment
 * One key element it is missing is bundle adjustment to improve the estimated camera location and 3D points.  The
 * current bundle adjustment in BoofCV is too inefficient.   Better noise removal and numerous other improvements
 * are needed before it can compete with commercial equivalents.
 *
 * @author Peter Abeles
 */
public class ExampleMultiviewSceneReconstruction {
	/**
	 * Process the images and reconstructor the scene as a point cloud using matching interest points between
	 * images.
	 */
	public void process(CameraPinholeRadial intrinsic , List<BufferedImage> colorImages ) {

		DetectDescribePoint detDesc = FactoryDetectDescribe.surfStable(null, null, null, GrayF32.class);
		EstimateSceneUnordered<GrayF32>  estimateScene = new EstimateSceneUnordered<GrayF32>(detDesc);
		estimateScene.setVerbose(true);

		String cameraName = "camera";
		estimateScene.addCamera(cameraName,LensDistortionOps.narrow(intrinsic),intrinsic.width,intrinsic.height);

		for (int i = 0; i < colorImages.size(); i++) {
			BufferedImage colorImage = colorImages.get(i);
			if( colorImage.getWidth() != intrinsic.width || colorImage.getHeight() != intrinsic.height )
				throw new RuntimeException("Looks like you tried to hack this example and run it on random images. Please RTFM");
			GrayF32 image = ConvertBufferedImage.convertFrom(colorImage, (GrayF32) null);
			estimateScene.add(image,cameraName);
		}

		if( !estimateScene.estimate() ) {
			throw new RuntimeException("Failed to generate an initiate estimate of the scene's structure!");
		}

		// get the results
		BundleAdjustmentSceneStructure structure = estimateScene.getSceneStructure();
		BundleAdjustmentObservations observations = estimateScene.getObservations();

		// Configure bundle adjustment
		BundleAdjustmentShur_DSCC sba = new BundleAdjustmentShur_DSCC(1e-3);
		sba.configure(1e-4,1e-4,20);
		structure.setCamera(0,true,intrinsic);

		// Optimize the results
		if( !sba.optimize(structure,observations) ) {
			throw new RuntimeException("Bundle adjustment failed!");
		}

		visualizeResults(structure,intrinsic,colorImages);
		System.out.println("Done!");
	}

	/**
	 * Opens a window showing the found point cloud. Points are colorized using the pixel value inside
	 * one of the input images
	 */
	private void visualizeResults( BundleAdjustmentSceneStructure structure, CameraPinholeRadial intrinsic ,
								   List<BufferedImage> colorImages ) {

		List<Point3D_F64> cloudXyz = new ArrayList<>();
		GrowQueue_I32 cloudRgb = new GrowQueue_I32();
		Point3D_F64 world = new Point3D_F64();
		Point3D_F64 camera = new Point3D_F64();
		Point2D_F64 pixel = new Point2D_F64();
		for( int i = 0; i < structure.points.length; i++ ) {
			// Get 3D location
			BundleAdjustmentSceneStructure.Point p = structure.points[i];
			p.get(world);

			// Project point into an arbitrary view
			for (int j = 0; j < p.views.size; j++) {
				int viewIdx  = p.views.get(j);
				SePointOps_F64.transform(structure.views[viewIdx].worldToView,world,camera);
				int cameraIdx = structure.views[viewIdx].camera;
				structure.cameras[cameraIdx].model.project(camera.x,camera.y,camera.z,pixel);

				// Get the points color
				BufferedImage image = colorImages.get(viewIdx);
				int x = (int)pixel.x;
				int y = (int)pixel.y;

				// After optimization it might have been moved out of the camera's original FOV.
				// hopefully this isn't too common
				if( x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight() )
					continue;
				cloudXyz.add( world.copy() );
				cloudRgb.add(image.getRGB((int)pixel.x,(int)pixel.y));
				break;
			}
		}

		PointCloudViewer viewer = VisualizeData.createPointCloudViewer();
		viewer.setTranslationStep(0.05);
		viewer.addCloud(cloudXyz,cloudRgb.data);
		viewer.setCameraHFov(UtilAngle.radian(60));

		SwingUtilities.invokeLater(()->{
			viewer.getComponent().setPreferredSize(new Dimension(500,500));
			ShowImages.showWindow(viewer.getComponent(), "Reconstruction Points", true);
		});

	}

	public static void main(String[] args) {

		String directory = UtilIO.pathExample("sfm/chair");

		CameraPinholeRadial intrinsic = CalibrationIO.load(
				new File(directory,"/intrinsic_DSC-HX5_3648x2736_to_640x480.yaml"));

		List<BufferedImage> images = UtilImageIO.loadImages(directory,".*jpg");

		int N = 8;
		while( images.size() > N ) {
			images.remove(N);
		}

		ExampleMultiviewSceneReconstruction example = new ExampleMultiviewSceneReconstruction();

		long before = System.currentTimeMillis();
		example.process(intrinsic,images);
		long after = System.currentTimeMillis();

		System.out.println("Elapsed time "+(after-before)/1000.0+" (s)");
	}
}

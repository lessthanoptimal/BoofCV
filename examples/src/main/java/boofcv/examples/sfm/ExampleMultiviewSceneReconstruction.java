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

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.geo.bundle.BundleAdjustment;
import boofcv.abst.geo.bundle.ScaleSceneStructure;
import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.distort.LensDistortionOps;
import boofcv.alg.sfm.structure.EstimateSceneCalibrated;
import boofcv.alg.sfm.structure.PairwiseImageMatching;
import boofcv.alg.sfm.structure.PruneStructureFromScene;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.geo.ConfigBundleAdjustment;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.GrayF32;
import boofcv.visualize.PointCloudViewer;
import boofcv.visualize.VisualizeData;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.optimization.lm.ConfigLevenbergMarquardt;
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
		ScoreAssociation scorer = FactoryAssociation.defaultScore(detDesc.getDescriptionType());
		AssociateDescription<TupleDesc> associate =
				FactoryAssociation.greedy(scorer, Double.MAX_VALUE, true);
		PairwiseImageMatching<GrayF32> imageMatching = new PairwiseImageMatching<>(detDesc,associate);
		imageMatching.setVerbose(System.out,0);

		String cameraName = "camera";
		imageMatching.addCamera(cameraName,LensDistortionOps.narrow(intrinsic).undistort_F64(true,false),intrinsic);

		for (int i = 0; i < colorImages.size(); i++) {
			BufferedImage colorImage = colorImages.get(i);
			if( colorImage.getWidth() != intrinsic.width || colorImage.getHeight() != intrinsic.height )
				throw new RuntimeException("Looks like you tried to hack this example and run it on random images. Please RTFM");
			GrayF32 image = ConvertBufferedImage.convertFrom(colorImage, (GrayF32) null);
			imageMatching.addImage(image,cameraName);
		}

		if( !imageMatching.process() ) {
			throw new RuntimeException("Failed to match images!");
		}

		EstimateSceneCalibrated estimateScene = new EstimateSceneCalibrated();
		estimateScene.setVerbose(System.out,0);

		if( !estimateScene.process(imageMatching.getGraph()))
			throw new RuntimeException("Scene estimation failed");

		// get the results
		SceneStructureMetric structure = estimateScene.getSceneStructure();
		SceneObservations observations = estimateScene.getObservations();

		// Configure bundle adjustment
		ConfigLevenbergMarquardt configLM = new ConfigLevenbergMarquardt();
		configLM.dampeningInitial = 1e-12;
		configLM.hessianScaling = true;
		ConfigBundleAdjustment configSBA = new ConfigBundleAdjustment();
		configSBA.configOptimizer = configLM;

		BundleAdjustment<SceneStructureMetric> sba = FactoryMultiView.bundleAdjustmentMetric(configSBA);
		sba.configure(1e-10,1e-10,100);
		sba.setVerbose(System.out,0);

		// Scale to improve numerical accuracy
		ScaleSceneStructure bundleScale = new ScaleSceneStructure();

		PruneStructureFromScene pruner = new PruneStructureFromScene(structure,observations);

		// Requiring 3 views per point reduces the number of outliers by a lot but also removes
		// many valid points
//		pruner.prunePoints(3);

		// Optimize the results
//		int pruneCycles=1;
//		for (int i = 0; i < pruneCycles; i++) {
//			System.out.println("BA + Prune iteration = "+i+"  points="+structure.points.length+"  obs="+observations.getObservationCount());
//			bundleScale.applyScale(structure,observations);
//			sba.setParameters(structure,observations);
//			if( !sba.optimize(structure) ) {
//				throw new RuntimeException("Bundle adjustment failed!");
//			}
//
//			bundleScale.undoScale(structure,observations);
//
//			if( i == pruneCycles-1 )
//				break;
//
//			System.out.println("Pruning....");
//			pruner.pruneObservationsByErrorRank(0.97);  // Prune outlier observations
//			pruner.prunePoints(3,0.4);            // Prune stray points in 3D space
//			pruner.prunePoints(2);                           // Prune invalid points
//			pruner.pruneViews(10);                           // Prune views with too few observations
//		}

		visualizeResults(structure,colorImages);
		System.out.println("Done!");
	}

	/**
	 * Opens a window showing the found point cloud. Points are colorized using the pixel value inside
	 * one of the input images
	 */
	private void visualizeResults( SceneStructureMetric structure,
								   List<BufferedImage> colorImages ) {

		List<Point3D_F64> cloudXyz = new ArrayList<>();
		GrowQueue_I32 cloudRgb = new GrowQueue_I32();
		Point3D_F64 world = new Point3D_F64();
		Point3D_F64 camera = new Point3D_F64();
		Point2D_F64 pixel = new Point2D_F64();
		for( int i = 0; i < structure.points.length; i++ ) {
			// Get 3D location
			SceneStructureMetric.Point p = structure.points[i];
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

		int N = 2;
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

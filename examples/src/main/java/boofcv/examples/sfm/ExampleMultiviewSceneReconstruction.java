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
import boofcv.abst.geo.TriangulateTwoViewsCalibrated;
import boofcv.alg.distort.LensDistortionOps;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.geo.ConfigEssential;
import boofcv.factory.geo.ConfigRansac;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.factory.geo.FactoryMultiViewRobust;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.BrightFeature;
import boofcv.struct.feature.SurfFeatureQueue;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.GrayF32;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.so.Rodrigues_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_F64;
import org.ddogleg.struct.GrowQueue_I32;

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

	// Converts a point from pixel to normalized image coordinates
	Point2Transform2_F64 pixelToNorm;

	// Don't consider two images for possible matches if their features have less than this fraction
	double FEATURE_MATCH_MINIMUM_FRACTION = 0.1;

	// Number of remaining features after applying RANSAC and computing the essential matrix
	double ESSENTIAL_MATCH_MINIMUM_FRACTION = 0.05;
	double ESSENTIAL_MATCH_MINIMUM = 30;

	// tolerance for inliers in pixels
	double inlierTol = 2.5;

	// Detects and describes image interest points
	DetectDescribePoint<GrayF32, BrightFeature> detDesc = FactoryDetectDescribe.surfStable(null, null, null, GrayF32.class);
	// score ans association algorithm
	ScoreAssociation<BrightFeature> scorer = FactoryAssociation.scoreEuclidean(BrightFeature.class, true);
	AssociateDescription<BrightFeature> associate = FactoryAssociation.greedy(scorer, Double.MAX_VALUE, true);

	// Triangulates the 3D coordinate of a point from two observations
	TriangulateTwoViewsCalibrated triangulate = FactoryMultiView.triangulateTwoGeometric();

	List<CameraView> cameras = new ArrayList<>();


	// The image which acts of the world coordinate system's origin
	int originImage;

	// List of all 3D features
	List<Feature3D> featuresAll = new ArrayList<>();

	// used to provide initial estimate of the 3D scene
	ModelMatcher<Se3_F64, AssociatedPair> estimateEssential;

	/**
	 * Process the images and reconstructor the scene as a point cloud using matching interest points between
	 * images.
	 */
	public void process(CameraPinholeRadial intrinsic , List<BufferedImage> colorImages ) {

		pixelToNorm = LensDistortionOps.narrow(intrinsic).undistort_F64(true,false);

		estimateEssential = FactoryMultiViewRobust.essentialRansac(
				new ConfigEssential(intrinsic),new ConfigRansac(4000,inlierTol));

		// find features in each image
		detectImageFeatures(colorImages);

		// see which images are the most similar to each o ther
		computeConnections();

		printConnectionMatrix();

		System.out.println("Generating initial estimate of 3D structure");
		// find the image which is connected to the most other images.  Use that as the origin of the arbitrary
		// coordinate system
		CameraView seed = selectMostConnectFrame();

		CameraView seedNext = estimateFeatureLocations(seed);

		estimateAllFeatures(seed,seedNext);

		System.out.println("Pruning point features for speed");
//		pruneFeaturesForBundleAdjustment();

		System.out.println("Bundle Adjustment to refine estimate");
//		performBundleAdjustment();

		visualizeResults();
	}

	private void visualizeResults() {
		//		// Use two images to initialize the scene reconstruction
//		initializeReconstruction(colorImages, matrix, bestImage);
//
//		// Process rest of the images and compute 3D coordinates
//		List<Integer> seed = new ArrayList<>();
//		seed.add(bestImage);
//		performReconstruction(seed, -1, matrix);
//
//		// Perform bundle adjustment to refine the initial estimates
//		performBundleAdjustment();
//
//		// display a point cloud from the 3D features
//		PointCloudViewer gui = new PointCloudViewer(intrinsic,1);
//
//		for( Feature3D t : featuresAll) {
//			gui.addPoint(t.worldPt.x,t.worldPt.y,t.worldPt.z,t.color);
//		}
//
//		gui.setPreferredSize(new Dimension(500,500));
//		ShowImages.showWindow(gui, "Points");
	}

	/**
	 * Select the frame which has the most connections to all other frames.  The is probably a good location
	 * to start since it will require fewer hops to estimate the motion of other frames
	 */
	private CameraView selectMostConnectFrame() {
		CameraView best = cameras.get(0);
		for (int i = 1; i < cameras.size(); i++) {
			if( cameras.get(i).connections.size() > best.connections.size() ) {
				best = cameras.get(i);
			}
		}
		return best;
	}

	/**
	 * Detect image features in all the images.  Save location, description, and color
	 */
	private void detectImageFeatures(List<BufferedImage> colorImages) {
		System.out.println("Detecting Features in each image.  Total "+colorImages.size());
		for (int i = 0; i < colorImages.size(); i++) {
			System.out.print("*");
			BufferedImage colorImage = colorImages.get(i);

			CameraView camera = new CameraView(i);
			detectFeatures(colorImage, camera.descriptions, camera.locations, camera.colors);
			camera.features = new Feature3D[camera.locations.size];
			cameras.add(camera);
		}
		System.out.println();
	}

	/**
	 * Compute connectivity matrix based on fraction of matching image features
	 */
	private void computeConnections() {
		for (int i = 0; i < cameras.size(); i++) {
			CameraView cameraA = cameras.get(i);

			for (int j = i+1; j < cameras.size(); j++) {
				CameraView cameraB = cameras.get(j);

				// Associate features using their descriptors
				associate.setSource(cameraA.descriptions);
				associate.setDestination(cameraB.descriptions);
				associate.associate();

				double fractionAB = associate.getMatches().size()/(double) cameraA.descriptions.size();
				double fractionBA = associate.getMatches().size()/(double) cameraB.descriptions.size();

				if( fractionAB < FEATURE_MATCH_MINIMUM_FRACTION || fractionBA < FEATURE_MATCH_MINIMUM_FRACTION ) {
					continue;
				}

				// Estimate an essential matrix to remove more false matches and provide an initial motion estimate
				// This motion estimate will have an ambiguous scaling
				Se3_F64 motionAtoB = new Se3_F64();
				List<AssociatedIndex> inliers = new ArrayList<>();
				if( !estimateStereoPose(i,j,motionAtoB,associate.getMatches(),inliers)) {
					continue;
				}

				if( inliers.size() < ESSENTIAL_MATCH_MINIMUM )
					continue;

				fractionAB = inliers.size()/(double) cameraA.descriptions.size();
				fractionBA = inliers.size()/(double) cameraB.descriptions.size();

				if( fractionAB < ESSENTIAL_MATCH_MINIMUM_FRACTION || fractionBA < ESSENTIAL_MATCH_MINIMUM_FRACTION ) {
					continue;
				}

				// Create an edge connecting these two images
				CameraMotion motion = new CameraMotion();
				motion.a_to_b.set(motionAtoB);
				motion.viewSrc = cameraA;
				motion.viewDst = cameraB;
				motion.copyFeatures(inliers);
				cameraA.connections.add(motion);
				cameraB.connections.add(motion);

				System.out.printf("Associated %02d %02d = %.3f %.3f | inliers=%d\n",i,j,fractionAB,fractionBA,inliers.size());
			}
		}
	}

	/**
	 * Prints out which frames are connected to each other
	 */
	private void printConnectionMatrix() {
		for (int i = 0; i < cameras.size(); i++) {
			CameraView cameraA = cameras.get(i);
			System.out.printf("%2d ",i);
			for (int j = 0; j < cameras.size(); j++) {
				boolean connected = false;
				if( i != j ) {
					for (CameraMotion m : cameraA.connections) {
						connected |= m.viewSrc.index == j || m.viewDst.index == j;
					}
				}

				if( connected )
					System.out.print("#");
				else
					System.out.print(".");
			}
			System.out.println();
		}
	}

	/**
	 * Detects image features.  Saves their location, description, and pixel color
	 */
	private void detectFeatures(BufferedImage colorImage,
								FastQueue<BrightFeature> features, FastQueue<Point2D_F64> pixels,
								GrowQueue_I32 colors ) {

		GrayF32 image = ConvertBufferedImage.convertFrom(colorImage, (GrayF32) null);

		features.reset();
		pixels.reset();
		colors.reset();
		detDesc.detect(image);
		for (int i = 0; i < detDesc.getNumberOfFeatures(); i++) {
			Point2D_F64 p = detDesc.getLocation(i);

			features.grow().set(detDesc.getDescription(i));
			// store pixels are normalized image coordinates
			pixelToNorm.compute(p.x, p.y, pixels.grow());

			colors.add( colorImage.getRGB((int)p.x,(int)p.y) );
		}
	}

	private CameraView estimateFeatureLocations(CameraView viewRoot) {
		// Choice of world reference frame is important. This is where the translation scale will be determined
		// We will select a view in which there are many connected features to this camera, but a large angle
		// separating the two views.
		CameraMotion best = null;
		double bestScore = 0;
		Rodrigues_F64 rod = new Rodrigues_F64();

		// Using a large angle isn't actually that good of an idea in general, but works with the example images
		// What you want is a view which maximizes translation and number of features, but the tranalations here
		// don't have the same scale so that's tricky to determine
		for( CameraMotion e : viewRoot.connections ) {
			ConvertRotation3D_F64.matrixToRodrigues(e.a_to_b.R,rod);
			double score = e.features.size()*rod.theta;

			System.out.println("score="+score+" features="+e.features.size()+" angle="+rod.theta);
			if( score > bestScore ) {
				bestScore = score;
				best = e;
			}
		}
		if( best == null )
			throw new RuntimeException("BUG!");

		// By defintion this transform is identity
		viewRoot.cameraToWorld.reset();
		viewRoot.distanceFromRoot = 0;

		// Ensure the scale is some what reasonable
		best.a_to_b.T.normalize();
		CameraView viewB = best.destination(viewRoot);
		viewB.cameraToWorld.set(best.motionSrcToDst(viewB));
		viewB.distanceFromRoot = 1;

		// Compute the 3D points. The world reference frame is now defined
		triangulateMatchedFeatures(best);

		return viewB;
	}

	/**
	 * Perform a breath first search to find the structure of all the remaining camrea views
	 */
	private void estimateAllFeatures(CameraView seedA, CameraView seedB ) {
		List<CameraView> open = new ArrayList<>();

		// Add features for all the other views connected to the root view and determine the translation scale factor
		addUnvistedToStack(seedA, open);
		addUnvistedToStack(seedB, open);

		// Do a breath first search. The queue is first in first out
		while( !open.isEmpty() ) {
			CameraView v = open.remove(0);

			// Find the connection closest to the root to minimize compound error
			int bestDistance = Integer.MAX_VALUE;
			CameraMotion best = null;
			for( CameraMotion m : v.connections ) {
				CameraView o = m.destination(v);
				if( o.distanceFromRoot >= 0 && o.distanceFromRoot < bestDistance ) {
					best = m;
					bestDistance = o.distanceFromRoot;
				}
			}

			if( best == null )
				throw new RuntimeException("BUG!");

			// Figure out it's 3D structure
			triangulateAndDetermineScale(best);

			// Update the open list
			addUnvistedToStack(v, open);
		}
	}

	/**
	 * Looks to see which connections have yet to be visited and adds them to the open list
	 */
	private void addUnvistedToStack(CameraView viewed, List<CameraView> open) {
		for (int i = 0; i < viewed.connections.size(); i++) {
			CameraView other = viewed.connections.get(i).destination(viewed);
			if( other.distanceFromRoot < 0) {
				open.add(other);
			}
		}
	}

	/**
	 * Uses the previously found motion between the two cameras to estimate the scale and 3D point of common features.
	 * If a feature already has a known 3D point that is not modified. Scale is found by computing the 3D coordinate
	 * of all points with a 3D point again then dividing the two distances. New features are also triangulated
	 * and have their location's update using this scale.
	 *
	 * A known feature has the current view added to its list of views.
	 */
	private void triangulateAndDetermineScale(CameraMotion edge ) {
		// There are two views. See if src or dst is the known with a known transform to the world frame
		CameraView viewA = edge.viewSrc;
		CameraView viewB = edge.viewDst;

		int distanceFromRoot;
		boolean useA = viewA.distanceFromRoot >= 0;

		Se3_F64 worldToView = new Se3_F64();
		if( useA ) {
			viewA.cameraToWorld.invert(worldToView);
			distanceFromRoot = viewA.distanceFromRoot;
		} else {
			viewB.cameraToWorld.invert(worldToView);
			distanceFromRoot = viewB.distanceFromRoot;
		}

		// Go through each point and see if it's known or not

		List<Feature3D> featuresNew = new ArrayList<>();
		Point3D_F64 local3D = new Point3D_F64();
		Point3D_F64 found3D = new Point3D_F64();
		Point2D_F64 normA = new Point2D_F64();
		Point2D_F64 normB = new Point2D_F64();
		GrowQueue_F64 scaleQueues = new GrowQueue_F64();
		for (int i = 0; i < edge.features.size(); i++) {
			AssociatedIndex f = edge.features.get(i);

			Feature3D feature3D;
			if( useA ) {
				feature3D = viewA.features[f.src];
			} else {
				feature3D = viewB.features[f.dst];
			}

			Point2D_F64 pixelA = viewA.locations.get(f.src);
			Point2D_F64 pixelB = viewB.locations.get(f.dst);

			pixelToNorm.compute(pixelA.x, pixelA.y, normA);
			pixelToNorm.compute(pixelB.x, pixelB.y, normB);

			//triangulate using the previously found motion between the two cameras. The motion's translation
			// still has an incorrect scale at this point
			if( !triangulate.triangulate(normA,normB,edge.a_to_b,found3D) ) {
				continue;
			}
			// put it in the coordinate system of the view with the known global transform
			if (!useA) {
				SePointOps_F64.transformReverse(edge.a_to_b, found3D, found3D);
			}

			if( feature3D != null  ) {
				// convert the feature's known world 3D location and find it in the 'use' view
				SePointOps_F64.transform(worldToView, feature3D.worldPt, local3D);

				scaleQueues.add(local3D.norm()/found3D.norm());

				// Add this view to the features list of views that have seen it
				if( useA ) {
					viewB.features[f.dst] = feature3D;
					feature3D.obs.grow().set(normB);
					feature3D.frame.add(viewB.index);
				} else {
					viewA.features[f.src] = feature3D;
					feature3D.obs.grow().set(normA);
					feature3D.frame.add(viewA.index);
				}
			} else {
				// Save the results for later when the scale is known
				if( viewA.features[f.src] != null || viewB.features[f.dst] != null)
					throw new RuntimeException("BUG!");

				// Create a new feature
				feature3D = new Feature3D();
				feature3D.worldPt.set(found3D); // current a local point, but that will be fixed later
				feature3D.obs.grow().set(normA);
				feature3D.obs.grow().set(normB);

				// mark this feature3D as being associated with these image features
				viewA.features[f.src] = feature3D;
				viewB.features[f.dst] = feature3D;

				// record which frame the feature was seen in
				feature3D.frame.add(f.src);
				feature3D.frame.add(f.dst);

				featuresNew.add(feature3D);
			}
		}

		// use the median value to represent the scale offset
		scaleQueues.sort();
		double scale = scaleQueues.get( scaleQueues.size/2 );

		// Fix the scale along this edge
		edge.a_to_b.T.scale(scale);

		if( useA ) {
			edge.a_to_b.invert(null).concat(viewA.cameraToWorld,viewB.cameraToWorld);
			viewB.distanceFromRoot = distanceFromRoot + 1;
		} else {
			edge.a_to_b.concat(viewB.cameraToWorld, viewA.cameraToWorld);
			viewA.distanceFromRoot = distanceFromRoot + 1;
		}

		// The scale is now known, let's fix all the newly created features
		for( Feature3D f : featuresNew ) {
			f.worldPt.scale(scale);
			featuresAll.add(f);
		}
	}

	/**
	 * For the two seed views just triangulate all the common features. The motion already has its translation
	 * normalized to one
	 */
	private void triangulateMatchedFeatures( CameraMotion edge ) {

		CameraView viewA = edge.viewSrc;
		CameraView viewB = edge.viewDst;

		for (int i = 0; i < edge.features.size(); i++) {
			AssociatedIndex f = edge.features.get(i);

			Point2D_F64 pixelA = viewA.locations.get(f.src);
			Point2D_F64 pixelB = viewB.locations.get(f.dst);

			Feature3D feature3D = new Feature3D();

			Point2D_F64 normA = feature3D.obs.grow();
			Point2D_F64 normB = feature3D.obs.grow();

			pixelToNorm.compute(pixelA.x, pixelA.y, normA);
			pixelToNorm.compute(pixelB.x, pixelB.y, normB);

			if( !triangulate.triangulate(normA,normB,edge.a_to_b,feature3D.worldPt) ) {
				continue;
			}
			// mark this feature3D as being associated with these image features
			viewA.features[f.src] = feature3D;
			viewB.features[f.dst] = feature3D;

			// record which frame the feature was seen in
			feature3D.frame.add(f.src);
			feature3D.frame.add(f.dst);

			// Add it to the overall list
			featuresAll.add(feature3D);
		}
	}

	/**
	 * Given two images compute the relative location of each image using the essential matrix.
	 */
	protected boolean estimateStereoPose(int imageA, int imageB, Se3_F64 motionAtoB,
										 FastQueue<AssociatedIndex> matches, List<AssociatedIndex> inliers)
	{
		// create the associated pair for motion estimation
		FastQueue<Point2D_F64> pixelsA = cameras.get(imageA).locations;
		FastQueue<Point2D_F64> pixelsB = cameras.get(imageB).locations;
		List<AssociatedPair> pairs = new ArrayList<>();
		for (int i = 0; i < matches.size(); i++) {
			AssociatedIndex a = matches.get(i);
			pairs.add(new AssociatedPair(pixelsA.get(a.src), pixelsB.get(a.dst)));
		}

		if( !estimateEssential.process(pairs) )
			return false;

		List<AssociatedPair> inliersEssential = estimateEssential.getMatchSet();

		motionAtoB.set(estimateEssential.getModelParameters());

		for (int i = 0; i < inliersEssential.size(); i++) {
			int index = estimateEssential.getInputIndex(i);

			inliers.add( matches.get(index));
		}

		return true;
	}

	public static class CameraView {
		int index;
		Se3_F64 cameraToWorld = new Se3_F64();
		int distanceFromRoot = -1;

		List<CameraMotion> connections = new ArrayList<>();

		// feature descriptor of all features in this image
		FastQueue<BrightFeature> descriptions = new SurfFeatureQueue(64);
		// observed location of all features in pixels
		FastQueue<Point2D_F64> locations = new FastQueue<>(Point2D_F64.class, true);
		// RGB color of each feature
		GrowQueue_I32 colors = new GrowQueue_I32();
		// Estimated 3D location for SOME of the features
		Feature3D[] features;

		public CameraView(int index) {
			this.index = index;
		}
	}

	public static class CameraMotion {
		// if the transform of both views is known then this will be scaled to be in world units
		// otherwise it's in arbitrary units
		Se3_F64 a_to_b = new Se3_F64();

		// index
		List<AssociatedIndex> features = new ArrayList<>();

		CameraView viewSrc;
		CameraView viewDst;

		public void copyFeatures( List<AssociatedIndex> list ) {
			features.clear();
			for (int i = 0; i < list.size(); i++) {
				features.add( list.get(i).copy() );
			}
		}

		public Se3_F64 motionSrcToDst( CameraView src ) {
			if( src == viewSrc) {
				return a_to_b.copy();
			} else if( src == viewDst){
				return a_to_b.invert(null);
			} else {
				throw new RuntimeException("BUG!");
			}
		}

		public CameraView destination( CameraView src ) {
			if( src == viewSrc) {
				return viewDst;
			} else if( src == viewDst){
				return viewSrc;
			} else {
				throw new RuntimeException("BUG!");
			}
		}

		public int featureIndex( CameraView view , int index  ) {
			if( view == viewSrc) {
				return features.get(index).src;
			} else if( view == viewDst) {
				return features.get(index).dst;
			} else {
				throw new RuntimeException("BUG!");
			}
		}
	}

	public static class Feature3D {
		// color of the pixel first found int
		int color;
		// estimate 3D position of the feature in world frame
		Point3D_F64 worldPt = new Point3D_F64();
		// observations in each frame that it's visible. These are in
		FastQueue<Point2D_F64> obs = new FastQueue<>(Point2D_F64.class, true);
		// index of each frame its visible in
		GrowQueue_I32 frame = new GrowQueue_I32();
		boolean included=false;
	}

	public static void main(String[] args) {

		String directory = UtilIO.pathExample("sfm/chair");

		CameraPinholeRadial intrinsic = CalibrationIO.load(
				new File(directory,"/intrinsic_DSC-HX5_3648x2736_to_640x480.yaml"));

		List<BufferedImage> images = UtilImageIO.loadImages(directory,".*jpg");

		while( images.size() > 5 ) {
			images.remove(5);
		}

		ExampleMultiviewSceneReconstruction example = new ExampleMultiviewSceneReconstruction();

		long before = System.currentTimeMillis();
		example.process(intrinsic,images);
		long after = System.currentTimeMillis();

		System.out.println("Elapsed time "+(after-before)/1000.0+" (s)");
	}
}

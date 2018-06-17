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
import boofcv.abst.geo.bundle.BundleAdjustmentObservations;
import boofcv.abst.geo.bundle.BundleAdjustmentSceneStructure;
import boofcv.abst.geo.bundle.BundleAdjustmentShur_DSCC;
import boofcv.alg.distort.LensDistortionOps;
import boofcv.alg.geo.WorldToCameraToPixel;
import boofcv.alg.geo.triangulate.TriangulateLinearDLT;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.factory.geo.ConfigEssential;
import boofcv.factory.geo.ConfigRansac;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.factory.geo.FactoryMultiViewRobust;
import boofcv.gui.d3.PointCloudViewer;
import boofcv.gui.image.ShowImages;
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
import georegression.geometry.GeometryMath_F64;
import georegression.geometry.UtilVector3D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.so.Rodrigues_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_F64;
import org.ddogleg.struct.GrowQueue_I32;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
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
	final double FEATURE_MATCH_MINIMUM_FRACTION = 0.1;

	// Number of remaining features after applying RANSAC and computing the essential matrix
	final double ESSENTIAL_MATCH_MINIMUM_FRACTION = 0.05;
	final int ESSENTIAL_MATCH_MINIMUM = 40;

	// How many features in each edge it tries to have. Can be used to limit the number of features optimized in SBA
	int TARGET_FEATURE_EDGE = 40;

	// tolerance for inliers in pixels
	final double inlierTol = 2.5;

	// Detects and describes image interest points
	DetectDescribePoint<GrayF32, BrightFeature> detDesc = FactoryDetectDescribe.surfStable(null, null, null, GrayF32.class);
	// score ans association algorithm
	ScoreAssociation<BrightFeature> scorer = FactoryAssociation.scoreEuclidean(BrightFeature.class, true);
	AssociateDescription<BrightFeature> associate = FactoryAssociation.greedy(scorer, Double.MAX_VALUE, true);

	// Triangulates the 3D coordinate of a point from two observations
	TriangulateTwoViewsCalibrated triangulate = FactoryMultiView.triangulateTwoGeometric();

	List<CameraView> graphNodes = new ArrayList<>();
	List<CameraMotion> graphEdges = new ArrayList<>();

	// List of all 3D features
	List<Feature3D> featuresAll = new ArrayList<>();
	List<Feature3D> featuresPruned = new ArrayList<>();

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

		System.out.println("Refining initial estimate");

//		retriangulateFeatures(intrinsic);

		System.out.println("Pruning point features for speed");
		pruneFeatures();
//		featuresPruned.addAll(featuresAll);

		System.out.println("   "+featuresAll.size()+"  after   "+featuresPruned.size());


		System.out.println("Bundle Adjustment to refine estimate");
		long before = System.currentTimeMillis();
		performBundleAdjustment(seed,intrinsic);
		long after = System.currentTimeMillis();
		System.out.println("  SBA processing time = "+(after-before)+" (ms)");
		visualizeResults(intrinsic);
		System.out.println("Done!");
	}

	private void visualizeResults(CameraPinholeRadial intrinsic) {
		// display a point cloud from the 3D features
		PointCloudViewer gui = new PointCloudViewer(intrinsic,1);

		for( Feature3D t : featuresPruned) {
			gui.addPoint(t.worldPt.x,t.worldPt.y,t.worldPt.z,t.color);
		}

		gui.setPreferredSize(new Dimension(500,500));
		ShowImages.showWindow(gui, "Points", true);
	}

	/**
	 * Select the frame which has the most connections to all other frames.  The is probably a good location
	 * to start since it will require fewer hops to estimate the motion of other frames
	 */
	private CameraView selectMostConnectFrame() {
		CameraView best = graphNodes.get(0);
		for (int i = 1; i < graphNodes.size(); i++) {
			if( graphNodes.get(i).connections.size() > best.connections.size() ) {
				best = graphNodes.get(i);
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
			detectFeatures(colorImage, camera.descriptions, camera.featurePixels, camera.colors);
			camera.features3D = new Feature3D[camera.featurePixels.size];
			for (int j = 0; j < camera.featurePixels.size; j++) {
				Point2D_F64 p = camera.featurePixels.get(j);
				pixelToNorm.compute(p.x,p.y,camera.featureNorm.grow());
			}
			graphNodes.add(camera);
		}
		System.out.println();
	}

	/**
	 * Compute connectivity matrix based on fraction of matching image features
	 */
	private void computeConnections() {
		for (int i = 0; i < graphNodes.size(); i++) {
			CameraView cameraA = graphNodes.get(i);

			for (int j = i+1; j < graphNodes.size(); j++) {
				CameraView cameraB = graphNodes.get(j);

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

				graphEdges.add(motion);

				System.out.printf("Associated %02d %02d = %.3f %.3f | inliers=%d\n",i,j,fractionAB,fractionBA,inliers.size());
			}
		}
	}

	/**
	 * Prints out which frames are connected to each other
	 */
	private void printConnectionMatrix() {
		for (int i = 0; i < graphNodes.size(); i++) {
			CameraView cameraA = graphNodes.get(i);
			System.out.printf("%2d ",i);
			for (int j = 0; j < graphNodes.size(); j++) {
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
			pixels.grow().set(p);

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

		// By definition this transform is identity
		viewRoot.viewToWorld.reset();
		viewRoot.distanceFromRoot = 0;

		// Ensure the scale is some what reasonable
		best.a_to_b.T.normalize();
		CameraView viewB = best.destination(viewRoot);
		viewB.viewToWorld.set(best.motionSrcToDst(viewB));
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
			if( !triangulateAndDetermineScale(best) ) {
				// Edge hsa too few valid connections. Disconnect
				best.destination(v).connections.remove(best);
				v.connections.remove(best);
				graphEdges.remove(best);
			}
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
	private boolean triangulateAndDetermineScale(CameraMotion edge ) {
		// There are two views. See if src or dst is the known with a known transform to the world frame
		CameraView viewA = edge.viewSrc;
		CameraView viewB = edge.viewDst;

		int distanceFromRoot;
		boolean useA = viewA.distanceFromRoot >= 0;

		Se3_F64 worldToView = new Se3_F64();
		if( useA ) {
			viewA.viewToWorld.invert(worldToView);
			distanceFromRoot = viewA.distanceFromRoot;
		} else {
			viewB.viewToWorld.invert(worldToView);
			distanceFromRoot = viewB.distanceFromRoot;
		}

		// Go through each point and see if it's known or not

		List<Feature3D> featuresNew = new ArrayList<>();
		Point3D_F64 local3D = new Point3D_F64();
		Point3D_F64 found3D = new Point3D_F64();
		GrowQueue_F64 scaleQueues = new GrowQueue_F64();
		for (int i = 0; i < edge.features.size(); i++) {
			AssociatedIndex f = edge.features.get(i);

			Feature3D feature3D;
			if( useA ) {
				feature3D = viewA.features3D[f.src];
			} else {
				feature3D = viewB.features3D[f.dst];
			}

			Point2D_F64 normA = viewA.featureNorm.get(f.src);
			Point2D_F64 normB = viewB.featureNorm.get(f.dst);

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
					if( viewB.features3D[f.dst] == null ) {
						// see if this feature has already been associated with this frame. If it has been
						// do nothing and don't create a connection
						if( feature3D.views.contains(viewB.index)) {
							continue;
						}
						viewB.features3D[f.dst] = feature3D;
						feature3D.feature.add(f.dst);
						feature3D.views.add(viewB.index);
					} else {
//						mergeFeatures( viewB.features3D[f.dst], feature3D);
					}
				} else {
					if( viewA.features3D[f.src] == null ) {
						if( feature3D.views.contains(viewA.index))
							continue;
						viewA.features3D[f.src] = feature3D;
						feature3D.feature.add(f.src);
						feature3D.views.add(viewA.index);
					} else {
//						mergeFeatures( viewA.features3D[f.src], feature3D);
					}
				}
			} else {
				// Just skip. It might have been assigned from another image not being considered yet
				if( viewA.features3D[f.src] != null || viewB.features3D[f.dst] != null) {
					continue; // TODO should it add to the feature's track instead?
				}

				// Create a new feature
				feature3D = new Feature3D();
				feature3D.color = viewA.colors.get(f.src);
				feature3D.worldPt.set(found3D); // current a local point, but that will be fixed later

				// mark this feature3D as being associated with these image features
				viewA.features3D[f.src] = feature3D;
				viewB.features3D[f.dst] = feature3D;

				if( feature3D.views.contains(viewA.index))
					throw new RuntimeException("Egads");
				if( feature3D.views.contains(viewB.index))
					throw new RuntimeException("Egads");

				// record which frame the feature was seen in
				feature3D.feature.add( f.src );
				feature3D.feature.add( f.dst );
				feature3D.views.add(viewA.index);
				feature3D.views.add(viewB.index);

				featuresNew.add(feature3D);
			}
		}

		if( scaleQueues.size < 5 )
			return false;

		// use the median value to represent the scale offset
		scaleQueues.sort();
		double scale = scaleQueues.get( scaleQueues.size/2 );

		// Fix the scale along this edge
		edge.a_to_b.T.scale(scale);

		if( useA ) {
			edge.a_to_b.invert(null).concat(viewA.viewToWorld,viewB.viewToWorld);
			viewB.distanceFromRoot = distanceFromRoot + 1;
		} else {
			edge.a_to_b.concat(viewB.viewToWorld, viewA.viewToWorld);
			viewA.distanceFromRoot = distanceFromRoot + 1;
		}

		// The scale is now known, let's fix all the newly created features
		for( Feature3D f : featuresNew ) {
			f.worldPt.scale(scale);
			featuresAll.add(f);
		}

		return true;
	}

	private void mergeFeatures( Feature3D a , Feature3D b ) {
		if( a == b )
			return;

		int sizeB = b.views.size;
		for (int i = 0; i < a.views.size; i++) {
			int frameA = a.views.get(i);
			boolean matched = false;
			for (int j = 0; j < sizeB; j++) {
				int frameB = b.views.get(j);
				if( frameA == frameB ) {
					matched = true;
					break;
				}
			}
			CameraView view = graphNodes.get(frameA);
			if( view.features3D[a.feature.get(i)] != a )
				throw new RuntimeException("BUG!");
			else if( matched ){
				// there's a conflict. Resolve the ambiguity by removing one of the features arbitrarily
				view.features3D[a.feature.get(i)] = null;
			} else {
				view.features3D[a.feature.get(i)] = b; // change the 3D feature it points to
				b.views.add( frameA );
				b.feature.add( a.feature.get(i) );
			}
		}

		featuresAll.remove(a);
	}

	/**
	 * For the two seed views just triangulate all the common features. The motion already has its translation
	 * normalized to one
	 */
	private void triangulateMatchedFeatures( CameraMotion edge ) {

		CameraView viewA = edge.viewSrc;
		CameraView viewB = edge.viewDst;

		double minAngle = Math.PI/10.0;
		Vector3D_F64 arrowA = new Vector3D_F64();
		Vector3D_F64 arrowB = new Vector3D_F64();

		for (int i = 0; i < edge.features.size(); i++) {
			AssociatedIndex f = edge.features.get(i);

			// some association algorithms allow multiple solutions. Just ignore that to keep the code simple
			if( viewA.features3D[f.src]!=null || viewB.features3D[f.dst]!=null )
				continue;

			Point2D_F64 normA = viewA.featureNorm.get(f.src);
			Point2D_F64 normB = viewB.featureNorm.get(f.dst);

			Feature3D feature3D = new Feature3D();

			feature3D.color = viewA.colors.get(f.src);
			feature3D.feature.add( f.src );
			feature3D.feature.add( f.dst );

			// the more parallel a line is worse the triangulation. Get rid of bad ideas early here
			arrowA.set(normA.x,normA.y,1);
			arrowB.set(normB.x,normB.y,1);
			GeometryMath_F64.mult(edge.a_to_b.R,arrowA,arrowA); // put them into the same reference frame

			double angle = UtilVector3D_F64.acute(arrowA,arrowB);
			if( angle < minAngle )
				continue;

			if( !triangulate.triangulate(normA,normB,edge.a_to_b,feature3D.worldPt) ) {
				continue;
			}
			// mark this feature3D as being associated with these image features
			viewA.features3D[f.src] = feature3D;
			viewB.features3D[f.dst] = feature3D;

			// record which frame the feature was seen in
			if( feature3D.views.contains(viewA.index))
				throw new RuntimeException("Egads");
			if( feature3D.views.contains(viewB.index))
				throw new RuntimeException("Egads");
			feature3D.views.add(viewA.index);
			feature3D.views.add(viewB.index);

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
		FastQueue<Point2D_F64> pixelsNormA = graphNodes.get(imageA).featureNorm;
		FastQueue<Point2D_F64> pixelsNormB = graphNodes.get(imageB).featureNorm;
		List<AssociatedPair> pairs = new ArrayList<>();
		for (int i = 0; i < matches.size(); i++) {
			AssociatedIndex a = matches.get(i);
			pairs.add(new AssociatedPair(pixelsNormA.get(a.src), pixelsNormB.get(a.dst)));
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

	/**
	 * Bundle adjustment can run very slow if there are too many features. This uses a heuristic explained in the code
	 * to reduce the number of 3D features by removing redundant ones. Could improve this approach by applying
	 * stricture geometric constraints that should improve the feature quality, in theory.
	 */
	private void pruneFeatures() {

		// Features which can be tracked across multiple views are much more valuable, so keep those
//		for (int i = 0; i < featuresAll.size(); i++) {
//			Feature3D f = featuresAll.get(i);
//			if( f.obs.size >= 3 ) {
//				f.included = true;
//			}
//		}

		// Try to reduce the number of features in an edge by randomly removing them until the count is above
		// some number
		List<Feature3D> candidates = new ArrayList<>();
		for( CameraMotion m : graphEdges ) {
			// create a list of 3D features which can be added for this edge specially
			candidates.clear();
			for (int i = 0; i < m.features.size(); i++) {
				AssociatedIndex a = m.features.get(i);
				Feature3D f = m.viewSrc.features3D[a.src];
				if( f!=null && !f.included && f.views.size >= 3 ) {
					candidates.add(f);
				}
			}

			// Randomly save a few of the features in this edge
			int totalSave = Math.min(TARGET_FEATURE_EDGE, candidates.size());

			Collections.shuffle(candidates);
			for (int i = 0; i < totalSave; i++) {
				candidates.get(i).included = true;
			}
		}

		// Save the marked features
		for( Feature3D f : featuresAll ) {
			if( f.included ) {
				featuresPruned.add(f);
			}
		}
	}

	private void retriangulateFeatures( CameraPinholeRadial intrinsic ) {

		TriangulateLinearDLT triangulate = new TriangulateLinearDLT();

		List<Point2D_F64> observations = new ArrayList<>();
		List<Se3_F64> worldToView = new ArrayList<>();

		WorldToCameraToPixel wcp = new WorldToCameraToPixel();
		Point2D_F64 pixel = new Point2D_F64();

		int removedCount = 0;
		for (int i = featuresAll.size()-1; i >= 0; i--) {
			Feature3D f = featuresAll.get(i);

			if( f.views.size < 3 )
				continue;
			observations.clear();
			worldToView.clear();

			for (int j = 0; j < f.views.size; j++) {
				CameraView v = graphNodes.get(f.views.get(j));

				worldToView.add( v.viewToWorld.invert(null));
				observations.add( v.featureNorm.get( f.feature.get(j)));
			}

			triangulate.triangulate(observations,worldToView,f.worldPt);

			for (int j = 0; j < f.views.size; j++) {
				CameraView v = graphNodes.get(f.views.get(j));
				wcp.configure(intrinsic,worldToView.get(j));
				wcp.transform(f.worldPt,pixel);

				Point2D_F64 observed = v.featurePixels.get(f.feature.get(j));
				if( observed.distance(pixel) > 20 ) {
					removedCount++;
					featuresAll.remove(i);
					break;
				}
			}
		}
		System.out.println("  Second triangulate removed "+removedCount);
	}

	private void performBundleAdjustment( CameraView seed , CameraPinholeRadial intrinsic ) {
		BundleAdjustmentShur_DSCC sba = new BundleAdjustmentShur_DSCC(1e-3);

		sba.configure(1e-4,1e-4,20);

		BundleAdjustmentSceneStructure structure = new BundleAdjustmentSceneStructure();
		BundleAdjustmentObservations observations = new BundleAdjustmentObservations(graphNodes.size());

		structure.initialize(1,graphNodes.size(),featuresPruned.size());

		// There is only one camera with known calibration
		structure.setCamera(0,true,intrinsic);

		for( int i = 0; i < graphNodes.size(); i++ ) {
			CameraView v = graphNodes.get(i);
			structure.setView(i,v==seed,v.viewToWorld.invert(null));
			structure.connectViewToCamera(i,0);
		}

		for (int indexPoint = 0; indexPoint < featuresPruned.size(); indexPoint++) {
			Feature3D f = featuresPruned.get(indexPoint);

			structure.setPoint(indexPoint,f.worldPt.x,f.worldPt.y,f.worldPt.z);

			for (int j = 0; j < f.views.size; j++) {
				int indexView = f.views.get(j);
				structure.connectPointToView(indexPoint,indexView);

				Point2D_F64 pixel = graphNodes.get(indexView).featurePixels.get(f.feature.get(j));
				observations.getView(indexView).add(indexPoint,(float)pixel.x,(float)pixel.y);
			}
		}

		if( !sba.optimize(structure,observations) ) {
			throw new RuntimeException("Bundle adjustment failed!");
		}
		System.out.println("Score before: "+sba.getErrorBefore()+"  After: "+sba.getErrorAfter());

		// Copy the results into the scene
		for( int i = 0; i < graphNodes.size(); i++ ) {
			structure.views[i].worldToView.invert(graphNodes.get(i).viewToWorld);
		}
		for (int i = 0; i < featuresPruned.size(); i++) {
			featuresPruned.get(i).worldPt.set( structure.points[i]);
		}
	}

	public static class CameraView {
		int index;
		Se3_F64 viewToWorld = new Se3_F64();
		int distanceFromRoot = -1;

		List<CameraMotion> connections = new ArrayList<>();

		// feature descriptor of all features in this image
		FastQueue<BrightFeature> descriptions = new SurfFeatureQueue(64);
		// observed location of all features in pixels
		FastQueue<Point2D_F64> featurePixels = new FastQueue<>(Point2D_F64.class, true);
		FastQueue<Point2D_F64> featureNorm = new FastQueue<>(Point2D_F64.class, true);
		// RGB color of each feature
		GrowQueue_I32 colors = new GrowQueue_I32();
		// Estimated 3D location for SOME of the features
		Feature3D[] features3D;

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
		// observations in each frame that it's visible. These are in normalized image coordinates
//		FastQueue<Point2D_F64> normObs = new FastQueue<>(Point2D_F64.class, true);
//		FastQueue<Point2D_F64> pixelObs = new FastQueue<>(Point2D_F64.class, true);
		// index of each frame its visible in
		GrowQueue_I32 feature = new GrowQueue_I32();
		GrowQueue_I32 views = new GrowQueue_I32();
		boolean included=false;
	}

	public static void main(String[] args) {

		String directory = UtilIO.pathExample("sfm/chair");

		CameraPinholeRadial intrinsic = CalibrationIO.load(
				new File(directory,"/intrinsic_DSC-HX5_3648x2736_to_640x480.yaml"));

		List<BufferedImage> images = UtilImageIO.loadImages(directory,".*jpg");

//		while( images.size() > 8 ) {
//			images.remove(8);
//		}

		ExampleMultiviewSceneReconstruction example = new ExampleMultiviewSceneReconstruction();

		long before = System.currentTimeMillis();
		example.process(intrinsic,images);
		long after = System.currentTimeMillis();

		System.out.println("Elapsed time "+(after-before)/1000.0+" (s)");
	}
}

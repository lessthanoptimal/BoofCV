/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.sfm.structure2;

import boofcv.abst.geo.TriangulateNViewsProjective;
import boofcv.abst.geo.bundle.BundleAdjustment;
import boofcv.abst.geo.bundle.ScaleSceneStructure;
import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureProjective;
import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.pose.PoseFromPairLinear6;
import boofcv.alg.sfm.structure2.PairwiseImageGraph2.Motion;
import boofcv.alg.sfm.structure2.PairwiseImageGraph2.View;
import boofcv.factory.geo.*;
import boofcv.misc.ConfigConverge;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.AssociatedTripleIndex;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.geo.TrifocalTensor;
import boofcv.struct.image.ImageDimension;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point4D_F64;
import org.ddogleg.fitting.modelset.ransac.Ransac;
import org.ddogleg.optimization.lm.ConfigLevenbergMarquardt;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Given a set of views and a set of features which are visible in all views, estimate their structure up to a
 * projective transform. Summary of processing steps:
 *
 * <ol>
 *     <li>Select initial set of 3 views</li>
 *     <li>Association between all 3 views</li>
 *     <li>RANSAC to find Trifocal Tensor</li>
 *     <li>3 Projective from trifocal</li>
 *     <li>Triangulate features</li>
 *     <li>Find remaining projective camera matrices</li>
 *     <li>Refine with bundle adjustment</li>
 * </ol>
 *
 * @author Peter Abeles
 */
public class ProjectiveInitializeAllCommon {

	public ConfigRansac configRansac = new ConfigRansac();
	public ConfigTrifocal configTriRansac = new ConfigTrifocal();
	public ConfigTrifocalError configError = new ConfigTrifocalError();
	public ConfigLevenbergMarquardt configLM = new ConfigLevenbergMarquardt();
	public ConfigBundleAdjustment configSBA = new ConfigBundleAdjustment();
	public ConfigConverge converge = new ConfigConverge(1e-8,1e-8,200);
	public boolean scaleSBA = true; // if true input is scaled for SBA

	// The estimated scene structure
	public SceneStructureProjective structure = new SceneStructureProjective(true);

	// estimating the trifocal tensor and storing which observations are in the inlier set
	public Ransac<TrifocalTensor, AssociatedTriple> ransac;
	public TriangulateNViewsProjective triangulator;
	public PoseFromPairLinear6 poseEstimator = new PoseFromPairLinear6();
	public BundleAdjustment<SceneStructureProjective> sba;
	public ScaleSceneStructure scaler = new ScaleSceneStructure();

	// used to get pixel coordinates of features in a view
	private LookupSimilarImages db;

	// Location of features in the image. Pixels
	FastQueue<Point2D_F64> featsA = new FastQueue<>(Point2D_F64.class,true);
	FastQueue<Point2D_F64> featsB = new FastQueue<>(Point2D_F64.class,true);
	FastQueue<Point2D_F64> featsC = new FastQueue<>(Point2D_F64.class,true);

	// Indexes of common features after all but the inliers have been removed by RANSAC
	GrowQueue_I32 inlierToSeed = new GrowQueue_I32();


	// Indicates if debugging information should be printed
	PrintStream verbose;
	int verboseLevel;

	//-------------- Internal workspace variables
	int selectedTriple[] = new int[2];
	FastQueue<AssociatedTripleIndex> matchesTripleIdx = new FastQueue<>(AssociatedTripleIndex.class,true);
	FastQueue<AssociatedTriple> matchesTriple = new FastQueue<>(AssociatedTriple.class,true);
	// triangulated 3D homogenous points in seed reference frame
	FastQueue<Point4D_F64> points3D = new FastQueue<>(Point4D_F64.class,true);
	// Associated pixel observations
	FastQueue<AssociatedPair> assocPixel = new FastQueue<>(AssociatedPair.class,true);
	ImageDimension shape = new ImageDimension();
	// lookup table from feature ID in seed view to structure
	GrowQueue_I32 seedToStructure = new GrowQueue_I32();

	public ProjectiveInitializeAllCommon() {
		configRansac.maxIterations = 500;
		configRansac.inlierThreshold = 1;

		triangulator = FactoryMultiView.triangulateNView(ConfigTriangulation.GEOMETRIC);

		fixate();
	}

	/**
	 * Must call if you change configurations.
	 */
	public void fixate() {
		ransac = FactoryMultiViewRobust.trifocalRansac(configTriRansac,configError,configRansac);
		sba = FactoryMultiView.bundleAdjustmentProjective(configSBA);
	}

	/**
	 *
	 * @param seed The seed view that will act as the origin
	 * @param common Indexes of common features. Index in seed view
	 * @param motions Index of motions in the seed view to use when initializing
	 * @return
	 */
	public boolean process( LookupSimilarImages db , View seed , GrowQueue_I32 common , GrowQueue_I32 motions ) {
		this.db = db;

		if( motions.size == 1 ) {
			// stereo is a special case and requires different logic
			throw new IllegalArgumentException("Can't handle the stereo case yet");
		}
		// find the 3 view combination with the best score
		if( !selectInitialTriplet(seed,motions,selectedTriple))
			return false;

		// Find tracks between all 3 views
		Motion seedB = seed.connections.get(selectedTriple[0]);
		Motion seedC = seed.connections.get(selectedTriple[1]);
		View viewB = seedB.other(seed);
		View viewC = seedC.other(seed);

		findTripleMatches(seed,seedB,seedC, matchesTripleIdx);
		if( matchesTripleIdx.size == 0 )
			return false;

		// Convert associated features into associated triple pixels
		convertAssociatedTriple(db, seed, viewB, viewC);

		// Use trifocal tensor to prune tracks from that set
		initializeProjective3(matchesTriple,matchesTripleIdx,motions.size,seed,viewB,viewC,
				selectedTriple[0],selectedTriple[1]);

		// Estimate projective for each view not in the original triplet
		// This is simple because the 3D coordinate of each point is already known
		if( common.size > 2 ) { // only do if more than 3 views
			if (!findRemainingCameraMatrices(db, seed, motions))
				return false;
		}

		// create observation data structure for SBA
		SceneObservations observations = createObservationsForBundleAdjustment(db, seed, motions);

		// Refine results with projective bundle adjustment
		return refineWithBundleAdjustment(observations);
	}

	/**
	 * Exhaustively look at all triplets that connect with the seed view
	 */
	boolean selectInitialTriplet( View seed , GrowQueue_I32 motions , int selected[] ) {
		double bestScore = 0;
		for (int i = 0; i < motions.size; i++) {
			View viewB = seed.connections.get(i).other(seed);

			for (int j = i+1; j < motions.size; j++) {
				View viewC = seed.connections.get(j).other(seed);

				double s = scoreTripleView(seed,viewB,viewC);
				if( s > bestScore ) {
					bestScore = s;
					selected[0] = i;
					selected[1] = j;
				}
			}
		}
		return bestScore != 0;
	}

	double scoreTripleView(View seedA, View viewB , View viewC ) {
		Motion motionAB = seedA.findMotion(viewB);
		Motion motionAC = seedA.findMotion(viewC);
		Motion motionBC = viewB.findMotion(viewC);
		if( motionBC == null )
			return 0;

		double score = 0;
		score += DoStuffFromPairwiseGraph.score(motionAB);
		score += DoStuffFromPairwiseGraph.score(motionAC);
		score += DoStuffFromPairwiseGraph.score(motionBC);

		return score;
	}

	void findTripleMatches(View seedA, Motion edgeAB , Motion edgeAC ,
						   FastQueue<AssociatedTripleIndex> matches ) {
		matches.reset();

		boolean srcAB = edgeAB.src == seedA;
		boolean srcAC = edgeAC.src == seedA;

		View viewB = srcAB ? edgeAB.dst : edgeAB.src;
		View viewC = srcAC ? edgeAC.dst : edgeAC.src;

		// see if there's an edge from viewA to viewB. There should be...
		Motion edgeBC = viewB.findMotion(viewC);
		if( edgeBC == null ) {
			return;
		}

		int[] table_B_to_A = createFeatureLookup(edgeAB, srcAB, viewB);
		int[] table_C_to_A = createFeatureLookup(edgeAC, srcAC, viewC);

		// Go through all the matches from B to C and see if the path is consistent between all the views
		boolean srcIsB = edgeBC.src == viewB;
		for (int i = 0; i < edgeBC.associated.size; i++) {
			AssociatedIndex assoc = edgeBC.associated.get(i);
			if( srcIsB ) {
				if( table_B_to_A[assoc.src] != -1 ) {
					AssociatedTripleIndex tri = matches.grow();
					tri.a = table_B_to_A[assoc.src];
					if( table_C_to_A[assoc.dst] == tri.a ) {
						tri.b = assoc.src;
						tri.c = assoc.dst;
					} else {
						matches.removeTail();
					}
				}
			}
		}
	}

	private int[] createFeatureLookup(Motion edgeAC, boolean srcAC, View viewC) {
		int[] table_C_to_A = new int[viewC.totalFeatures];
		Arrays.fill(table_C_to_A, 0, viewC.totalFeatures, -1);
		for (int i = 0; i < edgeAC.associated.size; i++) {
			AssociatedIndex assoc = edgeAC.associated.get(i);
			if (srcAC) {
				table_C_to_A[assoc.dst] = assoc.src;
			} else {
				table_C_to_A[assoc.src] = assoc.dst;
			}
		}
		return table_C_to_A;
	}


	/**
	 * Triangulates the location of each features in homogenous space
	 */
	private void triangulateFeatures(List<AssociatedTriple> inliers,
									 DMatrixRMaj P1, DMatrixRMaj P2, DMatrixRMaj P3) {
		List<DMatrixRMaj> cameraMatrices = new ArrayList<>();
		cameraMatrices.add(P1);
		cameraMatrices.add(P2);
		cameraMatrices.add(P3);

		// need elements to be non-empty so that it can use set().  probably over optimization
		List<Point2D_F64> triangObs = new ArrayList<>();
		triangObs.add(null);
		triangObs.add(null);
		triangObs.add(null);

		Point4D_F64 X = new Point4D_F64();
		for (int i = 0; i < inliers.size(); i++) {
			AssociatedTriple t = inliers.get(i);

			triangObs.set(0,t.p1);
			triangObs.set(1,t.p2);
			triangObs.set(2,t.p3);

			// triangulation can fail if all 3 views have the same pixel value. This has been observed in
			// simulated 3D scenes
			if( triangulator.triangulate(triangObs,cameraMatrices,X)) {
				structure.points[i].set(X.x,X.y,X.z,X.w);
			} else {
				throw new RuntimeException("Failed to triangulate a point in the inlier set?! Handle if this is common");
			}
		}
	}

	private void convertAssociatedTriple(LookupSimilarImages db, View seed, View viewB, View viewC) {
		db.lookupFeatures(seed.id,featsA);
		db.lookupFeatures(viewB.id,featsB);
		db.lookupFeatures(viewC.id,featsC);
		matchesTriple.reset(); // reset and declare memory
		matchesTriple.growArray(matchesTripleIdx.size);
		for (int i = 0; i < matchesTripleIdx.size; i++) {
			AssociatedTripleIndex idx = matchesTripleIdx.get(i);
			AssociatedTriple a = matchesTriple.grow();
			a.p1.set(featsA.get(idx.a));
			a.p2.set(featsB.get(idx.b));
			a.p3.set(featsC.get(idx.c));
		}
	}

	/**
	 * Initializes projective reconstruction from 3-views.
	 *
	 * 1) RANSAC to fit a trifocal tensor
	 * 2) Extract camera matrices that have a common projective space
	 * 3) Triangulate location of 3D homogenous points
	 *
	 * @param associated List of associated pixels
	 * @param associatedIdx List of associated feature indexes
	 */
	private void initializeProjective3(FastQueue<AssociatedTriple> associated ,
									   FastQueue<AssociatedTripleIndex> associatedIdx ,
									   int totalViews,
									   View viewA , View viewB , View viewC ,
									   int idxViewB , int idxViewC ) {
		ransac.process(associated.toList());

		List<AssociatedTriple> inliers = ransac.getMatchSet();
		TrifocalTensor model = ransac.getModelParameters();
		if( verbose != null )
			verbose.println("Remaining after RANSAC "+inliers.size()+" / "+associated.size());

		// projective camera matrices for each view
		DMatrixRMaj P1 = CommonOps_DDRM.identity(3,4);
		DMatrixRMaj P2 = new DMatrixRMaj(3,4);
		DMatrixRMaj P3 = new DMatrixRMaj(3,4);

		MultiViewOps.extractCameraMatrices(model,P2,P3);

		// Initialize the 3D scene structure, stored in a format understood by bundle adjustment
		structure.initialize(totalViews,inliers.size());

		// specify the found projective camera matrices
		db.lookupShape(viewA.id,shape);
		// The first view is assumed to be the coordinate system's origin and is identity by definition
		structure.setView(0,true, P1,shape.width,shape.height);
		db.lookupShape(viewB.id,shape);
		structure.setView(idxViewB,false,P2,shape.width,shape.height);
		db.lookupShape(viewC.id,shape);
		structure.setView(idxViewC,false,P3,shape.width,shape.height);

		// triangulate homogenous coordinates for each point in the inlier set
		triangulateFeatures(inliers, P1, P2, P3);

		// Update the list of common features by pruning features not in the inlier set
		seedToStructure.resize(viewA.totalFeatures);
		seedToStructure.fill(-1); // -1 indicates no match
		inlierToSeed.resize(inliers.size());
		for (int i = 0; i < inliers.size(); i++) {
			int inputIdx = ransac.getInputIndex(i);

			// table to go from inlier list into seed feature index
			inlierToSeed.data[i] = matchesTripleIdx.get(inputIdx).a;
			// seed feature index into the ouptut structure index
			seedToStructure.data[inlierToSeed.data[i]] = i;
		}
	}

	/**
	 * Uses the triangulated points and observations in the root view to estimate the camera matrix for
	 * all the views which are remaining
	 * @return true if successful or false if not
	 */
	private boolean findRemainingCameraMatrices(LookupSimilarImages db, View seed, GrowQueue_I32 motions) {
		points3D.reset(); // points in 3D
		for (int i = 0; i < structure.points.length; i++) {
			structure.points[i].get(points3D.grow());
		}
		assocPixel.reset(); // contains associated pairs of pixel observations
		for (int i = 0; i < inlierToSeed.size; i++) {
			assocPixel.grow().p1.set(matchesTriple.get(i).p1);
		}

		for (int motionIdx = 0; motionIdx < motions.size; motionIdx++) {
			// skip views already in the scene's structure
			if( motionIdx == selectedTriple[0] || motionIdx == selectedTriple[1])
				continue;
			Motion edge = seed.connections.get(motions.get(motionIdx));
			View viewI = edge.other(seed);

			db.lookupFeatures(viewI.id,featsB);
			boolean seedSrc = edge.src == seed;

			int matched = 0;
			for (int i = 0; i < edge.associated.size; i++) {
				AssociatedIndex a = edge.associated.get(i);
				int featId = seedToStructure.data[seedSrc ? a.src : a.dst];
				if( featId == -1 )
					continue;
				assocPixel.get(featId).p2.set( featsB.get(seedSrc?a.dst:a.src) );
				matched++;
			}
			if( matched != assocPixel.size)
				throw new RuntimeException("BUG! Didn't find all features in the view");

			if( poseEstimator.processHomogenous(assocPixel.toList(),points3D.toList()) ) {
				db.lookupShape(viewI.id,shape);
				structure.setView(motionIdx,false,poseEstimator.getProjective(),shape.width,shape.height);
			} else {
				if( verbose != null ) {
					verbose.println("Pose estimator failed! motionIdx="+motionIdx);
				}
				return false;
			}
		}
		return true;
	}

	/**
	 * Convert observations into a format which bundle adjustment will understand
	 * @param seed The first view which all other views are connected to
	 * @param motions Which edges in seed
	 * @return observations for SBA
	 */
	private SceneObservations createObservationsForBundleAdjustment(LookupSimilarImages db, View seed, GrowQueue_I32 motions) {
		// seed view + the motions
		SceneObservations observations = new SceneObservations(motions.size+1);

		// Observations for the seed view are a special case
		SceneObservations.View obsView = observations.getView(0);
		for (int i = 0; i < inlierToSeed.size; i++) {
			int id = inlierToSeed.data[i];
			Point2D_F64 o = featsA.get(id); // featsA is never modified after initially loaded

			id = seedToStructure.data[id];
			obsView.add(id,(float)o.x,(float)o.y);
		}

		// Now add observations for edges connected to the seed
		for (int i = 0; i < motions.size(); i++) {
			obsView = observations.getView(i+1);
			Motion m = seed.connections.get(motions.get(i));
			View v = m.other(seed);
			boolean seedIsSrc = m.src == seed;
			db.lookupFeatures(v.id,featsB);
			for (int j = 0; j < m.associated.size; j++) {
				AssociatedIndex a = m.associated.get(j);
				int id = seedToStructure.data[seedIsSrc?a.src:a.dst];
				if( id < 0 )
					continue;
				Point2D_F64 o = featsB.get(seedIsSrc?a.dst:a.src);
				obsView.add(id,(float)o.x,(float)o.y);
			}
		}
		return observations;
	}

	/**
	 * Last step is to refine the current initial estimate with bundle adjustment
	 */
	private boolean refineWithBundleAdjustment(SceneObservations observations) {
		if( scaleSBA ) {
			scaler.applyScale(structure,observations);
		}

		sba.setVerbose(verbose,verboseLevel);
		sba.setParameters(structure,observations);
		sba.configure(converge.ftol,converge.gtol,converge.maxIterations);

		if( !sba.optimize(structure) ) {
			return false;
		}

		if( scaleSBA ) {
			// only undo scaling on camera matrices since observations are discarded
			for (int i = 0; i < structure.views.length; i++) {
				DMatrixRMaj P = structure.views[i].worldToView;
				scaler.pixelScaling.get(i).remove(P,P);
			}
			scaler.undoScale(structure,observations);
		}
		return true;
	}

	/**
	 * Adjusts the level of verbosity for debugging
	 */
	public void setVerbose( PrintStream verbose , int level ) {
		this.verbose = verbose;
		this.verboseLevel = level;
	}
}

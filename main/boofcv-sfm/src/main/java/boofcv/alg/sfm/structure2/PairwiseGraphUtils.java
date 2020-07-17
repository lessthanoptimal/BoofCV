/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.sfm.structure2.SceneWorkingGraph.InlierInfo;
import boofcv.factory.geo.ConfigTriangulation;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.factory.geo.FactoryMultiViewRobust;
import boofcv.misc.ConfigConverge;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.geo.TrifocalTensor;
import boofcv.struct.image.ImageDimension;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point4D_F64;
import lombok.Getter;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_B;
import org.ddogleg.struct.GrowQueue_I32;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

import java.util.ArrayList;
import java.util.List;

import static boofcv.misc.BoofMiscOps.assertBoof;

/**
 * Various utility functions for dealing with {@link PairwiseImageGraph2}
 *
 * @author Peter Abeles
 */
public class PairwiseGraphUtils {

	LookupSimilarImages db;

	GrowQueue_B visibleAll = new GrowQueue_B();
	GrowQueue_B visibleMotion = new GrowQueue_B();

	//-------------------------------- Configurations. Must call fixate() if modified
	/** Converge tolerance for SBA */
	public ConfigConverge configConvergeSBA = new ConfigConverge(1e-8,1e-8,50);
	/** Toggles scaling inputs for SBA */
	public boolean configScaleSBA = false;

	/** The estimated scene structure. This the final estimated scene state */
	protected final @Getter SceneStructureProjective structure = new SceneStructureProjective(true);
	protected final @Getter SceneObservations observations = new SceneObservations();

	protected @Getter ModelMatcher<TrifocalTensor, AssociatedTriple> ransac;
	protected @Getter TriangulateNViewsProjective triangulator;
	protected @Getter PoseFromPairLinear6 poseEstimator = new PoseFromPairLinear6();
	protected @Getter BundleAdjustment<SceneStructureProjective> sba;
	protected @Getter ScaleSceneStructure scaler = new ScaleSceneStructure();

	/** The three views used in three view algorithms */
	public View seed, viewB, viewC;

	public final FastQueue<AssociatedTriple> matchesTriple = new FastQueue<>(AssociatedTriple::new);
	/** Inliers from robust fitting of trifocal tensor */
	public List<AssociatedTriple> inliersThreeView;

	/** Projective camera matrices for 3-View reconstruction. P1 is always identity */
	public final DMatrixRMaj P1 = new DMatrixRMaj(3,4);
	public final DMatrixRMaj P2 = new DMatrixRMaj(3,4);
	public final DMatrixRMaj P3 = new DMatrixRMaj(3,4);

	// Location of features in the image. Pixels
	public final FastQueue<Point2D_F64> featsA = new FastQueue<>(Point2D_F64::new);
	public final FastQueue<Point2D_F64> featsB = new FastQueue<>(Point2D_F64::new);
	public final FastQueue<Point2D_F64> featsC = new FastQueue<>(Point2D_F64::new);

	// look up tables for feature indexes from one view to another
	public final GrowQueue_I32 table_A_to_B = new GrowQueue_I32();
	public final GrowQueue_I32 table_A_to_C = new GrowQueue_I32();
	public final GrowQueue_I32 table_B_to_C = new GrowQueue_I32();
	// List indexes in the seed view that are common among all the views
	public final GrowQueue_I32 commonIdx = new GrowQueue_I32();

	/** Score function used to evaluate which motions should be used */
	public ScoreMotion scoreMotion = new DefaultScoreMotion();

	protected final ImageDimension shape = new ImageDimension();

	public PairwiseGraphUtils(TriangulateNViewsProjective triangulator,
							  ModelMatcher<TrifocalTensor, AssociatedTriple> ransac,
							  BundleAdjustment<SceneStructureProjective> sba ) {
		this.triangulator = triangulator;
		this.ransac = ransac;
		this.sba = sba;
	}

	public PairwiseGraphUtils(ConfigProjectiveReconstruction config) {
		config.checkValidity();
		triangulator = FactoryMultiView.triangulateNView(ConfigTriangulation.GEOMETRIC());
		ransac = FactoryMultiViewRobust.trifocalRansac(
				config.ransacTrifocal,config.ransacError,config.ransac);
		sba = FactoryMultiView.bundleSparseProjective(config.sba);
		configConvergeSBA.setTo(config.sbaConverge);
		configScaleSBA = config.sbaScale;
	}

	protected PairwiseGraphUtils(){}

	{
		CommonOps_DDRM.setIdentity(P1);
	}

	/**
	 * Finds the indexes of tracks which are common to all views
	 *
	 * @param seed (Input) The view which is used as the reference point
	 * @param connectIdx (Input) Indexes of connections in the seed view that will be searched for common connections
	 * @return indexes of common tracks. Indexes are indexes in the seed View
	 */
	public GrowQueue_I32 findCommonFeatures(View seed , GrowQueue_I32 connectIdx ) {
		if( connectIdx.size < 1 )
			throw new RuntimeException("Called when there are no connections");
		// if true then it is visible in all tracks
		visibleAll.resize(seed.totalObservations);
		// used to keep track of which features are visible in the current motion
		visibleMotion.resize(seed.totalObservations);

		// Only look at features in the motions that were used to compute the score
		for (int idxMotion = 0; idxMotion < connectIdx.size; idxMotion++) {
			// Mark features which are inside the inlier set connecting these two views
			Motion m = seed.connections.get(connectIdx.get(idxMotion));
			boolean seedIsSrc = m.src==seed;
			visibleMotion.fill(false);
			for (int i = 0; i < m.inliers.size; i++) {
				AssociatedIndex a = m.inliers.get(i);
				visibleMotion.data[seedIsSrc?a.src:a.dst] = true;
			}
			// Update the table visible in all views
			if( idxMotion == 0 ) {
				visibleAll.setTo(visibleMotion);
				continue;
			}
			for (int i = 0; i < seed.totalObservations; i++) {
				visibleAll.data[i] &= visibleMotion.data[i];
			}
		}
		GrowQueue_I32 common = new GrowQueue_I32(visibleAll.count(true));
		for (int i = 0; i < seed.totalObservations; i++) {
			if( visibleAll.data[i] ) {
				common.add(i);
			}
		}
		return common;
	}

	/**
	 * Creates three feature look up tables: A -> B, A -> C, and B -> C
	 */
	public void createThreeViewLookUpTables() {
		Motion connAB = seed.findMotion(viewB);
		Motion connAC = seed.findMotion(viewC);
		Motion connBC = viewB.findMotion(viewC);

		PairwiseGraphUtils.createTableViewAtoB(seed,connAB,table_A_to_B);
		PairwiseGraphUtils.createTableViewAtoB(seed,connAC,table_A_to_C);
		PairwiseGraphUtils.createTableViewAtoB(viewB,connBC,table_B_to_C);
	}

	/**
	 * Finds which features are common between all three views using the look up tables. Results are stored in
	 * {@link #commonIdx} by index of feature in {@link #seed}.
	 */
	public void findCommonFeatures() {
		final int N = seed.totalObservations;
		commonIdx.reset();
		for (int featureIdxA = 0; featureIdxA < N; featureIdxA++) {
			// this feature must be visible in all 3 views
			if( table_A_to_B.data[featureIdxA] < 0 || table_A_to_C.data[featureIdxA] < 0 )
				continue;
			// There also must be a connection from B to C for this feature
			if( table_B_to_C.data[table_A_to_C.data[featureIdxA]] < 0 )
				continue;
			commonIdx.add(featureIdxA);
		}
	}

	/**
	 * Same as {@link #findCommonFeatures()} but with a list that specified a subset of features in {@link #seed}.
	 * @param selectedIdxA List of feature indexes in A to consider
	 */
	public void findCommonFeatures( GrowQueue_I32 selectedIdxA ) {
		commonIdx.reset();
		for (int selectedIdx = 0; selectedIdx < selectedIdxA.size; selectedIdx++) {
			final int featureIdxA = selectedIdxA.get(selectedIdx);

			// this feature must be visible in all 3 views
			if( table_A_to_B.data[featureIdxA] < 0 || table_A_to_C.data[featureIdxA] < 0 )
				continue;
			// There also must be a connection from B to C for this feature
			if( table_B_to_C.data[table_A_to_C.data[featureIdxA]] < 0 )
				continue;
			commonIdx.add(featureIdxA);
		}
	}

	/**
	 * Convert triple from indexes into coordinates
	 */
	public void createTripleFromCommon() {
		// Get coordinates of features in each view
		db.lookupPixelFeats(seed.id,featsA);
		db.lookupPixelFeats(viewB.id,featsB);
		db.lookupPixelFeats(viewC.id,featsC);

		// pre-declare memory
		matchesTriple.reset();
		matchesTriple.resize(commonIdx.size);

		// Copy coordinates into triples list
		for (int i = 0; i < commonIdx.size; i++) {
			int indexA = commonIdx.get(i);
			int indexB = table_A_to_B.data[indexA];
			int indexC = table_A_to_C.data[indexA];

			matchesTriple.get(i).set(featsA.get(indexA),featsB.get(indexB),featsC.get(indexC));
		}
	}

	/**
	 * Robustly estimates the trifocal tensor and extracts canonical camera matrices
	 */
	public boolean estimateProjectiveCamerasRobustly() {
		// Robustly fit trifocal tensor to 3-view
		if( !ransac.process(matchesTriple.toList()) )
			return false;

		// Extract camera matrices
		TrifocalTensor model = ransac.getModelParameters();
		MultiViewOps.trifocalCameraMatrices(model,P2,P3);

		inliersThreeView = ransac.getMatchSet();

		return true;
	}

	/**
	 * Saves which features were inside the inlier set as indexes of the seed view's original feature set
	 */
	public void saveRansacInliers(SceneWorkingGraph.View view ) {
		int N = inliersThreeView.size();
		InlierInfo info = view.projectiveInliers;

		info.observations.reset();
		info.observations.resize(3);
		info.observations.get(0).setTo(commonIdx);
		GrowQueue_I32 indexesB = info.observations.get(1);
		GrowQueue_I32 indexesC =info.observations.get(2);

		info.observations.resize(N);
		for (int i = 0; i < N; i++) {
			int inputIdx = ransac.getInputIndex(i);
			int indexA = commonIdx.get(inputIdx);
			indexesB.add( table_A_to_B.data[indexA] );
			indexesC.add( table_A_to_C.data[indexA] );
		}

		info.views.clear();
		info.views.add(seed);
		info.views.add(viewB);
		info.views.add(viewC);
	}

	/**
	 * Initializes projective reconstruction from 3-views.
	 *
	 * 1) RANSAC to fit a trifocal tensor
	 * 2) Extract camera matrices that have a common projective space
	 * 3) Triangulate location of 3D homogenous points
	 */
	public void initializeSbaSceneThreeView() {
		// Initialize the 3D scene structure, stored in a format understood by bundle adjustment
		structure.initialize(3,inliersThreeView.size());

		// specify the found projective camera matrices
		db.lookupShape(seed.id,shape);
		// The first view is assumed to be the coordinate system's origin and is identity by definition
		structure.setView(0,true, P1,shape.width,shape.height);
		db.lookupShape(viewB.id,shape);
		structure.setView(1,false,P2,shape.width,shape.height);
		db.lookupShape(viewC.id,shape);
		structure.setView(2,false,P3,shape.width,shape.height);

		// triangulate homogenous coordinates for each point in the inlier set
		triangulateFeatures();
	}

	/**
	 * Triangulates the location of each features in homogenous space and save to bundle adjustment scene
	 */
	protected void triangulateFeatures() {
		assertBoof(structure.views.size>0,"Must initialize the structure first");
		assertBoof(structure.points.size==inliersThreeView.size(),
				"Number of inliers must match the number of points in the scene");

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
		for (int i = 0; i < inliersThreeView.size(); i++) {
			AssociatedTriple t = inliersThreeView.get(i);

			triangObs.set(0,t.p1);
			triangObs.set(1,t.p2);
			triangObs.set(2,t.p3);

			// triangulation can fail if all 3 views have the same pixel value. This has been observed in
			// simulated 3D scenes
			if( triangulator.triangulate(triangObs,cameraMatrices,X)) {
				structure.points.data[i].set(X.x,X.y,X.z,X.w);
			} else {
				throw new RuntimeException("Failed to triangulate a point in the inlier set?! Handle if this is common");
			}
		}
	}

	/**
	 * Converts the set of inlier observations from the three view inliers into observations that bundle adjustment
	 * can understand
	 */
	public void initializeSbaObservationsThreeView() {
		observations.initialize(3);

		SceneObservations.View view1 = observations.getView(0);
		SceneObservations.View view2 = observations.getView(1);
		SceneObservations.View view3 = observations.getView(2);

		for (int i = 0; i < inliersThreeView.size(); i++) {
			AssociatedTriple t = inliersThreeView.get(i);

			view1.add(i,(float)t.p1.x,(float)t.p1.y);
			view2.add(i,(float)t.p2.x,(float)t.p2.y);
			view3.add(i,(float)t.p3.x,(float)t.p3.y);
		}
	}

	/**
	 * Last step is to refine the current initial estimate with bundle adjustment
	 */
	public boolean refineWithBundleAdjustment() {
		if( configConvergeSBA.maxIterations <= 0 )
			return true;

		if(configScaleSBA) {
			scaler.applyScale(structure,observations);
		}

		sba.setParameters(structure,observations);
		sba.configure(configConvergeSBA.ftol, configConvergeSBA.gtol, configConvergeSBA.maxIterations);

		if( !sba.optimize(structure) ) {
			return false;
		}

		if(configScaleSBA) {
			scaler.undoScale(structure,observations);
		}
		return true;
	}

	/**
	 * Creates a look up table for converting features indexes from view A to view B from the list of inliers.
	 *
	 * @param viewA (input) The view that is the src in the table
	 * @param edge (input) Edge connect view A to B, this contains the dst in the table
	 * @param table_a_to_b (output) The resulting lookup table
	 */
	public static void createTableViewAtoB(View viewA,
										   Motion edge,
										   GrowQueue_I32 table_a_to_b)
	{
		table_a_to_b.resize(viewA.totalObservations,-1);
		boolean src_is_A = edge.src == viewA;
		for (int i = 0; i < edge.inliers.size; i++) {
			AssociatedIndex assoc = edge.inliers.get(i);
			table_a_to_b.data[src_is_A?assoc.src:assoc.dst] = src_is_A?assoc.dst:assoc.src;
		}
	}

	/**
	 * Scores a motion for its ability to capture 3D scene information. Higher relative numbers mean more 3D structure
	 * can be recovered. The minimum score would be two identical images
	 */
	public interface ScoreMotion {
		double score( PairwiseImageGraph2.Motion m );
	}

	public static class DefaultScoreMotion implements ScoreMotion {
		@Override
		public double score( PairwiseImageGraph2.Motion m ) {
			// countF and countF will be <= totalFeatures

			// Prefer a scene more features from a fundamental matrix than a homography.
			// This can be sign that the scene has a rich 3D structure and is poorly represented by
			// a plane or rotational motion
			double score = Math.min(5,m.countF/(double)(m.countH+1));
			// Also prefer more features from the original image to be matched
			score *= m.countF;

			return score;
		}
	}
}

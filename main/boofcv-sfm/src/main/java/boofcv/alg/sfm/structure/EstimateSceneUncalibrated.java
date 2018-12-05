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

package boofcv.alg.sfm.structure;

import boofcv.abst.geo.Estimate1ofEpipolar;
import boofcv.abst.geo.RefineEpipolar;
import boofcv.abst.geo.TriangulateTwoViewsProjective;
import boofcv.abst.geo.bundle.BundleAdjustment;
import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureProjective;
import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.sfm.EstimateSceneStructure;
import boofcv.alg.sfm.structure.PairwiseImageGraph.Camera;
import boofcv.alg.sfm.structure.PairwiseImageGraph.Motion;
import boofcv.alg.sfm.structure.PairwiseImageGraph.View;
import boofcv.factory.geo.EpipolarError;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.geo.AssociatedPair;
import georegression.struct.point.Point4D_F64;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_F64;
import org.ddogleg.struct.GrowQueue_I32;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class EstimateSceneUncalibrated
	implements EstimateSceneStructure<SceneStructureProjective>
{

	BundleAdjustment<SceneStructureProjective> sba =
			FactoryMultiView.bundleAdjustmentProjective(null);

	FastQueue<ProjectiveView> views = new FastQueue<>(ProjectiveView.class,true);
	FastQueue<Feature3D> features = new FastQueue<>(Feature3D.class,true);

	// score of each motion for triangulation
	GrowQueue_F64 scores = new GrowQueue_F64();

	PairwiseImageGraph graph;

	Estimate1ofEpipolar computeH = FactoryMultiView.homographyDLT(true);
	RefineEpipolar refineH = FactoryMultiView.homographyRefine(1e-6,5, EpipolarError.SAMPSON);
	FastQueue<AssociatedPair> pairs = new FastQueue<>(AssociatedPair.class,true);
	GrowQueue_F64 errors = new GrowQueue_F64();

	TriangulateTwoViewsProjective triangulator = FactoryMultiView.triangulateTwoDLT();

	// Verbose output to standard out
	PrintStream verbose;

	boolean stopRequested;

	@Override
	public boolean process(PairwiseImageGraph graph ) {
		this.graph = graph;
		this.stopRequested = false;

		views.reset();
		features.reset();

		for (int i = 0; i < graph.nodes.size(); i++) {
			View v = graph.nodes.get(i);
			views.grow().initialize(v);
		}

		for( String cameraName : graph.cameras.keySet() ) {

			Camera camera = graph.cameras.get(cameraName);

			// List all motions which belong to this camera and only this camera
			List<Motion> open = graph.findCameraMotions(camera,null);

			if( open.isEmpty() )
				continue;

			// Compute how good each view is for triangulation
			scores.resize(open.size());
			for (int i = 0; i < open.size(); i++) {
				scores.data[i] = scoreForTriangulation( open.get(i) );
			}

			// Select the edge with the best geometry and initialize structure
			int bestIndex = scores.indexOfGreatest();
			if( !initializeStructure(open.get(bestIndex)))
				throw new RuntimeException("Failed?!?");
			scores.data[bestIndex] = -1; // mark it so that it isn't selected again

			// Add all the other connected views
			while( open.size() > 0 ) {
				Motion next = selectNextMotion(open);
				if( next == null )
					break;

				// mark so that it isn't selected again
				scores.data[next.index] = -1;

				// TODO Estimate pose of this view
				// TODO triangulate any points  not already triangulated
				// TODO bundle adjustment
			}
		}

		return true;
	}

	@Override
	public SceneStructureProjective getSceneStructure() {
		return null;
	}

	@Override
	public SceneObservations getObservations() {
		return null;
	}

	@Override
	public void reset() {

	}

	Motion selectNextMotion(List<Motion> motions ) {
		double best = 0;
		Motion selected = null;

		for (int i = 0; i < motions.size(); i++) {
			if( scores.data[i] <= 0 )
				continue;

			Motion v = motions.get(i);
			ProjectiveView viewA = views.get( v.viewSrc.index );
			ProjectiveView viewB = views.get( v.viewDst.index );

			// TODO consider the number of already triangulated features?

			if( viewA.estimated || viewB.estimated ) {
				if( scores.data[i] > best ) {
					best = scores.data[i];
					selected = v;
				}
			}
		}

		return selected;
	}


	boolean initializeStructure( Motion selected )
	{
		ProjectiveView viewA = views.get(selected.viewSrc.index);
		ProjectiveView viewB = views.get(selected.viewDst.index);

		CommonOps_DDRM.setIdentity(viewA.P);
		viewB.P.set(MultiViewOps.fundamentalToProjective(selected.F));

		viewA.estimated = true;
		viewB.estimated = true;

		// mark this motion as being processed
		selected.viewSrc = selected.viewDst = null;

		Point4D_F64 X = new Point4D_F64();

		for (int i = 0; i < selected.associated.size(); i++) {
			AssociatedIndex ai = selected.associated.get(i);

			if( !triangulator.triangulate(
					viewA.view.observationPixels.get(ai.src),
					viewB.view.observationPixels.get(ai.dst),viewA.P,viewB.P,
					X) ) {
				continue;
			}

			Feature3D f3 = new Feature3D();
			f3.obsIdx.add( ai.src );
			f3.obsIdx.add( ai.dst );
			f3.views.add(viewA);
			f3.views.add(viewB);
			f3.worldPt.set(X);

			features.add(f3);
		}
		return true;
	}

	/**
	 * Compute score to decide which motion to initialize structure from. A homography is fit to the
	 * observations and the error compute. The homography should be a poor fit if the scene had 3D structure.
	 * The 50% homography error is then scaled by the number of pairs to bias the score good matches
	 * @param motion input
	 * @return fit score. Larger is better.
	 */
	double scoreForTriangulation( Motion motion ) {
		DMatrixRMaj H = new DMatrixRMaj(3,3);

		View viewA = motion.viewSrc;
		View viewB = motion.viewDst;

		// Compute initial estimate for H
		pairs.reset();
		for (int i = 0; i < motion.associated.size(); i++) {
			AssociatedIndex ai = motion.associated.get(i);
			pairs.grow().set(
					viewA.observationPixels.get(ai.src),
					viewB.observationPixels.get(ai.dst));
		}

		if(!computeH.process(pairs.toList(),H))
			return -1;

		// remove bias from linear model
		if( !refineH.fitModel(pairs.toList(),H,H) )
			return -1;


		// Compute 50% errors to avoid bias from outliers
		MultiViewOps.errorsHomographySymm(pairs.toList(),H,null,errors);
		errors.sort();

		return errors.getFraction(0.5)*Math.max(5,pairs.size-20);
	}

	@Override
	public void requestStop() {
		stopRequested = true;
	}

	@Override
	public boolean isStopRequested() {
		return stopRequested;
	}

	public void setVerbose(PrintStream verbose) {
		this.verbose = verbose;
	}

	static class ProjectiveView {
		public DMatrixRMaj P = new DMatrixRMaj(3, 4);
		public View view;
		public boolean estimated;

		public void initialize( View view )
		{
			CommonOps_DDRM.setIdentity(P);
			this.view = view;
			this.estimated = false;
		}
	}

	static class Feature3D {
		// estimate 3D position of the feature in world frame. homogenous coordinates
		public Point4D_F64 worldPt = new Point4D_F64();
		// Index of the observation in the corresponding view which the feature is visible in
		public GrowQueue_I32 obsIdx = new GrowQueue_I32();
		// List of views this feature is visible in
		public List<ProjectiveView> views = new ArrayList<>();
	}
}

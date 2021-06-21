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

package boofcv.alg.structure;

import boofcv.BoofTesting;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.WorldToCameraToPixel;
import boofcv.alg.geo.bundle.BundleAdjustmentOps;
import boofcv.misc.BoofLambdas;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.geo.AssociatedTriple;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.geometry.UtilPoint3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.so.Rodrigues_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ejml.data.DMatrixRMaj;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Simulation where not all the features will be visible at the same time
 *
 * @author Peter Abeles
 */
public class MockLookupSimilarImagesRealistic implements LookUpSimilarImages {
	public CameraPinhole intrinsic = new CameraPinhole(400, 410, 0, 420, 420, 800, 800);
	public int numFeatures = 100;
	public Random rand = BoofTesting.createRandom(3);
	public boolean loop = true;

	public int defaultCameraID = 2;

	// recent query image for findSimilar()
	String queryID;

	public List<Feature> points = new ArrayList<>();
	public List<View> views = new ArrayList<>();

	public MockLookupSimilarImagesRealistic() {}

	public MockLookupSimilarImagesRealistic setIntrinsic( CameraPinhole intrinsic ) {
		this.intrinsic = intrinsic;
		return this;
	}

	public MockLookupSimilarImagesRealistic setFeatures( int numFeatures ) {
		this.numFeatures = numFeatures;
		return this;
	}

	public MockLookupSimilarImagesRealistic setSeed( long seed ) {
		rand = BoofTesting.createRandom(seed);
		return this;
	}

	public MockLookupSimilarImagesRealistic setLoop( boolean loop ) {
		this.loop = loop;
		return this;
	}

	public MockLookupSimilarImagesRealistic pathLine( int numViews, double stepLength, double pathLength, int numViewConnect ) {
		double r = 0.5;

		for (Point3D_F64 X : UtilPoint3D_F64.random(new Point3D_F64(0, 0, 2.0*r), -r, pathLength + r, -r, r, -r, r, numFeatures, rand)) {
			Feature f = new Feature();
			f.world.setTo(X);
			f.featureIdx = points.size();
			points.add(f);
		}

		List<Se3_F64> list_camera_to_world = new ArrayList<>();

		for (int viewCnt = 0; viewCnt < numViews; viewCnt++) {
			Se3_F64 camera_to_world = new Se3_F64();

			// Move the camera down the x-axis and push back enough to see most of the points
			camera_to_world.T.x = stepLength*viewCnt;
			camera_to_world.T.y = rand.nextGaussian()*0.1;
			camera_to_world.T.z = rand.nextGaussian()*0.05;

			// Point camera towards the cloud of points
			double noiseRotX = rand.nextGaussian()*0.01;
			double noiseRotY = rand.nextGaussian()*0.01;
			double noiseRotZ = rand.nextGaussian()*0.01;
			ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, noiseRotX, noiseRotY, noiseRotZ, camera_to_world.R);
			list_camera_to_world.add(camera_to_world);
		}

		generate(list_camera_to_world, numViewConnect);
		return this;
	}

	public MockLookupSimilarImagesRealistic pathCircle( int numViews, int numViewConnect ) {
		// Radius of the cameras circling the origin
		double pathRadius = 2;

		for (Point3D_F64 X : UtilPoint3D_F64.random(new Point3D_F64(0, 0, 0), -0.5, 0.5, numFeatures, rand)) {
			Feature f = new Feature();
			f.world.setTo(X);
			f.featureIdx = points.size();
			points.add(f);
		}

		List<Se3_F64> list_camera_to_world = new ArrayList<>();

		for (int viewCnt = 0; viewCnt < numViews; viewCnt++) {
			Se3_F64 camera_to_world = new Se3_F64();

			double yaw = 2.0*Math.PI*viewCnt/numViews;

			// camera lie on the (X,Z) plane with +y pointed down.
			// This is done to make the camera coordinate system and the world coordinate system have a more close
			// relationship
			camera_to_world.T.x = Math.cos(yaw)*pathRadius;
			camera_to_world.T.y = rand.nextGaussian()*pathRadius*0.1; // geometric diversity for self calibration
			camera_to_world.T.z = Math.sin(yaw)*pathRadius;

			// camera is pointing in the opposite direction of it's world location
			ConvertRotation3D_F64.rodriguesToMatrix(new Rodrigues_F64(yaw + Math.PI/2, 0, -1, 0), camera_to_world.R);

			list_camera_to_world.add(camera_to_world);
		}

		generate(list_camera_to_world, numViewConnect);
		return this;
	}

	/**
	 * Renders the scene using only ready generated points and image coordinates
	 */
	public MockLookupSimilarImagesRealistic generate( List<Se3_F64> list_camera_to_world, int numViewConnect ) {
		DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(intrinsic, (DMatrixRMaj)null);

		int numViews = list_camera_to_world.size();

		// render pixel coordinates of all points
		for (int viewCnt = 0; viewCnt < numViews; viewCnt++) {
			View v = new View();
			v.id = "" + viewCnt;

			Se3_F64 camera_to_world = list_camera_to_world.get(viewCnt);
			camera_to_world.invert(v.world_to_view);

			v.camera = PerspectiveOps.createCameraMatrix(v.world_to_view.R, v.world_to_view.T, K, null);

			// Project the features into the camera and see what's visible
			WorldToCameraToPixel w2p = new WorldToCameraToPixel();
			w2p.configure(intrinsic, v.world_to_view);
			Point2D_F64 pixel = new Point2D_F64();
			for (Feature f : points) {
				if (!w2p.transform(f.world, pixel))
					continue;

				// Adjust pixel coordinates if intrinsics has principle point at (0,0) instead of inside the image
				double xx = pixel.x + (intrinsic.cx == 0.0 ? intrinsic.width/2 : 0.0);
				double yy = pixel.y + (intrinsic.cy == 0.0 ? intrinsic.height/2 : 0.0);

				if (xx < 0 || yy < 0 || xx >= intrinsic.width || yy >= intrinsic.height)
					continue;

				Observation o = new Observation();
				o.feature = f;
				o.pixel.setTo(pixel);
				v.observations.add(o);
			}
			BoofMiscOps.checkTrue(v.observations.size() > 0);

//			System.out.println("view="+viewCnt+" obs.size="+v.observations.size());
			// Randomize the order of the observations
			Collections.shuffle(v.observations, rand);

//			for (int i = 0; i < v.observations.size(); i++) {
//				for (int j = i+1; j < v.observations.size(); j++) {
//					if (v.observations.get(i).feature == v.observations.get(j).feature ) {
//						throw new RuntimeException("Observing the same feature more than once!");
//					}
//				}
//			}

			views.add(v);
		}

		// See which views are connected. Require them to have a lot of views in common and be within the minimum
		// connected
		for (int idx0 = 0; idx0 < views.size(); idx0++) {
			View a = views.get(idx0);
			for (int offset = 1; offset <= numViewConnect; offset++) {
				int idx1 = idx0 + offset;
				// when wrapping be careful to not connect to the same node twice
				if (idx1 >= views.size()) {
					if (!loop)
						continue;
					idx1 %= views.size();
					if (idx1 + numViewConnect >= idx0)
						continue;
				}
				View b = views.get(idx1);
				if (a.fractionOverlap(b) < 0.5) {
//					System.out.println("REJECT "+idx0+" <-> "+idx1+" Fraction Overlap: "+a.fractionOverlap(b));
					continue;
				}
//				System.out.println(idx0+" <-> "+idx1+" Fraction Overlap: "+a.fractionOverlap(b));
				a.connected.add(b);
				b.connected.add(a);
			}
		}

		return this;
	}

	/**
	 * Create the camera database. There's only one camera for all the views
	 */
	public LookUpCameraInfo createLookUpCams() {
		var dbCams = new LookUpCameraInfo();
		for (int i = 0; i < views.size(); i++) {
			dbCams.addView(views.get(i).id, 2);
			// note everything points to camera 2
			// This is done to make sure the camera ID is actually used correctly and not using the default of 0
		}
		// Add multiple cameras, but only camera 2 has the correct parameters
		for (int i = 0; i < 5; i++) {
			double f = 2000*i*20;
			dbCams.listCalibration.grow().fsetK(f, f, 0, 610, 610, 1000, 1000);
		}
		dbCams.listCalibration.get(2).setTo(intrinsic);

		return dbCams;
	}

	public PairwiseImageGraph createPairwise() {
		var graph = new PairwiseImageGraph();
		// Create all the views in the graph
		views.forEach(v -> graph.createNode(v.id));

		for (int viewCnt = 0; viewCnt < views.size(); viewCnt++) {
			View v = views.get(viewCnt);
			PairwiseImageGraph.View pv = graph.nodes.get(viewCnt);

			pv.totalObservations = v.observations.size();

			for (View b : v.connected) {
				int indexViewB = views.indexOf(b);
				if (indexViewB < viewCnt)
					continue;
				PairwiseImageGraph.View pb = graph.nodes.get(indexViewB);
				PairwiseImageGraph.Motion m = graph.connect(pv, pb);
				m.is3D = true;

				boolean swap = rand.nextBoolean();

				m.src = swap ? pb : pv;
				m.dst = swap ? pv : pb;

				List<AssociatedIndex> shared = v.findShared(b);

				m.score3D = 3.0;

				Collections.shuffle(shared, rand);
				int minShared = (int)(0.85*shared.size());
				int numInliers = minShared + rand.nextInt(shared.size() - minShared);
				for (int i = 0; i < numInliers; i++) {
					AssociatedIndex a = shared.get(i);

					if (swap)
						m.inliers.grow().setTo(a.dst, a.src);
					else
						m.inliers.grow().setTo(a);
				}
			}
		}
		return graph;
	}

	/**
	 * Create a working graph filled with metric and projective ground truth
	 */
	public SceneWorkingGraph createWorkingGraph( PairwiseImageGraph pairwise ) {
		var working = new SceneWorkingGraph();

		SceneWorkingGraph.Camera c = working.addCamera(defaultCameraID);
		c.prior.setTo(intrinsic);
		BundleAdjustmentOps.convert(intrinsic, c.intrinsic);

		pairwise.nodes.forIdx(( i, v ) -> working.addView(v, c));

		BoofMiscOps.forIdx(working.listViews, ( i, v ) -> v.projective.setTo(views.get(i).camera));
		BoofMiscOps.forIdx(working.listViews, ( i, v ) -> v.world_to_view.setTo(views.get(i).world_to_view));
		BoofMiscOps.forIdx(working.listViews, ( i, v ) -> v.index = i);
		BoofMiscOps.forIdx(working.listViews, ( i, v ) -> v.cameraIdx = c.localIndex);
		return working;
	}

	/**
	 * Adds inlier info to the specified view
	 */
	public void addInlierInfo( PairwiseImageGraph pairwise,
							   SceneWorkingGraph.View wv, int... connected ) {
		var commonCount = new DogArray_I32();
		commonCount.resize(points.size(), 0);

		// Find features visible in every view
		views.get(wv.pview.index).observations.forEach(o -> commonCount.data[o.feature.featureIdx]++);
		for (int viewIdx : connected) {
			BoofMiscOps.checkTrue(viewIdx != wv.pview.index);
			views.get(viewIdx).observations.forEach(o -> commonCount.data[o.feature.featureIdx]++);
		}

		// Create the info now
		SceneWorkingGraph.InlierInfo info = wv.inliers.grow();
		info.views.add(wv.pview);
		for (int i = 0; i < connected.length; i++) {
			info.views.add(pairwise.nodes.get(connected[i]));
		}
		info.observations.resize(info.views.size);
		info.scoreGeometric = 10; // give it a positive score so it isn't ignored.

		// Count the number of features seen by all views
		for (int featureID = 0; featureID < commonCount.size; featureID++) {
			// See if this is a common feature
			if (commonCount.data[featureID] != connected.length + 1)
				continue;

			// Go through each view and determine which observation belongs to this feature
			for (int inlierViewIdx = 0; inlierViewIdx < info.views.size; inlierViewIdx++) {
				PairwiseImageGraph.View pview = info.views.get(inlierViewIdx);
				View v = views.get(pview.index);

				// brute force search but it works. Could change to O(N) later
				boolean found = false;
				for (int obsIdx = 0; obsIdx < v.observations.size(); obsIdx++) {
					if (v.observations.get(obsIdx).feature.featureIdx != featureID) {
						continue;
					}

					found = true;
					info.observations.get(inlierViewIdx).add(obsIdx);
					break;
				}
				BoofMiscOps.checkTrue(found, "Inlier not in all views?!");
			}
		}

		BoofMiscOps.checkTrue(info.observations.get(0).size > 0);
	}

	@Override
	public List<String> getImageIDs() {
		List<String> ids = new ArrayList<>();
		views.forEach(o -> ids.add(o.id));
		return ids;
	}

	@Override
	public void findSimilar( String target, BoofLambdas.Filter<String> filter, List<String> similar ) {
		this.queryID = target;
		View view = getView(target);

		similar.clear();
		view.connected.forEach(v -> similar.add(v.id));
	}

	@Override
	public void lookupPixelFeats( String target, DogArray<Point2D_F64> features ) {
		View view = getView(target);

		features.reset();
		view.observations.forEach(o -> features.grow().setTo(o.pixel));
	}

	private View getView( String target ) {
		for (int i = 0; i < views.size(); i++) {
			if (views.get(i).id.equals(target))
				return views.get(i);
		}
		throw new RuntimeException("Did not find view id=" + target);
	}

	@Override
	public boolean lookupAssociated( String identB, DogArray<AssociatedIndex> pairs ) {
		View viewA = getView(queryID);
		View viewB = getView(identB);

		if (!viewA.connected.contains(viewB))
			return false;

		pairs.reset();
		List<AssociatedIndex> shared = viewA.findShared(viewB);
		shared.forEach(a -> pairs.grow().setTo(a));

		return true;
	}

	/**
	 * Create a set of 3 observations of the same feature from any set of 3 views
	 *
	 * @param viewIdx which views to use
	 * @param triples (output) pixel observations
	 * @param featureIdx (output)which features were in common
	 */
	public void createTripleObs( int[] viewIdx, DogArray<AssociatedTriple> triples, DogArray_I32 featureIdx ) {
		BoofMiscOps.checkTrue(viewIdx.length == 3);

		triples.reset();
		featureIdx.reset();

		View view0 = views.get(viewIdx[0]);
		AssociatedTriple a = new AssociatedTriple();
		for (int obsI = 0; obsI < view0.observations.size(); obsI++) {
			Observation o = view0.observations.get(obsI);
			a.set(0, o.pixel.x, o.pixel.y);
			boolean matched = true;
			for (int i = 1; i < 3; i++) {
				View viewI = views.get(viewIdx[i]);
				int obsIdx = viewI.findIndex(o.feature);
				if (obsIdx < 0) {
					matched = false;
					break;
				}
				Point2D_F64 pixel = viewI.observations.get(obsIdx).pixel;
				a.set(i, pixel.x, pixel.y);
			}
			if (!matched) {
				continue;
			}
			triples.grow().setTo(a);
			featureIdx.add(points.indexOf(o.feature));
		}
	}

	/**
	 * Returns the feature index given a view and the observation in the view
	 */
	public int observationToFeatureIdx( int viewIdx, int observationIdx ) {
		View v = views.get(viewIdx);
		Feature f = v.observations.get(observationIdx).feature;
		return f.featureIdx;
	}

	public Observation featureToObservation( int viewIdx, int featureIdx ) {
		View v = views.get(viewIdx);
		Feature f = points.get(featureIdx);
		for (int i = 0; i < v.observations.size(); i++) {
			if (v.observations.get(i).feature == f)
				return v.observations.get(i);
		}
		return null;
	}

	public static class Feature {
		public int featureIdx;
		public Point3D_F64 world = new Point3D_F64();
	}

	public static class Observation {
		public Feature feature;
		public Point2D_F64 pixel = new Point2D_F64();
	}

	public static class View {
		public String id;
		// List of all observation
		public List<Observation> observations = new ArrayList<>();
		// Transform from world to this view
		public Se3_F64 world_to_view = new Se3_F64();
		// camera matrix. used in projective transform
		public DMatrixRMaj camera = new DMatrixRMaj(3, 4);

		public List<View> connected = new ArrayList<>();

		public int findIndex( Feature f ) {
			for (int i = 0; i < observations.size(); i++) {
				Observation o = observations.get(i);
				if (o.feature == f) {
					return i;
				}
			}
			return -1;
		}

		public List<AssociatedIndex> findShared( View v ) {
			List<AssociatedIndex> shared = new ArrayList<>();
			for (int indexA = 0; indexA < observations.size(); indexA++) {
				int indexB = v.findIndex(observations.get(indexA).feature);
				if (indexB < 0)
					continue;

				shared.add(new AssociatedIndex(indexA, indexB));
			}
			return shared;
		}

		public double fractionOverlap( View v ) {
			int total = findShared(v).size();
			return Math.min(total/(double)v.observations.size(), total/(double)observations.size());
		}
	}
}

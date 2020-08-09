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

import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.WorldToCameraToPixel;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.image.ImageDimension;
import boofcv.testing.BoofTesting;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.geometry.UtilPoint3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.so.Rodrigues_F64;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;
import org.ejml.data.DMatrixRMaj;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static boofcv.misc.BoofMiscOps.assertBoof;

/**
 * Simulation where not all the features will be visible at the same time
 *
 * @author Peter Abeles
 */
class MockLookupSimilarImagesRealistic implements LookupSimilarImages {
	public CameraPinhole intrinsic = new CameraPinhole(400,410,0,420,420,800,800);
	public int numFeatures = 100;
	public Random rand = BoofTesting.createRandom(3);
	public boolean loop = true;

	List<Feature> points = new ArrayList<>();
	List<View> views = new ArrayList<>();

	public MockLookupSimilarImagesRealistic(){}

	public MockLookupSimilarImagesRealistic setIntrinsic(CameraPinhole intrinsic ) {
		this.intrinsic = intrinsic;
		return this;
	}

	public MockLookupSimilarImagesRealistic setFeatures(int numFeatures ) {
		this.numFeatures = numFeatures;
		return this;
	}

	public MockLookupSimilarImagesRealistic setSeed(long seed ) {
		rand = BoofTesting.createRandom(seed);
		return this;
	}

	public MockLookupSimilarImagesRealistic setLoop(boolean loop ) {
		this.loop = loop;
		return this;
	}

	public MockLookupSimilarImagesRealistic pathLine(int numViews , double stepLength, double pathLength, int numViewConnect) {
		double r = 0.5;

		for( Point3D_F64 X : UtilPoint3D_F64.random(new Point3D_F64(0, 0, 0), -r, pathLength+r,-r,r,-r,r, numFeatures, rand)) {
			Feature f = new Feature();
			f.world.set(X);
			points.add(f);
		}

		List<Se3_F64> list_camera_to_world = new ArrayList<>();

		for (int viewCnt = 0; viewCnt < numViews; viewCnt++) {
			Se3_F64 camera_to_world = new Se3_F64();

			// Move the camera down the x-axis and push back enough to see most of the points
			camera_to_world.T.x = stepLength*viewCnt;
			camera_to_world.T.y = rand.nextGaussian() * 0.05;
			camera_to_world.T.z = rand.nextGaussian() * 0.05 - 2 * r;

			// Point camera towards the cloud of points
			double noiseRotX = rand.nextGaussian() * 0.01;
			double noiseRotY = rand.nextGaussian() * 0.01;
			double noiseRotZ = rand.nextGaussian() * 0.01;
			ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, noiseRotX, noiseRotY, noiseRotZ, camera_to_world.R);
			list_camera_to_world.add( camera_to_world );
		}

		generate(list_camera_to_world,numViewConnect);
		return this;
	}

	public MockLookupSimilarImagesRealistic pathCircle(int numViews , int numViewConnect) {
		// Radius of the cameras circling the origin
		double pathRadius = 2;

		for( Point3D_F64 X : UtilPoint3D_F64.random(new Point3D_F64(0, 0, 0), -0.5, 0.5, numFeatures, rand)) {
			Feature f = new Feature();
			f.world.set(X);
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
			ConvertRotation3D_F64.rodriguesToMatrix(new Rodrigues_F64(yaw+Math.PI/2,0,-1,0),camera_to_world.R);

			list_camera_to_world.add( camera_to_world );
		}

		generate(list_camera_to_world,numViewConnect);
		return this;
	}

	/**
	 * Renders the scene using only ready generated points and image coordinates
	 */
	public MockLookupSimilarImagesRealistic generate(List<Se3_F64> list_camera_to_world, int numViewConnect) {
		DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(intrinsic,(DMatrixRMaj)null);

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
				if (pixel.x < 0 || pixel.y < 0 || pixel.x > intrinsic.width - 1 || pixel.y > intrinsic.height - 1)
					continue;
				Observation o = new Observation();
				o.feature = f;
				o.pixel.set(pixel);
				v.observations.add(o);
			}
			assertBoof(v.observations.size()>0);

//			System.out.println("view="+viewCnt+" obs.size="+v.observations.size());
			// Randomize the order of the observations
			Collections.shuffle(v.observations,rand);
			views.add(v);
		}

		// See which views are connected. Require them to have a lot of views in common and be within the minimum
		// connected
		for (int idx0 = 0; idx0 < views.size(); idx0++) {
			View a = views.get(idx0);
			for (int offset = 1; offset <= numViewConnect; offset++) {
				int idx1 = idx0+offset;
				// when wrapping be careful to not connect to the same node twice
				if( idx1 >= views.size() ) {
					if( !loop )
						continue;
					idx1 %= views.size();
					if( idx1+numViewConnect >= idx0 )
						continue;
				}
				View b = views.get(idx1);
				if( a.fractionOverlap(b) < 0.5 ) {
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

	public PairwiseImageGraph2 createPairwise() {
		var graph = new PairwiseImageGraph2();
		// Create all the views in the graph
		views.forEach(v->graph.createNode(v.id));

		for (int viewCnt = 0; viewCnt < views.size(); viewCnt++) {
			View v = views.get(viewCnt);
			PairwiseImageGraph2.View pv = graph.nodes.get(viewCnt);

			pv.totalObservations = v.observations.size();

			for( View b : v.connected ) {
				int indexViewB = views.indexOf(b);
				if( indexViewB < viewCnt )
					continue;
				PairwiseImageGraph2.View pb = graph.nodes.get(indexViewB);
				PairwiseImageGraph2.Motion m = graph.connect(pv,pb);
				m.is3D = true;

				boolean swap = rand.nextBoolean();

				m.src = swap ? pb : pv;
				m.dst = swap ? pv : pb;

				List<AssociatedIndex> shared = v.findShared(b);

				m.countF = shared.size();
				m.countH = m.countF / 3;

				Collections.shuffle(shared,rand);
				int minShared = (int)(0.85*shared.size());
				int numInliers = minShared+rand.nextInt(shared.size()-minShared);
				for (int i = 0; i < numInliers; i++) {
					AssociatedIndex a = shared.get(i);

					if( swap )
						m.inliers.grow().set(a.dst,a.src);
					else
						m.inliers.grow().set(a);
				}
			}
		}
		return graph;
	}

	/**
	 * Create a working graph filled with metric and projective ground truth
	 */
	public SceneWorkingGraph createWorkingGraph( PairwiseImageGraph2 pairwise ) {
		var working = new SceneWorkingGraph();
		pairwise.nodes.forEach((i,v)->working.addView(v));

		working.viewList.forEach(v->v.pinhole.set(intrinsic));
		BoofMiscOps.forIdx(working.viewList,(i,v)->v.projective.set(views.get(i).camera));
		BoofMiscOps.forIdx(working.viewList,(i,v)->v.world_to_view.set(views.get(i).world_to_view));
		BoofMiscOps.forIdx(working.viewList,(i,v)->v.index=i);

		return working;
	}

	@Override
	public List<String> getImageIDs() {
		List<String> ids = new ArrayList<>();
		views.forEach(o->ids.add(o.id));
		return ids;
	}

	@Override
	public void findSimilar(String target, List<String> similar) {
		View view = getView(target);

		similar.clear();
		view.connected.forEach(v->similar.add(v.id));
	}

	@Override
	public void lookupPixelFeats(String target, FastQueue<Point2D_F64> features) {
		View view = getView(target);

		features.reset();
		view.observations.forEach(o->features.grow().setTo(o.pixel));
	}

	private View getView(String target) {
		for (int i = 0; i < views.size(); i++) {
			if( views.get(i).id.equals(target))
				return views.get(i);
		}
		throw new RuntimeException("Did not find view id="+target);
	}

	@Override
	public boolean lookupMatches(String identA, String identB, FastQueue<AssociatedIndex> pairs)
	{
		View viewA = getView(identA);
		View viewB = getView(identB);

		if( !viewA.connected.contains(viewB) )
			return false;

		pairs.reset();
		List<AssociatedIndex> shared = viewA.findShared(viewB);
		shared.forEach(a->pairs.grow().set(a));

		return true;
	}

	@Override
	public void lookupShape(String target, ImageDimension shape) {
		shape.set(intrinsic.width,intrinsic.height);
	}

	/**
	 * Create a set of 3 observations of the same feature from any set of 3 views
	 * @param viewIdx which views to use
	 * @param triples (output) pixel observations
	 * @param featureIdx (output)which features were in common
	 */
	public void createTripleObs(int[] viewIdx, FastQueue<AssociatedTriple> triples, GrowQueue_I32 featureIdx )
	{
		BoofMiscOps.assertBoof(viewIdx.length==3);

		triples.reset();
		featureIdx.reset();

		View view0 = views.get(viewIdx[0]);
		AssociatedTriple a = new AssociatedTriple();
		for (int obsI = 0; obsI < view0.observations.size(); obsI++) {
			Observation o = view0.observations.get(obsI);
			a.set(0,o.pixel.x,o.pixel.y);
			boolean matched = true;
			for (int i = 1; i < 3; i++) {
				View viewI = views.get(viewIdx[i]);
				int obsIdx = viewI.findIndex(o.feature);
				if( obsIdx < 0 ) {
					matched = false;
					break;
				}
				Point2D_F64 pixel = viewI.observations.get(obsIdx).pixel;
				a.set(i, pixel.x, pixel.y);
			}
			if( !matched ) {
				continue;
			}
			triples.grow().set(a);
			featureIdx.add( points.indexOf(o.feature) );
		}
	}

	/**
	 * Returns the feature index given a view and the observation in the view
	 */
	public int observationToFeatureIdx(int viewIdx, int observationIdx) {
		View v = views.get(viewIdx);
		Feature f = v.observations.get(observationIdx).feature;
		return points.indexOf(f);
	}

	public Observation featureToObservation(int viewIdx , int featureIdx ) {
		View v = views.get(viewIdx);
		Feature f = points.get(featureIdx);
		for (int i = 0; i < v.observations.size(); i++) {
			if( v.observations.get(i).feature == f )
				return v.observations.get(i);
		}
		return null;
	}

	static class Feature {
		public Point3D_F64 world = new Point3D_F64();
	}

	static class Observation {
		public Feature feature;
		public Point2D_F64 pixel = new Point2D_F64();
	}

	public static class View {
		public String id;
		// List of all observation
		public List<Observation> observations = new ArrayList<>();
		// Tranform from world to this view
		public Se3_F64 world_to_view = new Se3_F64();
		// camera matrix. used in projective transform
		public DMatrixRMaj camera = new DMatrixRMaj(3,4);

		public List<View> connected = new ArrayList<>();

		public int findIndex( Feature f ) {
			for (int i = 0; i < observations.size(); i++) {
				Observation o = observations.get(i);
				if( o.feature == f ) {
					return i;
				}
			}
			return -1;
		}

		public List<AssociatedIndex> findShared( View v ) {
			List<AssociatedIndex> shared = new ArrayList<>();
			for (int indexA = 0; indexA < observations.size(); indexA++) {
				int indexB = v.findIndex(observations.get(indexA).feature);
				if( indexB < 0 )
					continue;

				shared.add( new AssociatedIndex(indexA,indexB) );
			}
			return shared;
		}

		public double fractionOverlap( View v ) {
			int total = findShared(v).size();
			return Math.min(total/(double)v.observations.size(),total/(double)observations.size());
		}
	}
}
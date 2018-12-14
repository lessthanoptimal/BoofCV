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

package boofcv.abst.geo.bundle;

import boofcv.alg.nn.KdTreePoint3D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import org.ddogleg.nn.FactoryNearestNeighbor;
import org.ddogleg.nn.NearestNeighbor;
import org.ddogleg.nn.NnData;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * makes it easy to removing elements from the scene's structure. Different criteria can be specified for each
 * type of element you wish to remove.
 *
 * @author Peter Abeles
 */
public class PruneStructureFromSceneMetric {

	SceneStructureMetric structure;
	SceneObservations observations;

	public PruneStructureFromSceneMetric(SceneStructureMetric structure,
										 SceneObservations observations)
	{
		this.structure = structure;
		this.observations = observations;
	}

	/**
	 * Computes reprojection error for all features. Sorts the resulting residuals by magnitude.
	 * Prunes observations which have the largest errors first. After calling this function you should
	 * call {@link #prunePoints(int)} and {@link #pruneViews(int)} to ensure the scene is still valid.
	 *
	 * @param inlierFraction Fraction of observations to keep. 0 to 1. 1 = no change. 0 = everything is pruned.
	 */
	public void pruneObservationsByErrorRank( double inlierFraction ) {

		Point2D_F64 observation = new Point2D_F64();
		Point2D_F64 predicted = new Point2D_F64();
		Point3D_F64 X = new Point3D_F64();

		// Create a list of observation errors
		List<Errors> errors = new ArrayList<>();
		for (int viewIndex = 0; viewIndex < observations.views.length; viewIndex++) {
			SceneObservations.View v = observations.views[viewIndex];
			SceneStructureMetric.View view = structure.views[viewIndex];

			for (int pointIndex = 0; pointIndex < v.point.size; pointIndex++) {
				int pointID = v.point.data[pointIndex];
				SceneStructureMetric.Point f = structure.points[pointID];

				// Get feature location in world
				f.get(X);
				// Get observation in image pixels
				v.get(pointIndex, observation);

				// World to View
				view.worldToView.transform(X, X);

				// predicted pixel
				SceneStructureMetric.Camera camera = structure.cameras[view.camera];
				camera.model.project(X.x, X.y, X.z, predicted);

				Errors e = new Errors();
				e.view = viewIndex;
				e.pointIndexInView = pointIndex;
				e.error = predicted.distance2(observation);
				errors.add(e);
			}
		}

		errors.sort(Comparator.comparingDouble(a -> a.error));

		// Mark observations which are to be removed. Can't remove yet since the indexes will change
		for (int i = (int) (errors.size() * inlierFraction); i < errors.size(); i++) {
			Errors e = errors.get(i);

			SceneObservations.View v = observations.views[e.view];
			v.set(e.pointIndexInView, Float.NaN, Float.NaN);
		}

		// Remove all marked features
		removeMarkedObservations();
	}

	/**
	 * Removes observations which have been marked with NaN
	 */
	private void removeMarkedObservations() {
		Point2D_F64 observation = new Point2D_F64();

		for (int viewIndex = 0; viewIndex < observations.views.length; viewIndex++) {
//			System.out.println("ViewIndex="+viewIndex);
			SceneObservations.View v = observations.views[viewIndex];
			for(int pointIndex = v.point.size-1; pointIndex >= 0; pointIndex-- ) {
				int pointID = v.getPointId(pointIndex);
				SceneStructureMetric.Point f = structure.points[pointID];
//				System.out.println("   pointIndex="+pointIndex+" pointID="+pointID+" hash="+f.hashCode());
				v.get(pointIndex, observation);

				if( !Double.isNaN(observation.x))
					continue;

				if( !f.views.contains(viewIndex))
					throw new RuntimeException("BUG!");

				// Tell the feature it is no longer visible in this view
				f.removeView(viewIndex);
				// Remove the observation of this feature from the view
				v.remove(pointIndex);
			}
		}
	}

	/**
	 * Check to see if a point is behind the camera which is viewing it. If it is remove that observation
	 * since it can't possibly be observed.
	 */
	public void pruneObservationsBehindCamera() {
		Point3D_F64 X = new Point3D_F64();

		for (int viewIndex = 0; viewIndex < observations.views.length; viewIndex++) {
			SceneObservations.View v = observations.views[viewIndex];
			SceneStructureMetric.View view = structure.views[viewIndex];

			for (int pointIndex = 0; pointIndex < v.point.size; pointIndex++) {
				SceneStructureMetric.Point f = structure.points[v.getPointId(pointIndex)];

				// Get feature location in world
				f.get(X);

				if( !f.views.contains(viewIndex))
					throw new RuntimeException("BUG!");

				// World to View
				view.worldToView.transform(X, X);

				// Is the feature behind this view and can't be seen?
				if( X.z <= 0 ) {
					v.set(pointIndex, Float.NaN, Float.NaN);
				}
			}
		}

		removeMarkedObservations();
	}

	/**
	 * Prunes Points/features with less than 'count' observations. Observations of the points are also removed.
	 *
	 * Call {@link #pruneViews(int)} to ensure that all views have points in view
	 *
	 * @param count Minimum number of observations
	 */
	public void prunePoints(int count ) {
		// Remove all observations of the Points which are going to be removed
		for (int viewIndex = observations.views.length-1; viewIndex >= 0; viewIndex--) {
			SceneObservations.View v = observations.views[viewIndex];

			for(int pointIndex = v.point.size-1; pointIndex >= 0; pointIndex-- ) {
				SceneStructureMetric.Point p = structure.points[v.getPointId(pointIndex)];

				if( p.views.size < count ) {
					v.remove(pointIndex);
				}
			}
		}

		// Create a look up table containing from old to new indexes for each point
		int oldToNew[] = new int[ structure.points.length ];
		Arrays.fill(oldToNew,-1); // crash is bug

		GrowQueue_I32 prune = new GrowQueue_I32(); // List of point ID's which are to be removed.
		for (int i = 0; i < structure.points.length; i++) {
			if( structure.points[i].views.size < count ) {
				prune.add(i);
			} else {
				oldToNew[i] = i-prune.size;
			}

		}
		pruneUpdatePointID(oldToNew, prune);
	}

	private void pruneUpdatePointID(int[] oldToNew, GrowQueue_I32 prune) {
		if( prune.size == 0)
			return;

		// Remove the points from the structure
		structure.removePoints(prune);

		// Update the references from observation to features
		for (int viewIndex = observations.views.length-1; viewIndex >= 0; viewIndex--) {
			SceneObservations.View v = observations.views[viewIndex];

			for(int featureIndex = v.point.size-1; featureIndex >= 0; featureIndex-- ) {
				v.point.data[featureIndex] = oldToNew[v.point.data[featureIndex]];
			}
		}
	}

	/**
	 * Prune a feature it has fewer than X neighbors within Y distance. Observations
	 * associated with this feature are also pruned.
	 *
	 * Call {@link #pruneViews(int)} to makes sure the graph is valid.
	 *
	 * @param neighbors Number of other features which need to be near by
	 * @param distance Maximum distance a point can be to be considered a feature
	 */
	public void prunePoints(int neighbors , double distance ) {

		// Use a nearest neighbor search to find near by points
		Point3D_F64 worldX = new Point3D_F64();
		List<Point3D_F64> cloud = new ArrayList<>();
		for (int i = 0; i < structure.points.length; i++) {
			SceneStructureMetric.Point structureP = structure.points[i];
			structureP.get(worldX);
			cloud.add(worldX.copy());
		}

		NearestNeighbor<Point3D_F64> nn = FactoryNearestNeighbor.kdtree(new KdTreePoint3D_F64());
		nn.setPoints(cloud,false);
		FastQueue<NnData<Point3D_F64>> resultsNN = new FastQueue(NnData.class,true);

		// Create a look up table containing from old to new indexes for each point
		int oldToNew[] = new int[ structure.points.length ];
		Arrays.fill(oldToNew,-1); // crash is bug
		// List of point ID's which are to be removed.
		GrowQueue_I32 prunePointID = new GrowQueue_I32();

		// identify points which need to be pruned
		for (int pointId = 0; pointId < structure.points.length; pointId++) {
			SceneStructureMetric.Point structureP = structure.points[pointId];
			structureP.get(worldX);

			// distance is squared
			nn.findNearest(cloud.get(pointId),distance*distance,neighbors+1,resultsNN);

			// Don't prune if it has enough neighbors. Remember that it will always find itself.
			if( resultsNN.size() > neighbors ) {
				oldToNew[pointId] = pointId-prunePointID.size;
				continue;
			}

			prunePointID.add(pointId);

			// Remove observations of this point
			for (int viewIdx = 0; viewIdx < structureP.views.size; viewIdx++) {
				SceneObservations.View v = observations.getView(structureP.views.data[viewIdx]);

				int pointIdx = v.point.indexOf(pointId);
				if( pointIdx < 0 )
					throw new RuntimeException("Bad structure. Point not found in view's observation " +
							"which was in its structure");
				v.remove(pointIdx);
			}
		}

		pruneUpdatePointID(oldToNew, prunePointID);
	}

	/**
	 * Removes views with less than 'count' features visible. Observations of features in removed views are also
	 * removed.
	 *
	 * @param count Prune if it has this number of views or less
	 */
	public void pruneViews( int count ) {

		List<SceneStructureMetric.View> remainingS = new ArrayList<>();
		List<SceneObservations.View> remainingO = new ArrayList<>();

		for (int viewId = 0; viewId < structure.views.length; viewId++) {
			SceneObservations.View view = observations.views[viewId];
			// See if has enough observations to not prune
			if( view.size() > count ) {
				remainingS.add(structure.views[viewId]);
				remainingO.add(view);
				continue;
			}

			// Go through list of points and remove this view from them
			for (int pointIdx = 0; pointIdx < view.point.size; pointIdx++) {
				int pointId = view.getPointId(pointIdx);

				int viewIdx = structure.points[pointId].views.indexOf(viewId);
				if( viewIdx < 0 )
					throw new RuntimeException("Bug in structure. view has point but point doesn't have view");
				structure.points[pointId].views.remove(viewIdx);
			}
		}

		// Create new arrays with the views that were not pruned
		structure.views = new SceneStructureMetric.View[remainingS.size()];
		observations.views = new SceneObservations.View[remainingO.size()];

		for (int i = 0; i < structure.views.length; i++) {
			structure.views[i] = remainingS.get(i);
			observations.views[i] = remainingO.get(i);
		}
	}

	/**
	 * Prunes cameras that are not referenced by any views.
	 */
	public void pruneUnusedCameras() {
		// Count how many views are used by each camera
		int histogram[] = new int[structure.cameras.length];

		for (int i = 0; i < structure.views.length; i++) {
			histogram[structure.views[i].camera]++;
		}

		// See which cameras need to be removed and create a look up table from old to new camera IDs
		int oldToNew[] = new int[structure.cameras.length];
		List<SceneStructureMetric.Camera> remaining = new ArrayList<>();
		for (int i = 0; i < structure.cameras.length; i++) {
			if( histogram[i] > 0 ) {
				oldToNew[i] = remaining.size();
				remaining.add(structure.cameras[i]);
			}
		}

		// Create the new camera array without the unused cameras
		structure.cameras = new SceneStructureMetric.Camera[remaining.size()];
		for (int i = 0; i < remaining.size(); i++) {
			structure.cameras[i] = remaining.get(i);
		}

		// Update the references to the cameras
		for (int i = 0; i < structure.views.length; i++) {
			SceneStructureMetric.View v = structure.views[i];
			v.camera = oldToNew[v.camera];
		}
	}

	private static class Errors {
		int view;
		int pointIndexInView;
		double error;
	}
}

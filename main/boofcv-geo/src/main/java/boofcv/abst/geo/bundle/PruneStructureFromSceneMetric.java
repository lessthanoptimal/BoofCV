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

package boofcv.abst.geo.bundle;

import georegression.helper.KdTreePoint3D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.nn.FactoryNearestNeighbor;
import org.ddogleg.nn.NearestNeighbor;
import org.ddogleg.nn.NnData;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;

import java.util.*;

/**
 * makes it easy to removing elements from the scene's structure. Different criteria can be specified for each
 * type of element you wish to remove.
 *
 * @author Peter Abeles
 */
public class PruneStructureFromSceneMetric {

	SceneStructureMetric structure;
	SceneObservations observations;

	public PruneStructureFromSceneMetric( SceneStructureMetric structure,
										  SceneObservations observations ) {
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

		var observation = new Point2D_F64();
		var predicted = new Point2D_F64();
		var X = new Point3D_F64();
		var world_to_view = new Se3_F64();
		var tmp = new Se3_F64();

		// Create a list of observation errors
		List<Errors> errors = new ArrayList<>();
		for (int viewIndex = 0; viewIndex < observations.views.size; viewIndex++) {
			SceneObservations.View v = observations.views.data[viewIndex];
			SceneStructureMetric.View view = structure.views.data[viewIndex];
			structure.getWorldToView(view, world_to_view, tmp);

			for (int pointIndex = 0; pointIndex < v.point.size; pointIndex++) {
				int pointID = v.point.data[pointIndex];
				SceneStructureCommon.Point f = structure.points.data[pointID];

				// Get feature location in world
				f.get(X);
				// Get observation in image pixels
				v.getPixel(pointIndex, observation);

				// Find the point in this view
				world_to_view.transform(X, X);

				// predicted pixel
				SceneStructureCommon.Camera camera = structure.cameras.data[view.camera];
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
		for (int i = (int)(errors.size()*inlierFraction); i < errors.size(); i++) {
			Errors e = errors.get(i);

			SceneObservations.View v = observations.views.get(e.view);
			v.setPixel(e.pointIndexInView, Float.NaN, Float.NaN);
		}

		// Remove all marked features
		removeMarkedObservations();
	}

	/**
	 * Removes observations which have been marked with NaN
	 */
	private void removeMarkedObservations() {
		Point2D_F64 observation = new Point2D_F64();

		for (int viewIndex = 0; viewIndex < observations.views.size; viewIndex++) {
//			System.out.println("ViewIndex="+viewIndex);
			SceneObservations.View v = observations.views.data[viewIndex];
			for (int pointIndex = v.point.size - 1; pointIndex >= 0; pointIndex--) {
				int pointID = v.getPointId(pointIndex);
				SceneStructureCommon.Point f = structure.points.data[pointID];
//				System.out.println("   pointIndex="+pointIndex+" pointID="+pointID+" hash="+f.hashCode());
				v.getPixel(pointIndex, observation);

				if (!Double.isNaN(observation.x))
					continue;

				if (!f.views.contains(viewIndex))
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
		var X = new Point3D_F64();
		var world_to_view = new Se3_F64();
		var tmp = new Se3_F64();

		for (int viewIndex = 0; viewIndex < observations.views.size; viewIndex++) {
			SceneObservations.View v = observations.views.get(viewIndex);
			SceneStructureMetric.View view = structure.views.get(viewIndex);
			structure.getWorldToView(view, world_to_view, tmp);

			for (int pointIndex = 0; pointIndex < v.point.size; pointIndex++) {
				SceneStructureCommon.Point f = structure.points.get(v.getPointId(pointIndex));

				// Get feature location in world
				f.get(X);

				if (!f.views.contains(viewIndex))
					throw new RuntimeException("BUG!");

				// World to View
				world_to_view.transform(X, X);

				// Is the feature behind this view and can't be seen?
				if (X.z <= 0) {
					v.setPixel(pointIndex, Float.NaN, Float.NaN);
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
	public void prunePoints( int count ) {
		// Remove all observations of the Points which are going to be removed
		for (int viewIndex = observations.views.size - 1; viewIndex >= 0; viewIndex--) {
			SceneObservations.View v = observations.views.data[viewIndex];

			for (int pointIndex = v.point.size - 1; pointIndex >= 0; pointIndex--) {
				SceneStructureCommon.Point p = structure.points.data[v.getPointId(pointIndex)];

				if (p.views.size < count) {
					v.remove(pointIndex);
				}
			}
		}

		// Create a look up table containing from old to new indexes for each point
		int[] oldToNew = new int[structure.points.size];
		Arrays.fill(oldToNew, -1); // crash is bug

		DogArray_I32 prune = new DogArray_I32(); // List of point ID's which are to be removed.
		for (int i = 0; i < structure.points.size; i++) {
			if (structure.points.data[i].views.size < count) {
				prune.add(i);
			} else {
				oldToNew[i] = i - prune.size;
			}
		}
		pruneUpdatePointID(oldToNew, prune);
	}

	private void pruneUpdatePointID( int[] oldToNew, DogArray_I32 prune ) {
		if (prune.size == 0)
			return;

		// Remove the points from the structure
		structure.removePoints(prune);

		// Update the references from observation to features
		for (int viewIndex = observations.views.size - 1; viewIndex >= 0; viewIndex--) {
			SceneObservations.View v = observations.views.data[viewIndex];

			for (int featureIndex = v.point.size - 1; featureIndex >= 0; featureIndex--) {
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
	public void prunePoints( int neighbors, double distance ) {

		// Use a nearest neighbor search to find near by points
		Point3D_F64 worldX = new Point3D_F64();
		List<Point3D_F64> cloud = new ArrayList<>();
		for (int i = 0; i < structure.points.size; i++) {
			SceneStructureCommon.Point structureP = structure.points.data[i];
			structureP.get(worldX);
			cloud.add(worldX.copy());
		}

		NearestNeighbor<Point3D_F64> nn = FactoryNearestNeighbor.kdtree(new KdTreePoint3D_F64());
		NearestNeighbor.Search<Point3D_F64> search = nn.createSearch();
		nn.setPoints(cloud, false);
		DogArray<NnData<Point3D_F64>> resultsNN = new DogArray<>(NnData::new);

		// Create a look up table containing from old to new indexes for each point
		int[] oldToNew = new int[structure.points.size];
		Arrays.fill(oldToNew, -1); // crash is bug
		// List of point ID's which are to be removed.
		DogArray_I32 prunePointID = new DogArray_I32();

		// identify points which need to be pruned
		for (int pointId = 0; pointId < structure.points.size; pointId++) {
			SceneStructureCommon.Point structureP = structure.points.data[pointId];
			structureP.get(worldX);

			// distance is squared
			search.findNearest(cloud.get(pointId), distance*distance, neighbors + 1, resultsNN);

			// Don't prune if it has enough neighbors. Remember that it will always find itself.
			if (resultsNN.size() > neighbors) {
				oldToNew[pointId] = pointId - prunePointID.size;
				continue;
			}

			prunePointID.add(pointId);

			// Remove observations of this point
			for (int viewIdx = 0; viewIdx < structureP.views.size; viewIdx++) {
				SceneObservations.View v = observations.getView(structureP.views.data[viewIdx]);

				int pointIdx = v.point.indexOf(pointId);
				if (pointIdx < 0)
					throw new RuntimeException("Bad structure. Point not found in view's observation " +
							"which was in its structure");
				v.remove(pointIdx);
			}
		}

		pruneUpdatePointID(oldToNew, prunePointID);
	}

	/**
	 * Removes views with less than or equal to 'count' features visible. Observations of features
	 * in removed views are also removed. Views which are parents (after children than can be removed have been
	 * removed) will remain since they are needed for the correct transform.
	 *
	 * @param count Prune if less than or equal to this many features
	 * @return true if the graph has been modified
	 */
	public boolean pruneViews( int count ) {
		DogArray_I32 removeIdx = new DogArray_I32();
		Set<SceneStructureMetric.View> parents = new HashSet<>();

		// Traverse in reverse order since parents always have an index less than the child. This way if an entire
		// chain of views needs to be removed they will be removed
		for (int viewId = structure.views.size - 1; viewId >= 0; viewId--) {
			SceneObservations.View oview = observations.views.data[viewId];
			SceneStructureMetric.View sview = structure.views.data[viewId];

			// See if has enough observations to not prune or is a parent and can't be pruned
			if (oview.size() > count || parents.contains(sview)) {
				// mark it's parent as a parent
				if (sview.parent != null)
					parents.add(sview.parent);
				continue;
			}
			removeIdx.add(viewId);

			// Go through list of points and remove this view from them
			for (int pointIdx = 0; pointIdx < oview.point.size; pointIdx++) {
				int pointId = oview.getPointId(pointIdx);

				int viewIdx = structure.points.data[pointId].views.indexOf(viewId);
				if (viewIdx < 0)
					throw new RuntimeException("Bug in structure. view has point but point doesn't have view");
				structure.points.data[pointId].views.remove(viewIdx);
			}
		}

		if (removeIdx.isEmpty())
			return false;

		// Remove the views
		structure.views.remove(removeIdx.data, 0, removeIdx.size, null);
		observations.views.remove(removeIdx.data, 0, removeIdx.size, null);

		return true;
	}

	/**
	 * Prunes cameras that are not referenced by any views.
	 *
	 * @return true if the graph has been modified
	 */
	public boolean pruneUnusedCameras() {
		// Count how many views are used by each camera
		int[] histogram = new int[structure.cameras.size];

		for (int i = 0; i < structure.views.size; i++) {
			histogram[structure.views.data[i].camera]++;
		}

		// See which cameras need to be removed and create a look up table from old to new camera IDs
		int[] oldToNew = new int[structure.cameras.size];
		var removeIdx = new DogArray_I32();
		for (int i = 0; i < structure.cameras.size; i++) {
			if (histogram[i] > 0) {
				oldToNew[i] = i - removeIdx.size();
			} else {
				removeIdx.add(i);
			}
		}

		if (removeIdx.isEmpty())
			return false;

		// Create the new camera array without the unused cameras
		structure.cameras.remove(removeIdx.data, 0, removeIdx.size, null);

		// Update the references to the cameras
		for (int i = 0; i < structure.views.size; i++) {
			SceneStructureMetric.View v = structure.views.data[i];
			v.camera = oldToNew[v.camera];
		}

		return true;
	}

	/**
	 * Prunes Motions that are not referenced by any views.
	 *
	 * @return true if the graph has been modified
	 */
	public boolean pruneUnusedMotions() {
		// Count how many views are used by each camera
		int[] histogram = new int[structure.motions.size];

		for (int i = 0; i < structure.views.size; i++) {
			histogram[structure.views.data[i].parent_to_view]++;
		}

		// See which cameras need to be removed and create a look up table from old to new camera IDs
		int[] oldToNew = new int[structure.motions.size];
		var removeIdx = new DogArray_I32();
		for (int i = 0; i < structure.motions.size; i++) {
			if (histogram[i] > 0) {
				oldToNew[i] = i - removeIdx.size();
			} else {
				removeIdx.add(i);
			}
		}

		if (removeIdx.isEmpty())
			return false;

		// Create the new camera array without the unused cameras
		structure.motions.remove(removeIdx.data, 0, removeIdx.size, null);

		// Update the references to the cameras
		for (int i = 0; i < structure.views.size; i++) {
			SceneStructureMetric.View v = structure.views.data[i];
			v.parent_to_view = oldToNew[v.parent_to_view];
		}

		return true;
	}

	private static class Errors {
		int view;
		int pointIndexInView;
		double error;
	}
}

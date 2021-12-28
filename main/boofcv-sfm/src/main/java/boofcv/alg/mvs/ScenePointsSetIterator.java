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

package boofcv.alg.mvs;

import boofcv.abst.geo.bundle.SceneStructureCommon;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.misc.IteratorReset;
import boofcv.struct.geo.PointIndex;
import georegression.struct.GeoTuple;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import org.ddogleg.struct.DogArray_I32;

/**
 * Given a {@link SceneStructureMetric scene}, this will iterate through points in that scene that are inside
 * of a provided array full of indexes. This handles 3D and homogenous points.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class ScenePointsSetIterator<T extends PointIndex<T, P>, P extends GeoTuple<P>> implements IteratorReset<T> {
	// Reference to the scene. Only it's points are used
	SceneStructureCommon scene;
	// Set if points in the scene
	DogArray_I32 indexes;
	// which element in 'indexes' is the current index
	int index;
	// Storage for the point. Returned by next
	T point;

	/**
	 * Constructor
	 *
	 * @param scene (Input) The scene which is to be iterated through
	 * @param indexes (Input) Indexes that specify which features in the scene it should be iterating through
	 * @param point (Input,Output) Storage for the point. Make sure you get 3D vs homogenous correct.
	 */
	public ScenePointsSetIterator( SceneStructureCommon scene, DogArray_I32 indexes, T point ) {
		this(point);
		initialize(scene, indexes);
	}

	public ScenePointsSetIterator( T point ) {this.point = point;}

	/**
	 * Re-initializes and can be used to change the scene and set of indexes
	 */
	public void initialize( SceneStructureCommon scene, DogArray_I32 indexes ) {
		if (scene.isHomogenous() != point.p instanceof Point4D_F64)
			throw new IllegalArgumentException("Scene point type does not match provided point type");

		this.scene = scene;
		this.indexes = indexes;
		this.index = 0;
	}

	@Override public void reset() {
		index = 0;
	}

	@Override public boolean hasNext() {
		return index < indexes.size;
	}

	/**
	 * Returns a copy of the next point in the scene. This is overwritten on the next call.
	 */
	@Override public T next() {
		// Get the index of the point in this scene
		int indexScene = indexes.get(index++);
		if (scene.isHomogenous())
			scene.points.get(indexScene).get((Point4D_F64)point.p);
		else
			scene.points.get(indexScene).get((Point3D_F64)point.p);
		point.index = indexScene;
		return point;
	}
}

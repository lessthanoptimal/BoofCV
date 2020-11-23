/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.struct.geo.PointIndex;
import georegression.struct.GeoTuple;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.Iterator;

/**
 * Given a set of points by index for a scene, iterator through them one at a time.
 *
 * @author Peter Abeles
 */
public class ScenePointsSetIterator<T extends PointIndex<T,P>, P extends GeoTuple<P>> implements Iterator<T> {
	SceneStructureMetric scene;
	GrowQueue_I32 indexes;
	int index;
	T point;

	public ScenePointsSetIterator( SceneStructureMetric scene, GrowQueue_I32 indexes, T point ) {
		this(point);
		initialize(scene, indexes);
	}

	public ScenePointsSetIterator(T point) {this.point=point;}

	public void initialize( SceneStructureMetric scene, GrowQueue_I32 indexes) {
		if (scene.isHomogenous() != point.p instanceof Point4D_F64)
			throw new IllegalArgumentException("Scene point type does not match provided point type");

		this.scene = scene;
		this.indexes = indexes;
		this.index = 0;
	}

	public void reset() {
		index = 0;
	}

	@Override public boolean hasNext() {
		return index < indexes.size;
	}

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

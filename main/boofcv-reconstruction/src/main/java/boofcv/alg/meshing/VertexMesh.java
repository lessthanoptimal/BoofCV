/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.meshing;

import boofcv.struct.packed.PackedBigArrayPoint3D_F64;
import org.ddogleg.struct.DogArray_I32;

/**
 * Specifies a 3D mesh. BigArray types are used since a 3D mesh can have a very large number of points in it.
 *
 * @author Peter Abeles
 */
public class VertexMesh {
	/** 3D location of each vertex */
	public final PackedBigArrayPoint3D_F64 vertexes = new PackedBigArrayPoint3D_F64(10);

	/** Which indexes correspond to each vertex in a shape*/
	public final DogArray_I32 indexes = new DogArray_I32();

	/** Start index of each shape + the last index */
	public final DogArray_I32 offsets = new DogArray_I32();

	public VertexMesh setTo( VertexMesh src ) {
		this.vertexes.setTo(src.vertexes);
		this.indexes.setTo(src.indexes);
		this.offsets.setTo(src.offsets);
		return this;
	}

	public void reset() {
		vertexes.reset();
		indexes.reset();
		offsets.reset();
		offsets.add(0);
	}
}

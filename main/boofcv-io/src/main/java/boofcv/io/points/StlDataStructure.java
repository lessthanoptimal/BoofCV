/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

package boofcv.io.points;

import boofcv.alg.meshing.VertexMesh;
import boofcv.struct.packed.PackedBigArrayPoint3D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.jetbrains.annotations.Nullable;

/**
 * <p>Data structure used to store 3D objects that is native to the STL file format [1,2]. One deviation is
 * that vertexes are stored as an integer reference. This is done to reduce storage size and make
 * verifying the correctness of a solid easier.</p>
 *
 * <p>All Facets have exactly 3 vertexes.</p>
 *
 * <p>Rules from specification:</p>
 * <p>
 *     <ul>
 *         <li>Vertex-to-vertex rule. Each triangle must share two vertices with each of its adjacent triangles. In other words, a vertex of one triangle cannot lie on the side of another.</li>
 *         <li>Facet orientation. The facets define the surface of a 3-dimensional object. As such, each facet is part of the boundary between the interior and the exterior of the object. The orientation of the facets (which way is “out” and which way is “in”) is specified redundantly in two ways which must be consistent. First, the direction of the normal is outward. Second, the vertices are listed in counterclockwise order when looking at the object from the outside (right-hand rule).</li>
 *     </ul>
 * </p>
 *
 * <p>[1] StereoLithography Interface Specification, 3D Systems, Inc., October 1989</p>
 * <p>[2] <a href=https://www.fabbers.com/tech/STL_Format>The STL Format</a></p>
 *
 * @author Peter Abeles
 */
public class StlDataStructure {
	/** 3D location of each vertex */
	public final PackedBigArrayPoint3D_F64 vertexes = new PackedBigArrayPoint3D_F64(10);

	/** Normals stored in a compact format */
	public final PackedBigArrayPoint3D_F64 normals = new PackedBigArrayPoint3D_F64(10);

	/**
	 * Mapping from vertex in a Facet to the value of a vertex in 3D stored in vertexes
	 */
	public DogArray_I32 facetVertsIdx = new DogArray_I32(2);

	/** What's the name of this Solid */
	public String name = "";

	public void reset() {
		vertexes.reset();
		normals.reset();
		facetVertsIdx.reset();
		name = "";
	}

	/**
	 * Number of Facets in this solid
	 */
	public int facetCount() {
		return normals.size();
	}

	/**
	 * Retrieves the 3D structure of a facet
	 *
	 * @param which Which facet in this solid is being requested.
	 * @param outNormal The normal vector of the facet
	 * @param outVertexes All the vertexes for this facet
	 */
	public void getFacet( int which, Vector3D_F64 outNormal, DogArray<Point3D_F64> outVertexes ) {
		if (which < 0 || which >= normals.size()) {
			throw new IllegalArgumentException("Index of facet is out of bounds");
		}

		normals.getCopy(which, outNormal);

		// Clear and resize the output
		outVertexes.resize(3);

		// All Facets have 3 vertexes
		int idx0 = which*3;

		// Copy the 3D coordinates of each vertex
		for (int i = 0; i < 3; i++) {
			StlDataStructure.this.vertexes.getCopy(facetVertsIdx.get(idx0 + i), outVertexes.get(i));
		}
	}

	/**
	 * Converts this into a {@link VertexMesh}. Information about normals and the name are discarded.
	 *
	 * @param out Storage for output mesh. Can be null.
	 * @return The converted mesh.
	 */
	public VertexMesh toMesh( @Nullable VertexMesh out ) {
		if (out == null)
			out = new VertexMesh();

		out.vertexes.setTo(vertexes);
		out.indexes.setTo(facetVertsIdx);

		// All facets are triangles
		out.offsets.resize(facetCount());
		for (int i = 0; i < out.offsets.size; i++) {
			out.offsets.set(i, i*3);
		}

		return out;
	}
}

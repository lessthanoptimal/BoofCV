/*
 * Copyright (c) 2024, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.mesh;

import boofcv.struct.packed.PackedArrayPoint3D_F64;
import boofcv.struct.packed.PackedBigArrayPoint2D_F32;
import boofcv.struct.packed.PackedBigArrayPoint3D_F64;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Specifies a 3D mesh. BigArray types are used since a 3D mesh can have a very large number of points in it.
 *
 * @author Peter Abeles
 */
public class VertexMesh {
	/** 3D location of each vertex */
	public final PackedBigArrayPoint3D_F64 vertexes = new PackedBigArrayPoint3D_F64(10);

	/** Which indexes correspond to each vertex in a shape */
	public final DogArray_I32 indexes = new DogArray_I32();

	/** Start index of each shape + the last index */
	public final DogArray_I32 offsets = new DogArray_I32();

	/** 2D coordinate of textures. units = fraction of width / height */
	public final PackedBigArrayPoint2D_F32 texture = new PackedBigArrayPoint2D_F32();

	/** Optional precomputed surface normals for each shape */
	public final PackedArrayPoint3D_F64 normals = new PackedArrayPoint3D_F64();

	{
		offsets.add(0);
	}

	/** Returns number of shapes / polygons in the mesh */
	public int size() {
		return offsets.size - 1;
	}

	/**
	 * Copies the shape vertex into the point.
	 */
	public Point3D_F64 getShapeVertex( int which, @Nullable Point3D_F64 output ) {
		if (output == null)
			output = new Point3D_F64();
		vertexes.getCopy(indexes.get(which), output);
		return output;
	}

	/**
	 * Number of elements in a specific shapes
	 */
	public int getShapeSize( int which ) {
		return offsets.get(which + 1) - offsets.get(which);
	}

	/**
	 * Copies texture coordinates from a polygon
	 *
	 * @param which Which shape to copy
	 * @param output Output storage for the shape
	 */
	public void getTexture( int which, DogArray<Point2D_F32> output ) {
		int idx0 = offsets.get(which);
		int idx1 = offsets.get(which + 1);

		output.reset().resize(idx1 - idx0);
		for (int i = idx0; i < idx1; i++) {
			texture.getCopy(i, output.get(i - idx0));
		}
	}

	/**
	 * Appends the texture coordinates onto the end of the array. NOTE: This will not update
	 * the offsets, which are assumed to be the same as with the vertexes
	 *
	 * @param count Number of points in the coordinates
	 * @param coordinates Array containing interleaved coordinates [x0,y0, x1,y1, ... , x[n], y[n]]
	 */
	public void addTexture( int count, float[] coordinates ) {
		for (int i = 0; i < count; i++) {
			texture.append(coordinates[i*2], coordinates[i*2 + 1]);
		}
	}

	/**
	 * Copies the entire shape into the output array
	 *
	 * @param which Which shape to copy
	 * @param output Output storage for the shape
	 */
	public void getShape( int which, DogArray<Point3D_F64> output ) {
		int idx0 = offsets.get(which);
		int idx1 = offsets.get(which + 1);

		output.reset().resize(idx1 - idx0);
		for (int i = idx0; i < idx1; i++) {
			vertexes.getCopy(indexes.get(i), output.get(i - idx0));
		}
	}

	/**
	 * Adds a new shape with the specified vertexes.
	 */
	public void addShape( List<Point3D_F64> shape ) {
		// All the vertexes for this shape will reference newly added vertexes
		int idx0 = vertexes.size();
		for (int i = 0; i < shape.size(); i++) {
			indexes.add(idx0 + i);
		}

		offsets.add(idx0 + shape.size());
		vertexes.appendAll(shape);
	}

	/**
	 * Computes normal vectors from shapes
	 */
	public void computeNormals() {
		var vector1 = new Vector3D_F64();
		var vector2 = new Vector3D_F64();
		var norm = new Vector3D_F64();

		for (int idxShape = 1; idxShape < offsets.size; idxShape++) {
			int idx0 = offsets.get(idxShape - 1);

			// Use cross product to find the normal in the correct orientation
			pointsToVector(idx0, idx0 + 1, vector1);
			pointsToVector(idx0 + 1, idx0 + 2, vector2);

			GeometryMath_F64.cross(vector1, vector2, norm);
			norm.normalize();
			normals.append(norm.x, norm.y, norm.z);
		}
	}

	private void pointsToVector( int idxA, int idxB, Vector3D_F64 out ) {
		Point3D_F64 p = vertexes.getTemp(indexes.get(idxA));

		double x0 = p.x;
		double y0 = p.y;
		double z0 = p.z;

		p = vertexes.getTemp(indexes.get(idxB));
		out.x = p.x - x0;
		out.y = p.y - y0;
		out.z = p.z - z0;
	}

	public VertexMesh setTo( VertexMesh src ) {
		this.vertexes.setTo(src.vertexes);
		this.indexes.setTo(src.indexes);
		this.offsets.setTo(src.offsets);
		this.texture.setTo(src.texture);
		return this;
	}

	public void reset() {
		vertexes.reset();
		indexes.reset();
		offsets.reset();
		texture.reset();
		offsets.add(0);
	}

	/**
	 * Wraps the data structure inside {@link MeshPolygonAccess}.
	 */
	public MeshPolygonAccess toAccess() {
		return new MeshPolygonAccess() {
			@Override public int size() {
				return VertexMesh.this.size();
			}

			@Override public void getPolygon( int which, DogArray<Point3D_F64> vertexes ) {
				VertexMesh.this.getShape(which, vertexes);
			}
		};
	}
}

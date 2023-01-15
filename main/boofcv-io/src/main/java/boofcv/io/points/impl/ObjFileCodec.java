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

package boofcv.io.points.impl;

import boofcv.alg.cloud.PointCloudReader;
import boofcv.alg.cloud.PointCloudWriter;
import boofcv.alg.meshing.VertexMesh;
import georegression.struct.point.Point3D_F64;
import org.ddogleg.struct.DogArray_I32;

import java.io.*;

/**
 * High level interface for reading and writing OBJ files. Takes in BoofCV specific data structures
 * for input and output. This format was created by Wavefront and a link to format is found below.
 *
 * @see <a href="https://people.computing.clemson.edu/~dhouse/courses/405/docs/brief-obj-file-format.html">OBJ Format</a>
 */
public class ObjFileCodec {
	/**
	 * Writes a point cloud with no color information since the format doesn't support
	 * colored points.
	 */
	public static void save( PointCloudReader cloud, Writer writer ) throws IOException {
		var obj = new ObjFileWriter(writer);
		obj.addComment("Created by BoofCV");

		var point = new Point3D_F64();
		int N = cloud.size();
		for (int i = 0; i < N; i++) {
			cloud.get(i, point);
			obj.addVertex(point.x, point.y, point.z);
			obj.addPoint(-1);
		}
	}

	public static void save( VertexMesh mesh, Writer writer ) throws IOException {
		var obj = new ObjFileWriter(writer);
		obj.addComment("Created by BoofCV");

		// First save the vertexes
		int N = mesh.vertexes.size();
		for (int i = 0; i < N; i++) {
			Point3D_F64 p = mesh.vertexes.getTemp(i);
			obj.addVertex(p.x, p.y, p.z);
		}

		// Create the faces
		var indexes = new DogArray_I32();
		for (int i = 1; i < mesh.offsets.size; i++) {
			int idx0 = mesh.offsets.get(i - 1);
			int idx1 = mesh.offsets.get(i);

			indexes.reset();
			indexes.addAll(mesh.indexes.data, idx0, idx1);
			obj.addFace(indexes);
		}
	}

	public static void load( InputStream input, PointCloudWriter output ) throws IOException {
		var obj = new ObjFileReader() {
			@Override protected void addVertex( double x, double y, double z ) {
				output.add(x, y, z, 0);
			}
			@Override protected void addPoint( int vertex ) {}
			@Override protected void addLine( DogArray_I32 vertexes ) {}
			@Override protected void addFace( DogArray_I32 vertexes ) {}
		};
		obj.parse(new BufferedReader(new InputStreamReader(input)));
	}

	public static void load( InputStream input, VertexMesh output ) throws IOException {
		output.reset();
		var obj = new ObjFileReader() {
			@Override protected void addVertex( double x, double y, double z ) {
				output.vertexes.append(x, y, z);
			}
			@Override protected void addPoint( int vertex ) {}
			@Override protected void addLine( DogArray_I32 vertexes ) {}
			@Override protected void addFace( DogArray_I32 vertexes ) {
				output.indexes.addAll(vertexes);
				output.offsets.add(output.indexes.size);
			}
		};
		obj.parse(new BufferedReader(new InputStreamReader(input)));
	}
}

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

package boofcv.io.points.impl;

import org.ddogleg.struct.DogArray_I32;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Writer;

/**
 * Interface for creating an OBJ file. As components are added they are written to. It's possible to
 * add a face that automatically uses recent vertexes. Only vertexes and faces are currently supported.
 */
public class ObjFileWriter {
	public String formatFloat = "%.8f";

	Writer writer;

	int vertexCount = 0;
	int lastObjectVertexCount;

	public ObjFileWriter( Writer writer ) {
		this.writer = writer;
	}

	public void addComment( String comment ) throws IOException {
		writer.write("# " + comment + "\n");
	}

	public void addVertex( double x, double y, double z ) throws IOException {
		writer.write('v');
		writer.write(String.format(" " + formatFloat, x));
		writer.write(String.format(" " + formatFloat, y));
		writer.write(String.format(" " + formatFloat + "\n", z));
		vertexCount++;
	}

	public void addTextureVertex( double x, double y ) throws IOException {
		writer.write("vt");
		writer.write(String.format(" " + formatFloat, x));
		writer.write(String.format(" " + formatFloat + "\n", y));
	}

	public void addVertexNormal( double dx, double dy, double dz ) throws IOException {
		writer.write("vn");
		writer.write(String.format(" " + formatFloat, dx));
		writer.write(String.format(" " + formatFloat, dy));
		writer.write(String.format(" " + formatFloat + "\n", dz));
		vertexCount++;
	}

	public void addMaterial( String name ) throws IOException {
		writer.write("usemtl " + name + "\n");
	}

	public void addLibrary( String name ) throws IOException {
		writer.write("mtllib " + name + "\n");
	}

	public void addGroup( String name ) throws IOException {
		writer.write("g " + name + "\n");
	}

	public void addPoint( int vertex ) throws IOException {
		writer.write("p " + vertex + "\n");
	}

	/**
	 * Adds a line with the specified vertexes.
	 *
	 * <p>NOTE: Vertexes indexes are 0-indexed. Not 1-indexed like they are in OBJ files.</p>
	 *
	 * @param vertexes Index of vertexes in this face. If null then automatic.
	 */
	public void addLine( @Nullable DogArray_I32 vertexes ) throws IOException {
		writeObject('l', vertexes, 1);
	}

	/**
	 * Adds a face with the specified vertexes. If there are no vertex indexes it will
	 * reference all the vertexes added since the last object was written.
	 *
	 * <p>NOTE: Vertexes indexes are 0-indexed. Not 1-indexed like they are in OBJ files.</p>
	 *
	 *
	 * @param count Number of different vertex types, i.e. 3D, texture, normal. All are assumed to
	 * reference a point with the same index.
	 * @param vertexes Index of vertexes in this face. If null then automatic.
	 */
	public void addFace( @Nullable DogArray_I32 vertexes, int count ) throws IOException {
		writeObject('f', vertexes, count);
	}

	/**
	 *
	 * @param count How many times it should duplicate the index. It's assumed all vertexes reference
	 * objects with the same index values.
	 */
	private void writeObject( char name, @Nullable DogArray_I32 vertexes, int count ) throws IOException {
		writer.write(name);
		if (vertexes == null || vertexes.size == 0) {
			int v = -1;
			for (int i = lastObjectVertexCount; i < vertexCount; i++, v--) {
				writer.write(" " + v);
			}
		} else {
			for (int i = 0; i < vertexes.size; i++) {
				var vertIndex = vertexes.get(i);

				// sanity check to catch mistakes early
				if (vertIndex >= vertexCount)
					throw new IllegalArgumentException("Vertex index must be less than len(vertexes)");
				else if (vertIndex <= -vertexCount)
					throw new IllegalArgumentException("Vertex index must be <= -len(vertexes)");

				// Write the index and compensate for the file format being 1-indexed.
				if (vertIndex >= 0)
					vertIndex++;

				writer.write(" " + vertIndex);
				for (int idxCount = 1; idxCount < count; idxCount++) {
					writer.write("/" + vertIndex);
				}
			}
		}
		writer.write('\n');
		lastObjectVertexCount = vertexCount;
	}
}

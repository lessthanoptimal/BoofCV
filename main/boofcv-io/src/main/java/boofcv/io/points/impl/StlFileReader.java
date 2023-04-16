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

import boofcv.io.points.StlDataStructure;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Reads in a file that's in STL format and saves to a {@link StlDataStructure}.
 *
 * @author Peter Abeles
 * @see StlDataStructure
 */
public class StlFileReader {
	/**
	 * Operation used to add a vertex. This can be used to compress the amount of storage used
	 * to save the mesh by re-using vertexes. By default this employs a mindless approach
	 */
	public AddVertex opVertexAdd = ( x, y, z, out ) -> {
		out.facetVertsIdx.add(out.vertexes.size());
		out.vertexes.append(x, y, z);
	};

	/**
	 * Parses a file in the STL ASCII format
	 */
	public void readAscii( BufferedReader input, StlDataStructure out ) throws IOException {
		out.reset();

		// How many lines have been read. used to make debugging easier.
		int lineCount = 0;
		while (true) {
			String line = input.readLine();
			if (line == null)
				break;

			// remove whitespace at the ends
			line = line.strip();

			if (line.isEmpty())
				continue;

			String[] words = line.split("\\s+");

			switch (words[0]) {
				case "solid" -> {
					if (words.length != 2)
						throw new IOException("Line " + lineCount + " : Expected 2 words for a solid");
					out.name = words[1];
				}

				case "facet" -> {
					if (words.length != 5)
						throw new IOException("Line " + lineCount + " : Expected 5 words for a facet");

					// Parse the normal
					double nx = Double.parseDouble(words[2]);
					double ny = Double.parseDouble(words[3]);
					double nz = Double.parseDouble(words[4]);

					// Save and reference the Facet's normal
					out.normals.append(nx, ny, nz);
				}

				case "vertex" -> {
					if (words.length != 4)
						throw new IOException("Line " + lineCount + " : Expected 4 words for a vertex");

					// Parse the vertex's 3D location
					double vx = Double.parseDouble(words[1]);
					double vy = Double.parseDouble(words[2]);
					double vz = Double.parseDouble(words[3]);

					opVertexAdd.addVertex(vx, vy, vz, out);
				}

				// Ignore all the other keywords as they are redundant
				default -> {
				}
			}
		}
	}

	/**
	 * Parses a file in the STL binary format
	 */
	public void readBinary( InputStream input, StlDataStructure out ) throws IOException {
		out.reset();

		final byte[] line = new byte[80];
		int amountRead = input.read(line, 0, 80);
		if (amountRead != 80)
			throw new IOException("Couldn't read in entire header. amount=" + amountRead);

		// Find length of text, assuming it's null terminating
		int stringLength = line.length;
		for (int i = 0; i < line.length; i++) {
			if (line[i] == 0) {
				stringLength = i;
				break;
			}
		}
		out.name = new String(line, 0, stringLength, StandardCharsets.UTF_8);

		if (4 != input.read(line, 0, 4)) {
			throw new IOException("Failed to read number of facets");
		}

		final ByteBuffer bb = ByteBuffer.wrap(line);
		int numFacets = bb.getInt(0);

		// Number of bytes it takes to store a Facet
		int facetBytes = 4*3*4;

		// pre-allocate memory
		out.facetVertsIdx.resize(numFacets*3).reset();
		out.vertexes.reserve(numFacets*3);
		out.normals.reserve(numFacets);

		// Read in all the facets
		for (int i = 0; i < numFacets; i++) {
			// Read in all the data for a facet at once
			if (facetBytes != input.read(line, 0, facetBytes)) {
				throw new IOException("Failed to read data for facet " + i);
			}

			out.normals.append(bb.getFloat(0), bb.getFloat(4), bb.getFloat(8));
			opVertexAdd.addVertex(bb.getFloat(12), bb.getFloat(16), bb.getFloat(20), out);
			opVertexAdd.addVertex(bb.getFloat(24), bb.getFloat(28), bb.getFloat(32), out);
			opVertexAdd.addVertex(bb.getFloat(36), bb.getFloat(40), bb.getFloat(44), out);
		}
	}

	/**
	 * Operator for adding vertexes. Ideally this would see if the same vertex has already been added then
	 * reference that index instead of double counting.
	 */
	@FunctionalInterface interface AddVertex {
		void addVertex( double x, double y, double z, StlDataStructure out );
	}
}

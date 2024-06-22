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

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Reads an OBJ file and adds objects as they are read.
 */
public abstract class ObjFileReader {
	DogArray_I32 vertexIndexes = new DogArray_I32();
	int vertexCount = 0;
	int vertexTextureCount = 0;
	int vertexNormalCount = 0;

	/**
	 * Decodes / reads the OBJ file encoded as text in the reader
	 */
	public void parse( BufferedReader reader ) throws IOException {
		vertexCount = 0;
		vertexTextureCount = 0;
		vertexNormalCount = 0;

		var builder = new StringBuilder();
		int actualLineCount = 0;
		while (true) {
			String l = reader.readLine();
			if (l == null)
				break;
			actualLineCount++;

			// Skip empty strings or comment lines
			if (l.length() == 0 || l.charAt(0) == '#')
				continue;

			String line;

			// Read one line at a time while checking for the continue character at the end
			String chunk = l.trim();
			if (chunk.isEmpty()) {
				continue;
			} else if (chunk.endsWith("\\")) {
				if (builder.length() != 0)
					builder.append(' ');
				builder.append(chunk);
				continue;
			} else if (builder.length() == 0) {
				line = chunk;
			} else {
				line = builder.toString() + ' ' + chunk;
				builder.setLength(0);
			}

			String[] words = line.split("\\s+");

			try {
				switch (words[0]) {
					case "v" -> {
						double x = Double.parseDouble(words[1]);
						double y = Double.parseDouble(words[2]);
						double z = Double.parseDouble(words[3]);
						addVertex(x, y, z);
						vertexCount++;
					}

					case "vn" -> {
						double x = Double.parseDouble(words[1]);
						double y = Double.parseDouble(words[2]);
						double z = Double.parseDouble(words[3]);
						addVertexNormal(x, y, z);
						vertexNormalCount++;
					}

					case "vt" -> {
						double x = Double.parseDouble(words[1]);
						double y = Double.parseDouble(words[2]);
						addVertexTexture(x, y);
						vertexTextureCount++;
					}

					case "p" -> addPoint(ensureIndex(Integer.parseInt(words[1])));
					case "l" -> {
						readPoints(words);
						addLine(vertexIndexes);
					}
					case "f" -> {
						readFaceIndexes(words);
						addFace(vertexIndexes, words.length - 1);
					}
					default -> handleError(actualLineCount + " Unknown object type. '" + words[0] + "'");
				}
			} catch (Exception e) {
				// Skip over locally bad data
				handleError(actualLineCount + " Bad object description " + words[0] + " '" + e.getMessage() + "'");
			}
		}
	}

	/**
	 * Converts the vertex number found in the file into an array index. OBJ files are 1 index and accept
	 * negative numbers.
	 */
	int ensureIndex( int found ) {
		if (found > 0)
			return found - 1;
		return vertexCount + found;
	}

	/**
	 * Converts words into an array of indexes
	 */
	private void readPoints( String[] words ) {
		vertexIndexes.reset();
		for (int i = 1; i < words.length; i++) {
			vertexIndexes.add(ensureIndex(Integer.parseInt(words[i])));
		}
	}

	private void readFaceIndexes( String[] words ) {
		vertexIndexes.reset();
		for (int i = 1; i < words.length; i++) {
			String word = words[i];

			int idx0 = 0;
			int idx1 = word.indexOf('/');
			while (idx1 != -1) {
				vertexIndexes.add(ensureIndex(Integer.parseInt(word.substring(idx0, idx1))));
				idx0 = idx1 + 1;
				idx1 = word.indexOf('/', idx0);
			}
			vertexIndexes.add(ensureIndex(Integer.parseInt(word.substring(idx0))));
		}
	}

	protected abstract void addVertex( double x, double y, double z );

	protected abstract void addVertexNormal( double x, double y, double z );

	protected abstract void addVertexTexture( double x, double y );

	protected abstract void addPoint( int vertex );

	protected abstract void addLine( DogArray_I32 vertexes );

	/**
	 * Adds a face. Indexes are interleaved in vertex, normal, and texture order.
	 *
	 * @param indexes Indexes of vertex values
	 * @param vertexCount Number of vertexes in the face
	 */
	protected abstract void addFace( DogArray_I32 indexes, int vertexCount );

	/**
	 * If something goes where it's passed here
	 */
	protected void handleError( String message ) {
		System.err.println(message);
	}
}

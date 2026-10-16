/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.mesh.VertexMesh;
import org.ddogleg.struct.DogArray_I32;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/// Loads a OBJ file and associated MTL file. This makes assumptions that it's reading from a file system and doesn't.
/// This allows it to search for files referenced in the OBJ.
public class ObjLoadFromFiles {
	private final Map<String, String> materialToTextureFile = new HashMap<>();

	/// Loads the OBJ file from disk. Opens up and reads in any related MTL files as needed. Each material's texture
	/// file is added to [VertexMesh#textureNames] and [VertexMesh#faceTextures] specifies which one a
	/// face uses.
	///
	/// @param outputMesh (Output) storage for the mesh
	public void load( File file, VertexMesh outputMesh ) {
		materialToTextureFile.clear();
		outputMesh.reset();
		var reader = new ObjFileReader() {
			// Index of the texture used by faces that are read in
			int activeTexture = 0;

			@Override protected void addLibrary( String name ) {
				readTextureFilesFromMTL(new File(file.getParent(), name));
			}

			@Override protected void addMaterial( String name ) {
				String textureFile = materialToTextureFile.get(name);
				if (textureFile == null) {
					System.err.println("No texture file for material '" + name + "'. Using the first texture");
					activeTexture = 0;
					return;
				}

				// Materials can share a texture file
				activeTexture = outputMesh.textureNames.indexOf(textureFile);
				if (activeTexture < 0) {
					activeTexture = outputMesh.textureNames.size();
					outputMesh.textureNames.add(textureFile);
				}
			}

			@Override protected void addVertex( double x, double y, double z ) {
				outputMesh.vertexes.append(x, y, z);
			}

			@Override
			protected void addVertexWithColor( double x, double y, double z, double red, double green, double blue ) {
				outputMesh.vertexes.append(x, y, z);
				outputMesh.rgb.add(ObjFileCodec.convertToInt(red, green, blue));
			}

			@Override protected void addVertexNormal( double x, double y, double z ) {
				outputMesh.normals.append((float)x, (float)y, (float)z);
			}

			@Override protected void addVertexTexture( double x, double y ) {
				outputMesh.texture.append((float)x, (float)y);
			}

			@Override protected void addPoint( int vertex ) {}

			@Override protected void addLine( DogArray_I32 vertexes ) {}

			@Override protected void addFace( DogArray_I32 indexes, int vertexCount ) {
				ObjFileCodec.addFactToMesh(indexes, vertexCount, outputMesh);
				outputMesh.faceTextures.add(activeTexture);
			}
		};

		// Read in the OBJ file
		try (var input = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
			reader.parse(new BufferedReader(input));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// Per-face texture indexes are only needed if there is more than one texture
		if (outputMesh.textureNames.size() <= 1)
			outputMesh.faceTextures.reset();
	}

	/// Just read in the texture file names from the MTL file
	void readTextureFilesFromMTL( File file ) {
		String materialName = "";
		try (var input = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
			String line = input.readLine();
			while (line != null) {
				try {
					// Find the first word
					int idx1 = line.indexOf(' ');
					if (idx1 == -1)
						continue;

					// Find the second word
					int idx2 = line.indexOf(' ', idx1 + 1);

					// See if it's at the end of the line
					if (idx2 == -1)
						idx2 = line.length();

					String command = line.substring(0, idx1);
					String value = line.substring(idx1 + 1, idx2);

					switch (command) {
						case "newmtl" -> materialName = value;
						case "map_Kd" -> materialToTextureFile.put(materialName, value);
					}
				} finally {
					line = input.readLine();
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}

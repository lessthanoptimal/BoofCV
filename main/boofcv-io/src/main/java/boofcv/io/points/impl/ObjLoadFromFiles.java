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

import boofcv.struct.mesh.VertexMesh;
import lombok.Getter;
import org.ddogleg.struct.DogArray_I32;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads a OBJ file and associated MTL file. This makes assumptions that it's reading from a file system and doesn't.
 * This allows it to search for files referenced in the OBJ.
 */
public class ObjLoadFromFiles {
	private Map<String, String> materialToTextureFile = new HashMap<>();

	/** Returns a map containing all the shapes that were loaded from the OBJ file */
	private @Getter Map<String, VertexMesh> shapeToMesh = new HashMap<>();

	/** Indicates that it found multiple materials in the file. */
	private @Getter boolean ignoredMaterial = false;

	// If true then the material being seen is the first one
	private boolean first = true;

	/**
	 * Loads the OBJ file from disk. Opens up and reads in any related MTL files as needed. To see if any materials
	 * were ignored because a mesh was passed in, look at {@link #ignoredMaterial}. If no material is defined then
	 * the output mesh will be placed in the map with a key of an empty String.
	 *
	 * @param outputMesh If not null the proved mesh will be used for storage. If more than one mesh is defined in
	 * the file then only the first one will be read.
	 */
	public void load( File file, @Nullable VertexMesh outputMesh ) {
		ignoredMaterial = false;
		first = true;
		var reader = new ObjFileReader() {
			VertexMesh active;

			{
				// If the output mesh was passed in default to that, otherwise create a new mesh to default to
				active = outputMesh != null ? outputMesh : new VertexMesh();
				active.reset();
				shapeToMesh.put("", active);
			}

			@Override protected void addLibrary( String name ) {
				readTextureFilesFromMTL(new File(file.getParent(), name));
			}

			@Override protected void addMaterial( String name ) {
				// If this is the first material to add AND a shape has not been defined already,
				// remove the default mesh from the map since it isn't being used
				if (first && active.vertexes.size() == 0) {
					shapeToMesh.remove("");
				}

				if (outputMesh != null) {
					// If first isn't true that means another material was encountered. If vertexes are not zero that
					// means it was reading in a default object already
					if (!first || active.vertexes.size() != 0) {
						ignoredMaterial = true;
						throw new MultipleMaterials();
					}
				} else {
					active = new VertexMesh();
				}

				first = false;

				// Set the texture file name if it's specified for this material
				if (materialToTextureFile.containsKey(name)) {
					active.textureName = materialToTextureFile.get(name);
				} else {
					System.err.println("Unknown material '" + name + "'");
				}
				shapeToMesh.put(name, active);
			}

			@Override protected void addVertex( double x, double y, double z ) {
				active.vertexes.append(x, y, z);
			}

			@Override
			protected void addVertexWithColor( double x, double y, double z, double red, double green, double blue ) {
				active.vertexes.append(x, y, z);
				active.rgb.add(ObjFileCodec.convertToInt(red, green, blue));
			}

			@Override protected void addVertexNormal( double x, double y, double z ) {
				active.normals.append((float)x, (float)y, (float)z);
			}

			@Override protected void addVertexTexture( double x, double y ) {
				active.texture.append((float)x, (float)y);
			}

			@Override protected void addPoint( int vertex ) {}

			@Override protected void addLine( DogArray_I32 vertexes ) {}

			@Override protected void addFace( DogArray_I32 indexes, int vertexCount ) {
				ObjFileCodec.addFactToMesh(indexes, vertexCount, active);
			}
		};

		// Read in the OBJ file
		try (var input = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
			reader.parse(new BufferedReader(input));
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (MultipleMaterials e) {
			ignoredMaterial = true;
		}
	}

	/**
	 * Just read in the texture file names from the MTL file
	 */
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

	/** Thrown as a way to escape if it starts to read a second material and a mesh was passed in */
	static class MultipleMaterials extends RuntimeException {}
}

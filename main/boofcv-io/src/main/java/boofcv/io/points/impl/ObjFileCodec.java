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

import boofcv.alg.cloud.PointCloudReader;
import boofcv.alg.cloud.PointCloudWriter;
import boofcv.struct.mesh.VertexMesh;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point3D_F32;
import georegression.struct.point.Point3D_F64;
import org.apache.commons.io.FilenameUtils;
import org.ddogleg.struct.DogArray_I32;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

/// High level interface for reading and writing OBJ files. Takes in BoofCV specific data structures
/// for input and output. This format was created by Wavefront and a link to format is found below.
///
/// @see <a href="https://people.computing.clemson.edu/~dhouse/courses/405/docs/brief-obj-file-format.html">OBJ Format</a>
public class ObjFileCodec {
	/// Writes a point cloud with no color information since the format doesn't support
	/// colored points.
	public static void save( PointCloudReader cloud, Writer writer ) throws IOException {
		var obj = new ObjFileWriter(writer);
		obj.addComment("Created by BoofCV");

		boolean hasColor = cloud.colors();

		var point = new Point3D_F64();
		int N = cloud.size();
		for (int i = 0; i < N; i++) {
			cloud.get(i, point);
			if (hasColor) {
				addRgbVertex(obj, point, cloud.getRGB(i));
			} else {
				obj.addVertex(point.x, point.y, point.z);
			}
			obj.addPoint(-1);
		}
	}

	/// Creates a MTL file with one material for each texture file. This is required if the mesh is textured mapped.
	/// Each material is named after its texture file, see [#materialName].
	///
	/// @param textureFiles Path to each texture mapped file
	/// @param writer Where the MTL will be written to
	public static void saveMtl( List<String> textureFiles, Writer writer ) throws IOException {
		// Use hard coded values
		String text = """
				newmtl %s
				Ka 1.0 1.0 1.0
				Kd 1.0 1.0 1.0
				Ks 0.0 0.0 0.0
				d 1.0
				Ns 0.0
				illum 0
				map_Kd %s
				""";

		for (String textureFile : textureFiles) {
			writer.write(String.format(text, materialName(textureFile), textureFile));
		}
	}

	/// Name of the material that uses a texture file
	static String materialName( String textureFile ) {
		return FilenameUtils.getBaseName(textureFile);
	}

	/// Saves the mesh. If there are texture files then the MTL file is assumed to have the same base name as
	/// the first texture file. Use [#saveMtl] to create it.
	public static void save( VertexMesh mesh, Writer writer ) throws IOException {
		var obj = new ObjFileWriter(writer);
		obj.addComment("Created by BoofCV");

		boolean hasMaterials = !mesh.textureNames.isEmpty();
		if (hasMaterials) {
			obj.addLibrary(materialName(mesh.textureNames.get(0)) + ".mtl");
		}

		boolean hasVertexColors = mesh.rgb.size > 0;

		// First save the vertexes
		int N = mesh.vertexes.size();
		for (int i = 0; i < N; i++) {
			Point3D_F64 p = mesh.vertexes.getTemp(i);
			if (hasVertexColors) {
				addRgbVertex(obj, p, mesh.rgb.get(i));
			} else {
				obj.addVertex(p.x, p.y, p.z);
			}
		}

		// Save vertex normals
		for (int i = 0; i < mesh.normals.size(); i++) {
			Point3D_F32 p = mesh.normals.getTemp(i);
			obj.addVertexNormal(p.x, p.y, p.z);
		}

		// Save vertex textures
		for (int i = 0; i < mesh.texture.size(); i++) {
			Point2D_F32 p = mesh.texture.getTemp(i);
			obj.addTextureVertex(p.x, p.y);
		}

		// Create the faces
		var vertexes = new DogArray_I32();
		var textures = new DogArray_I32();
		var normals = new DogArray_I32();
		int activeTexture = -1;
		for (int face = 0; face < mesh.size(); face++) {
			// Change the material when the texture changes
			if (hasMaterials && mesh.getFaceTexture(face) != activeTexture) {
				activeTexture = mesh.getFaceTexture(face);
				obj.addMaterial(materialName(mesh.textureNames.get(activeTexture)));
			}

			int idx0 = mesh.faceOffsets.get(face);
			int idx1 = mesh.faceOffsets.get(face + 1);

			vertexes.reset();
			textures.reset();
			normals.reset();
			for (int i = idx0; i < idx1; i++) {
				vertexes.add(mesh.faceVertexes.get(i));

				// If not specified, there is one texture coordinate for each vertex in a face
				if (mesh.isTextureCoors())
					textures.add(mesh.faceVertexTextures.isEmpty() ? i : mesh.faceVertexTextures.get(i));

				// If not specified, there is one normal for each vertex
				if (mesh.isNormals())
					normals.add(mesh.faceVertexNormals.isEmpty() ? mesh.faceVertexes.get(i) : mesh.faceVertexNormals.get(i));
			}
			obj.addFace(vertexes, textures, normals);
		}
	}

	private static void addRgbVertex( ObjFileWriter obj, Point3D_F64 p, int rgb ) throws IOException {
		// Convert to float format
		double red = ((rgb >> 16) & 0xFF)/255.0;
		double green = ((rgb >> 8) & 0xFF)/255.0;
		double blue = (rgb & 0xFF)/255.0;
		obj.addVertex(p.x, p.y, p.z, red, green, blue);
	}

	public static void load( InputStream input, PointCloudWriter output ) throws IOException {
		var obj = new ObjFileReader() {
			@Override protected void addLibrary( String name ) {}

			@Override protected void addMaterial( String name ) {}

			@Override protected void addVertex( double x, double y, double z ) {
				output.startPoint();
				output.location(x, y, z);
				output.stopPoint();
			}

			@Override
			protected void addVertexWithColor( double x, double y, double z, double red, double green, double blue ) {
				output.startPoint();
				output.location(x, y, z);
				output.color(convertToInt(red, green, blue));
				output.stopPoint();
			}

			@Override protected void addVertexNormal( double x, double y, double z ) {}

			@Override protected void addVertexTexture( double x, double y ) {}

			@Override protected void addPoint( int vertex ) {}

			@Override protected void addLine( DogArray_I32 vertexes ) {}

			@Override protected void addFace( DogArray_I32 indexes, int vertexCount ) {}
		};
		obj.parse(new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8)));
	}

	public static void load( InputStream input, VertexMesh output ) throws IOException {
		output.reset();
		var obj = new ObjFileReader() {
			// Library and material doesn't translate well a stream input and mesh output
			// A single obj file can define multiple objects with different texture maps
			@Override protected void addLibrary( String name ) {}

			@Override protected void addMaterial( String name ) {}

			@Override protected void addVertex( double x, double y, double z ) {
				output.vertexes.append(x, y, z);
			}

			@Override
			protected void addVertexWithColor( double x, double y, double z, double red, double green, double blue ) {
				output.vertexes.append(x, y, z);
				output.rgb.add(convertToInt(red, green, blue));
			}

			@Override protected void addVertexNormal( double x, double y, double z ) {
				output.normals.append((float)x, (float)y, (float)z);
			}

			@Override protected void addVertexTexture( double x, double y ) {
				output.texture.append((float)x, (float)y);
			}

			@Override protected void addPoint( int vertex ) {}

			@Override protected void addLine( DogArray_I32 vertexes ) {}

			@Override protected void addFace( DogArray_I32 indexes, int vertexCount ) {
				addFactToMesh(indexes, vertexCount, output);
			}
		};
		obj.parse(new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8)));
	}

	static void addFactToMesh( DogArray_I32 indexes, int vertexCount, VertexMesh output ) {
		// order of that indexes are specifies is always vertex/texture/normal

		boolean hasTexture = output.texture.size() > 0;
		boolean hasNormals = output.normals.size() > 0;

		int typeCount = indexes.size/vertexCount;
		for (int idxVert = 0; idxVert < vertexCount; idxVert++) {
			int index = idxVert*typeCount;;
			output.faceVertexes.add(indexes.get(index++));

			if (hasTexture) {
				output.faceVertexTextures.add(indexes.get(index++));
			}

			if (hasNormals) {
				output.faceVertexNormals.add(indexes.get(index));
			}
		}

		output.faceOffsets.add(output.faceVertexes.size);
	}

	static int convertToInt( double red, double green, double blue ) {
		return ((int)(255*red + 0.5)) << 16 | ((int)(255*green + 0.5)) << 8 | ((int)(255*blue + 0.5));
	}
}

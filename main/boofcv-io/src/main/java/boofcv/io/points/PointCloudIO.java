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

package boofcv.io.points;

import boofcv.alg.cloud.AccessColorIndex;
import boofcv.alg.cloud.AccessPointIndex;
import boofcv.alg.cloud.PointCloudReader;
import boofcv.alg.cloud.PointCloudWriter;
import boofcv.io.points.impl.*;
import boofcv.struct.Point3dRgbI_F64;
import boofcv.struct.mesh.VertexMesh;
import georegression.struct.point.Point3D_F32;
import georegression.struct.point.Point3D_F64;
import org.apache.commons.io.FilenameUtils;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

/**
 * Code for reading different point cloud formats
 *
 * @author Peter Abeles
 */
public class PointCloudIO {
	/**
	 * Saves point cloud to disk using a high level API. For more control over the format use the CODEC directly.
	 *
	 * @see PlyCodec
	 */
	public static void save3D( Format format, PointCloudReader cloud, boolean saveRGB, OutputStream outputStream )
			throws IOException {
		switch (format) {
			case PLY -> PlyCodec.saveCloudBinary(cloud, ByteOrder.BIG_ENDIAN, saveRGB, false, outputStream);
			case OBJ -> ObjFileCodec.save(cloud, new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
			case STL -> throw new IllegalArgumentException("STL doesn't support point clouds");
			default -> throw new IllegalArgumentException("Unknown format " + format);
		}
	}

	/**
	 * Saves a mesh.
	 */
	public static void save3D( Format format, VertexMesh mesh, @Nullable DogArray_I32 colorRGB,
							   OutputStream outputStream )
			throws IOException {
		switch (format) {
			case PLY -> PlyCodec.saveMeshBinary(mesh, colorRGB, ByteOrder.BIG_ENDIAN, false, outputStream);
			case OBJ -> ObjFileCodec.save(mesh, new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
			case STL -> new StlFileWriter().writeBinary(mesh.toAccess(), "MeshBoofCV", outputStream);
			default -> throw new IllegalArgumentException("Unknown format " + format);
		}
	}

	/**
	 * Saves point cloud using access API.
	 *
	 * @see AccessPointIndex
	 * @see AccessColorIndex
	 * @see PlyCodec
	 */
	public static void save3D( Format format,
							   AccessPointIndex<Point3D_F64> accessPoint,
							   AccessColorIndex accessColor,
							   int size, boolean saveRGB, OutputStream outputStream )
			throws IOException {

		PointCloudReader reader = new PointCloudReader() {
			final Point3D_F64 tmp = new Point3D_F64();

			@Override public void get( int index, Point3D_F32 point ) {
				accessPoint.getPoint(index, tmp);
				point.setTo((float)tmp.x, (float)tmp.y, (float)tmp.z);
			}

			@Override public int size() {return size;}

			@Override public boolean colors() {return true;}

			@Override public void get( int index, Point3D_F64 point ) {accessPoint.getPoint(index, point);}

			@Override public int getRGB( int index ) {return accessColor.getRGB(index);}
		};

		save3D(format, reader, saveRGB, outputStream);
	}

	public static DogArray<Point3D_F32>
	load3D32F( Format format, InputStream input, @Nullable DogArray<Point3D_F32> storage ) throws IOException {
		if (storage == null)
			storage = new DogArray<>(Point3D_F32::new);
		PointCloudWriter output = PointCloudWriter.wrapF32(storage);
		load(format, input, output);
		return storage;
	}

	public static DogArray<Point3D_F64>
	load3D64F( Format format, InputStream input, @Nullable DogArray<Point3D_F64> storage ) throws IOException {
		if (storage == null)
			storage = new DogArray<>(Point3D_F64::new);
		PointCloudWriter output = PointCloudWriter.wrapF64(storage);
		load(format, input, output);
		return storage;
	}

	public static DogArray<Point3dRgbI_F64>
	load3DRgb64F( Format format, InputStream input, @Nullable DogArray<Point3dRgbI_F64> storage ) throws IOException {
		if (storage == null)
			storage = new DogArray<>(Point3dRgbI_F64::new);
		PointCloudWriter output = PointCloudWriter.wrapF64RGB(storage);
		load(format, input, output);
		return storage;
	}

	/**
	 * Reads a point cloud from the input stream in the specified format and writes it to the output.
	 *
	 * @param format Storage format
	 * @param input Input stream
	 * @param output Output cloud writer
	 */
	public static void load( Format format, InputStream input, PointCloudWriter output ) throws IOException {
		switch (format) {
			case PLY -> PlyCodec.readCloud(input, output);
			case OBJ -> ObjFileCodec.load(input, output);
			default -> throw new RuntimeException("Unknown format");
		}
	}

	/**
	 * Loads the mesh from a file. File type is determined by the file's extension.
	 *
	 * <p>For OBJ files, if there are multiple shapes defined then only the first one is returned.</p>
	 *
	 * @param file Which file it should load
	 * @param mesh (Output) storage for the mesh
	 */
	public static void load( File file, VertexMesh mesh ) throws IOException {
		String extension = FilenameUtils.getExtension(file.getName()).toLowerCase(Locale.ENGLISH);
		var type = switch (extension) {
			case "ply" -> PointCloudIO.Format.PLY;
			case "stl" -> PointCloudIO.Format.STL;
			case "obj" -> PointCloudIO.Format.OBJ;
			default -> throw new RuntimeException("Unknown file type: " + extension);
		};


		// OBJ files are special. They need to read in multiple files to get the texture map image and
		// there can be multiple shapes defined. This will handle all those situations
		if (type == PointCloudIO.Format.OBJ) {
			var reader = new ObjLoadFromFiles();
			reader.load(file, mesh);
			return;
		}

		try (var input = new FileInputStream(file)) {
			PointCloudIO.load(type, input, mesh);
		}
	}

	/**
	 * Loads a set of {@link VertexMesh} from an OBJ file. This can ready any type of OBJ file as it doesn't make
	 * assumptions about what is contained inside of it
	 */
	public static Map<String, VertexMesh> loadObj( File file ) {
		var reader = new ObjLoadFromFiles();
		reader.load(file, null);
		return reader.getShapeToMesh();
	}

	/**
	 * Reads a 3D mesh from the input stream in the specified format and writes it to the output.
	 *
	 * @param format Storage format
	 * @param input Input stream
	 * @param mesh (Output) 3D mesh
	 */
	public static void load( Format format, InputStream input, VertexMesh mesh ) throws IOException {
		switch (format) {
			case PLY -> PlyCodec.readMesh(input, mesh);
			case OBJ -> ObjFileCodec.load(input, mesh);
			case STL -> {
				var stlMesh = new StlDataStructure();
				new StlFileReader().readBinary(input, stlMesh);
				stlMesh.toMesh(mesh);
				// NOTE: In the future it might be a good idea to add the capability to read straight into mesh
				//       because large meshes might use up all memory
			}
			default -> throw new RuntimeException("Unknown format");
		}
	}

	/**
	 * The same as {@link #load(Format, InputStream, PointCloudWriter)}, but with a simplified writer that
	 * removes the initialization function. Result is more concise code with less flexibility
	 */
	public static void load( Format format, InputStream input, FunctionalWriter output ) throws IOException {
		PointCloudWriter pcw = new PointCloudWriter() {
			Point3D_F64 location = new Point3D_F64();
			int color = 0;

			@Override public void initialize( int size, boolean hasColor ) {}

			@Override public void startPoint() {}

			@Override public void stopPoint() {
				output.add(location.x, location.y, location.z, color);
			}

			@Override public void location( double x, double y, double z ) {
				this.location.setTo(x, y, z);
			}

			@Override public void color( int rgb ) {
				this.color = rgb;
			}
		};
		load(format, input, pcw);
	}

	/**
	 * A writer without the initialization step. Used to simplify the code
	 */
	@FunctionalInterface
	public interface FunctionalWriter {
		void add( double x, double y, double z, int rgb );
	}

	public enum Format {
		/**
		 * <a href="https://en.wikipedia.org/wiki/PLY_(file_format)">PLY</a>
		 *
		 * @see PlyCodec
		 */
		PLY,
		/**
		 * Save in Wavefront's OBJ file format
		 *
		 * @see ObjFileReader
		 * @see ObjFileWriter
		 */
		OBJ,
		/**
		 * Save in 3D Systems' STL file format
		 *
		 * @see StlFileReader
		 * @see StlFileWriter
		 */
		STL,
	}
}

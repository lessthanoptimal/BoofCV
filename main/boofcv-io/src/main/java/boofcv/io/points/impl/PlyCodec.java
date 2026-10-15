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
import boofcv.io.UtilIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.mesh.VertexMesh;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point3D_F32;
import georegression.struct.point.Point3D_F64;
import georegression.struct.tuples.GeoTuple3D_F64;
import org.ddogleg.struct.DogArray_I32;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/// For reading PLY point files.
public class PlyCodec {
	public static void saveAscii( PlyWriter data, Writer outputWriter ) throws IOException {
		writeAsciiHeader(data.getVertexCount(), data.getPolygonCount(), data.isColor(), data.isTextureCoors(),
				data.getTextureNames(), data.isVertexNormals(), outputWriter);
		boolean faceTextures = isFaceTextures(data.isTextureCoors(), data.getTextureNames());

		boolean color = data.isColor();

		var p = new Point3D_F64();
		var v = new Point3D_F64();
		for (int i = 0; i < data.getVertexCount(); i++) {
			data.getVertex(i, p);
			outputWriter.write(String.format("%.6f %.6f %.6f", p.x, p.y, p.z));
			if (data.isVertexNormals()) {
				data.getVertexNormal(i, v);
				outputWriter.write(String.format(" %f %f %f", v.x, v.y, v.z));
			}
			if (color) {
				int rgb = data.getColor(i);
				int r = rgb >> 16 & 0xFF;
				int g = rgb >> 8 & 0xFF;
				int b = rgb & 0xFF;
				outputWriter.write(String.format(" %d %d %d", r, g, b));
			}
			outputWriter.write('\n');
		}
		var arrayI = new int[100];
		var arrayF = new float[100];

		for (int i = 0; i < data.getPolygonCount(); i++) {
			int size = data.getIndexes(i, arrayI);
			if (size == 0) {
				System.err.println("Warning: Face is malformed and has a size of zero. i=" + i);
				continue;
			}
			outputWriter.write(Integer.toString(size));
			for (int idx = 0; idx < size; idx++) {
				outputWriter.write(" " + arrayI[idx]);
			}

			if (data.isTextureCoors()) {
				BoofMiscOps.checkEq(size, data.getTextureCoors(i, arrayF));
				outputWriter.write(" " + (size*2));
				for (int idx = 0; idx < size*2; idx++) {
					outputWriter.write(String.format(" %.6f", arrayF[idx]));
				}
			}

			if (faceTextures)
				outputWriter.write(" " + data.getFaceTexture(i));

			outputWriter.write('\n');
		}
		outputWriter.flush();
	}

	/// Per-face texture indexes are only saved if there are texture coordinates and more than one texture
	static boolean isFaceTextures( boolean hasTextureCoors, List<String> textureNames ) {
		return hasTextureCoors && textureNames.size() > 1;
	}

	public static void saveMeshAscii( VertexMesh mesh, @Nullable DogArray_I32 colorRGB
			, Writer outputWriter ) throws IOException {
		saveAscii(wrapMeshForWriting(mesh, colorRGB), outputWriter);
	}

	public static void saveCloudAscii( PointCloudReader cloud, boolean saveRgb, Writer outputWriter ) throws IOException {
		saveAscii(wrapCloudForWriting(cloud, saveRgb), outputWriter);
	}

	private static void writeAsciiHeader( int vertexCount, int triangleCount, boolean hasColor, boolean hasTextureCoors,
										  List<String> textureNames, boolean hasNormals, Writer outputWriter )
			throws IOException {
		outputWriter.write("ply\n");
		outputWriter.write("format ascii 1.0\n");
		outputWriter.write("comment Created using BoofCV!\n");
		if (textureNames.isEmpty() && hasTextureCoors)
			System.err.println("Texture file name not specified and it has texture coordinates");
		for (String name : textureNames)
			outputWriter.write("comment TextureFile " + name + "\n");

		outputWriter.write("element vertex " + vertexCount + "\n" +
				"property float x\n" +
				"property float y\n" +
				"property float z\n");
		if (hasNormals) {
			outputWriter.write("""
					property float nx
					property float ny
					property float nz
					""");
		}
		if (hasColor) {
			outputWriter.write("""
					property uchar red
					property uchar green
					property uchar blue
					""");
		}
		if (triangleCount > 0) {
			outputWriter.write("element face " + triangleCount + "\n" +
					"property list uchar int vertex_indices\n");

			if (hasTextureCoors) {
				outputWriter.write("property list uchar float texcoord\n");
			}
			if (isFaceTextures(hasTextureCoors, textureNames)) {
				outputWriter.write("property int texnumber\n");
			}
		}
		outputWriter.write("end_header\n");
	}

	/// Saves data in binary format
	///
	/// @param cloud (Input) Point cloud data
	/// @param order The byte order of the binary data. ByteOrder.BIG\_ENDIAN is recommended
	/// @param saveRgb if true it will save RGB information
	/// @param saveAsFloat if true it will save it as a 4-byte float and if false as an 8-byte double
	/// @param outputWriter Stream it will write to
	public static void saveCloudBinary( PointCloudReader cloud, ByteOrder order, boolean saveRgb, boolean saveAsFloat,
										OutputStream outputWriter ) throws IOException {
		saveBinary(wrapCloudForWriting(cloud, saveRgb), order, saveAsFloat, outputWriter);
	}

	/// Saves data in binary format
	///
	/// @param mesh (Input) Point cloud data
	/// @param colorRGB (Input) color of each vertex
	/// @param saveAsFloat if true it will save it as a 4-byte float and if false as an 8-byte double
	/// @param outputWriter Stream it will write to
	public static void saveMeshBinary( VertexMesh mesh,
									   @Nullable DogArray_I32 colorRGB,
									   ByteOrder order, boolean saveAsFloat, OutputStream outputWriter )
			throws IOException {
		saveBinary(wrapMeshForWriting(mesh, colorRGB), order, saveAsFloat, outputWriter);
	}

	public static void saveBinary( PlyWriter data, ByteOrder order, boolean saveAsFloat, OutputStream outputWriter )
			throws IOException {
		String format = "UTF-8";
		int dataLength = saveAsFloat ? 4 : 8;
		writeBinaryHeader(data.getVertexCount(), data.getPolygonCount(), order, data.isColor(),
				data.isTextureCoors(), data.isVertexNormals(), data.getTextureNames(),
				saveAsFloat, format, outputWriter);
		boolean faceTextures = isFaceTextures(data.isTextureCoors(), data.getTextureNames());

		boolean color = data.isColor();

		int locNorm = dataLength*3;
		int locColor = locNorm + (data.isVertexNormals() ? dataLength*3 : 0);

		var bytes = ByteBuffer.allocate(locColor + (color ? 3 : 0));
		bytes.order(order);
		var p = new Point3D_F64();
		var n = new Point3D_F64();

		for (int i = 0; i < data.getVertexCount(); i++) {
			data.getVertex(i, p);
			if (saveAsFloat) {
				bytes.putFloat(0, (float)p.x);
				bytes.putFloat(4, (float)p.y);
				bytes.putFloat(8, (float)p.z);
			} else {
				bytes.putDouble(0, p.x);
				bytes.putDouble(8, p.y);
				bytes.putDouble(16, p.z);
			}

			if (data.isVertexNormals()) {
				data.getVertexNormal(i, n);
				if (saveAsFloat) {
					bytes.putFloat(locNorm, (float)n.x);
					bytes.putFloat(locNorm + 4, (float)n.y);
					bytes.putFloat(locNorm + 8, (float)n.z);
				} else {
					bytes.putDouble(locNorm, n.x);
					bytes.putDouble(locNorm + 8, n.y);
					bytes.putDouble(locNorm + 16, n.z);
				}
			}

			if (color) {
				int rgb = data.getColor(i);
				int r = rgb >> 16 & 0xFF;
				int g = rgb >> 8 & 0xFF;
				int b = rgb & 0xFF;
				bytes.put(locColor, (byte)r);
				bytes.put(locColor + 1, (byte)g);
				bytes.put(locColor + 2, (byte)b);
			}
			outputWriter.write(bytes.array());
		}

		var arrayI = new int[100];
		var arrayF = new float[100];
		bytes = ByteBuffer.allocate(1 + arrayI.length*4);
		bytes.order(order);
		for (int i = 0; i < data.getPolygonCount(); i++) {
			int size = data.getIndexes(i, arrayI);
			bytes.position(0);
			bytes.put((byte)size);
			for (int idx = 0; idx < size; idx++) {
				bytes.putInt(arrayI[idx]);
			}
			outputWriter.write(bytes.array(), 0, 1 + 4*size);

			if (!data.isTextureCoors())
				continue;

			bytes.position(0);
			BoofMiscOps.checkEq(size, data.getTextureCoors(i, arrayF));
			bytes.put((byte)(2*size));
			for (int idx = 0; idx < size*2; idx++) {
				if (saveAsFloat) {
					bytes.putFloat(arrayF[idx]);
				} else {
					bytes.putDouble(arrayF[idx]);
				}
			}
			outputWriter.write(bytes.array(), 0, 1 + 2*dataLength*size);

			if (faceTextures) {
				bytes.putInt(0, data.getFaceTexture(i));
				outputWriter.write(bytes.array(), 0, 4);
			}
		}
		outputWriter.flush();
	}

	private static void writeBinaryHeader( int vertexCount, int triangleCount, ByteOrder order, boolean hasColor,
										   boolean hasTextureCoors, boolean hasVertexNormals, List<String> textureNames,
										   boolean saveAsFloat, String format, OutputStream outputWriter )
			throws IOException {
		String dataType = saveAsFloat ? "float" : "double";
		outputWriter.write("ply\n".getBytes(format));
		String orderStr = switch (order.toString()) {
			case "LITTLE_ENDIAN" -> "little";
			case "BIG_ENDIAN" -> "big";
			default -> throw new RuntimeException("Unexpected order=" + order);
		};
		outputWriter.write(("format binary_" + orderStr + "_endian 1.0\n").getBytes(format));
		outputWriter.write("comment Created using BoofCV!\n".getBytes(format));

		if (textureNames.isEmpty() && hasTextureCoors)
			System.err.println("Texture file name not specified and it has texture coordinates");
		for (String name : textureNames)
			outputWriter.write(("comment TextureFile " + name + "\n").getBytes(format));

		outputWriter.write(("element vertex " + vertexCount + "\n").getBytes(format));
		outputWriter.write((
				"property " + dataType + " x\n" +
						"property " + dataType + " y\n" +
						"property " + dataType + " z\n").getBytes(format));

		if (hasVertexNormals) {
			outputWriter.write((
					"property " + dataType + " nx\n" +
							"property " + dataType + " ny\n" +
							"property " + dataType + " nz\n").getBytes(format));
		}
		if (hasColor) {
			outputWriter.write(
					"""
							property uchar red
							property uchar green
							property uchar blue
							""".getBytes(format));
		}
		if (triangleCount > 0) {
			outputWriter.write(("element face " + triangleCount + "\n" +
					"property list uchar int vertex_indices\n").getBytes(format));
			if (hasTextureCoors) {
				outputWriter.write(("property list uchar " + dataType + " texcoord\n").getBytes(format));
			}
			if (isFaceTextures(hasTextureCoors, textureNames)) {
				outputWriter.write("property int texnumber\n".getBytes(format));
			}
		}
		outputWriter.write("end_header\n".getBytes(format));
	}

	private static String readNextPly( InputStream reader, boolean failIfNull, StringBuilder buffer ) throws IOException {
		String line = UtilIO.readLine(reader, buffer);
		if (line.isEmpty() && failIfNull)
			throw new IOException("Unexpected end of file");
		return line;
	}

	static void readHeader( InputStream input, Header header ) throws IOException {
		// Assume no triangles by default
		header.triangleCount = 0;

		var buffer = new StringBuilder();

		String line = UtilIO.readLine(input, buffer);
		if (line.isEmpty()) throw new IOException("Missing first line");
		if (line.compareToIgnoreCase("ply") != 0) throw new IOException("Expected PLY at start of file");

		line = readNextPly(input, true, buffer);
		boolean previousVertex = false;
		boolean previousFace = false;
		while (!line.isEmpty()) {
			if (line.startsWith("comment")) {
				if (line.contains("TextureFile")) {
					int start = "comment TextureFile ".length();
					header.textureNames.add(line.substring(start));
				}
				line = readNextPly(input, true, buffer);
				continue;
			}

			if (line.equals("end_header"))
				break;
			String[] words = line.split("\\s+");
			if (words.length == 1)
				throw new IOException("Expected more than one word");
			if (line.startsWith("format")) {
				header.format = switch (words[1]) {
					case "ascii" -> Format.ASCII;
					case "binary_little_endian" -> Format.BINARY_LITTLE;
					case "binary_big_endian" -> Format.BINARY_BIG;
					default -> throw new IOException("Unknown format " + words[1]);
				};
			} else if (line.startsWith("element")) {
				previousVertex = false;
				previousFace = false;
				header.elements.add(new Element(words[1], Integer.parseInt(words[2])));
				if (words[1].equals("vertex")) {
					previousVertex = true;
					header.vertexCount = Integer.parseInt(words[2]);
				} else if (words[1].equals("face")) {
					previousFace = true;
					header.triangleCount = Integer.parseInt(words[2]);
				}
			} else if (words[0].equals("property") && header.elements.isEmpty()) {
				throw new IOException("Property before any element: '" + line + "'");
			} else if (words[0].equals("property") && !previousVertex && !previousFace) {
				// An element that is not read. Its properties are recorded so its records can be skipped
				Element element = header.elements.get(header.elements.size() - 1);
				if (words[1].equals("list")) {
					if (words.length != 5)
						throw new IOException("Unexpected number of words in list property: '" + line + "'");
					element.properties.add(new PropertyList(words[4], DataType.parse(words[2]), DataType.parse(words[3])));
				} else {
					element.properties.add(new PropertyList(words[2], null, DataType.parse(words[1])));
				}
			} else if (words[0].equals("property") && previousVertex) {
				if (words[1].equals("list"))
					throw new IOException("List properties on vertexes are not supported: '" + line + "'");
				DataType d = DataType.parse(words[1]);
				VarType v;
				switch (words[2].toLowerCase(Locale.US)) {
					case "x" -> v = VarType.X;
					case "y" -> v = VarType.Y;
					case "z" -> v = VarType.Z;
					case "nx" -> v = VarType.NX;
					case "ny" -> v = VarType.NY;
					case "nz" -> v = VarType.NZ;
					case "red" -> v = VarType.R;
					case "green" -> v = VarType.G;
					case "blue" -> v = VarType.B;
					default -> v = VarType.UNKNOWN;
				}
				header.dataWords.add(new DataWord(v, d));
			} else if (words[0].equals("property") && words[1].equals("list")) {
				if (words.length != 5) {
					throw new IOException("Unexpected number of words in properties. Unsupported file format? count=" + words.length);
				}

				// Parse the data type info
				var countType = DataType.parse(words[2]);
				var valueType = DataType.parse(words[3]);

				header.properties.add(new PropertyList(words[4], countType, valueType));
			} else if (words[0].equals("property") && previousFace && words.length == 3) {
				// A scalar on a face, e.g. Metashape's texnumber
				header.properties.add(new PropertyList(words[2], null, DataType.parse(words[1])));
			} else {
				throw new IOException("Unknown header element: '" + line + "'");
			}
			line = readNextPly(input, true, buffer);
		}

		// See what type of data is contained
		for (int i = 0; i < header.dataWords.size(); i++) {
			PlyCodec.DataWord w = header.dataWords.get(i);
			switch (w.var) {
				case R, G, B -> header.rgb = true;
				case NX, NY, NZ -> header.normals = true;
				default -> {
				}
			}
		}
	}

	public static void readCloud( InputStream input, PointCloudWriter output ) throws IOException {
		read(input, new PlyReader() {
			@Override public void initialize( int vertexes, int triangles, boolean color ) {
				output.initialize(vertexes, color);
			}

			@Override public void startVertex() {
				output.startPoint();
			}

			@Override public void stopVertex() {
				output.stopPoint();
			}

			@Override public void setVertexLocation( double x, double y, double z ) {
				output.location(x, y, z);
			}

			@Override public void setVertexColor( int r, int g, int b ) {
				output.color(r << 16 | g << 8 | b);
			}

			@Override public void setVertexNormal( double nx, double ny, double nz ) {}

			@Override public void addPolygon( int[] indexes, int offset, int length ) {}

			@Override public void addTextureName( String textureName ) {}

			@Override public void addTexture( int count, float[] coor ) {}

			@Override public void addFaceTexture( int index ) {}
		});
	}

	public static void readMesh( InputStream input, VertexMesh mesh ) throws IOException {
		read(input, new PlyReader() {
			@Override public void initialize( int vertexes, int triangles, boolean color ) {
				mesh.reset();
				mesh.vertexes.reserve(vertexes);
				mesh.faceVertexes.reserve(triangles*3);
			}

			@Override public void startVertex() {}

			@Override public void stopVertex() {}

			@Override public void setVertexLocation( double x, double y, double z ) {
				mesh.vertexes.append(x, y, z);
			}

			@Override public void setVertexColor( int r, int g, int b ) {
				mesh.rgb.add(r << 16 | g << 8 | b);
			}

			@Override public void setVertexNormal( double nx, double ny, double nz ) {
				mesh.normals.append((float)nx, (float)ny, (float)nz);
			}

			@Override public void addPolygon( int[] indexes, int offset, int length ) {
				mesh.faceOffsets.add(mesh.faceVertexes.size + length);
				mesh.faceVertexes.addAll(indexes, offset, offset + length);
			}

			@Override public void addTextureName( String textureName ) {
				mesh.textureNames.add(textureName);
			}

			@Override public void addTexture( int count, float[] coor ) {mesh.addTexture(count, coor);}

			@Override public void addFaceTexture( int index ) {mesh.faceTextures.add(index);}
		});
	}

	public static void read( InputStream input, PlyReader output ) throws IOException {
		var header = new Header();
		readHeader(input, header);

		if (header.vertexCount == -1)
			throw new IOException("File is missing vertex count");
		if (header.triangleCount == -1)
			throw new IOException("Triangle count was not properly initialized");
		if (header.format == null)
			throw new IOException("Format is never specified");

		output.initialize(header.vertexCount, header.triangleCount, header.rgb);
		for (String name : header.textureNames)
			output.addTextureName(name);

		// Elements are read in the order they were declared. Ones that are not understood are skipped
		boolean ascii = header.format == Format.ASCII;
		var buffer = new StringBuilder();
		for (Element element : header.elements) {
			switch (element.name) {
				case "vertex" -> {
					if (ascii) readVerticesAscii(output, input, header, buffer);
					else readVerticesBinary(output, input, header);
				}
				case "face" -> {
					if (ascii) readFacesAscii(output, input, header, buffer);
					else readFacesBinary(output, input, header);
				}
				default -> {
					if (ascii) {
						for (int i = 0; i < element.count; i++)
							readNextPly(input, true, buffer);
					} else {
						skipElementBinary(input, header, element);
					}
				}
			}
		}
	}

	/// Skips every record of an element that is not used. Lists are skipped by reading their count
	private static void skipElementBinary( InputStream reader, Header header, Element element ) throws IOException {
		var bb = ByteBuffer.allocate(8).order(header.byteOrder());
		for (int i = 0; i < element.count; i++) {
			for (PropertyList prop : element.properties) {
				if (prop.countType == null) {
					skipBytes(reader, prop.valueType.size);
				} else {
					skipList(reader, bb, prop);
				}
			}
		}
	}

	/// Reads a list's count then skips its values
	private static void skipList( InputStream reader, ByteBuffer bb, PropertyList prop ) throws IOException {
		long count = readIntegerBinary(reader, bb, Objects.requireNonNull(prop.countType));
		skipBytes(reader, count*prop.valueType.size);
	}

	/// Reads a single binary integer of the specified type
	private static long readIntegerBinary( InputStream reader, ByteBuffer bb, DataType type ) throws IOException {
		readFully(reader, bb.array(), type.size);
		return switch (type) {
			case CHAR -> bb.get(0);
			case UCHAR -> bb.get(0) & 0xFF;
			case SHORT -> bb.getShort(0);
			case USHORT -> bb.getShort(0) & 0xFFFF;
			case INT, UINT -> bb.getInt(0) & 0xFFFFFFFFL;
			default -> throw new IOException("Must be an integer type, not " + type);
		};
	}

	private static void skipBytes( InputStream reader, long count ) throws IOException {
		while (count > 0) {
			long skipped = reader.skip(count);
			if (skipped <= 0) {
				if (reader.read() < 0)
					throw new IOException("Unexpected end of file");
				skipped = 1;
			}
			count -= skipped;
		}
	}

	private static void readFully( InputStream reader, byte[] data, int length ) throws IOException {
		int total = 0;
		while (total < length) {
			int found = reader.read(data, total, length - total);
			if (found < 0)
				throw new IOException("Unexpected end of file");
			total += found;
		}
	}

	private static void readVerticesAscii( PlyReader output, InputStream reader, Header header, StringBuilder buffer )
			throws IOException {

		// storage for read in values
		int I32 = -1;
		double F64 = -1;

		// values that are writen to that we care about
		int r = 0, g = 0, b = 0;
		double x = -1, y = -1, z = -1;
		double nx = -1, ny = -1, nz = -1;

		for (int i = 0; i < header.vertexCount; i++) {
			String line = readNextPly(reader, true, buffer);
			String[] words = line.split("\\s+");
			if (words.length != header.dataWords.size())
				throw new IOException("unexpected number of words. " + line);

			for (int j = 0; j < header.dataWords.size(); j++) {
				DataWord d = header.dataWords.get(j);
				String word = words[j];
				switch (d.data) {
					case FLOAT, DOUBLE -> F64 = Double.parseDouble(word);
					case UINT, INT, USHORT, SHORT, UCHAR, CHAR -> I32 = Integer.parseInt(word);
					default -> throw new RuntimeException("Unsupported");
				}
				switch (d.var) {
					case X -> x = F64;
					case Y -> y = F64;
					case Z -> z = F64;
					case NX -> nx = F64;
					case NY -> ny = F64;
					case NZ -> nz = F64;
					case R -> r = I32;
					case G -> g = I32;
					case B -> b = I32;
					default -> {
					}
				}
			}

			// Creates the vertex and specify its attributes
			output.startVertex();
			output.setVertexLocation(x, y, z);
			if (header.rgb)
				output.setVertexColor(r, g, b);
			if (header.normals) {
				output.setVertexNormal(nx, ny, nz);
			}
			output.stopVertex();
		}
	}

	private static void readFacesAscii( PlyReader output, InputStream reader, Header header, StringBuilder buffer )
			throws IOException {
		var indexes = new int[100];
		var texture = new float[100];
		for (int idxTriangle = 0; idxTriangle < header.triangleCount; idxTriangle++) {
			String line = readNextPly(reader, true, buffer);
			String[] words = line.split("\\s+");
			int idxWord = 0;

			for (int idxProperty = 0; idxProperty < header.properties.size(); idxProperty++) {
				PropertyList prop = header.properties.get(idxProperty);
				if (prop.countType == null) {
					if (prop.label.equals("texnumber"))
						output.addFaceTexture(Integer.parseInt(words[idxWord]));
					idxWord++;
					continue;
				}
				int n = Integer.parseInt(words[idxWord++]);
				if (words.length < n + idxWord) {
					throw new RuntimeException("Unexpected number of words. " + words.length + " vs " + (n + 1));
				}

				switch (prop.label) {
					case "vertex_indices" -> {
						for (int i = 0; i < n; i++) {
							indexes[i] = Integer.parseInt(words[idxWord++]);
							checkVertexIndex(indexes[i], header.vertexCount);
						}
						output.addPolygon(indexes, 0, n);
					}
					case "texcoord" -> {
						for (int i = 0; i < n; i++) {
							texture[i] = Float.parseFloat(words[idxWord++]);
						}
						output.addTexture(n/2, texture);
					}
					default -> idxWord += n;
				}
			}
		}
	}

	private static void readVerticesBinary( PlyReader output, InputStream reader, Header header ) throws IOException {
		int countVertexBytes = 0;
		for (int i = 0; i < header.dataWords.size(); i++) {
			countVertexBytes += header.dataWords.get(i).data.size;
		}

		// Guess the number of vertexes in a polygon so we can estimate array size
		int maxPolygonOrder = 10;

		// Go through all the properties and see what the maximum size will be for those
		int countBytes = countVertexBytes;
		for (int i = 0; i < header.properties.size(); i++) {
			int countPropBytes = header.properties.get(i).valueType.size*maxPolygonOrder;
			countBytes = Math.max(countBytes, countPropBytes);
		}

		final byte[] line = new byte[countBytes];
		final ByteBuffer bb = ByteBuffer.wrap(line);
		bb.order(header.byteOrder());

		// storage for read in values
		int I32 = -1;
		double F64 = -1;

		// values that are writen to that we care about
		int r = -1, g = -1, b = -1;
		double x = -1, y = -1, z = -1;
		double nx = -1, ny = -1, nz = -1;

		for (int i = 0; i < header.vertexCount; i++) {
			int found = reader.read(line, 0, countVertexBytes);
			if (countVertexBytes != found)
				throw new IOException("Read unexpected number of bytes. " + found + " vs " + countVertexBytes);

			int location = 0;

			for (int j = 0; j < header.dataWords.size(); j++) {
				DataWord d = header.dataWords.get(j);
				switch (d.data) {
					case FLOAT -> F64 = bb.getFloat(location);
					case DOUBLE -> F64 = bb.getDouble(location);
					case CHAR -> I32 = bb.get(location);
					case UCHAR -> I32 = bb.get(location) & 0xFF;
					case SHORT -> I32 = bb.getShort(location);
					case USHORT -> I32 = bb.getShort(location) & 0xFFFF;
					case INT -> I32 = bb.getInt(location);
					case UINT -> I32 = bb.getInt(location); // NOTE: not really uint...
					default -> throw new RuntimeException("Unsupported");
				}
				location += d.data.size;
				switch (d.var) {
					case X -> x = F64;
					case Y -> y = F64;
					case Z -> z = F64;
					case NX -> nx = F64;
					case NY -> ny = F64;
					case NZ -> nz = F64;
					case R -> r = I32;
					case G -> g = I32;
					case B -> b = I32;
					default -> {
					}
				}
			}

			// Creates the vertex and specify its attributes
			output.startVertex();
			output.setVertexLocation(x, y, z);
			if (header.rgb)
				output.setVertexColor(r, g, b);
			if (header.normals) {
				output.setVertexNormal(nx, ny, nz);
			}
			output.stopVertex();
		}
	}

	private static void readFacesBinary( PlyReader output, InputStream reader, Header header ) throws IOException {
		// Large enough for a polygon of up to 100 vertexes and any property's values
		int countBytes = 8;
		for (int i = 0; i < header.properties.size(); i++) {
			countBytes = Math.max(countBytes, header.properties.get(i).valueType.size*100);
		}
		final byte[] line = new byte[countBytes];
		final ByteBuffer bb = ByteBuffer.wrap(line);
		bb.order(header.byteOrder());

		var arrayI = new int[100];
		var arrayF = new float[100];
		for (int i = 0; i < header.triangleCount; i++) {
			for (int idxProperty = 0; idxProperty < header.properties.size(); idxProperty++) {
				PropertyList prop = header.properties.get(idxProperty);
				if (prop.countType == null) {
					switch (prop.label) {
						case "texnumber" -> output.addFaceTexture((int)readIntegerBinary(reader, bb, prop.valueType));
						default -> skipBytes(reader, prop.valueType.size);
					}
					continue;
				}
				switch (prop.label) {
					case "vertex_indices" -> {
						checkUCharCount(prop);
						readPolygon(reader, bb, line, arrayI, header.vertexCount, output);
					}
					case "texcoord" -> {
						checkUCharCount(prop);
						readTextureCoor(reader, prop.valueType, bb, line, arrayF, output);
					}
					default -> skipList(reader, bb, prop);
				}
			}
		}
	}

	/// Throws an exception if the vertex index is out of range
	static void checkVertexIndex( int index, int vertexCount ) throws IOException {
		if (index < 0 || index >= vertexCount)
			throw new IOException("Vertex index out of range. index=" + index + " count=" + vertexCount);
	}

	private static void checkUCharCount( PropertyList prop ) throws IOException {
		if (prop.countType != DataType.UCHAR)
			throw new IOException("Expected unsigned byte for count type, not " + prop.countType);
	}

	private static void readPolygon( InputStream reader, ByteBuffer bb, byte[] lineBytes, int[] indexes, int vertexCount,
									 PlyReader output ) throws IOException {
		if (1 != reader.read(lineBytes, 0, 1))
			throw new RuntimeException("Couldn't read count byte");

		int count = lineBytes[0] & 0xFF;
		int bytesInLine = count*4;
		if (lineBytes.length < bytesInLine)
			throw new RuntimeException("polygonLine is too small. allocated=" + lineBytes.length + " required=" + bytesInLine + " degree=" + count);

		int found = reader.read(lineBytes, 0, bytesInLine);
		if (found != bytesInLine)
			throw new IOException("Read unexpected number of bytes. " + found + " vs " + bytesInLine);

		for (int wordIndex = 0; wordIndex < count; wordIndex++) {
			indexes[wordIndex] = bb.getInt(wordIndex*4);
			checkVertexIndex(indexes[wordIndex], vertexCount);
		}

		output.addPolygon(indexes, 0, count);
	}

	private static void readTextureCoor( InputStream reader, DataType valueType, ByteBuffer bb, byte[] workBytes,
										 float[] tempF, PlyReader output ) throws IOException {
		if (1 != reader.read(workBytes, 0, 1))
			throw new RuntimeException("Couldn't read count byte");

		// Number of bytes in the array
		int count = workBytes[0] & 0xFF;

		// Length of array given the specified data type
		int bytesToRead = count*valueType.size;

		if (workBytes.length < bytesToRead)
			throw new RuntimeException("workspace too small to read in array of " + valueType.name() + " count=" + count);

		int found = reader.read(workBytes, 0, bytesToRead);
		if (found != bytesToRead)
			throw new IOException("Read unexpected number of bytes. " + found + " vs " + bytesToRead);

		for (int wordIndex = 0; wordIndex < count; wordIndex++) {
			switch (valueType) {
				case FLOAT -> tempF[wordIndex] = bb.getFloat(wordIndex*valueType.size);
				case DOUBLE -> tempF[wordIndex] = (float)bb.getDouble(wordIndex*valueType.size);
				default -> throw new RuntimeException("Unexpected texcoord value type " + valueType);
			}
		}
		output.addTexture(count/2, tempF);
	}

	static PlyWriter wrapMeshForWriting( VertexMesh mesh, @Nullable DogArray_I32 colorRGB ) {
		return new PlyWriter() {
			@Override public int getVertexCount() {return mesh.vertexes.size();}

			@Override public int getPolygonCount() {return mesh.faceOffsets.size - 1;}

			@Override public boolean isColor() {return colorRGB != null;}

			@Override public boolean isTextureCoors() {
				return mesh.texture.size() > 0;
			}

			@Override public boolean isVertexNormals() {
				return mesh.normals.size() > 0;
			}

			@Override public List<String> getTextureNames() {return mesh.textureNames;}

			@Override public int getFaceTexture( int which ) {
				// Empty means every face uses the first texture
				if (mesh.faceTextures.isEmpty())
					return 0;
				return mesh.faceTextures.get(which);
			}

			@Override public void getVertex( int which, Point3D_F64 vertex ) {mesh.vertexes.getCopy(which, vertex);}

			@Override public void getVertexNormal( int which, GeoTuple3D_F64<?> normal ) {
				Point3D_F32 tmp = mesh.normals.getTemp(which);
				normal.setTo(tmp.x, tmp.y, tmp.z);
			}

			@SuppressWarnings("NullAway")
			@Override public int getColor( int which ) {return colorRGB.get(which);}

			@Override public int getIndexes( int which, int[] indexes ) {
				int idx0 = mesh.faceOffsets.get(which);
				int idx1 = mesh.faceOffsets.get(which + 1);

				for (int i = idx0; i < idx1; i++) {
					indexes[i - idx0] = mesh.faceVertexes.get(i);
				}

				return idx1 - idx0;
			}

			@Override public int getTextureCoors( int which, float[] coordinates ) {
				int idx0 = mesh.faceOffsets.get(which);
				int idx1 = mesh.faceOffsets.get(which + 1);

				if (mesh.faceVertexTextures.isEmpty()) {
					int idxArray = 0;
					for (int i = idx0; i < idx1; i++) {
						Point2D_F32 p = mesh.texture.getTemp(i);
						coordinates[idxArray++] = p.x;
						coordinates[idxArray++] = p.y;
					}
				} else {
					int idxArray = 0;
					for (int i = idx0; i < idx1; i++) {
						int idxTexture = mesh.faceVertexTextures.get(i);
						Point2D_F32 p = mesh.texture.getTemp(idxTexture);
						coordinates[idxArray++] = p.x;
						coordinates[idxArray++] = p.y;
					}
				}

				return idx1 - idx0;
			}
		};
	}

	private static PlyWriter wrapCloudForWriting( PointCloudReader cloud, boolean saveRgb ) {
		return new PlyWriter() {
			@Override public int getVertexCount() {return cloud.size();}

			@Override public int getPolygonCount() {return 0;}

			@Override public boolean isColor() {return saveRgb;}

			@Override public boolean isTextureCoors() {return false;}

			@Override public boolean isVertexNormals() {return false;}

			@Override public List<String> getTextureNames() {return List.of();}

			@Override public int getFaceTexture( int which ) {return 0;}

			@Override public void getVertex( int which, Point3D_F64 vertex ) {cloud.get(which, vertex);}

			@Override public void getVertexNormal( int which, GeoTuple3D_F64<?> normal ) {}

			@Override public int getColor( int which ) {return cloud.getRGB(which);}

			@Override public int getIndexes( int which, int[] indexes ) {return 0;}

			@Override public int getTextureCoors( int which, float[] coordinates ) {return 0;}
		};
	}

	static class Header {
		List<DataWord> dataWords = new ArrayList<>();
		int vertexCount = -1;
		int triangleCount = -1;
		/// true of rgb colors for each vertex is encoded
		boolean rgb = false;
		/// true if normals for each shape is encoded
		boolean normals = false;
		List<PropertyList> properties = new ArrayList<>();
		/// Every element in the order they appear. Properties are filled in for ones that are skipped
		List<Element> elements = new ArrayList<>();
		Format format = Format.ASCII;

		ByteOrder byteOrder() {
			return format == Format.BINARY_LITTLE ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
		}

		List<String> textureNames = new ArrayList<>();
	}

	static class PropertyList {
		/// Property name
		String label;

		/// What the number of elements is stored in. Null for a scalar property
		@Nullable DataType countType;

		/// Array value data type
		DataType valueType;

		PropertyList( String label, @Nullable DataType countType, DataType valueType ) {
			this.label = label;
			this.countType = countType;
			this.valueType = valueType;
		}
	}

	private static class Element {
		final String name;
		final int count;
		final List<PropertyList> properties = new ArrayList<>();

		Element( String name, int count ) {
			this.name = name;
			this.count = count;
		}
	}

	private static class DataWord {
		VarType var;
		DataType data;

		DataWord( VarType var, DataType data ) {
			this.var = var;
			this.data = data;
		}
	}

	private enum VarType {
		X, Y, Z, R, G, B, NX, NY, NZ, UNKNOWN
	}

	private enum DataType {
		FLOAT(4),
		DOUBLE(8),
		CHAR(1),
		SHORT(2),
		INT(4),
		UCHAR(1),
		USHORT(2),
		UINT(4);

		final int size;

		DataType( int size ) {
			this.size = size;
		}

		/// Accepts both the original names and the sized aliases, e.g. int8 or float32
		static DataType parse( String name ) throws IOException {
			return switch (name.toLowerCase(Locale.US)) {
				case "float", "float32" -> FLOAT;
				case "double", "float64" -> DOUBLE;
				case "char", "int8" -> CHAR;
				case "uchar", "uint8" -> UCHAR;
				case "short", "int16" -> SHORT;
				case "ushort", "uint16" -> USHORT;
				case "int", "int32" -> INT;
				case "uint", "uint32" -> UINT;
				default -> throw new IOException("Unknown PLY data type '" + name + "'");
			};
		}
	}

	private enum Format {
		ASCII,
		BINARY_LITTLE,
		BINARY_BIG
	}
}

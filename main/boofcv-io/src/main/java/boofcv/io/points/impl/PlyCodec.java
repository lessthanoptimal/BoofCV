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
import boofcv.io.UtilIO;
import georegression.struct.point.Point3D_F64;
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

/**
 * For reading PLY point files.
 *
 * @author Peter Abeles
 */
public class PlyCodec {
	public static void saveAscii( PlyWriter data, Writer outputWriter ) throws IOException {
		writeAsciiHeader(data.getVertexCount(), data.getPolygonCount(), data.isColor(), outputWriter);

		boolean color = data.isColor();

		Point3D_F64 p = new Point3D_F64();
		for (int i = 0; i < data.getVertexCount(); i++) {
			data.getVertex(i, p);
			if (color) {
				int rgb = data.getColor(i);
				int r = (rgb >> 16) & 0xFF;
				int g = (rgb >> 8) & 0xFF;
				int b = rgb & 0xFF;
				outputWriter.write(String.format("%f %f %f %d %d %d\n", p.x, p.y, p.z, r, g, b));
			} else {
				outputWriter.write(String.format("%f %f %f\n", p.x, p.y, p.z));
			}
		}
		int[] indexes = new int[100];
		for (int i = 0; i < data.getPolygonCount(); i++) {
			int size = data.getIndexes(i, indexes);
			outputWriter.write(size);
			for (int idxidx = 0; idxidx < size; idxidx++) {
				outputWriter.write(" " + indexes[idxidx]);
			}
			outputWriter.write('\n');
		}
		outputWriter.flush();
	}

	public static void saveMeshAscii( VertexMesh mesh, @Nullable DogArray_I32 colorRGB, Writer outputWriter ) throws IOException {
		saveAscii(wrapMeshForWriting(mesh, colorRGB), outputWriter);
	}

	public static void saveCloudAscii( PointCloudReader cloud, boolean saveRgb, Writer outputWriter ) throws IOException {
		saveAscii(wrapCloudForWriting(cloud, saveRgb), outputWriter);
	}

	private static void writeAsciiHeader( int vertexCount, int triangleCount, boolean hasColor, Writer outputWriter )
			throws IOException {
		outputWriter.write("ply\n");
		outputWriter.write("format ascii 1.0\n");
		outputWriter.write("comment Created using BoofCV!\n");
		outputWriter.write("element vertex " + vertexCount + "\n" +
				"property float x\n" +
				"property float y\n" +
				"property float z\n");
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
		}
		outputWriter.write("end_header\n");
	}

	/**
	 * Saves data in binary format
	 *
	 * @param cloud (Input) Point cloud data
	 * @param order The byte order of the binary data. ByteOrder.BIG_ENDIAN is recommended
	 * @param saveRgb if true it will save RGB information
	 * @param saveAsFloat if true it will save it as a 4-byte float and if false as an 8-byte double
	 * @param outputWriter Stream it will write to
	 */
	public static void saveCloudBinary( PointCloudReader cloud, ByteOrder order, boolean saveRgb, boolean saveAsFloat,
										OutputStream outputWriter ) throws IOException {
		saveBinary(wrapCloudForWriting(cloud, saveRgb), order, saveAsFloat, outputWriter);
	}

	/**
	 * Saves data in binary format
	 *
	 * @param mesh (Input) Point cloud data
	 * @param colorRGB (Input) color of each vertex
	 * @param saveAsFloat if true it will save it as a 4-byte float and if false as an 8-byte double
	 * @param outputWriter Stream it will write to
	 */
	public static void saveMeshBinary( VertexMesh mesh, @Nullable DogArray_I32 colorRGB,
									   ByteOrder order, boolean saveAsFloat, OutputStream outputWriter )
			throws IOException {
		saveBinary(wrapMeshForWriting(mesh, colorRGB), order, saveAsFloat, outputWriter);
	}

	public static void saveBinary( PlyWriter data, ByteOrder order, boolean saveAsFloat, OutputStream outputWriter )
			throws IOException {
		String format = "UTF-8";
		int dataLength = saveAsFloat ? 4 : 8;
		writeBinaryHeader(data.getVertexCount(), data.getPolygonCount(), order, data.isColor(), saveAsFloat, format, outputWriter);

		boolean color = data.isColor();

		int end = dataLength*3;
		var bytes = ByteBuffer.allocate(dataLength*3 + (color ? 3 : 0));
		bytes.order(order);
		var p = new Point3D_F64();
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

			if (color) {
				int rgb = data.getColor(i);
				int r = (rgb >> 16) & 0xFF;
				int g = (rgb >> 8) & 0xFF;
				int b = rgb & 0xFF;
				bytes.put(end, (byte)r);
				bytes.put(end + 1, (byte)g);
				bytes.put(end + 2, (byte)b);
			}
			outputWriter.write(bytes.array());
		}

		var indexes = new int[100];
		bytes = ByteBuffer.allocate(1 + indexes.length*4);
		for (int i = 0; i < data.getPolygonCount(); i++) {
			int size = data.getIndexes(i, indexes);
			bytes.position(0);
			bytes.put((byte)size);
			for (int idx = 0; idx < indexes.length; idx++) {
				bytes.putInt(indexes[idx]);
			}
			outputWriter.write(bytes.array(), 0, 1 + 4*size);
		}
		outputWriter.flush();
	}

	private static void writeBinaryHeader( int vertexCount, int triangleCount, ByteOrder order, boolean hasColor,
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
		outputWriter.write(("element vertex " + vertexCount + "\n").getBytes(format));
		outputWriter.write((
				"property " + dataType + " x\n" +
						"property " + dataType + " y\n" +
						"property " + dataType + " z\n").getBytes(format));
		if (hasColor) {
			outputWriter.write(
					("property uchar red\n" +
							"property uchar green\n" +
							"property uchar blue\n").getBytes(format));
		}
		if (triangleCount > 0) {
			outputWriter.write(("element face " + triangleCount + "\n" +
					"property list uchar int vertex_indices\n").getBytes(format));
		}
		outputWriter.write("end_header\n".getBytes(format));
	}

	private static String readNextPly( InputStream reader, boolean failIfNull, StringBuilder buffer ) throws IOException {
		String line = UtilIO.readLine(reader, buffer);
		while (line.length() != 0) {
			if (line.startsWith("comment"))
				line = UtilIO.readLine(reader, buffer);
			else {
				return line;
			}
		}
		if (failIfNull)
			throw new IOException("Unexpected end of file");
		return line;
	}

	private static void readHeader( InputStream input, Header header ) throws IOException {
		var buffer = new StringBuilder();

		String line = UtilIO.readLine(input, buffer);
		if (line.length() == 0) throw new IOException("Missing first line");
		if (line.compareToIgnoreCase("ply") != 0) throw new IOException("Expected PLY at start of file");

		line = readNextPly(input, true, buffer);
		boolean previousVertex = false;
		while (line.length() != 0) {
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
				if (words[1].equals("vertex")) {
					previousVertex = true;
					header.vertexCount = Integer.parseInt(words[2]);
				} else if (words[1].equals("face")) {
					header.triangleCount = Integer.parseInt(words[2]);
				}
			} else if (words[0].equals("property") && previousVertex) {
				DataType d = switch (words[1].toLowerCase()) {
					case "float" -> DataType.FLOAT;
					case "double" -> DataType.DOUBLE;
					case "char" -> DataType.CHAR;
					case "short" -> DataType.SHORT;
					case "int" -> DataType.INT;
					case "uchar" -> DataType.UCHAR;
					case "ushort" -> DataType.USHORT;
					case "uint" -> DataType.UINT;
					default -> throw new RuntimeException("Add support for " + words[1]);
				};
				VarType v;
				switch (words[2].toLowerCase()) {
					case "x" -> v = VarType.X;
					case "y" -> v = VarType.Y;
					case "z" -> v = VarType.Z;
					case "red" -> {
						v = VarType.R;
						header.rgb = true;
					}
					case "green" -> {
						v = VarType.G;
						header.rgb = true;
					}
					case "blue" -> {
						v = VarType.B;
						header.rgb = true;
					}
					default -> v = VarType.UNKNOWN;
				}
				header.dataWords.add(new DataWord(v, d));
			} else if (words[0].equals("property") && words[1].equals("list")) {
				// just ignore it. Previous line specified number of elements
			} else {
				throw new IOException("Unknown header element: '" + line + "'");
			}
			line = readNextPly(input, true, buffer);
		}
	}

	public static void readCloud( InputStream input, PointCloudWriter output ) throws IOException {
		read(input, new PlyReader() {
			@Override public void initialize( int vertexes, int triangles, boolean color ) {
				output.initialize(vertexes, color);
			}

			@Override public void addVertex( double x, double y, double z, int rgb ) {
				output.add(x, y, z, rgb);
			}

			@Override public void addPolygon( int[] indexes, int offset, int length ) {}
		});
	}

	public static void readMesh( InputStream input, VertexMesh mesh, DogArray_I32 colorRGB ) throws IOException {
		read(input, new PlyReader() {
			@Override public void initialize( int vertexes, int triangles, boolean color ) {
				colorRGB.reset();
				mesh.vertexes.reset();
				mesh.indexes.reset();
				mesh.vertexes.reserve(vertexes);
				mesh.indexes.reserve(triangles*3);
				mesh.offsets.add(0);
			}

			@Override public void addVertex( double x, double y, double z, int rgb ) {
				mesh.vertexes.append(x, y, z);
				colorRGB.add(rgb);
			}

			@Override public void addPolygon( int[] indexes, int offset, int length ) {
				mesh.offsets.add(mesh.indexes.size + length);
				mesh.indexes.addAll(indexes, offset, offset + length);
			}
		});
	}

	public static void read( InputStream input, PlyReader output ) throws IOException {
		Header header = new Header();
		readHeader(input, header);

		if (header.vertexCount == -1)
			throw new IOException("File is missing vertex count");
		if (header.format == null)
			throw new IOException("Format is never specified");

		output.initialize(header.vertexCount, header.triangleCount, header.rgb);

		switch (header.format) {
			case ASCII -> readAscii(output, input, header.dataWords, header.vertexCount,
					header.rgb, header.triangleCount);
			case BINARY_LITTLE -> readCloudBinary(output, input, header.dataWords,
					ByteOrder.LITTLE_ENDIAN, header.vertexCount, header.rgb, header.triangleCount);
			case BINARY_BIG -> readCloudBinary(output, input, header.dataWords,
					ByteOrder.BIG_ENDIAN, header.vertexCount, header.rgb, header.triangleCount);
			default -> throw new RuntimeException("BUG!");
		}
	}

	private static void readAscii( PlyReader output, InputStream reader, List<DataWord> dataWords,
								   int vertexCount, boolean rgb, int triangleCount ) throws IOException {
		var buffer = new StringBuilder();

		// storage for read in values
		int I32 = -1;
		double F64 = -1;

		// values that are writen to that we care about
		int r = 0, g = 0, b = 0;
		double x = -1, y = -1, z = -1;

		for (int i = 0; i < vertexCount; i++) {
			String line = readNextPly(reader, true, buffer);
			String[] words = line.split("\\s+");
			if (words.length != dataWords.size())
				throw new IOException("unexpected number of words. " + line);

			for (int j = 0; j < dataWords.size(); j++) {
				DataWord d = dataWords.get(j);
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
					case R -> r = I32;
					case G -> g = I32;
					case B -> b = I32;
					default -> {
					}
				}
			}
			output.addVertex(x, y, z, r << 16 | g << 8 | b);
		}

		int[] indexes = new int[100];
		for (int i = 0; i < triangleCount; i++) {
			String line = readNextPly(reader, true, buffer);
			String[] words = line.split("\\s+");
			int n = Integer.parseInt(words[0]);
			if (words.length != n + 1) {
				throw new RuntimeException("Unexpected number of words.");
			}
			for (int wordIdx = 1; wordIdx <= n; wordIdx++) {
				indexes[wordIdx - 1] = Integer.parseInt(words[i]);
			}

			output.addPolygon(indexes, 0, n);
		}
	}

	private static void readCloudBinary( PlyReader output, InputStream reader, List<DataWord> dataWords,
										 ByteOrder order,
										 int vertexCount, boolean rgb, int triangleCount ) throws IOException {

		int totalBytes = 0;
		for (int i = 0; i < dataWords.size(); i++) {
			totalBytes += dataWords.get(i).data.size;
		}

		final byte[] line = new byte[totalBytes];
		final ByteBuffer bb = ByteBuffer.wrap(line);
		bb.order(order);

		// storage for read in values
		int I32 = -1;
		double F64 = -1;

		// values that are writen to that we care about
		int r = -1, g = -1, b = -1;
		double x = -1, y = -1, z = -1;

		for (int i = 0; i < vertexCount; i++) {
			int found = reader.read(line);
			if (line.length != found)
				throw new IOException("Read unexpected number of bytes. " + found + " vs " + line.length);

			int location = 0;

			for (int j = 0; j < dataWords.size(); j++) {
				DataWord d = dataWords.get(j);
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
					case R -> r = I32;
					case G -> g = I32;
					case B -> b = I32;
					default -> {
					}
				}
			}

			output.addVertex(x, y, z, r << 16 | g << 8 | b);
		}

		final var polygonLine = new byte[4*10];
		final ByteBuffer polygonBB = ByteBuffer.wrap(polygonLine);
		int[] indexes = new int[100];
		for (int i = 0; i < triangleCount; i++) {
			if (1 != reader.read(line, 0, 1))
				throw new RuntimeException("Couldn't read count byte");

			int count = line[0] & 0xFF;
			int lineLength = count*4;
			if (polygonLine.length < lineLength)
				throw new RuntimeException("polygonLine is too small. vertexes=" + count);

			int found = reader.read(polygonLine, 0, lineLength);
			if (found != lineLength)
				throw new IOException("Read unexpected number of bytes. " + found + " vs " + lineLength);

			for (int wordIndex = 0; wordIndex < count; wordIndex++) {
				indexes[wordIndex] = polygonBB.getInt(wordIndex*4);
			}

			output.addPolygon(indexes, 0, count);
		}
	}

	private static PlyWriter wrapMeshForWriting( VertexMesh mesh, @Nullable DogArray_I32 colorRGB ) {
		return new PlyWriter() {
			@Override public int getVertexCount() {return mesh.vertexes.size();}

			@Override public int getPolygonCount() {return mesh.offsets.size - 1;}

			@Override public boolean isColor() {return colorRGB != null;}

			@Override public void getVertex( int which, Point3D_F64 vertex ) {mesh.vertexes.getCopy(which, vertex);}

			@SuppressWarnings("NullAway")
			@Override public int getColor( int which ) {return colorRGB.get(which);}

			@Override public int getIndexes( int which, int[] indexes ) {
				int idx0 = mesh.offsets.get(which);
				int idx1 = mesh.offsets.get(which + 1);

				for (int i = idx0; i < idx1; i++) {
					indexes[i - idx0] = mesh.indexes.get(i);
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

			@Override public void getVertex( int which, Point3D_F64 vertex ) {cloud.get(which, vertex);}

			@Override public int getColor( int which ) {return cloud.getRGB(which);}

			@Override public int getIndexes( int which, int[] indexes ) {return 0;}
		};
	}

	private static class Header {
		List<DataWord> dataWords = new ArrayList<>();
		int vertexCount = -1;
		int triangleCount = -1;
		boolean rgb = false;
		Format format = Format.ASCII;
	}

	private static class DataWord {
		VarType var;
		DataType data;

		public DataWord( VarType var, DataType data ) {
			this.var = var;
			this.data = data;
		}
	}

	private enum VarType {
		X, Y, Z, R, G, B, UNKNOWN
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
	}

	private enum Format {
		ASCII,
		BINARY_LITTLE,
		BINARY_BIG
	}
}

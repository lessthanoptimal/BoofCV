/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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
import georegression.struct.point.Point3D_F64;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * For reading PLY point files
 *
 * @author Peter Abeles
 */
public class PlyCodec {
	public static void saveAscii( PointCloudReader cloud, boolean saveRgb, Writer outputWriter ) throws IOException {
		outputWriter.write("ply\n");
		outputWriter.write("format ascii 1.0\n");
		outputWriter.write("comment Created using BoofCV!\n");
		outputWriter.write("element vertex " + cloud.size() + "\n" +
				"property float x\n" +
				"property float y\n" +
				"property float z\n");
		if (saveRgb) {
			outputWriter.write(
					"property uchar red\n" +
						"property uchar green\n" +
						"property uchar blue\n");
		}
		outputWriter.write("end_header\n");

		Point3D_F64 p = new Point3D_F64();
		for (int i = 0; i < cloud.size(); i++) {
			cloud.get(i, p);
			if (saveRgb) {
				int rgb = cloud.getRGB(i);
				int r = (rgb >> 16) & 0xFF;
				int g = (rgb >> 8) & 0xFF;
				int b = rgb & 0xFF;
				outputWriter.write(String.format("%f %f %f %d %d %d\n", p.x, p.y, p.z, r, g, b));
			} else {
				outputWriter.write(String.format("%f %f %f\n", p.x, p.y, p.z));
			}
		}
		outputWriter.flush();
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
	public static void saveBinary( PointCloudReader cloud, ByteOrder order, boolean saveRgb, boolean saveAsFloat,
								   OutputStream outputWriter ) throws IOException {
		String format = "UTF-8";
		String dataType = saveAsFloat ? "float" : "double";
		int dataLength = saveAsFloat ? 4 : 8;
		outputWriter.write("ply\n".getBytes(format));
		outputWriter.write("format binary_big_endian 1.0\n".getBytes(format));
		outputWriter.write("comment Created using BoofCV!\n".getBytes(format));
		outputWriter.write(("element vertex " + cloud.size() + "\n").getBytes(format));
		outputWriter.write((
				"property " + dataType + " x\n" +
				"property " + dataType + " y\n" +
				"property " + dataType + " z\n").getBytes(format));
		if (saveRgb) {
			outputWriter.write(
					("property uchar red\n" +
					 "property uchar green\n" +
					 "property uchar blue\n").getBytes(format));
		}
		outputWriter.write("end_header\n".getBytes(format));

		int end = dataLength*3;
		var bytes = ByteBuffer.allocate(dataLength*3 + (saveRgb ? 3 : 0));
		bytes.order(order);
		Point3D_F64 p = new Point3D_F64();
		for (int i = 0; i < cloud.size(); i++) {
			cloud.get(i, p);
			if (saveAsFloat) {
				bytes.putFloat(0, (float)p.x);
				bytes.putFloat(4, (float)p.y);
				bytes.putFloat(8, (float)p.z);
			} else {
				bytes.putDouble(0, p.x);
				bytes.putDouble(8, p.y);
				bytes.putDouble(16, p.z);
			}

			if (saveRgb) {
				int rgb = cloud.getRGB(i);
				int r = (rgb >> 16) & 0xFF;
				int g = (rgb >> 8) & 0xFF;
				int b = rgb & 0xFF;
				bytes.put(end, (byte)r);
				bytes.put(end + 1, (byte)g);
				bytes.put(end + 2, (byte)b);
			}
			outputWriter.write(bytes.array());
		}
		outputWriter.flush();
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

	public static void read( InputStream input, PointCloudWriter output ) throws IOException {
		StringBuilder buffer = new StringBuilder();

		String line = UtilIO.readLine(input, buffer);
		if (line.length() == 0) throw new IOException("Missing first line");
		if (line.compareToIgnoreCase("ply") != 0) throw new IOException("Expected PLY at start of file");

		var dataWords = new ArrayList<DataWord>();

		int vertexCount = -1;

		Format format = null;
		boolean rgb = false;
		line = readNextPly(input, true, buffer);
		while (line.length() != 0) {
			if (line.equals("end_header"))
				break;
			String[] words = line.split("\\s+");
			if (words.length == 1)
				throw new IOException("Expected more than one word");
			if( line.startsWith("format")) {
				format = switch (words[1]) {
					case "ascii" -> Format.ASCII;
					case "binary_little_endian" -> Format.BINARY_LITTLE;
					case "binary_big_endian" -> Format.BINARY_BIG;
					default -> throw new IOException("Unknown format " + words[1]);
				};
			} else if (line.startsWith("element")) {
				if (words[1].equals("vertex")) {
					vertexCount = Integer.parseInt(words[2]);
				}
			} else if (words[0].equals("property")) {
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
					case "red" -> { v = VarType.R; rgb = true; }
					case "green" -> { v = VarType.G; rgb = true; }
					case "blue" -> { v = VarType.B; rgb = true; }
					default -> v = VarType.UNKNOWN;
				}
				dataWords.add(new DataWord(v, d));
			} else {
				throw new IOException("Unknown header element");
			}
			line = readNextPly(input, true, buffer);
		}
		if (vertexCount == -1)
			throw new IOException("File is missing vertex count");
		if (format == null)
			throw new IOException("Format is never specified");

		output.initialize(vertexCount, rgb);

		switch (format) {
			case ASCII -> readAscii(output, input, dataWords, buffer, vertexCount, rgb);
			case BINARY_LITTLE -> readBinary(output, input, dataWords, ByteOrder.LITTLE_ENDIAN, vertexCount, rgb);
			case BINARY_BIG -> readBinary(output, input, dataWords, ByteOrder.BIG_ENDIAN, vertexCount, rgb);
			default -> throw new RuntimeException("BUG!");
		}
	}

	private static void readAscii( PointCloudWriter output, InputStream reader, List<DataWord> dataWords,
								   StringBuilder buffer, int vertexCount, boolean rgb ) throws IOException {
		// storage for read in values
		int I32 = -1;
		double F64 = -1;

		// values that are writen to that we care about
		int r = -1, g = -1, b = -1;
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
				switch( d.var ) {
					case X: x = F64; break;
					case Y: y = F64; break;
					case Z: z = F64; break;
					case R: r = I32; break;
					case G: g = I32; break;
					case B: b = I32; break;
					default: break;
				}
			}
			if (rgb) {
				output.add(x, y, z, r << 16 | g << 8 | b);
			} else {
				output.add(x, y, z, 0x0);
			}
		}
	}

	private static void readBinary( PointCloudWriter output, InputStream reader, List<DataWord> dataWords,
									ByteOrder order,
									int vertexCount, boolean rgb ) throws IOException {

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
				switch( d.var ) {
					case X: x = F64; break;
					case Y: y = F64; break;
					case Z: z = F64; break;
					case R: r = I32; break;
					case G: g = I32; break;
					case B: b = I32; break;
					default: break;
				}
			}

			if (rgb) {
				output.add(x, y, z, r << 16 | g << 8 | b);
			} else {
				output.add(x, y, z, 0x0);
			}
		}
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

/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

import boofcv.io.UtilIO;
import boofcv.io.points.PointCloudReader;
import boofcv.io.points.PointCloudWriter;
import georegression.struct.point.Point3D_F64;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * For reading PLY point files
 *
 * @author Peter Abeles
 */
public class PlyCodec {
	public static void saveAscii(PointCloudReader cloud , boolean saveRgb , Writer outputWriter ) throws IOException {
		outputWriter.write("ply\n");
		outputWriter.write("format ascii 1.0\n");
		outputWriter.write("comment Created using BoofCV!\n");
		outputWriter.write("element vertex "+cloud.size()+"\n" +
				"property float x\n" +
				"property float y\n" +
				"property float z\n");
		if( saveRgb ) {
			outputWriter.write(
					"property uchar red\n" +
						"property uchar green\n" +
						"property uchar blue\n");
		}
		outputWriter.write("end_header\n");

		Point3D_F64 p = new Point3D_F64();
		for (int i = 0; i < cloud.size(); i++) {
			cloud.get(i,p);
			if( saveRgb ) {
				int rgb = cloud.getRGB(i);
				int r = (rgb >> 16)&0xFF;
				int g = (rgb >> 8)&0xFF;
				int b = rgb&0xFF;
				outputWriter.write(String.format("%f %f %f %d %d %d\n",p.x,p.y,p.z,r,g,b));
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
	 * @param outputWriter Stream it will write to
	 * @throws IOException
	 */
	public static void saveBinary(PointCloudReader cloud , ByteOrder order, boolean saveRgb , boolean saveAsFloat ,
								  OutputStream outputWriter ) throws IOException {
		String format = "UTF-8";
		String dataType = saveAsFloat ? "float" : "double";
		int dataLength = saveAsFloat ? 4 : 8;
		outputWriter.write("ply\n".getBytes(format));
		outputWriter.write("format binary_big_endian 1.0\n".getBytes(format));
		outputWriter.write("comment Created using BoofCV!\n".getBytes(format));
		outputWriter.write(("element vertex "+cloud.size()+"\n").getBytes(format));
		outputWriter.write((
				"property "+dataType+" x\n" +
				"property "+dataType+" y\n" +
				"property "+dataType+" z\n").getBytes(format));
		if( saveRgb ) {
			outputWriter.write(
					("property uchar red\n" +
					 "property uchar green\n" +
					 "property uchar blue\n").getBytes(format));
		}
		outputWriter.write("end_header\n".getBytes(format));

		int end = dataLength*3;
		var bytes = ByteBuffer.allocate(dataLength*3 + (saveRgb?3:0));
		bytes.order(order);
		Point3D_F64 p = new Point3D_F64();
		for (int i = 0; i < cloud.size(); i++) {
			cloud.get(i,p);
			if( saveAsFloat ) {
				bytes.putFloat(0, (float) p.x);
				bytes.putFloat(4, (float) p.y);
				bytes.putFloat(8, (float) p.z);
			} else {
				bytes.putDouble(0, p.x);
				bytes.putDouble(8, p.y);
				bytes.putDouble(16, p.z);
			}

			if( saveRgb ) {
				int rgb = cloud.getRGB(i);
				int r = (rgb >> 16)&0xFF;
				int g = (rgb >> 8)&0xFF;
				int b = rgb&0xFF;
				bytes.put(end,(byte)r);
				bytes.put(end+1,(byte)g);
				bytes.put(end+2,(byte)b);
			}
			outputWriter.write(bytes.array());
		}
		outputWriter.flush();
	}

	private static String readNextPly(InputStream reader , boolean failIfNull , StringBuffer buffer ) throws IOException {
		String line = UtilIO.readLine(reader,buffer);
		while( line.length() != 0 ) {
			if( line.startsWith("comment") )
				line = UtilIO.readLine(reader,buffer);
			else {
				return line;
			}
		}
		if( failIfNull )
			throw new IOException("Unexpected end of file");
		return line;
	}

	public static void read(InputStream input, PointCloudWriter output ) throws IOException {
		StringBuffer buffer = new StringBuffer();

		String line = UtilIO.readLine(input,buffer);
		if( line.length() == 0 ) throw new IOException("Missing first line");
		if( line.compareToIgnoreCase("ply")!=0 ) throw new IOException("Expected PLY at start of file");

		int vertexCount = -1;

		int floatBytes = -1;

		Format format = null;
		boolean rgb = false;
		line = readNextPly(input,true, buffer);
		while( line.length() != 0) {
			if( line.equals("end_header") )
				break;
			String[] words = line.split("\\s+");
			if( words.length == 1 )
				throw new IOException("Expected more than one word");
			if( line.startsWith("format")) {
				switch(words[1]) {
					case "ascii":format = Format.ASCII;break;
					case "binary_little_endian":format = Format.BINARY_LITTLE;break;
					case "binary_big_endian":format = Format.BINARY_BIG;break;
					default: throw new IOException("Unknown format "+words[1]);
				}
			} else if( line.startsWith("element")) {
				if( words[1].equals("vertex")) {
					vertexCount = Integer.parseInt(words[2]);
				}
			} else if( words[0].equals("property") ) {
				if( words[2].equals("red")) {
					rgb = true;
				} else if( words[2].equals("x")) {
					floatBytes = words[1].equals("float") ? 4 : 8;
				}
			} else {
				throw new IOException("Unknown header element");
			}
			line = readNextPly(input,true, buffer);
		}
		if( vertexCount == -1 )
			throw new IOException("File is missing vertex count");
		if( format == null )
			throw new IOException("Format is never specified");

		output.init(vertexCount);

		switch (format) {
			case ASCII:readAscii(output, input, buffer, vertexCount, rgb);break;
			case BINARY_LITTLE:readBinary(output, input, floatBytes, ByteOrder.LITTLE_ENDIAN, vertexCount, rgb);break;
			case BINARY_BIG:readBinary(output, input, floatBytes, ByteOrder.BIG_ENDIAN, vertexCount, rgb);break;
			default: throw new RuntimeException("BUG!");
		}
	}

	private static void readAscii(PointCloudWriter output, InputStream reader, StringBuffer buffer, int vertexCount, boolean rgb) throws IOException {
		for (int i = 0; i < vertexCount; i++) {
			String line = readNextPly(reader,true, buffer);
			String[] words = line.split("\\s+");
			if( words.length != (rgb?6:3))
				throw new IOException("unexpected number of words. "+line);
			double x = Double.parseDouble(words[0]);
			double y = Double.parseDouble(words[1]);
			double z = Double.parseDouble(words[2]);

			if( rgb ) {
				int r = Integer.parseInt(words[3]);
				int g = Integer.parseInt(words[4]);
				int b = Integer.parseInt(words[3]);
				output.add(x,y,z, r << 16 | g << 8 | b);
			} else {
				output.add(x,y,z);
			}
		}
	}

	private static void readBinary(PointCloudWriter output, InputStream reader, int floatBytes,
								   ByteOrder order,
								   int vertexCount, boolean rgb) throws IOException {
		final int startRGB = floatBytes*3;
		final byte[] line = new byte[startRGB + (rgb?3:0)];
		final ByteBuffer bb = ByteBuffer.wrap(line);
		bb.order(order);
		for (int i = 0; i < vertexCount; i++) {
			int found = reader.read(line);
			if( line.length != found )
				throw new IOException("Read unexpected number of bytes. "+found+" vs "+line.length);

			double x,y,z;

			if( floatBytes == 4 ) {
				x = bb.getFloat(0);
				y = bb.getFloat(4);
				z = bb.getFloat(8);
			} else {
				x = bb.getDouble(0);
				y = bb.getDouble(8);
				z = bb.getDouble(16);
			}

			if( rgb ) {
				int r = line[startRGB ]&0xFF;
				int g = line[startRGB+1]&0xFF;
				int b = line[startRGB+2]&0xFF;
				output.add(x,y,z, r << 16 | g << 8 | b);
			} else {
				output.add(x,y,z);
			}
		}
	}

	private enum Format {
		ASCII,
		BINARY_LITTLE,
		BINARY_BIG
	}

}

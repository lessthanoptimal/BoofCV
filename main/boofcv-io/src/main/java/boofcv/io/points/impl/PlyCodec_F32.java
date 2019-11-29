/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.Point3dRgbI_F32;
import georegression.struct.point.Point3D_F32;
import org.ddogleg.struct.FastQueue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.List;

/**
 * For reading PLY point files
 *
 * @author Peter Abeles
 */
public class PlyCodec_F32 {
	public static void saveAscii(List<Point3D_F32> cloud , Writer outputWriter ) throws IOException {
		outputWriter.write("ply\n");
		outputWriter.write("format ascii 1.0f\n");
		outputWriter.write("comment Created using BoofCV!\n");
		outputWriter.write("element vertex "+cloud.size()+"\n" +
				"property float x\n" +
				"property float y\n" +
				"property float z\n" +
				"end_header\n");

		for (int i = 0; i < cloud.size(); i++) {
			Point3D_F32 p = cloud.get(i);
			outputWriter.write(String.format("%f %f %f\n",p.x,p.y,p.z));
		}
		outputWriter.flush();
	}

	public static void read(Reader inputReader, FastQueue<Point3D_F32> output ) throws IOException {
		BufferedReader reader = new BufferedReader(inputReader);
		String line = reader.readLine();
		if( line == null ) throw new IOException("Missing first line");
		if( line.compareToIgnoreCase("ply")!=0 ) throw new IOException("Expected PLY at start of file");

		int vertexCount = -1;

		boolean ascii = false;
		line = readNextPly(reader,true);
		while( line != null ) {
			if( line.equals("end_header") )
				break;
			String[] words = line.split("\\s+");
			if( words.length == 1 )
				throw new IOException("Expected more than one word");
			if( line.startsWith("format")) {
				if( words[1].equals("ascii")) {
					ascii = true;
				} else {
					ascii = false;
				}
			} else if( line.startsWith("element")) {
				if( words[1].equals("vertex")) {
					vertexCount = Integer.parseInt(words[2]);
				}
			} else if( words[0].equals("property") ) {
				// I should do something here
			} else {
				throw new IOException("Unknown header element");
			}
			line = readNextPly(reader,true);
		}
		if( vertexCount == -1 )
			throw new IOException("File is missing vertex count");

		if( !ascii )
			throw new IllegalArgumentException("Currently only ASCII format is supported");

		output.growArray(output.data.length+vertexCount);

		for (int i = 0; i < vertexCount; i++) {
			line = readNextPly(reader,true);
			String[] words = line.split("\\s+");
			Point3D_F32 p = output.grow();
			p.x = Float.parseFloat(words[0]);
			p.y = Float.parseFloat(words[1]);
			p.z = Float.parseFloat(words[2]);
		}
	}

	private static String readNextPly(BufferedReader reader , boolean failIfNull ) throws IOException {
		String line = reader.readLine();
		while( line != null ) {
			if( line.startsWith("comment") )
				line = reader.readLine();
			else {
				return line;
			}
		}
		if( failIfNull )
			throw new IOException("Unexpected end of file");
		return null;
	}

	public static void saveAsciiRgbI(List<Point3dRgbI_F32> cloud , Writer outputWriter ) throws IOException {
		outputWriter.write("ply\n");
		outputWriter.write("format ascii 1.0f\n");
		outputWriter.write("comment Created using BoofCV!\n");
		outputWriter.write("element vertex "+cloud.size()+"\n" +
				"property float x\n" +
				"property float y\n" +
				"property float z\n" +
				"property uchar red\n" +
				"property uchar green\n" +
				"property uchar blue\n" +
				"end_header\n");

		for (int i = 0; i < cloud.size(); i++) {
			Point3dRgbI_F32 p = cloud.get(i);
			int r = (p.rgb >> 16)&0xFF;
			int g = (p.rgb >> 8)&0xFF;
			int b = p.rgb&0xFF;
			outputWriter.write(String.format("%f %f %f %d %d %d\n",p.x,p.y,p.z,r,g,b));
		}
		outputWriter.flush();
	}

	public static void readRgbI(Reader inputReader, FastQueue<Point3dRgbI_F32> output ) throws IOException {
		BufferedReader reader = new BufferedReader(inputReader);
		String line = reader.readLine();
		if( line == null ) throw new IOException("Missing first line");
		if( line.compareToIgnoreCase("ply")!=0 ) throw new IOException("Expected PLY at start of file");

		int vertexCount = -1;

		boolean ascii = false;
		boolean rgb = false;
		line = readNextPly(reader,true);
		while( line != null ) {
			if( line.equals("end_header") )
				break;
			String[] words = line.split("\\s+");
			if( words.length == 1 )
				throw new IOException("Expected more than one word");
			if( line.startsWith("format")) {
				if( words[1].equals("ascii")) {
					ascii = true;
				} else {
					ascii = false;
				}
			} else if( line.startsWith("element")) {
				if( words[1].equals("vertex")) {
					vertexCount = Integer.parseInt(words[2]);
				}
			} else if( words[0].equals("property") ) {
				if( words[2].equals("red")) {
					rgb = true;
				}
			} else {
				throw new IOException("Unknown header element");
			}
			line = readNextPly(reader,true);
		}
		if( vertexCount == -1 )
			throw new IOException("File is missing vertex count");

		if( !ascii )
			throw new IllegalArgumentException("Currently only ASCII format is supported");

		output.growArray(output.data.length+vertexCount);

		for (int i = 0; i < vertexCount; i++) {
			line = readNextPly(reader,true);
			String[] words = line.split("\\s+");
			if( words.length != (rgb?6:3))
				throw new IOException("unexpected number of words. "+line);
			Point3dRgbI_F32 p = output.grow();
			p.x = Float.parseFloat(words[0]);
			p.y = Float.parseFloat(words[1]);
			p.z = Float.parseFloat(words[2]);

			if( rgb ) {
				int r = Integer.parseInt(words[3]);
				int g = Integer.parseInt(words[4]);
				int b = Integer.parseInt(words[3]);
				p.rgb = r << 16 | g << 8 | b;
			}
		}
	}
}

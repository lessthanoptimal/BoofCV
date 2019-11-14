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

package boofcv.io.points;

import georegression.struct.point.Point3D_F64;
import org.ddogleg.struct.FastQueue;

import java.io.*;
import java.util.List;

/**
 * Code for reading different point cloud formats
 *
 * @author Peter Abeles
 */
public class PointCloudIO {

	public void save( Format format , List<Point3D_F64> cloud , File path ) throws IOException {
		Writer writer = new FileWriter(path);
		switch( format ) {
			case PLY_ASCII:
				savePlyAscii_F64(cloud,writer);
				break;
			default:
				throw new IllegalArgumentException("Unknown format "+format);
		}
	}

	public void load( Format format , File path , FastQueue<Point3D_F64> storage  ) throws IOException {
		Reader reader = new FileReader(path);
		switch( format ) {
			case PLY_ASCII:
				readPly_F64(reader,storage);
			default:
				throw new IllegalArgumentException("Unknown format "+format);
		}
	}

	private void savePlyAscii_F64(List<Point3D_F64> cloud , Writer outputWriter ) throws IOException {
		outputWriter.write("PLY\n");
		outputWriter.write("format ascii 1.0\n");
		outputWriter.write("comment Created using BoofCV!\n");
		outputWriter.write("element vertex "+cloud.size()+"\n" +
				"property float x\n" +
				"property float y\n" +
				"property float z\n" +
				"end_header\n");

		for (int i = 0; i < cloud.size(); i++) {
			Point3D_F64 p = cloud.get(i);
			outputWriter.write(String.format("%f %f %f\n",p.x,p.y,p.z));
		}
	}

	private void readPly_F64(Reader inputReader, FastQueue<Point3D_F64> output ) throws IOException {
		BufferedReader reader = new BufferedReader(inputReader);
		String line = reader.readLine();
		if( line == null ) throw new IOException("Missing first line");
		if( !line.equals("PLY") ) throw new IOException("Expected PLY at start of file");

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

		output.growArray(output.data.length+vertexCount);

		for (int i = 0; i < vertexCount; i++) {
			line = readNextPly(reader,true);
			String[] words = line.split("\\s+");
			Point3D_F64 p = output.grow();
			p.x = Double.parseDouble(words[0]);
			p.y = Double.parseDouble(words[1]);
			p.z = Double.parseDouble(words[2]);
		}

	}

	private String readNextPly(BufferedReader reader , boolean failIfNull ) throws IOException {
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

	public enum Format {
		/**
		 * https://en.wikipedia.org/wiki/PLY_(file_format)
		 */
		PLY_ASCII
	}
}

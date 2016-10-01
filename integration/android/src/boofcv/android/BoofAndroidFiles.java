/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.android;

import boofcv.struct.calib.CameraPinholeRadial;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * Functions for reading and writing data structures specific to BoofCV on Android.  An attempt to make up for the
 * lack of XML serialization
 *
 * @author Peter Abeles
 */
public class BoofAndroidFiles {

	/**
	 * Writes intrinsic parameters to an output stream
	 * @param param
	 * @param writer
	 * @throws IOException
	 */
	public static void write(PinholeRadial param , Writer writer ) throws IOException {
		writer.write("# Intrinsic camera parameters. width height\\nfx skew cx fy cy\\nradial parameters\n");
		writer.write(param.width+" "+param.height+"\n");
		writer.write(param.fx+" "+param.skew+" "+param.cx+" "+param.fy+" "+param.cy+"\n");
		if( param.radial == null || param.radial.length == 0)
			writer.write("0\n");
		else {
			writer.write(""+param.radial.length);
			for( int i = 0; i < param.radial.length; i++ ) {
				writer.write(" "+param.radial[i]);
			}
			writer.write("\n");
		}
		writer.flush();
	}

	/**
	 * Reads intrinsic parameters from an input stream
	 *
	 * @param reader Input stream
	 * @return Parsed {@link PinholeRadial}
	 * @throws IOException
	 */
	public static PinholeRadial readIntrinsic(Reader reader ) throws IOException {
		PinholeRadial ret = new PinholeRadial();

		BufferedReader r = new BufferedReader(reader);
		// skip info header
		readLine(r);
		String tokens[] = readLine(r).split(" ");

		ret.width = Integer.parseInt(tokens[0]);
		ret.height = Integer.parseInt(tokens[1]);

		tokens = readLine(r).split(" ");

		ret.fx = Double.parseDouble(tokens[0]);
		ret.skew = Double.parseDouble(tokens[1]);
		ret.cx = Double.parseDouble(tokens[2]);
		ret.fy = Double.parseDouble(tokens[3]);
		ret.cy = Double.parseDouble(tokens[4]);

		tokens = readLine(r).split(" ");
		int num = Integer.parseInt(tokens[0]);
		if( num != 0 ) {
			ret.radial = new double[ num ];
			for( int i = 0; i < num; i++ ) {
				ret.radial[i] = Double.parseDouble( tokens[i+1]);
			}
		}

		return ret;
	}

	private static String readLine( BufferedReader r ) throws IOException {
		String line = r.readLine();
		if( line == null )
			throw new IOException("Read line failed");
		return line;
	}
}

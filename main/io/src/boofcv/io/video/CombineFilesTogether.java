/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.io.video;

import boofcv.misc.BoofMiscOps;
import org.ddogleg.struct.GrowQueue_I8;

import java.io.*;
import java.util.Collections;
import java.util.List;

/**
 * Combines a sequence of files together using a simple format.  At the beginning of each segment/file [0xff,0xff,0xff]
 * is written, followed by the 4-byte integer in big endian order specifying the file size.  After that the file
 * is written.  This is repeated until all the files are done.
 *
 * @author Peter Abeles
 */
public class CombineFilesTogether {
	public static void combine( List<String> fileNames , String outputName ) throws IOException {
		FileOutputStream fos = new FileOutputStream(outputName);

		GrowQueue_I8 buffer = new GrowQueue_I8();

		for( String s : fileNames ) {
			File f = new File(s);
			FileInputStream fis = new FileInputStream(f);

			long length = f.length();
			buffer.resize((int)length);

			// write out header
			fos.write(255);
			fos.write(255);
			fos.write(255);
			fos.write((byte)(length >> 24));
			fos.write((byte)(length >> 16));
			fos.write((byte)(length >> 8));
			fos.write((byte)(length));

			fis.read(buffer.data, 0, (int) length);
			fos.write(buffer.data,0,(int)length);

		}
	}

	public static boolean readNext( DataInputStream fis , GrowQueue_I8 output ) throws IOException {
		int r;
		if( (r =fis.read()) != 0xFF || (r = fis.read()) != 0xFF || (r=fis.read()) != 0xFF )
			if( r == -1 )
				return false;
			else
				throw new IllegalArgumentException("Bad header byte: "+r);

		int length = ((fis.read() & 0xFF) << 24) |  ((fis.read() & 0xFF) << 16) |
				((fis.read() & 0xFF) << 8)  | (fis.read() & 0xFF);

		output.resize(length);
		fis.read(output.data,0,length);

		return true;
	}

	public static void main( String args[] ) throws IOException {
		List<String> fileNames = BoofMiscOps.directoryList("log", "depth");
		Collections.sort(fileNames);
		CombineFilesTogether.combine(fileNames,"combined.mpng");
	}
}

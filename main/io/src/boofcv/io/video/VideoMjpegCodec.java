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

package boofcv.io.video;


import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Very simple MJPEG reader
 *
 * @author Peter Abeles
 */
public class VideoMjpegCodec {
	// start of image
	public static final byte SOI = (byte)0xD8;
	// end of image
	public static final byte EOI = (byte)0xD9;

	public List<byte[]> read( InputStream streamIn ) {
		// read the whole movie in at once to make it faster

		List<byte[]> ret = new ArrayList<>();
		try {
			byte[] b = convertToByteArray(streamIn);
//			System.out.println("MJPEG file is "+b.length+" bytes");

			DataInputStream in = new DataInputStream(new ByteArrayInputStream(b));

			while( findMarker(in,SOI) && in.available() > 0 ) {
				byte data[] = readJpegData(in, EOI);
				ret.add(data);
			}
		} catch (IOException e) {
		}
		return ret;
	}

	/**
	 * Read a single frame at a time
	 */
	public byte[] readFrame( DataInputStream in ) {
		try {
			if( findMarker(in,SOI) && in.available() > 0 ) {
				return readJpegData(in, EOI);
			}
		} catch (IOException e) {}
		return null;
	}


	public static byte[] convertToByteArray(InputStream streamIn) throws IOException {
		ByteArrayOutputStream temp = new ByteArrayOutputStream(1024);
		byte[] data = new byte[ 1024 ];
		int length;
		while( ( length = streamIn.read(data)) != -1 ) {
			temp.write(data,0,length);
		}
		return temp.toByteArray();
	}

	private boolean findMarker( DataInputStream in , byte marker ) throws IOException {
		boolean foundFF = false;

		while( in.available() > 0 )  {
			byte b = in.readByte();
			if( foundFF ) {
				if( b == marker ) {
					return true;
				} else if( b != (byte)0xFF )
					foundFF = false;
			} else if( b == (byte)0xFF ) {
				foundFF = true;
			}
		}
		return foundFF;
	}

	private byte[] readJpegData(DataInputStream in, byte marker) throws IOException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream(1024);

		// add the SOI marker back into it
		bout.write(0xFF);
		bout.write(SOI);

		boolean foundFF = false;

		while( in.available() > 0 ) {
			byte d = in.readByte();
			if( foundFF ) {
				if( d == marker )
					break;
				else {
					bout.write(0xFF);
					bout.write(d);
					foundFF = false;
				}
			} else if( d ==(byte)0xFF ) {
				foundFF = true;
			} else {
				bout.write(d);
			}
		}
		return bout.toByteArray();
	}


}

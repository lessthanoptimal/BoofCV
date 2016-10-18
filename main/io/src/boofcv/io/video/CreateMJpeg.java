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
import java.util.Collections;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class CreateMJpeg {
	private static byte buffer[] = new byte[ 10240 ];

	private static byte[] filterData( DataInputStream in ) throws IOException {
		while( in.available() > 0 ) {
			int header = in.read();
			if( header != 0xFF )
				throw new IllegalArgumentException("Expected 0xFF not "+Integer.toHexString(header));
			int mark = in.readUnsignedByte();

			System.out.println(" mark = "+Integer.toHexString(mark));

			switch( mark ) {
				case 0xD8: // start of image
					return createMarker((byte)mark);

				case 0xC0: // start of frame: Baseline
					return readUntilMarker(in,(byte)mark,(byte)0xD9);

				case 0xC2: // start of frame: Progressive
					return readUntilMarker(in,(byte)mark,(byte)0xD9);

				case 0xC4: // Huffman table
					return readVariableLength(in, (byte) mark);

				case 0xDB: // Quantization table
					return readVariableLength(in,(byte)mark);

				case 0xDD: // Define Restart Interval
					throw new RuntimeException("Need to handle");

				case 0xDA: // Start of scan
					return readVariableLength(in,(byte)mark);

				case 0xD0: // restart
				case 0xD1: // restart
				case 0xD2: // restart
				case 0xD3: // restart
				case 0xD4: // restart
				case 0xD5: // restart
				case 0xD6: // restart
				case 0xD7: // restart
					return createMarker((byte) mark);

				// Application-specific
				case 0xE0:case 0xE1:case 0xE2:case 0xE3:
				case 0xE4:case 0xE5:case 0xE6:case 0xE7:
				case 0xE8:case 0xE9:case 0xEA:case 0xEB:
				case 0xEC:case 0xED:case 0xEE:case 0xEF:
					readVariableLength(in, (byte) mark);
					break;

				case 0xFE: // comment
					readVariableLength(in,(byte)mark);
					break;

				case 0xD9: // end of image
					throw new IllegalArgumentException("Should never read this");

				default:
					throw new IllegalArgumentException("Unexpected");
			}
		}
		return null;
	}

	private static byte[] createMarker( byte type ) {
		return new byte[]{(byte)0xFF,type};
	}

	private static byte[] readVariableLength(DataInputStream in, byte type) throws IOException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream(1024);

		// put the marker back in
		bout.write(0xFF);
		bout.write(type&0xFF);

		int length = in.readUnsignedShort()-2;
		bout.write(length);

		while( length > 0 && in.available() > 0 ) {
			int amount = Math.min(buffer.length,length);
			int actual = in.read(buffer,0,amount);
			length -= actual;
			bout.write(buffer,0,actual);
		}

		return bout.toByteArray();
	}

	private static byte[] readUntilMarker(DataInputStream in, byte type , byte endMarker) throws IOException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream(1024);

		// put the marker back in
		bout.write(0xFF);
		bout.write(type&0xFF);

		int length = in.readUnsignedShort();
		bout.write(length);

		boolean foundFF = false;
		while( length > 0 && in.available() > 0 ) {
			byte b = in.readByte();
			if( foundFF ) {
				if( b == endMarker ) {
					return bout.toByteArray();
				}
				bout.write(0xFF);
				bout.write(b);
				if( b != 0xFF )
					foundFF = false;
			} else if( b == (byte)0xFF) {
				foundFF = true;
			} else {
				bout.write(b);
			}
		}

		return bout.toByteArray();
	}


	public static void main( String args[] ) throws IOException {
		File directory = new File("/home/pja/a/b");

		File[] files = directory.listFiles();

		List<String> list = new ArrayList<>();
		for( File f : files ) {
			if( f.isFile() && f.getName().endsWith(".jpg")) {
				list.add(f.getPath());
			}
		}
		Collections.sort(list);

		int i = -1;

		DataOutputStream out = new DataOutputStream(new FileOutputStream(directory.getPath()+"/movie.mjpeg"));
		for( String n : list ) {
			i++;
			if( !(i % 2 == 0) )
				continue;

//			if( i < 338 || i > 800 )
//				continue;

			System.out.println("Reading in: "+n);
			DataInputStream in = new DataInputStream(new FileInputStream(n));

//			while( true ) {
//				byte data[] = filterData(in);
//				if( data == null )
//					break;
//
//				out.write(data);
//			}

			while( in.available() > 0 ) {
				byte data[] = new byte[ in.available()];
				in.read(data);
				out.write(data);
			}

			in.close();
		}
		out.close();
	}
}

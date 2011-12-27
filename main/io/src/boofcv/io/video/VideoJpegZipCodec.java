/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Codec for reading JPEG Zip format.  Internal format used by BoofCV due to Java's lack of a video format that
 * applet's can use.  Its simply a sequence of jpeg images compressed in a zip file.
 *
 * @author Peter Abeles
 */
public class VideoJpegZipCodec {

	public List<byte[]> read( InputStream streamIn ) {

		ZipInputStream zin = new ZipInputStream(new BufferedInputStream(streamIn));

		ZipEntry entry;

		List<byte[]> jpegData = new ArrayList<byte[]>();

		try {
			while( (zin.getNextEntry()) != null ) {
//				System.out.println("Extracting: " +entry);
				int count;
				byte data[] = new byte[1024];
				// write the files to the disk
				ByteArrayOutputStream bout = new ByteArrayOutputStream(1024);

				while ((count = zin.read(data, 0, 1024)) != -1) {
					bout.write(data,0,count);
				}
				jpegData.add(bout.toByteArray());
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return jpegData;
	}
}

/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
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

package boofcv.applet;

import boofcv.io.image.SimpleImageSequence;
import boofcv.io.video.VideoListManager;
import boofcv.io.wrapper.images.JpegByteImageSequence;
import boofcv.struct.image.ImageBase;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author Peter Abeles
 */
public class AppletVideoListManager<T extends ImageBase> extends VideoListManager {
	URL codebase;

	public AppletVideoListManager( Class<T> imageType , URL codebase) {
		super(imageType);
		this.codebase = codebase;
	}

	@Override
	public SimpleImageSequence<T> loadSequence( int labelIndex , int imageIndex ) {

		try {
			String n = ((String[])fileNames.get(labelIndex))[imageIndex];
			URL url = new URL(codebase, n);

			ZipInputStream zin = new
					ZipInputStream(new BufferedInputStream(url.openStream()));


			List<BufferedImage> frames = new ArrayList<BufferedImage>();
			ZipEntry entry;

			List<byte[]> jpegData = new ArrayList<byte[]>();

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

			return new JpegByteImageSequence<T>(imageType,jpegData, true);
		} catch (MalformedURLException e) {
			System.err.println("MalformedURL"+fileNames.get(labelIndex));
		} catch (IOException e) {
			System.err.println("IOException reading "+fileNames.get(labelIndex));
		}
		return null;
	}
}

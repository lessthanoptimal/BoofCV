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
import boofcv.io.video.VideoJpegZipCodec;
import boofcv.io.video.VideoListManager;
import boofcv.io.video.VideoMjpegCodec;
import boofcv.io.wrapper.images.JpegByteImageSequence;
import boofcv.struct.image.ImageSingleBand;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class AppletVideoListManager<T extends ImageSingleBand> extends VideoListManager {
	URL codebase;

	public AppletVideoListManager( Class<T> imageType , URL codebase) {
		super(imageType);
		this.codebase = codebase;
	}

	@Override
	public SimpleImageSequence<T> loadSequence( int labelIndex , int imageIndex ) {

		String type = (String)videoType.get(labelIndex);
		String n = ((String[])fileNames.get(labelIndex))[imageIndex];
		URL url = null;
		BufferedInputStream stream = null;
		try {
			url = new URL(codebase, n);
			stream = new BufferedInputStream(url.openStream());
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		if( type.compareToIgnoreCase("JPEG_ZIP") == 0 ) {
			VideoJpegZipCodec codec = new VideoJpegZipCodec();
			List<byte[]> data = codec.read(stream);
			return new JpegByteImageSequence<T>(imageType,data,true);
		} else if( type.compareToIgnoreCase("MJPEG") == 0 ) {
			VideoMjpegCodec codec = new VideoMjpegCodec();
			List<byte[]> data = codec.read(stream);
//			System.out.println("Loaded "+data.size()+" jpeg images!");
			return new JpegByteImageSequence<T>(imageType,data,true);
		} else {
			System.err.println("Unknown video type: "+type);
		}

		return null;
	}
}

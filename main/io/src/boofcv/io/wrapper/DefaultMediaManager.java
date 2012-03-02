/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.io.wrapper;

import boofcv.io.MediaManager;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.image.UtilImageIO;
import boofcv.io.video.VideoMjpegCodec;
import boofcv.io.wrapper.images.JpegByteImageSequence;
import boofcv.struct.image.ImageBase;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Peter Abeles
 */
public class DefaultMediaManager implements MediaManager {

	public static final DefaultMediaManager INSTANCE = new DefaultMediaManager();

	Map<String,BufferedImage> cachedImage = new HashMap<String, BufferedImage>();
	
	@Override
	public Reader openFile(String fileName) {
		try {
			return new FileReader(fileName);
		} catch (FileNotFoundException e) {
			return null;
		}
	}

	@Override
	public BufferedImage openImage(String fileName) {
		
		BufferedImage b = cachedImage.get(fileName);
		
		if( b == null ) {
			b = UtilImageIO.loadImage(fileName);
			
			if( b == null )
				throw new RuntimeException("Image cannot be found! "+fileName);
			
			cachedImage.put(fileName,b);
		}
		
		return b;
	}

	@Override
	public <T extends ImageBase> SimpleImageSequence<T>
	openVideo(String fileName, Class<T> type) {

		if( fileName.endsWith("mjpeg") || fileName.endsWith("MJPEG") ) {
			try {
				VideoMjpegCodec codec = new VideoMjpegCodec();
				List<byte[]> data = codec.read(new FileInputStream(fileName));
				return new JpegByteImageSequence<T>(type,data,true);
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		} else {
			throw new RuntimeException("Unknown movie type.  Must be an mjpeg");
		}
	}
}

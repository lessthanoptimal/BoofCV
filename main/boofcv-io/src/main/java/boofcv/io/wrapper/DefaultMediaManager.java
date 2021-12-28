/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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
import boofcv.io.UtilIO;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.image.UtilImageIO;
import boofcv.io.video.DynamicVideoInterface;
import boofcv.io.video.VideoInterface;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * The default media manager used by BoofCV. Uses the best available for each data source type.
 *
 * @author Peter Abeles
 */
public class DefaultMediaManager implements MediaManager {

	public static final DefaultMediaManager INSTANCE = new DefaultMediaManager();

	Map<String, BufferedImage> cachedImage = new HashMap<>();
	VideoInterface videoInterface = new DynamicVideoInterface();
	WebcamInterface webcamInterface = new DynamicWebcamInterface();

	@Override public @Nullable Reader openFile( String fileName ) {
		InputStream stream = UtilIO.openStream(fileName);
		if (stream == null)
			return null;
		return new InputStreamReader(stream, StandardCharsets.UTF_8);
	}

	@Override public @Nullable BufferedImage openImage( String fileName ) {
		BufferedImage b = cachedImage.get(fileName);

		if (b == null) {
			b = UtilImageIO.loadImage(fileName);

			if (b == null)
				return null;

			cachedImage.put(fileName, b);
		}

		// return a copy of the image so that if it is modified strangeness won't happen
		BufferedImage c = new BufferedImage(b.getWidth(), b.getHeight(), b.getType());
		Graphics2D g2 = c.createGraphics();
		g2.drawImage(b, 0, 0, null);
		return c;
	}

	@Override public <T extends ImageBase<T>>
	@Nullable SimpleImageSequence<T> openVideo( String fileName, ImageType<T> type ) {
		return videoInterface.load(fileName, type);
	}

	@Override public <T extends ImageBase<T>> SimpleImageSequence<T>
	openCamera( String device, int width, int height, ImageType<T> imageType ) {
		return webcamInterface.open(device, width, height, imageType);
	}
}

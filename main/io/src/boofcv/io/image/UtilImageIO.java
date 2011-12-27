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

package boofcv.io.image;

import boofcv.core.image.ConvertBufferedImage;
import boofcv.struct.image.ImageSingleBand;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Class for loading and saving images.
 *
 * @author Peter Abeles
 */
public class UtilImageIO {

	/**
	 * A function that load the specified image.  If anything goes wrong it returns a
	 * null.
	 */
	public static BufferedImage loadImage(String fileName) {
		BufferedImage img;
		try {
			img = ImageIO.read(new File(fileName));
		} catch (IOException e) {
			return null;
		}

		return img;
	}

	/**
	 * Loads the image and converts into the specified image type.
	 *
	 *
	 * @param fileName Path to image file.
	 * @param imageType Type of image that should be returned.
	 * @return The image or null if the image could not be loaded.
	 */
	public static <T extends ImageSingleBand> T loadImage(String fileName, Class<T> imageType ) {
		BufferedImage img = loadImage(fileName);
		if( img == null )
			return null;

		return ConvertBufferedImage.convertFromSingle(img, (T) null, imageType);
	}

	public static void saveImage(BufferedImage img, String fileName) {
		try {
			String type;
			String a[] = fileName.split("[.]");
			if (a.length > 0) {
				type = a[a.length - 1];
			} else {
				type = "jpg";
			}

			ImageIO.write(img, type, new File(fileName));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}

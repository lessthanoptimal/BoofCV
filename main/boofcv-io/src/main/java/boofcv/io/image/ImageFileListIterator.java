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

package boofcv.io.image;

import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import lombok.Getter;

import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.List;

/**
 * Iterator that returns images loaded from disk
 *
 * @author Peter Abeles
 */
public class ImageFileListIterator<T extends ImageGray<T>> implements Iterator<T> {
	protected @Getter T image;
	protected @Getter List<String> paths;
	protected @Getter int index = 0;

	public ImageFileListIterator( List<String> paths, ImageType<T> imageType ) {
		image = imageType.createImage(1, 1);
		this.paths = paths;
	}

	@Override public boolean hasNext() {
		return index < paths.size();
	}

	@Override public T next() {
		BufferedImage buffered = UtilImageIO.loadImage(paths.get(index++));
		ConvertBufferedImage.convertFrom(buffered, true, image);
		return image;
	}
}

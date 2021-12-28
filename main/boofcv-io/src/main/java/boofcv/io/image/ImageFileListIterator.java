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

import boofcv.misc.BoofLambdas;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import lombok.Getter;
import lombok.Setter;

import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.List;

/**
 * Iterator that returns images loaded from disk
 *
 * @author Peter Abeles
 */
public class ImageFileListIterator<T extends ImageBase<T>> implements Iterator<T> {
	protected @Getter T image;
	protected @Getter List<String> paths;
	protected @Getter int index;

	/** Pre-processing filter that should be applied to the erad in image */
	protected @Getter @Setter BoofLambdas.Transform<T> filter = ( a ) -> a;

	/** Called when it encounters image can't be read due to an exception. */
	protected @Getter @Setter HandleException exception = ( idx, path, e ) -> {
		e.printStackTrace(System.err);
		System.err.println("Bad Image: " + paths.get(index));
	};

	public ImageFileListIterator( List<String> paths, ImageType<T> imageType ) {
		image = imageType.createImage(1, 1);
		this.paths = paths;
		reset();
	}

	public void reset() {
		index = -1;
	}

	@Override public boolean hasNext() {
		return index + 1 < paths.size();
	}

	@Override public T next() {
		do {
			try {
				index++;
				loadImage(index);
				break;
			} catch (RuntimeException e) {
				exception.process(index, paths.get(index), e);
			}
		} while (index < paths.size());

		return filter.process(image);
	}

	public T loadImage( int index ) {
		BufferedImage buffered = UtilImageIO.loadImage(paths.get(index));
		if (buffered == null)
			throw new RuntimeException("Unknown path="+paths.get(index));

		ConvertBufferedImage.convertFrom(buffered, true, image);
		return image;
	}

	public @FunctionalInterface interface HandleException {
		/**
		 * Passes in information about the exception and decides how it should be handled
		 *
		 * @param index Which image index it blew up on
		 * @param path Path to the image
		 * @param e The actual exception
		 */
		void process( int index, String path, RuntimeException e );
	}
}

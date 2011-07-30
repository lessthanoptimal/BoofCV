/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.struct.pyramid;

import gecv.core.image.ImageGenerator;
import gecv.struct.image.ImageBase;

import java.lang.reflect.Array;


/**
 * An {@link ImagePyramid} where the scale factor between each level is specified using an integer number.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class ImagePyramidI<T extends ImageBase> extends ImagePyramid<T> {

	// scale of each layer relative to the previous layer
	public int scale[];

	/**
	 * Specifies input image size and behavior of top most layer.
	 *
	 * @param saveOriginalReference If a reference to the full resolution image should be saved instead of  copied.
	 */
	public ImagePyramidI( boolean saveOriginalReference, PyramidUpdater<T> updater ,  int ...scale ) {
		super(saveOriginalReference, updater);
		this.scale = scale.clone();
	}

	@Override
	public double getScale(int layer) {
		return scale[layer];
	}

	@Override
	public void declareLayers(ImageGenerator<T> generator, int width , int height ) {
		this.bottomWidth = width;
		this.bottomHeight = height;

		Class<T> type = generator.getType();

		layers = (T[]) Array.newInstance(type, scale.length);
		int scaleFactor = scale[0];

		if (scale[0] == 1) {
			if (!saveOriginalReference) {
				layers[0] = generator.createInstance(bottomWidth, bottomHeight);
			}
		} else {
			layers[0] = generator.createInstance(bottomWidth / scaleFactor, bottomHeight / scaleFactor);
		}

		for (int i = 1; i < scale.length; i++) {
			scaleFactor *= scale[i];
			layers[i] = generator.createInstance(bottomWidth / scaleFactor, bottomHeight / scaleFactor);
		}
	}
}

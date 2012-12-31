/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.template;

import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;

/**
 * Base class which implements common elements
 *
 * @author Peter Abeles
 */
public abstract class BaseTemplateIntensity<T extends ImageBase>
		implements TemplateMatchingIntensity<T> {
	// Match intensity image
	private ImageFloat32 intensity = new ImageFloat32(1, 1);

	// references to the input
	protected T image;
	protected T template;

	// offset from pixel intensity coordinate to top left corner of template
	private int offsetX;
	private int offsetY;

	@Override
	public void process(T image, T template) {
		this.image = image;
		this.template = template;
		intensity.reshape(image.width, image.height);

		int w = image.width - template.width;
		int h = image.height - template.height;

		offsetX = template.width / 2;
		offsetY = template.height / 2;

		for (int y = 0; y < h; y++) {
			int index = intensity.startIndex + (y + offsetY) * intensity.stride + offsetX;
			for (int x = 0; x < w; x++) {
				intensity.data[index++] = evaluate(x, y);
			}
		}
	}

	/**
	 * Evaluate the template at the specified location.
	 *
	 * @param tl_x Template's top left corner x-coordinate
	 * @param tl_y Template's top left corner y-coordinate
	 * @return match value with better matches having a more positive value
	 */
	protected abstract float evaluate(int tl_x, int tl_y);

	@Override
	public ImageFloat32 getIntensity() {
		return intensity;
	}

	@Override
	public int getOffsetX() {
		return offsetX;
	}

	@Override
	public int getOffsetY() {
		return offsetY;
	}
}
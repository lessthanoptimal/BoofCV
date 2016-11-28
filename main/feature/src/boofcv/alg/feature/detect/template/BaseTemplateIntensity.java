/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageBase;

/**
 * Base class which implements common elements
 *
 * @author Peter Abeles
 */
public abstract class BaseTemplateIntensity<T extends ImageBase>
		implements TemplateMatchingIntensity<T> {
	// Match intensity image
	private GrayF32 intensity = new GrayF32(1, 1);

	// references to the input
	protected T image;
	protected T template;
	protected T mask;

	// thickness of the border along the lower extents of the image
	private int borderX0,borderY0;
	private int borderX1,borderY1;

	@Override
	public void setInputImage(T image) {
		this.image = image;
	}

	@Override
	public void process(T template) {
		this.template = template;
		intensity.reshape(image.width, image.height);

		int w = image.width - template.width;
		int h = image.height - template.height;

		borderX0 = template.width / 2;
		borderY0 = template.height / 2;
		borderX1 = template.width-borderX0;
		borderY1 = template.height-borderY0;

		for (int y = 0; y < h; y++) {
			int index = intensity.startIndex + (y + borderY0) * intensity.stride + borderX0;
			for (int x = 0; x < w; x++) {
				intensity.data[index++] = evaluate(x, y);
			}
		}

		// deference to avoid causing a memory leak
		this.template = null;
		this.mask = null;
	}

	@Override
	public void process(T template, T mask ) {
		if( mask == null ) {
			process(template);
			return;
		}

		this.template = template;
		this.mask = mask;
		intensity.reshape(image.width, image.height);

		int w = image.width - template.width;
		int h = image.height - template.height;

		borderX0 = template.width / 2;
		borderY0 = template.height / 2;

		for (int y = 0; y < h; y++) {
			int index = intensity.startIndex + (y + borderY0) * intensity.stride + borderX0;
			for (int x = 0; x < w; x++) {
				intensity.data[index++] = evaluateMask(x, y);
			}
		}

		// deference to avoid causing a memory leak
		this.template = null;
		this.mask = null;
	}

	/**
	 * Evaluate the template at the specified location.
	 *
	 * @param tl_x Template's top left corner x-coordinate
	 * @param tl_y Template's top left corner y-coordinate
	 * @return match value with better matches having a more positive value
	 */
	protected abstract float evaluate(int tl_x, int tl_y);

	/**
	 * Evaluate the masked template at the specified location.
	 *
	 * @param tl_x Template's top left corner x-coordinate
	 * @param tl_y Template's top left corner y-coordinate
	 * @return match value with better matches having a more positive value
	 */
	protected abstract float evaluateMask(int tl_x, int tl_y);

	@Override
	public GrayF32 getIntensity() {
		return intensity;
	}

	@Override
	public int getBorderX0() {
		return borderX0;
	}

	@Override
	public int getBorderY0() {
		return borderY0;
	}

	@Override
	public int getBorderX1() {
		return borderX1;
	}

	@Override
	public int getBorderY1() {
		return borderY1;
	}
}
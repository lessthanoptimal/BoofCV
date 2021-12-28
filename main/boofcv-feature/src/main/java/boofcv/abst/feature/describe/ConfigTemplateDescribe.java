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

package boofcv.abst.feature.describe;

import boofcv.struct.Configuration;

/**
 * Template based image descriptor. Template descriptors use each pixel in the region as a feature.
 *
 * @author Peter Abeles
 */
public class ConfigTemplateDescribe implements Configuration {
	/**
	 * The type of descriptor. Not all error types can be used with all descriptors.
	 */
	public Type type = Type.NCC;

	/**
	 * The region's width
	 */
	public int width = 11;
	/**
	 * The region's height.
	 */
	public int height = 11;

	@Override public void checkValidity() {}

	public enum Type {
		/**
		 * The raw image values are used.
		 */
		PIXEL,
		/**
		 * Normalized Cross Correlation. Tends to be more stable and lighting invariant than PIXEL
		 */
		NCC
	}

	public ConfigTemplateDescribe setTo( ConfigTemplateDescribe src ) {
		this.type = src.type;
		this.width = src.width;
		this.height = src.height;
		return this;
	}
}

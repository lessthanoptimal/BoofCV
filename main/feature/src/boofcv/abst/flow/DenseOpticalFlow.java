/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.flow;

import boofcv.struct.flow.ImageFlow;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

/**
 * High level interface for computing the dense optical flow across the whole image.
 *
 * @author Peter Abeles
 */
public interface DenseOpticalFlow<T extends ImageBase> {

	/**
	 * Computes the optical flow.
	 *
	 * @param source (Input) First image
	 * @param destination (Input) Second image
	 * @param flow (Output) Computed flow information from source to destination
	 */
	public void process( T source , T destination, ImageFlow flow );

	/**
	 * Input image type
	 * @return image type
	 */
	public ImageType<T> getInputType();
}

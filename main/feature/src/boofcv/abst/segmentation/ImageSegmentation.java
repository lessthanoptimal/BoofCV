/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.segmentation;

import boofcv.struct.ConnectRule;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageType;

/**
 * High level interface for image segmentation.  Each pixel in the segmented image is assigned an integer label
 * to identify which region in the image it belongs to.  A region is a continuous connected set of pixels.
 *
 * @author Peter Abeles
 */
// TODO add size to the interface since everything computes the size?
public interface ImageSegmentation<T extends ImageBase> {

	public void segment( T input , ImageSInt32 output );

	public int getTotalSegments();

	public ConnectRule getRule();

	public ImageType<T> getImageType();
}

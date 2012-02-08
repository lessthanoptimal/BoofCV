/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.core.image;

import boofcv.struct.image.ImageSingleBand;

/**
 * Generalized interface for single banded images.  Due to the slow performance associated with working with this
 * interface its usage is not recommended except for testing purposes.
 *
 * @author Peter Abeles
 */
public interface GImageSingleBand {

	int getWidth();

	int getHeight();

	boolean isFloatingPoint();

	Number get( int x , int y );

	void set( int x , int y , Number num );

	ImageSingleBand getImage();
}

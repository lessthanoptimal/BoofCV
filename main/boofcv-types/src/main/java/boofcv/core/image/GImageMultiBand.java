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

package boofcv.core.image;

import boofcv.struct.image.ImageBase;

/**
 * Generalized interface for working with multi-band images
 *
 * @author Peter Abeles
 */
public interface GImageMultiBand {

	void wrap( ImageBase image );

	int getWidth();

	int getHeight();

	int getNumberOfBands();

	int getPixelStride();

	int getIndex( int x, int y );

	void set( int x, int y, float[] value );

	void get( int x, int y, float[] value );

	Number get( int x, int y, int band );

	void setF( int index, float[] value );

	void getF( int index, float[] value );

	float getF( int index );

	<T extends ImageBase<T>> T getImage();
}

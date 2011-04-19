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

package gecv.filter.derivative;

import gecv.struct.image.ImageFloat32;


/**
 * A generic interface for computing image derivative along the x and y axes for {@link ImageFloat32}.
 *
 * @author Peter Abeles
 */
public interface DerivativeXY_F32 {

	public void setOutputs(ImageFloat32 derivX, ImageFloat32 derivY);

	public void setInputs(ImageFloat32 image);

	public void createOutputs(int imageWidth, int imageHeight);

	public void process();

	/**
	 * How many pixels wide is the region that is not processed along the outside
	 * border of the image.
	 *
	 * @return number of pixels.
	 */
	public int getBorder();

	public ImageFloat32 getDerivX();

	public ImageFloat32 getDerivY();
}

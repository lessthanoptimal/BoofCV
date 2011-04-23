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

package gecv.abst.filter.derivative;

import gecv.struct.image.ImageFloat32;


/**
 * An abstract class that does implements functiosn relating to setting and getting input and output
 * images.
 *
 * @author Peter Abeles
 */
public abstract class DerivativeXYBase_F32 implements DerivativeXY_F32 {

	protected ImageFloat32 image;
	protected ImageFloat32 derivX;
	protected ImageFloat32 derivY;

	@Override
	public void setOutputs(ImageFloat32 derivX, ImageFloat32 derivY) {
		this.derivX = derivX;
		this.derivY = derivY;
	}

	@Override
	public void createOutputs(int imageWidth, int imageHeight) {
		this.derivX = new ImageFloat32(imageWidth, imageHeight);
		this.derivY = new ImageFloat32(imageWidth, imageHeight);
	}

	@Override
	public void setInputs(ImageFloat32 image) {
		this.image = image;
	}

	@Override
	public ImageFloat32 getDerivX() {
		return derivX;
	}

	@Override
	public ImageFloat32 getDerivY() {
		return derivY;
	}
}

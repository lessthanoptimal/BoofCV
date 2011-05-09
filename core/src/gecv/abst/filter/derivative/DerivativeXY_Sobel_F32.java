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

import gecv.alg.filter.derivative.GradientSobel;
import gecv.struct.image.ImageFloat32;


/**
 * @author Peter Abeles
 */
public class DerivativeXY_Sobel_F32 implements DerivativeXY<ImageFloat32, ImageFloat32> {

	@Override
	public void process(ImageFloat32 inputImage , ImageFloat32 derivX, ImageFloat32 derivY) {
		GradientSobel.process(inputImage, derivX, derivY);
	}

	@Override
	public int getBorder() {
		return 1;
	}
}

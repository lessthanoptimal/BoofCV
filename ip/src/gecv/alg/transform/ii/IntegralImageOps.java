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

package gecv.alg.transform.ii;

import gecv.alg.InputSanityCheck;
import gecv.alg.transform.ii.impl.ImplIntegralImageOps;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt32;
import gecv.struct.image.ImageUInt8;


/**
 * @author Peter Abeles
 */
public class IntegralImageOps {

	public ImageFloat32 transform( ImageFloat32 input , ImageFloat32 transformed ) {
		transformed = InputSanityCheck.checkDeclare(input,transformed);

		ImplIntegralImageOps.process(input,transformed);

		return transformed;
	}

	public ImageSInt32 transform( ImageUInt8 input , ImageSInt32 transformed ) {
		transformed = InputSanityCheck.checkDeclare(input,transformed,ImageSInt32.class);

		ImplIntegralImageOps.process(input,transformed);

		return transformed;
	}
}

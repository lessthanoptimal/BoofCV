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

package gecv.alg.detect.corner;

import gecv.alg.InputSanityCheck;
import gecv.alg.detect.corner.impl.ImplLaplaceBlobIntensity;
import gecv.struct.image.ImageFloat32;

/**
 * <p>
 * Detects "blob" intensity using the Laplacian. TODO more on theory
 * </p>
 *
 * <p>
 * <ul>
 * <li>Determinant: |D<sub>xx</sub>*D<sub>yy</sub> + D<sub>xy</sub><sup>2</sup>|</li>
 * <li>Trace: |D<sub>xx</sub> + D<sub>yy</sub>|</li>
 * </ul>
 * </p>
 *
 * @author Peter Abeles
 */
// todo change KitRos to this same format
public class LaplaceBlobIntensity {

	public static enum Type
	{
		DETERMINANT,
		TRACE,
		QUICK
	}

	public static void determinant( ImageFloat32 featureIntensity , ImageFloat32 hessianXX, ImageFloat32 hessianYY , ImageFloat32 hessianXY )
	{
		InputSanityCheck.checkSameShape(featureIntensity,hessianXX,hessianYY,hessianXY);

		ImplLaplaceBlobIntensity.determinant(featureIntensity,hessianXX,hessianYY,hessianXY);
	}

	public static void trace( ImageFloat32 featureIntensity , ImageFloat32 hessianXX, ImageFloat32 hessianYY )
	{
		InputSanityCheck.checkSameShape(featureIntensity,hessianXX,hessianYY);

		ImplLaplaceBlobIntensity.trace(featureIntensity,hessianXX,hessianYY);
	}
}

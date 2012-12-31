/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.intensity;

import boofcv.alg.filter.derivative.GradientSobel;
import boofcv.alg.filter.derivative.HessianThree;
import boofcv.core.image.border.*;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt16;

/**
 * Tests basic properties of a corner detector which use the image gradient
 *
 * @author Peter Abeles
 */
public abstract class GenericCornerIntensityGradientTests extends GenericCornerIntensityTests {

	protected ImageSInt16 derivX_I16 = new ImageSInt16(width,height);
	protected ImageSInt16 derivY_I16 = new ImageSInt16(width,height);
	protected ImageSInt16 derivXX_I16 = new ImageSInt16(width,height);
	protected ImageSInt16 derivYY_I16 = new ImageSInt16(width,height);
	protected ImageSInt16 derivXY_I16 = new ImageSInt16(width,height);

	protected ImageFloat32 derivX_F32 = new ImageFloat32(width,height);
	protected ImageFloat32 derivY_F32 = new ImageFloat32(width,height);
	protected ImageFloat32 derivXX_F32 = new ImageFloat32(width,height);
	protected ImageFloat32 derivYY_F32 = new ImageFloat32(width,height);
	protected ImageFloat32 derivXY_F32 = new ImageFloat32(width,height);

	ImageBorder_F32 borderF32 = new ImageBorder1D_F32(BorderIndex1D_Extend.class);
	ImageBorder_I32 borderI32 = new ImageBorder1D_I32(BorderIndex1D_Extend.class);

	@Override
	protected void computeDerivatives() {
		GradientSobel.process(imageF,derivX_F32,derivY_F32, borderF32);
		GradientSobel.process(imageI,derivX_I16,derivY_I16, borderI32);
		HessianThree.process(imageF,derivXX_F32,derivYY_F32,derivXY_F32,borderF32);
		HessianThree.process(imageI,derivXX_I16,derivYY_I16,derivXY_I16,borderI32);
	}
}

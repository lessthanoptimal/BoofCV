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

package boofcv.abst.feature.detect.intensity;

import boofcv.alg.feature.detect.intensity.HessianBlobIntensity;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Wrapper around {@link boofcv.alg.feature.detect.intensity.HessianBlobIntensity} for {@link GeneralFeatureIntensity}.
 *
 * @author Peter Abeles
 */
public class WrapperLaplacianBlobIntensity <I extends ImageBase, D extends ImageBase> implements GeneralFeatureIntensity<I,D> {

	HessianBlobIntensity.Type type;
	ImageFloat32 intensity = new ImageFloat32(1,1);
	Method m;

	public WrapperLaplacianBlobIntensity(HessianBlobIntensity.Type type, Class<D> derivType ) {
		this.type = type;
		try {
			switch( type ) {
				case DETERMINANT:
					m = HessianBlobIntensity.class.getMethod("determinant",ImageFloat32.class,derivType,derivType,derivType);
					break;

				case TRACE:
					m = HessianBlobIntensity.class.getMethod("trace",ImageFloat32.class,derivType,derivType);
					break;

				default:
					throw new RuntimeException("Not supported yet");
			}
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void process(I image, D derivX, D derivY, D derivXX, D derivYY, D derivXY) {

		intensity.reshape(image.width,image.height);

		try {
			switch( type ) {
				case DETERMINANT:
					m.invoke(null,intensity,derivXX,derivYY,derivXY);
					break;

				case TRACE:
					m.invoke(null,intensity,derivXX,derivYY);
					break;
			}
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public ImageFloat32 getIntensity() {
		return intensity;
	}

	@Override
	public QueueCorner getCandidates() {
		return null;
	}

	@Override
	public boolean getRequiresGradient() {
		return false;
	}

	@Override
	public boolean getRequiresHessian() {
		return( true );
	}

	@Override
	public boolean hasCandidates() {
		return false;
	}
}

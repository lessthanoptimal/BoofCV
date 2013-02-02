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

package boofcv.abst.sfm.d2;

import boofcv.alg.sfm.d2.ImageMotionPtkSmartRespawn;
import boofcv.struct.image.ImageBase;
import georegression.struct.InvertibleTransform;

/**
 * Wrapper around {@link boofcv.alg.sfm.d2.ImageMotionPtkSmartRespawn} for {@link ImageMotion2D}.
 *
 * @author Peter Abeles
 */
public class WrapImageMotionPtkSmartRespawn<T extends ImageBase, IT extends InvertibleTransform>
		implements ImageMotion2D<T,IT>
{
	ImageMotionPtkSmartRespawn<T,IT> alg;
	boolean first = true;

	public WrapImageMotionPtkSmartRespawn(ImageMotionPtkSmartRespawn<T, IT> alg) {
		this.alg = alg;
	}

	@Override
	public boolean process(T input) {
		boolean ret = alg.process(input);
		if( first ) {
			alg.getMotion().changeKeyFrame(true);
			first = false;
			return true;
		}
		return ret;
	}

	@Override
	public void reset() {
		first = true;
		alg.getMotion().reset();
	}

	@Override
	public void setToFirst() {
		// TODO this will force new features to be detected.  instead just adjust the initial transform
		alg.getMotion().changeKeyFrame(true);
	}

	@Override
	public IT getFirstToCurrent() {
		return alg.getMotion().getWorldToCurr();
	}

	@Override
	public Class<IT> getTransformType() {
		return alg.getMotion().getModelType();
	}
}

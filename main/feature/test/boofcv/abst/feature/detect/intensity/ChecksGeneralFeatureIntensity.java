/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.detect.intensity;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertTrue;

/**
 * Unit tests for implementators of {@link GeneralFeatureIntensity}.
 *
 * @author Peter Abeles
 */
public abstract class ChecksGeneralFeatureIntensity<I extends ImageGray, D extends ImageGray>
{
	public List<Class> listInputTypes = new ArrayList<>();
	public List<Class> listDerivTypes = new ArrayList<>();


	Random rand = new Random(234);
	int width = 30;
	int height = 40;

	I input;
	D derivX,derivY,derivXX,derivYY,derivXY;

	public void addTypes( Class inputType , Class derivType ) {
		listInputTypes.add( inputType );
		listDerivTypes.add( derivType );
	}

	public abstract GeneralFeatureIntensity<I,D> createAlg(Class<I> imageType, Class<D> derivType);

	/**
	 * For features which do not process the image border, the border should have a response of zero.
	 * A bug was found where if the input image size was changed the border would have "residual"
	 * values from past runs and not be zero.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void checkReshapeBorder() {
		for( int i = 0; i < listInputTypes.size(); i++ ) {
			checkReshapeBorder( listInputTypes.get(i), listDerivTypes.get(i));
		}
	}
	public void checkReshapeBorder( Class<I> imageType , Class<D> derivType ) {
		randomInit(imageType,derivType,width, height);

		GeneralFeatureIntensity<I,D> alg = createAlg(imageType,derivType);
		alg.process(input,derivX,derivY,derivXX,derivYY,derivXY);

		GrayF32 intensity = alg.getIntensity();

		int r = alg.getIgnoreBorder();
		checkBorderZero(intensity, r);

		// process again with smaller images
		randomInit(imageType,derivType,width-1, height-1);
		alg.process(input, derivX, derivY, derivXX, derivYY, derivXY);
		intensity = alg.getIntensity();
		checkBorderZero(intensity, r);
	}

	private void randomInit(Class<I> imageType , Class<D> derivType , int width, int height) {
		input = GeneralizedImageOps.createSingleBand(imageType, width, height);
		derivX = GeneralizedImageOps.createSingleBand(derivType,width,height);
		derivY = GeneralizedImageOps.createSingleBand(derivType,width,height);
		derivXX = GeneralizedImageOps.createSingleBand(derivType,width,height);
		derivYY = GeneralizedImageOps.createSingleBand(derivType,width,height);
		derivXY = GeneralizedImageOps.createSingleBand(derivType,width,height);

		GImageMiscOps.fillUniform(input, rand, 0, 255);
		GImageMiscOps.fillUniform(derivX, rand, -100, 100);
		GImageMiscOps.fillUniform(derivY, rand, -100, 100);
		GImageMiscOps.fillUniform(derivXX, rand, -100, 100);
		GImageMiscOps.fillUniform(derivYY, rand, -100, 100);
		GImageMiscOps.fillUniform(derivXY, rand, -100, 100);
	}

	private void checkBorderZero(GrayF32 intensity, int r) {
		for( int y = 0; y < intensity.height; y++ ) {
			if( y >= r && y < intensity.height-r )
				continue;

			for( int x = 0; x < intensity.width; x++ ) {
				if( x >= r && x < intensity.width-r )
					continue;

				assertTrue(0 == intensity.get(x, y));
			}
		}
	}
}

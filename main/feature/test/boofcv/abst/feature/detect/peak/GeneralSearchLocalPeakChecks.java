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

package boofcv.abst.feature.detect.peak;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.convolve.Kernel2D_F32;
import boofcv.struct.image.GrayF32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public abstract class GeneralSearchLocalPeakChecks {

	Class<GrayF32> imageType = GrayF32.class;
	GrayF32 image = new GrayF32(30,40);

	protected float toleranceCenter = 0.1f;

	public abstract SearchLocalPeak createSearch( Class<GrayF32> imageType );

	@Test
	public void gaussian() {
		ImageMiscOps.fill(image,0);
		Kernel2D_F32 k = FactoryKernelGaussian.gaussian(2,true,32,-1,5);

		int cx = 12;
		int cy = 15;

		for( int j = 0; j < 11; j++ ) {
			for( int i = 0; i < 11; i++ ) {
				image.set(i+cx-5,j+cy-5,k.get(i,j));
			}
		}

		SearchLocalPeak<GrayF32> search = createSearch(imageType);
		search.setImage(image);
		search.setSearchRadius(5);

		searchSolution(cx, cy, search);

	}

	@Test
	public void impulse() {
		ImageMiscOps.fill(image,0);

		int cx = 12;
		int cy = 15;

		image.set(cx,cy,10);

		SearchLocalPeak<GrayF32> search = createSearch(imageType);
		search.setImage(image);
		search.setSearchRadius(5);

		searchSolution(cx, cy, search);
	}

	private void searchSolution(int cx, int cy, SearchLocalPeak search) {
		for( int i = -1; i <= 1; i++ ) {
			for( int j = -1; j <= 1; j++ ) {

				float startX = cx+j;
				float startY = cy+i;

				if( startX < 0 ) startX = 0;
				if( startX > image.width-1) startX = image.width-1;

				if( startY < 0 ) startY = 0;
				if( startY > image.height-1) startY = image.height-1;

				search.search(startX,startY);
				assertEquals(cx,search.getPeakX(), toleranceCenter);
				assertEquals(cy,search.getPeakY(), toleranceCenter);
			}
		}
	}

	@Test
	public void edgeCaseLeft() {
		ImageMiscOps.fill(image,0);

		image.set(1,10,10);

		SearchLocalPeak<GrayF32> search = createSearch(imageType);
		search.setImage(image);
		search.setSearchRadius(5);

		searchSolution(1, 10, search);
	}

	@Test
	public void edgeCaseRight() {
		ImageMiscOps.fill(image,0);

		image.set(image.width-2,10,10);

		SearchLocalPeak<GrayF32> search = createSearch(imageType);
		search.setImage(image);
		search.setSearchRadius(5);

		searchSolution(image.width-2, 10, search);
	}

	@Test
	public void edgeCaseTop() {
		ImageMiscOps.fill(image,0);

		image.set(10,1,10);

		SearchLocalPeak<GrayF32> search = createSearch(imageType);
		search.setImage(image);
		search.setSearchRadius(5);

		searchSolution(10, 1, search);
	}

	@Test
	public void edgeCaseBottom() {
		ImageMiscOps.fill(image,0);

		image.set(10,image.height-2,10);

		SearchLocalPeak<GrayF32> search = createSearch(imageType);
		search.setImage(image);
		search.setSearchRadius(5);

		searchSolution(10, image.height-2, search);
	}
}

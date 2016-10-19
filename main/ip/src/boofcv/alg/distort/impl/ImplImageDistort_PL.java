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

package boofcv.alg.distort.impl;

import boofcv.alg.distort.ImageDistort;
import boofcv.struct.distort.PixelTransform2_F32;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.Planar;

/**
 * Implementation of {@link ImageDistort} for {@link Planar} images.
 * 
 * @author Peter Abeles
 */
public class ImplImageDistort_PL<Input extends ImageGray,Output extends ImageGray>
		implements ImageDistort<Planar<Input>,Planar<Output>> {

	ImageDistort<Input,Output> layerDistort;

	public ImplImageDistort_PL(ImageDistort<Input,Output> layerDistort) {
		this.layerDistort = layerDistort;
	}

	@Override
	public void setModel(PixelTransform2_F32 dstToSrc) {
		layerDistort.setModel(dstToSrc);
	}

	@Override
	public void apply(Planar<Input> srcImg, Planar<Output> dstImg) {
		if( srcImg.getNumBands() != dstImg.getNumBands() )
			throw new IllegalArgumentException("Number of bands must be the same");
		int N = srcImg.getNumBands();
		
		for( int i = 0; i < N; i++ ) {
			layerDistort.apply(srcImg.getBand(i),dstImg.getBand(i));
		}
	}

	@Override
	public void apply(Planar<Input> srcImg, Planar<Output> dstImg,
					  int dstX0, int dstY0, int dstX1, int dstY1) {
		if( srcImg.getNumBands() != dstImg.getNumBands() )
			throw new IllegalArgumentException("Number of bands must be the same");
		int N = srcImg.getNumBands();

		for( int i = 0; i < N; i++ ) {
			layerDistort.apply(srcImg.getBand(i),dstImg.getBand(i),dstX0, dstY0, dstX1, dstY1);
		}
	}

	@Override
	public void setRenderAll(boolean renderAll) {
		layerDistort.setRenderAll(renderAll);
	}

	@Override
	public boolean getRenderAll() {
		return layerDistort.getRenderAll();
	}
}

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

package boofcv.abst.segmentation;

import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public abstract class GeneralImageSuperpixelsChecks<T extends ImageBase> {

	ImageType imageTypes[];

	Random rand = new Random(234);

	int width = 20;
	int height = 30;

	public GeneralImageSuperpixelsChecks(ImageType... types) {
		this.imageTypes = types;
	}

	public abstract ImageSuperpixels<T> createAlg( ImageType<T> imageType );

	/**
	 * Makes sure all pixels with the same label are connected
	 */
	@Test
	public void connectivity() {
		for( ImageType<T> t : imageTypes ) {
//			System.out.println("Image type "+t);

			ImageSuperpixels<T> alg = createAlg(t);

			T input = t.createImage(width, height);
			GrayS32 output = new GrayS32(width, height);

			GImageMiscOps.fillUniform(input, rand, 0, 100);

			alg.segment(input, output);

			assertTrue(alg.getTotalSuperpixels() > 4);

			GrayU8 binary = new GrayU8(width,height);
			boolean selected[] = new boolean[ alg.getTotalSuperpixels()];

			for (int i = 0; i < alg.getTotalSuperpixels(); i++) {
				selected[i] = true;
				BinaryImageOps.labelToBinary(output,binary,selected);
				selected[i] = false;

				// the number of blobs should always be one
				ConnectRule rule = alg.getRule();
				assertEquals(1,BinaryImageOps.contour(binary,rule,null).size());
			}
		}
	}

	/**
	 * Make sure subimages produce the same results
	 */
	@Test
	public void subimage() {
		for( ImageType<T> t : imageTypes ) {
//			System.out.println("Image type "+t);

			ImageSuperpixels<T> alg = createAlg(t);

			T input = t.createImage(width,height);
			GrayS32 expected = new GrayS32(width,height);

			GImageMiscOps.fillUniform(input,rand,0,100);

			alg.segment(input,expected);

			// provide an output which is a sub-image
			GrayS32 found = new GrayS32(width+3,height+2).subimage(2,1,width+2,height+1);
			alg.segment(input,found);
			BoofTesting.assertEquals(expected,found,0);

			// Now make the input image an output
			input = BoofTesting.createSubImageOf(input);
			found = new GrayS32(width,height);

			alg.segment(input,found);
			BoofTesting.assertEquals(expected,found,0);
		}
	}

	/**
	 * Makes sure that there really are regions 0 N-1 in the output image
	 */
	@Test
	public void sequentialNumbers() {
		for( ImageType<T> t : imageTypes ) {

			ImageSuperpixels<T> alg = createAlg(t);

			T input = t.createImage(width,height);
			GrayS32 output = new GrayS32(width,height);
			GImageMiscOps.fillUniform(input,rand,0,100);

			alg.segment(input,output);

			int N = alg.getTotalSuperpixels();
			assertTrue(N > 2);

			boolean found[] = new boolean[N];

			for( int y = 0; y < height; y++ ) {
				for( int x = 0; x < width; x++ ) {
					found[ output.get(x,y) ] = true;
				}
			}

			for( int i = 0; i < N; i++ ) {
				assertTrue(found[i]);
			}
		}
	}

	/**


	/**
	 * Produces the same results when run multiple times
	 */
	@Test
	public void multipleCalls() {
		for( ImageType<T> t : imageTypes ) {

			ImageSuperpixels<T> alg = createAlg(t);

			T input = t.createImage(width,height);
			GrayS32 output = new GrayS32(width,height);
			GImageMiscOps.fillUniform(input,rand,0,100);

			alg.segment(input,output);

			GrayS32 output2 = new GrayS32(width,height);
			alg.segment(input,output2);

			for( int y = 0; y < height; y++ ) {
				for( int x = 0; x < width; x++ ) {
					assertEquals(output.get(x,y),output2.get(x,y));
				}
			}
		}
	}

	/**
	 * See if it won't blow up if input image size is changed
	 */
	@Test
	public void changeInImageSize() {
		for( ImageType<T> t : imageTypes ) {

			ImageSuperpixels<T> alg = createAlg(t);

			T input = t.createImage(width/2,height/2);
			GrayS32 output = new GrayS32(width/2,height/2);
			GImageMiscOps.fillUniform(input,rand,0,100);

			alg.segment(input,output);

			input = t.createImage(width,height);
			output = new GrayS32(width,height);

			alg.segment(input,output);
		}
	}

}

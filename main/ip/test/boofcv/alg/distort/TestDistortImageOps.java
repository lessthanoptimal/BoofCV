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

package boofcv.alg.distort;

import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.affine.Affine2D_F32;
import georegression.struct.shapes.Rectangle2D_F32;
import georegression.struct.shapes.Rectangle2D_I32;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class TestDistortImageOps {

	Random rand = new Random(234);
	int width = 20;
	int height = 30;

	/**
	 * Checks to see if the two ways of specifying interpolation work
	 */
	@Test
	public void scale_InterpTypeStyle() {
		ImageFloat32 input = new ImageFloat32(width,height);
		ImageFloat32 output = new ImageFloat32(width,height);

		GImageMiscOps.fillUniform(input, rand, 0, 100);

		DistortImageOps.scale(input,output, TypeInterpolate.BILINEAR);

		InterpolatePixel<ImageFloat32> interp = FactoryInterpolation.bilinearPixel(input);
		interp.setImage(input);

		float scaleX = (float)input.width/(float)output.width;
		float scaleY = (float)input.height/(float)output.height;

		if( input.getTypeInfo().isInteger() ) {
			for( int i = 0; i < output.height; i++ ) {
				for( int j = 0; j < output.width; j++ ) {
					float val = interp.get(j*scaleX,i*scaleY);
					assertEquals((int)val,output.get(j,i),1e-4);
				}
			}
		} else {
			for( int i = 0; i < output.height; i++ ) {
				for( int j = 0; j < output.width; j++ ) {
					float val = interp.get(j*scaleX,i*scaleY);
					assertEquals(val,output.get(j,i),1e-4);
				}
			}
		}
	}

	@Test
	public void scaleSanityCheck() {
		ImageFloat32 input = new ImageFloat32(width,height);
		ImageFloat32 output = new ImageFloat32(width/2,height/2);

		GImageMiscOps.fillUniform(input, rand, 0, 100);

		DistortImageOps.scale(input, output, TypeInterpolate.BILINEAR);

		double error = 0;
		for( int y = 0; y < output.height; y++ ) {
			for( int x = 0; x < output.width; x++ ) {
				double e = input.get(x*2,y*2)-output.get(x,y);
				error += Math.abs(e);
			}
		}
		assertTrue(error / (output.width * output.height) < 0.1);
	}

	/**
	 * Very simple test for rotation accuracy.
	 */
	@Test
	public void rotate_SanityCheck() {
		ImageFloat32 input = new ImageFloat32(width,height);
		ImageFloat32 output = new ImageFloat32(height,width);

		GImageMiscOps.fillUniform(input, rand, 0, 100);

		DistortImageOps.rotate(input, output, TypeInterpolate.BILINEAR, (float) Math.PI / 2f);

		double error = 0;
		// the outside pixels are ignored because numerical round off can cause those to be skipped
		for( int y = 1; y < input.height-1; y++ ) {
			for( int x = 1; x < input.width-1; x++ ) {
				int xx = output.width-y;
				int yy = x;

				double e = input.get(x,y)-output.get(xx,yy);
				error += Math.abs(e);
			}
		}
		assertTrue(error / (width * height) < 0.1);
	}

	/**
	 * boundBox that checks to see if it is contained inside the output image.
	 */
	@Test
	public void boundBox_check() {

		// basic sanity check
		Affine2D_F32 affine = new Affine2D_F32(1,0,0,1,2,3);
		PixelTransformAffine_F32 transform = new PixelTransformAffine_F32(affine);
		Rectangle2D_I32 found = DistortImageOps.boundBox(10,20,30,40,transform);
		
		assertEquals(2,found.tl_x);
		assertEquals(3,found.tl_y);
		assertEquals(10,found.width);
		assertEquals(20,found.height);
		
		// bottom right border
		found = DistortImageOps.boundBox(10,20,8,18,transform);
		assertEquals(2,found.tl_x);
		assertEquals(3,found.tl_y);
		assertEquals(6,found.width);
		assertEquals(15,found.height);
		
		// top right border
		affine.set(new Affine2D_F32(1,0,0,1,-2,-3));
		found = DistortImageOps.boundBox(10,20,8,18,transform);
		assertEquals(0,found.tl_x);
		assertEquals(0,found.tl_y);
		assertEquals(8,found.width);
		assertEquals(17,found.height);
	}

	@Test
	public void boundBox() {

		// basic sanity check
		Affine2D_F32 affine = new Affine2D_F32(1,0,0,1,2,3);
		PixelTransformAffine_F32 transform = new PixelTransformAffine_F32(affine);
		Rectangle2D_I32 found = DistortImageOps.boundBox(10,20,transform);

		assertEquals(2,found.tl_x);
		assertEquals(3,found.tl_y);
		assertEquals(10,found.width);
		assertEquals(20,found.height);
	}

	@Test
	public void boundBox_F32() {

		// basic sanity check
		Affine2D_F32 affine = new Affine2D_F32(1,0,0,1,2,3);
		PixelTransformAffine_F32 transform = new PixelTransformAffine_F32(affine);
		Rectangle2D_F32 found = DistortImageOps.boundBox_F32(10,20,transform);

		assertEquals(2,found.tl_x,1e-4);
		assertEquals(3,found.tl_y,1e-4);
		assertEquals(10,found.width,1e-4);
		assertEquals(20,found.height,1e-4);
	}
}

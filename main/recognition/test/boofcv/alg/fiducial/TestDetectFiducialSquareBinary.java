/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.shapes.polygon.BinaryPolygonConvexDetector;
import boofcv.core.image.ConvertImage;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.transform.se.SePointOps_F64;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestDetectFiducialSquareBinary {

	Random rand = new Random(234);
	BinaryPolygonConvexDetector squareDetector = FactoryShapeDetector.polygon(FactoryThresholdBinary.globalFixed(50, true, ImageUInt8.class)
			,new ConfigPolygonDetector(4,false),ImageUInt8.class);

	/**
	 * Makes sure the found rotation matrix is correct
	 */
	@Test
	public void checkFoundRotationMatrix() {
		IntrinsicParameters intrinsic =new IntrinsicParameters(500,500,0,320,240,640,480);

		ImageFloat32 rendered_F32 = create(DetectFiducialSquareBinary.w, 314);
		ImageUInt8 rendered = new ImageUInt8(rendered_F32.width,rendered_F32.height);
		ConvertImage.convert(rendered_F32,rendered);
		ImageUInt8 input = new ImageUInt8(640,480);
		ImageMiscOps.fill(input,255);
		input.subimage(200,250,200+rendered.width,250+rendered.height,null).setTo(rendered);

		DetectFiducialSquareBinary<ImageUInt8> alg = new DetectFiducialSquareBinary<ImageUInt8>(squareDetector,ImageUInt8.class);
		alg.setLengthSide(2);
		alg.configure(intrinsic,false);
		alg.process(input);

		assertEquals(1,alg.getFound().size());
		FoundFiducial ff = alg.getFound().get(0);

		// lower left hand corner in the fiducial.  side is of length 2
		Point3D_F64 lowerLeft = new Point3D_F64(-1,-1,0);
		Point3D_F64 cameraPt = new Point3D_F64();
		SePointOps_F64.transform(ff.targetToSensor, lowerLeft, cameraPt);
		Point2D_F64 pixelPt = new Point2D_F64();
		PerspectiveOps.convertNormToPixel(intrinsic, cameraPt.x / cameraPt.z, cameraPt.y / cameraPt.z, pixelPt);

		// see if that point projects into the correct location
		assertEquals(200,pixelPt.x,1e-4);
		assertEquals(250+rendered.height,pixelPt.y,1e-4);
	}

	/**
	 * Give it easy positive examples
	 */
	@Test
	public void processSquare() {
		for (int i = 0; i < 4; i++) {
			ImageFloat32 input = create(DetectFiducialSquareBinary.w, 314);

			for (int j = 0; j < i - 1; j++) {
				ImageMiscOps.rotateCCW(input.clone(), input);
			}
			DetectFiducialSquareBinary alg = new DetectFiducialSquareBinary(squareDetector,ImageUInt8.class);

			BaseDetectFiducialSquare.Result result = new BaseDetectFiducialSquare.Result();
			assertTrue(alg.processSquare(input, result));

			assertEquals(314, result.which);
			assertEquals(Math.max(0,i-1), result.rotation);
		}
	}

	/**
	 * Give it random noise.  It should fail
	 */
	@Test
	public void processSquare_negative() {
		ImageFloat32 input = create(DetectFiducialSquareBinary.w, 314);
		ImageMiscOps.fillUniform(input,rand,0,255);

		DetectFiducialSquareBinary alg = new DetectFiducialSquareBinary(squareDetector,ImageUInt8.class);

		BaseDetectFiducialSquare.Result result = new BaseDetectFiducialSquare.Result();
		assertFalse(alg.processSquare(input, result));
	}


	public static ImageFloat32 create( int square , int value ) {

		ImageFloat32 ret = new ImageFloat32(square*8,square*8);

		int s2 = 2*square;

		for (int i = 0; i < 12; i++) {
			if( (value& (1<<i)) != 0 )
				continue;

			int where = index(i);
			int x = where%4;
			int y = 3-(where/4);

			x = s2 + square*x;
			y = s2 + square*y;

			ImageMiscOps.fillRectangle(ret,0xFF,x,y,square,square);
		}
		ImageMiscOps.fillRectangle(ret,0xFF,s2,s2,square,square);
		ImageMiscOps.fillRectangle(ret,0xFF,square*5,square*5,square,square);
		ImageMiscOps.fillRectangle(ret,0xFF,square*5,s2,square,square);

		return ret;
	}

	private static int index( int bit ) {
		if( bit < 2 )
			bit++;
		else if( bit < 10 )
			bit += 2;
		else if( bit < 12 )
			bit += 3;
		else
			throw new RuntimeException("Bit must be between 0 and 11");

		return bit;
	}
}
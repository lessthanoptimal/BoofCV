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

package boofcv.alg.background.moving;

import boofcv.alg.background.BackgroundModelMoving;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofTesting;
import georegression.struct.homography.Homography2D_F32;
import georegression.struct.point.Point2D_F32;
import georegression.transform.homography.HomographyPointOps_F32;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public abstract class GenericBackgroundModelMovingChecks {

	Random rand = new Random(234);

	int width = 60;
	int height = 50;

	protected double backgroundOutsideTol = 0.01;

	protected List<ImageType> imageTypes = new ArrayList<>();

	public abstract<T extends ImageBase>
	BackgroundModelMoving<T,Homography2D_F32> create( ImageType<T> imageType );

	/**
	 * Basic check were multiple images are feed into the algorithm and another image,
	 * which has a region which is clearly different is then segmented.
	 */
	@Test
	public void basicCheck() {
		for( ImageType type : imageTypes ) {
			basicCheck(type);
		}
	}

	private <T extends ImageBase> void basicCheck( ImageType<T> imageType ) {

		BackgroundModelMoving<T,Homography2D_F32> alg = create(imageType);
		T frame = imageType.createImage(width,height);

		Homography2D_F32 homeToWorld = new Homography2D_F32(1,0,width/2,0,1,height/2,0,0,1);

		alg.initialize(width*2,height*2,homeToWorld);

		for (int i = 0; i < 30; i++) {
			Homography2D_F32 homeToCurrent = new Homography2D_F32();
			if( i > 0 ) {
				homeToCurrent.a13 = rand.nextFloat() * 5 - 2.5f;
				homeToCurrent.a23 = rand.nextFloat() * 5 - 2.5f;
			}
			noise(100, 2, frame);

			alg.updateBackground(new Homography2D_F32(),frame);
		}

		int x0 = 10, y0 = 12, x1 = 40, y1 = 38;

		noise(100,2,frame);
		GImageMiscOps.fillRectangle(frame,200,x0,y0,x1-x0,y1-y0);

		Homography2D_F32 homeToCurrent = new Homography2D_F32();
		GrayU8 segmented = new GrayU8(width,height);
		alg.segment(homeToCurrent, frame, segmented);

//		segmented.printBinary();

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if( x >= x0 && x < x1 && y >= y0 && y < y1 ) {
					assertEquals(1,segmented.get(x,y));
				} else {
					assertEquals(0,segmented.get(x,y));
				}
			}
		}
	}

	/**
	 * The current image is partially outside of the background image.  Check to see if it blows up
	 * and that segmented pixels are correctly marked as inside or outside
	 */
	@Test
	public void currentOutsideBackground() {
		for( ImageType type : imageTypes ) {
			currentOutsideBackground(type);
		}
	}

	private <T extends ImageBase>
	void currentOutsideBackground( ImageType<T> imageType ) {
		T frame = imageType.createImage(width, height);
		GrayU8 segmented = new GrayU8(width,height);

		BackgroundModelMoving<T, Homography2D_F32> alg = create(frame.getImageType());
		Homography2D_F32 homeToWorld = new Homography2D_F32();
		alg.initialize(width, height, homeToWorld);
		alg.setUnknownValue(2);

		double translationTol = backgroundOutsideTol/2;

		Homography2D_F32 homeToCurrent = new Homography2D_F32();

		homeToCurrent.a13 = 5;
		checkTransform(frame, segmented, alg, homeToCurrent,translationTol);

		homeToCurrent.a13 = -5;
		checkTransform(frame, segmented, alg, homeToCurrent,translationTol);

		homeToCurrent.a13 = 0;
		homeToCurrent.a23 = 5;
		checkTransform(frame, segmented, alg, homeToCurrent,translationTol);

		homeToCurrent.a23 = -5;
		checkTransform(frame, segmented, alg, homeToCurrent,translationTol);

		// make it more interesting
		homeToCurrent.set(1.0f, 0.6f, 20, -0.6f, 0.95f, 20, 0, 0, 1);
		checkTransform(frame, segmented, alg, homeToCurrent, backgroundOutsideTol);
	}

	private <T extends ImageBase> void checkTransform(T frame, GrayU8 segmented,
													  BackgroundModelMoving<T, Homography2D_F32> alg,
													  Homography2D_F32 homeToCurrent , double tol ) {

		Homography2D_F32 currentToHome = homeToCurrent.invert(null);

		alg.reset();
		alg.updateBackground(homeToCurrent, frame);
		alg.segment(homeToCurrent, frame, segmented);
		checkSegmented(currentToHome, segmented,tol);
		alg.segment(new Homography2D_F32(), frame, segmented);
		checkSegmented(homeToCurrent, segmented,tol);
	}

	/**
	 * Checks to see if pixels outside of BG are marked as unknown
	 */
	private void checkSegmented(Homography2D_F32 transform , GrayU8 segmented , double tol ) {

		Point2D_F32 p = new Point2D_F32();

		int numErrors = 0;
		for (int y = 0; y < segmented.height; y++) {
			for (int x = 0; x < segmented.width; x++) {
				HomographyPointOps_F32.transform(transform,x,y,p);
				int xx = (int)Math.floor(p.x);
				int yy = (int)Math.floor(p.y);

				if( segmented.isInBounds(xx,yy)) {
					if( segmented.get(x, y) == 2)
						numErrors++;
				}else {
					if( segmented.get(x, y) != 2)
						numErrors++;
				}
			}
		}

		assertTrue( numErrors/(double)(segmented.width*segmented.height) <=tol );
	}

	/**
	 * If a pixel in the current frame goes outside the background it should be marked as background
	 */
	@Test
	public void markNoBackgroundAsBackground() {
		for( ImageType type : imageTypes ) {
			markNoBackgroundAsBackground(type);
		}
	}

	private <T extends ImageBase>
	void markNoBackgroundAsBackground( ImageType<T> imageType ) {
		T frame = imageType.createImage(width, height);
		GrayU8 segmented = new GrayU8(width,height);

		BackgroundModelMoving<T, Homography2D_F32> alg = create(frame.getImageType());
		alg.setUnknownValue(2);
		Homography2D_F32 homeToWorld = new Homography2D_F32();
		alg.initialize(width, height, homeToWorld);

		Homography2D_F32 homeToCurrent = new Homography2D_F32();
		homeToCurrent.a13 = 5;
		alg.updateBackground(homeToCurrent,frame);
		alg.segment(homeToCurrent,frame,segmented);

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < 5; x++) {
				assertEquals(2,segmented.get(x,y));
			}
			for (int x = 5; x < width; x++) {
				assertEquals(0,segmented.get(x,y));
			}
		}
	}

	/**
	 * Mark pixels which haven't been observed as unknown
	 */
	@Test
	public void markUnobservedAsUnknown() {
		for( ImageType type : imageTypes ) {
			markUnobservedAsUnknown(type);
		}
	}

	private <T extends ImageBase>
	void markUnobservedAsUnknown( ImageType<T> imageType ) {
		T frame = imageType.createImage(width, height);
		GrayU8 segmented = new GrayU8(width,height);

		BackgroundModelMoving<T, Homography2D_F32> alg = create(frame.getImageType());
		alg.setUnknownValue(2);
		Homography2D_F32 homeToWorld = new Homography2D_F32();
		alg.initialize(width, height, homeToWorld);

		Homography2D_F32 homeToCurrent = new Homography2D_F32();
		alg.segment(homeToCurrent,frame,segmented);

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				assertEquals(2,segmented.get(x,y));
			}
		}
	}


	/**
	 * Sees if reset discard the previous history in the background image
	 */
	@Test
	public void reset() {
		for( ImageType type : imageTypes ) {
			reset(type);
		}
	}

	private <T extends ImageBase>
	void reset( ImageType<T> imageType ) {
		T frame = imageType.createImage(width, height);

		BackgroundModelMoving<T, Homography2D_F32> alg = create(frame.getImageType());
		Homography2D_F32 homeToWorld = new Homography2D_F32(1,0,width/2,0,1,height/2,0,0,1);
		alg.initialize(width*2,height*2,homeToWorld);

		Homography2D_F32 homeToCurrent = new Homography2D_F32();
		GImageMiscOps.fill(frame,100);
		alg.updateBackground(homeToCurrent,frame);
		alg.reset();
		GImageMiscOps.fill(frame,50);
		alg.updateBackground(homeToCurrent,frame);

		GrayU8 segmented = new GrayU8(width,height);
		GrayU8 expected = new GrayU8(width,height);

		// there should be no change
		// if reset isn't the case then this will fail
		alg.segment(homeToCurrent,frame,segmented);
		BoofTesting.assertEquals(expected,segmented,1e-8);

		GImageMiscOps.fill(frame,100);
		ImageMiscOps.fill(expected,1);

		// it should be all changed.  really just a sanity check
		alg.segment(homeToCurrent,frame,segmented);
		BoofTesting.assertEquals(expected,segmented,1e-8);
	}

	@Test
	public void checkSubImage() {
		for( ImageType type : imageTypes ) {
			checkSubImage(type);
		}
	}

	private <T extends ImageBase>
	void checkSubImage( ImageType<T> imageType ) {
		T frame = imageType.createImage(width, height);
		GrayU8 segmented = new GrayU8(width,height);

		checkSubImage_process(frame, segmented);
		GrayU8 expected = segmented.clone();

		frame = BoofTesting.createSubImageOf(frame);
		segmented = BoofTesting.createSubImageOf(segmented);
		ImageMiscOps.fill(segmented,0);

		checkSubImage_process(frame, segmented);
		GrayU8 found = segmented.clone();

		// see if both produce the same result

		BoofTesting.assertEquals(expected,found,1e-8);
	}

	private <T extends ImageBase>
	void checkSubImage_process( T frame, GrayU8 segmented)
	{
		rand = new Random(2345);

		BackgroundModelMoving<T, Homography2D_F32> alg = create(frame.getImageType());
		Homography2D_F32 homeToWorld = new Homography2D_F32(1,0,width/2,0,1,height/2,0,0,1);
		alg.initialize(width*2,height*2,homeToWorld);

		for (int i = 0; i < 5; i++) {
			Homography2D_F32 homeToCurrent = new Homography2D_F32();
			if( i > 0 ) {
				homeToCurrent.a13 = rand.nextFloat() * 5 - 2.5f;
				homeToCurrent.a23 = rand.nextFloat() * 5 - 2.5f;
			}
			noise(100, 30, frame);

			alg.updateBackground(homeToCurrent,frame);
		}

		Homography2D_F32 homeToCurrent = new Homography2D_F32();
		noise(100, 30, frame);
		alg.segment(homeToCurrent, frame, segmented);
	}

	private void noise( double mean , double range , ImageBase image ) {
		GImageMiscOps.fill(image,mean);
		GImageMiscOps.addUniform(image,rand,-range,range);
	}
}

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

package boofcv.alg.background.stationary;

import boofcv.alg.background.BackgroundModelStationary;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public abstract class GenericBackgroundModelStationaryChecks {
	Random rand = new Random(234);

	int width = 60;
	int height = 50;

	protected List<ImageType> imageTypes = new ArrayList<>();

	public abstract<T extends ImageBase>
	BackgroundModelStationary<T> create( ImageType<T> imageType );

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

		BackgroundModelStationary<T> alg = create(imageType);
		T frame = imageType.createImage(width,height);

		for (int i = 0; i < 30; i++) {
			noise(100, 2, frame);
			alg.updateBackground(frame);
		}

		int x0 = 10, y0 = 12, x1 = 40, y1 = 38;

		noise(100,2,frame);
		GImageMiscOps.fillRectangle(frame, 200, x0, y0, x1 - x0, y1 - y0);

		GrayU8 segmented = new GrayU8(width,height);
		alg.segment(frame, segmented);

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

		BackgroundModelStationary<T> alg = create(frame.getImageType());

		GImageMiscOps.fill(frame,100);
		alg.updateBackground(frame);
		alg.reset();
		GImageMiscOps.fill(frame,50);
		alg.updateBackground(frame);

		GrayU8 segmented = new GrayU8(width,height);
		GrayU8 expected = new GrayU8(width,height);

		// there should be no change
		// if reset isn't the case then this will fail
		alg.segment(frame,segmented);
		BoofTesting.assertEquals(expected, segmented, 1e-8);

		GImageMiscOps.fill(frame, 100);
		ImageMiscOps.fill(expected, 1);

		// it should be all changed.  really just a sanity check
		alg.segment(frame,segmented);
		BoofTesting.assertEquals(expected,segmented,1e-8);
	}

	/**
	 * The user tries to segment before specifying the background
	 */
	@Test
	public void segmentBeforeUpdateBackGround() {
		for( ImageType type : imageTypes ) {
			segmentBeforeUpdateBackGround(type);
		}
	}

	private <T extends ImageBase>
	void segmentBeforeUpdateBackGround( ImageType<T> imageType ) {
		T frame = imageType.createImage(width, height);

		BackgroundModelStationary<T> alg = create(frame.getImageType());

		alg.setUnknownValue(2);

		GrayU8 segmented = new GrayU8(width,height);
		GrayU8 expected = new GrayU8(width,height);
		ImageMiscOps.fill(expected, 2);

		alg.segment(frame, segmented);
		BoofTesting.assertEquals(expected, segmented, 1e-8);
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

		BackgroundModelStationary<T> alg = create(frame.getImageType());

		for (int i = 0; i < 5; i++) {
			noise(100, 30, frame);
			alg.updateBackground(frame);
		}

		noise(100, 30, frame);
		alg.segment(frame, segmented);
	}

	/**
	 * For each band in the image have all put one be filled with a constant uniform color.
	 * Alternate which band has motion in it.
	 */
	@Test
	public void checkBandsUsed() {
		for( ImageType type : imageTypes ) {
			checkBandsUsed(type);
		}
	}

	private <T extends ImageBase> void checkBandsUsed( ImageType<T> imageType ) {

		BackgroundModelStationary<T> alg = create(imageType);
		T frame = imageType.createImage(width,height);

		int numBands = imageType.getNumBands();
		for (int band = 0; band < numBands; band++) {
			alg.reset();
			for (int i = 0; i < 30; i++) {
				noiseBand(100, 2, frame, band);
				alg.updateBackground(frame);
			}

			GrayU8 segmented = new GrayU8(width,height);

			// segment with the current frame.  should be no motion
			alg.segment(frame, segmented);
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					assertEquals(0,segmented.get(x,y));
				}
			}

			// now the whole image should report motion
			noiseBand(200, 2, frame, band);
			alg.segment(frame, segmented);
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					assertEquals(1,segmented.get(x,y));
				}
			}
		}
	}

	protected void noiseBand( double mean , double range , ImageBase image , int band ) {
		double pixel[] = new double[ image.getImageType().getNumBands() ];
		Arrays.fill(pixel,10);
		for (int y = 0; y < image.height; y++) {
			for (int x = 0; x < image.width; x++) {
				pixel[band] = mean + rand.nextDouble()*2*range-range;
				GeneralizedImageOps.setM(image,x,y,pixel);
			}
		}
	}

	protected void noise( double mean , double range , ImageBase image ) {
		GImageMiscOps.fill(image, mean);
		GImageMiscOps.addUniform(image, rand, -range, range);
	}
}

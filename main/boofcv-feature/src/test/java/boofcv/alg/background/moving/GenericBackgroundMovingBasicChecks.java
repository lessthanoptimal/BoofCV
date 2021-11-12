/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.background.BackgroundModelStationary;
import boofcv.alg.background.stationary.BackgroundStationaryBasic;
import boofcv.alg.background.stationary.GenericBackgroundStationaryBasicChecks;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.InvertibleTransform;
import georegression.struct.homography.Homography2D_F32;
import org.junit.jupiter.api.Test;

/**
 * @author Peter Abeles
 */
public abstract class GenericBackgroundMovingBasicChecks extends GenericBackgroundModelMovingChecks {

	@Test void performStationaryTests() {
		GenericBackgroundStationaryBasicChecks stationary = new GenericBackgroundStationaryBasicChecks() {
			@Override
			public BackgroundModelStationary create( ImageType imageType ) {
				BackgroundModelMoving moving = GenericBackgroundMovingBasicChecks.this.create(imageType);
				return new MovingToStationary((BackgroundMovingBasic)moving, new Homography2D_F32());
			}
		};

		stationary.checkLearnRate();
		stationary.checkThreshold();
		stationary.checkBandsUsed();
	}

	private class MovingToStationary extends BackgroundStationaryBasic {

		BackgroundMovingBasic moving;
		InvertibleTransform stationary;

		public MovingToStationary( BackgroundMovingBasic moving, InvertibleTransform stationary ) {
			super(0, 0, moving.getImageType());
			this.stationary = stationary;
		}

		@Override
		public void updateBackground( ImageBase frame ) {
			moving.updateBackground(stationary, frame);
		}

		@Override
		public void segment( ImageBase frame, GrayU8 segmented ) {
			moving.segment(stationary, frame, segmented);
		}

		@Override
		public void reset() {
			moving.reset();
		}

		@Override
		public float getLearnRate() {
			return moving.getLearnRate();
		}

		@Override
		public void setLearnRate( float learnRate ) {
			moving.setLearnRate(learnRate);
		}

		@Override
		public float getThreshold() {
			return moving.getThreshold();
		}

		@Override
		public void setThreshold( float threshold ) {
			moving.setThreshold(threshold);
		}

		@Override
		public int getUnknownValue() {
			return moving.getUnknownValue();
		}

		@Override
		public void setUnknownValue( int unknownValue ) {
			moving.setUnknownValue(unknownValue);
		}
	}
}

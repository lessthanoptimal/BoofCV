/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.background.stationary.BackgroundStationaryGmm;
import boofcv.alg.background.stationary.GenericBackgroundStationaryGmmChecks;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.InvertibleTransform;
import georegression.struct.homography.Homography2D_F32;
import org.junit.Test;

/**
 * @author Peter Abeles
 */
public abstract class GenericBackgroundMovingGmmChecks extends GenericBackgroundModelMovingChecks {

	@Test
	public void performStationaryTests() {
		GenericBackgroundStationaryGmmChecks stationary = new GenericBackgroundStationaryGmmChecks() {
			@Override
			public BackgroundModelStationary create(ImageType imageType) {
				BackgroundModelMoving moving = GenericBackgroundMovingGmmChecks.this.create(imageType);
				return new MovingToStationary((BackgroundMovingGmm)moving,new Homography2D_F32());
			}
		};

		stationary.initialVariance();
		stationary.learnRate();
		stationary.checkBandsUsed();
	}

	private class MovingToStationary extends BackgroundStationaryGmm {

		BackgroundMovingGmm moving;
		InvertibleTransform stationary;

		public MovingToStationary( BackgroundMovingGmm moving , InvertibleTransform stationary ) {
			super(moving.getLearningPeriod(),moving.common.decay,moving.common.maxGaussians,moving.getImageType());
			this.stationary = stationary;
		}

		@Override
		public void updateBackground(ImageBase frame) {
			moving.updateBackground(stationary,frame);
		}

		@Override
		public void segment(ImageBase frame, GrayU8 segmented) {
			moving.segment(stationary,frame,segmented);
		}

		@Override
		public void reset() {
			moving.reset();
		}

		@Override
		public float getInitialVariance() {
			return moving.getInitialVariance();
		}

		@Override
		public void setInitialVariance(float initialVariance) {
			moving.setInitialVariance(initialVariance);
		}

		@Override
		public int getUnknownValue() {
			return moving.getUnknownValue();
		}

		@Override
		public void setUnknownValue(int unknownValue) {
			moving.setUnknownValue(unknownValue);
		}
	}
}

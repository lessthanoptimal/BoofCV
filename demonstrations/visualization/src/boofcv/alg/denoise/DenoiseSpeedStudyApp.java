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

package boofcv.alg.denoise;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.misc.Performer;
import boofcv.misc.ProfileOperation;
import boofcv.struct.image.ImageFloat32;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static boofcv.alg.denoise.DenoiseAccuracyStudyApp.TestItem;


/**
 * Benchmarks how fast each denoising technique is
 *
 * @author Peter Abeles
 */
public class DenoiseSpeedStudyApp {

	long TEST_TIME = 1000;
	int width = 640;
	int height = 480;

	Random rand = new Random(2234);
	ImageFloat32 image = new ImageFloat32(width,height);
	ImageFloat32 imageDenoised = new ImageFloat32(width,height);

	public class RunFilter implements Performer
	{
		public TestItem item;

		public RunFilter(TestItem item ) {
			this.item = item;
		}

		@Override
		public void process() {
			item.filter.process(image,imageDenoised);
		}

		@Override
		public String getName() {
			return item.name;
		}
	}

	public void process( List<TestItem> filters ) {

		ImageMiscOps.fillUniform(image,rand,0,20);

		for( TestItem i : filters ) {
			System.out.print("-");
		}
		System.out.println();

		for( TestItem i : filters ) {
			System.out.print("*");
			i.opsPerSecond = ProfileOperation.profileOpsPerSec(new RunFilter(i),TEST_TIME, false);
		}
		System.out.println();

		Collections.sort(filters,new Comparator<TestItem>(){
			@Override
			public int compare(TestItem o1, TestItem o2) {
				if( o1.opsPerSecond < o2.opsPerSecond )
					return -1;
				else if( o1.opsPerSecond > o2.opsPerSecond )
					return 1;
				else
					return 0;
			}
		});

		for( TestItem i : filters ) {
			System.out.printf("%30s  ops/sec = %6.2f\n",i.name,i.opsPerSecond);
		}
	}

	public static void main( String args[] ) {
		DenoiseSpeedStudyApp app = new DenoiseSpeedStudyApp();

		app.process(DenoiseAccuracyStudyApp.createStandard(2,4));
	}

}

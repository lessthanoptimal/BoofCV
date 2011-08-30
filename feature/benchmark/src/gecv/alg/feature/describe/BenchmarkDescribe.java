/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.feature.describe;

import gecv.Performer;
import gecv.ProfileOperation;
import gecv.abst.feature.describe.ExtractFeatureDescription;
import gecv.core.image.GeneralizedImageOps;
import gecv.factory.feature.describe.FactoryExtractFeatureDescription;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt32;
import jgrl.struct.point.Point2D_I32;

import java.util.Random;


/**
 * @author Peter Abeles
 */
public class BenchmarkDescribe<I extends ImageBase, D extends ImageBase> {

	static final long TEST_TIME = 1000;
	static Random rand = new Random(234234);
	static int NUM_POINTS = 1000;
	static int RADIUS = 6;

	final static int width = 640;
	final static int height = 480;

	I image;

	Point2D_I32 pts[];
	double scales[];

	Class<I> imageType;
	Class<D> derivType;

	public BenchmarkDescribe( Class<I> imageType , Class<D> derivType ) {

		this.imageType = imageType;
		this.derivType = derivType;

		Class integralType = ImageFloat32.class == imageType ? ImageFloat32.class : ImageSInt32.class;

		image = GeneralizedImageOps.createImage(imageType,width,height);

		GeneralizedImageOps.randomize(image,rand,0,100);

		pts = new Point2D_I32[ NUM_POINTS ];
		scales = new double[ NUM_POINTS ];
		int border = 20;
		for( int i = 0; i < NUM_POINTS; i++ ) {
			int x = rand.nextInt(width-border*2)+border;
			int y = rand.nextInt(height-border*2)+border;
			pts[i] = new Point2D_I32(x,y);
			scales[i] = rand.nextDouble()*3+1;
		}

	}

	public class Describe implements Performer {

		ExtractFeatureDescription<I> alg;
		String name;

		public Describe(String name, ExtractFeatureDescription<I> alg) {
			this.alg = alg;
			this.name = name;
		}

		@Override
		public void process() {
			alg.setImage(image);
			for( int i = 0; i < pts.length; i++ ) {
				Point2D_I32 p = pts[i];
				alg.process(p.x,p.y,scales[i]);
			}
		}

		@Override
		public String getName() {
			return name;
		}
	}

	public void perform() {
		System.out.println("=========  Profile Image Size " + width + " x " + height + " ========== "+imageType.getSimpleName());
		System.out.println();

		ProfileOperation.printOpsPerSec(new Describe("SURF", FactoryExtractFeatureDescription.<I>surf(true,imageType)),TEST_TIME);
		ProfileOperation.printOpsPerSec(new Describe("Steer", FactoryExtractFeatureDescription.steerableGaussian(12,false,imageType,derivType)),TEST_TIME);
		ProfileOperation.printOpsPerSec(new Describe("Steer Norm", FactoryExtractFeatureDescription.steerableGaussian(12,true,imageType,derivType)),TEST_TIME);
	}

	public static void main( String argsp[ ] ) {
		BenchmarkDescribe<ImageFloat32,ImageFloat32> alg = new BenchmarkDescribe<ImageFloat32,ImageFloat32>(ImageFloat32.class,ImageFloat32.class);
//		BenchmarkOrientation<ImageUInt8,ImageSInt16> alg = new BenchmarkOrientation<ImageUInt8,ImageSInt16>(ImageUInt8.class, ImageSInt16.class);

		alg.perform();
	}
}

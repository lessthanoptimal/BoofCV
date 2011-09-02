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
import gecv.alg.filter.derivative.GImageDerivativeOps;
import gecv.alg.transform.ii.GIntegralImageOps;
import gecv.core.image.GeneralizedImageOps;
import gecv.factory.feature.describe.FactoryExtractFeatureDescription;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;
import jgrl.struct.point.Point2D_I32;

import java.util.Random;


/**
 * @author Peter Abeles
 */
public class BenchmarkDescribe<I extends ImageBase, D extends ImageBase, II extends ImageBase> {

	static final long TEST_TIME = 1000;
	static Random rand = new Random(234234);
	static int NUM_POINTS = 1000;

	final static int width = 640;
	final static int height = 480;

	I image;

	Point2D_I32 pts[];
	double scales[];

	Class<I> imageType;
	Class<D> derivType;
	Class<II> integralType;

	public BenchmarkDescribe( Class<I> imageType ) {

		this.imageType = imageType;

		derivType = GImageDerivativeOps.getDerivativeType(imageType);
		integralType = GIntegralImageOps.getIntegralType(imageType);

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
				alg.process(p.x,p.y,scales[i],null);
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

		ProfileOperation.printOpsPerSec(new Describe("SURF", FactoryExtractFeatureDescription.<I,II>surf(true,imageType)),TEST_TIME);
		ProfileOperation.printOpsPerSec(new Describe("Steer r=12", FactoryExtractFeatureDescription.steerableGaussian(12,false,imageType,derivType)),TEST_TIME);
		ProfileOperation.printOpsPerSec(new Describe("Steer Norm r=12", FactoryExtractFeatureDescription.steerableGaussian(12,true,imageType,derivType)),TEST_TIME);
		ProfileOperation.printOpsPerSec(new Describe("Gaussian 12 r=12", FactoryExtractFeatureDescription.gaussian12(12,imageType,derivType)),TEST_TIME);
		ProfileOperation.printOpsPerSec(new Describe("Gaussian 12 r=20", FactoryExtractFeatureDescription.gaussian12(20,imageType,derivType)),TEST_TIME);

	}

	public static void main( String argsp[ ] ) {
		BenchmarkDescribe<ImageFloat32,?,?> alg = new BenchmarkDescribe(ImageFloat32.class);
//		BenchmarkDescribe<ImageUInt8,?,?> alg = new BenchmarkDescribe(ImageUInt8.class);

		alg.perform();
	}
}

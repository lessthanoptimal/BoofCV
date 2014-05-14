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

package boofcv.benchmark.feature.describe;

import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.describe.FactoryDescribeRegionPoint;
import boofcv.struct.feature.SurfFeature;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;

import java.util.Random;

/**
 * Prints out the description of a feature after the input image has been transformed.  Useful for debugging
 * a descriptor which should be invariant to a specific transform.
 *
 * @author Peter Abeles
 */
public class DebugDescribeTransform {

	public static <T extends ImageSingleBand> void doStuff( Class<T> imageType ) {
		DescribeRegionPoint<T,SurfFeature> alg =  FactoryDescribeRegionPoint.surfFast(null,imageType);

		int r = 8;
		int w = r*2+1;

		T orig = GeneralizedImageOps.createSingleBand(imageType, w, w);

		GImageMiscOps.fillUniform(orig, new Random(123), 0, 100);

		double scale = 1;
		double theta = Math.PI+0.4;

		T distorted = (T)orig._createNew((int)(w*scale) , (int)(w*scale) );

		DistortImageOps.rotate(orig,distorted, TypeInterpolate.BILINEAR,(float)theta);
//		DistortImageOps.scale(orig,distorted, TypeInterpolate.BILINEAR);

		alg.setImage(orig);
		SurfFeature desc1 = alg.createDescription();
		alg.process(r,r,0,1,desc1);

		alg.setImage(distorted);
		SurfFeature desc2 = alg.createDescription();
		alg.process(distorted.width/2,distorted.height/2,theta,scale,desc2);

		printDesc(desc1);
		printDesc(desc2);

		double error = 0;
		double norm = 0;
		for( int i = 0; i < desc1.value.length; i++ ) {
			error += Math.abs(desc1.value[i]-desc2.value[i]);
			norm += desc1.value[i]*desc1.value[i];
		}
		error /= Math.sqrt(norm);
		System.out.println("error = "+error);

//		BasicImageIO.print(orig);
//		System.out.println("------------------");
//		BasicImageIO.print(distorted);

	}

	private static void printDesc(TupleDesc_F64 desc1) {
		for( int i = 0; i < desc1.value.length; i++ ) {
			System.out.printf("%5.2f ",desc1.value[i]);
		}
		System.out.println();
	}

	public static void main( String args[] ) {
		doStuff(ImageFloat32.class);

	}
}

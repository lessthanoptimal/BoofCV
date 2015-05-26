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

package boofcv.alg.feature.shapes;

import boofcv.abst.distort.FDistort;
import boofcv.alg.distort.impl.DistortSupport;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.shapes.corner.SubpixelSparseCornerFit;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.distort.PixelTransform_F32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Peter Abeles
 */
public class BenchmarkSubpixelSparseCorner<T extends ImageSingleBand> {
	Class<T> imageType;

	T orig,rotated;
	PixelTransform_F32 inputToOutput;

	List<Point2D_F32> corners[];

	public BenchmarkSubpixelSparseCorner( Class<T> imageType ) {
		this.imageType = imageType;
		orig = GeneralizedImageOps.createSingleBand(imageType,300,300);
		rotated = GeneralizedImageOps.createSingleBand(imageType,300,300);

		GImageMiscOps.fill(orig,200);
		GImageMiscOps.fillRectangle(orig,10,140,130,50,60);

		float angle = 25;

		new FDistort(orig, rotated).border(200).rotate(angle).apply();
		int halfW = orig.width / 2, halfH = orig.height / 2;
		inputToOutput = DistortSupport.transformRotate(
				halfW, halfH, halfW, halfH, -angle);

		corners = new List[2];

		List<Point2D_F32> a = new ArrayList<Point2D_F32>();
		a.add( new Point2D_F32(140,130));
		a.add( new Point2D_F32(140,189));
		a.add( new Point2D_F32(189,189));
		a.add( new Point2D_F32(189,130));

		List<Point2D_F32> b = new ArrayList<Point2D_F32>();
		for( Point2D_F32 p : a ) {
			inputToOutput.compute((int)p.x,(int)p.y);
			b.add( new Point2D_F32(inputToOutput.distX,inputToOutput.distY));
		}
		addNoisyStarts(a);
		addNoisyStarts(b);

		corners[0] = a;
		corners[1] = b;
	}

	private void addNoisyStarts(List<Point2D_F32> list) {
		Random rand = new Random(234);
		for (int i = 0; i < 30; i++) {
			int n = rand.nextInt(4);
			Point2D_F32 p = list.get(n);
			Point2D_F32 o = new Point2D_F32();
			o.x = p.x + rand.nextFloat()*3-1.5f;
			o.y = p.y + rand.nextFloat()*3-1.5f;
			list.add(o);
		}
	}

	public void benchmark() {
		SubpixelSparseCornerFit<T> subGen = new SubpixelSparseCornerFit<T>(imageType);
//		SubpixelSparseCornerFit<T> subSpecific;

//		if( imageType == ImageUInt8.class ) {
//			subSpecific = (SubpixelSparseCornerFit)new SubpixelSparseCornerFit_U8();
//		} else {
//			subSpecific = null;//(SubpixelSparseCornerFit)new SubpixelSparseCornerFit_F32();
//		}

		System.out.println("orig G = "+benchmark(subGen,orig,corners[0]));
		System.out.println("rot  G = "+benchmark(subGen,rotated,corners[1]));

//		System.out.println("orig S = "+benchmark(subSpecific,orig,corners[0]));
//		System.out.println("rot  S = "+benchmark(subSpecific,rotated,corners[1]));
	}

	public long benchmark( SubpixelSparseCornerFit<T> alg , T image , List<Point2D_F32> corners ) {

		long before = System.currentTimeMillis();
		alg.setWeightToggle(-1);
		alg.setIgnoreRadius(1);
		alg.setLocalRadius(5);
		alg.setMaxOptimizeSteps(200);
		alg.initialize(image);

		Point2D_F64 refined = new Point2D_F64();
		for (int i = 0; i < 3000; i++) {
			for (int j = 0; j < corners.size(); j++) {
				Point2D_F32 c = corners.get(j);
				alg.refine(c.x,c.y,refined);
			}
		}
		long after = System.currentTimeMillis();
		return after-before;
	}

	public static void main(String[] args) {
		Class imageType = ImageUInt8.class;
		BenchmarkSubpixelSparseCorner benchmark = new BenchmarkSubpixelSparseCorner(imageType);

		benchmark.benchmark();
	}

}

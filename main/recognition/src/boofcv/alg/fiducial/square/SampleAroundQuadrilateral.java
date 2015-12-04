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

package boofcv.alg.fiducial.square;

import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.core.image.border.BorderType;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Quadrilateral_F64;

/**
 * @author Peter Abeles
 */
// TODO handle lens distortion
public class SampleAroundQuadrilateral<T extends ImageSingleBand>
{
	InterpolatePixelS<T> interpolation;

	float insideRadius;
	float outsideRadius;

	int samplesPerSides;

	float inside[];
	float outside[];

	float meanInside,meanOutside;

	public SampleAroundQuadrilateral( float insideRadius , float outsideRadius , int samplesPerSides, Class<T> imageType ) {
		this.insideRadius = insideRadius;
		this.outsideRadius = outsideRadius;
		interpolation = FactoryInterpolation.bilinearPixelS(imageType, BorderType.EXTENDED);
		this.samplesPerSides = samplesPerSides;
		inside = new float[samplesPerSides*4];
		outside = new float[samplesPerSides*4];
	}

	public void setImage( T image ) {
		interpolation.setImage(image);
	}

	public void process(Quadrilateral_F64 quadrilateral ) {
		sampleOutsideOfLine(quadrilateral.a,quadrilateral.b,0);
		sampleOutsideOfLine(quadrilateral.a,quadrilateral.b,samplesPerSides);
		sampleOutsideOfLine(quadrilateral.a,quadrilateral.b,samplesPerSides*2);
		sampleOutsideOfLine(quadrilateral.a,quadrilateral.b,samplesPerSides*3);

		meanInside = 0;
		meanOutside = 0;

		for (int i = 0; i < inside.length; i++) {
			meanInside += inside[i];
			meanOutside += outside[i];
		}

		meanInside /= inside.length;
		meanOutside /= inside.length;

		System.out.printf("values  %6.3f  %6.3f\n",meanInside,meanOutside);
	}

	protected void sampleOutsideOfLine(Point2D_F64 a , Point2D_F64 b , int startIndex ) {

		double dx = b.x-a.x;
		double dy = b.y-a.y;

		double r = Math.sqrt(dx*dx + dy*dy);

		dx /= r;
		dy /= r;

		for (int i = 0; i < samplesPerSides; i++) {
			double d = r*i/(samplesPerSides-1);

			double x = a.x + dx*d - dy*insideRadius;
			double y = a.y + dy*d + dx*insideRadius;

			float inside = interpolation.get((float)x,(float)y);

			x = a.x + dx*d + dy*outsideRadius;
			y = a.y + dy*d - dx*outsideRadius;
			float outside = interpolation.get((float)x,(float)y);

			this.inside[i+startIndex] = inside;
			this.outside[i+startIndex] = outside;
		}
	}

	public float getMeanInside() {
		return meanInside;
	}

	public float getMeanOutside() {
		return meanOutside;
	}
}

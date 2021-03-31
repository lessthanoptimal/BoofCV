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

package boofcv.abst.feature.detdesc;

import boofcv.abst.feature.convert.ConvertTupleDesc;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;

/**
 * Used to convert the TupleDesc type.
 *
 * @author Peter Abeles
 **/
public class DetectDescribeConvertTuple
		<Image extends ImageBase<Image>, In extends TupleDesc<In>, Out extends TupleDesc<Out>>
		implements DetectDescribePoint<Image, Out> {
	DetectDescribePoint<Image, In> detector;

	// Converts the tuple from one type into another
	ConvertTupleDesc<In, Out> converter;

	// Storage for the converted descriptor
	Out out;

	public DetectDescribeConvertTuple( DetectDescribePoint<Image, In> detector, ConvertTupleDesc<In, Out> converter ) {
		this.detector = detector;
		this.converter = converter;
		this.out = converter.createOutput();
	}

	@Override public Out createDescription() {
		return converter.createOutput();
	}

	@Override public Class<Out> getDescriptionType() {
		return converter.getOutputType();
	}

	@Override public Out getDescription( int index ) {
		converter.convert(detector.getDescription(index), out);
		return out;
	}

	// @formatter:off
	@Override public int getNumberOfSets() {return detector.getNumberOfSets();}
	@Override public int getSet( int index ) {return detector.getSet(index);}
	@Override public int getNumberOfFeatures() {return detector.getNumberOfFeatures();}
	@Override public Point2D_F64 getLocation( int featureIndex ) {return detector.getLocation(featureIndex);}
	@Override public double getRadius( int featureIndex ) {return detector.getRadius(featureIndex);}
	@Override public double getOrientation( int featureIndex ) {return detector.getOrientation(featureIndex);}
	@Override public void detect( Image input ) {detector.detect(input);}
	@Override public boolean hasScale() {return detector.hasScale();}
	@Override public boolean hasOrientation() {return detector.hasOrientation();}
	@Override public ImageType<Image> getInputType() {return detector.getInputType();}
	// @formatter:on
}

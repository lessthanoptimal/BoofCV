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

package boofcv.abst.feature.describe;

import boofcv.misc.BoofLambdas;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

/**
 * Default implementations for all functions in {@link DescribePoint}. Primary for testing purposes.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway"})
public abstract class DescribePointAbstract<T extends ImageBase<T>, TD extends TupleDesc<TD>>
		implements DescribePoint<T, TD> {

	protected Class<TD> type;
	protected BoofLambdas.Factory<TD> factory;

	protected DescribePointAbstract() {}

	protected DescribePointAbstract( BoofLambdas.Factory<TD> factory ) {
		this.factory = factory;
		type = (Class)factory.newInstance().getClass();
	}

	// @formatter:off
	@Override public void setImage( T image ) {}
	@Override public boolean process( double x, double y, TD description ) {return false;}
	@Override public ImageType<T> getImageType() {return null;}
	@Override public TD createDescription() {return factory==null?null:factory.newInstance();}
	@Override public Class<TD> getDescriptionType() {return type;}
	// @formatter:on
}

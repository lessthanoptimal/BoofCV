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

package boofcv.alg.feature;

import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.abst.feature.detdesc.DetectDescribeMulti;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.feature.TupleDesc_F64;
import org.ddogleg.struct.FastQueue;

/**
 * Various utilities related to image features
 *
 * @author Peter Abeles
 */
public class UtilFeature {

	/**
	 * Creates a FastQueue and declares new instances of the descriptor using the provided
	 * {@link DetectDescribePoint}.  The queue will have declareInstance set to true, otherwise
	 * why would you be using this function?
	 */
	public static <TD extends TupleDesc>
	FastQueue<TD> createQueue( final DescribeRegionPoint<?, TD> detDesc , int initialMax ) {
		return new FastQueue<TD>(initialMax,detDesc.getDescriptionType(),true) {
			@Override
			protected TD createInstance() {
				return detDesc.createDescription();
			}
		};
	}

	/**
	 * Creates a FastQueue and declares new instances of the descriptor using the provided
	 * {@link DetectDescribePoint}.  The queue will have declareInstance set to true, otherwise
	 * why would you be using this function?
	 */
	public static <TD extends TupleDesc>
	FastQueue<TD> createQueue( final DetectDescribePoint<?, TD> detDesc , int initialMax ) {
		return new FastQueue<TD>(initialMax,detDesc.getDescriptionType(),true) {
			@Override
			protected TD createInstance() {
				return detDesc.createDescription();
			}
		};
	}

	/**
	 * Creates a FastQueue and declares new instances of the descriptor using the provided
	 * {@link DetectDescribePoint}.  The queue will have declareInstance set to true, otherwise
	 * why would you be using this function?
	 */
	public static <TD extends TupleDesc>
	FastQueue<TD> createQueue( final DetectDescribeMulti<?, TD> detDesc , int initialMax ) {
		return new FastQueue<TD>(initialMax,detDesc.getDescriptionType(),true) {
			@Override
			protected TD createInstance() {
				return detDesc.createDescription();
			}
		};
	}

	/**
	 * <p>
	 * Normalized the tuple such that the L2-norm is equal to 1.  This is also often referred to as
	 * the Euclidean or frobenius (all though that's a matrix norm).
	 * </p>
	 *
	 * <p>
	 * value[i] = value[i]/sqrt(sum(value[j]*value[j], for all j))
	 * </p>
	 *
	 * @param desc tuple
	 */
	public static void normalizeL2( TupleDesc_F64 desc ) {
		double norm = 0;
		for (int i = 0; i < desc.size(); i++) {
			double v = desc.value[i];
			norm += v*v;
		}
		if( norm == 0 )
			return;

		norm = Math.sqrt(norm);
		for (int i = 0; i < desc.size(); i++) {
			desc.value[i] /= norm;
		}
	}
}

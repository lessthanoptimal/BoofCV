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

package boofcv.struct.feature;


/**
 * Indexes of three associated features and the fit score.
 *
 * @author Peter Abeles
 */
public class AssociatedTripleIndex {

	/**
	 * Index of feature in each view
	 */
	public int a,b,c;

	public AssociatedTripleIndex(AssociatedTripleIndex original ) {
		set(original);
	}

	public AssociatedTripleIndex(int a , int b , int c ) {
		this.a = a;
		this.b = b;
		this.c = c;
	}

	public AssociatedTripleIndex() {
	}

	public void set(int a , int b , int c ) {
		this.a = a;
		this.b = b;
		this.c = c;
	}

	public void set( AssociatedTripleIndex original ) {
		this.a = original.a;
		this.b = original.b;
		this.c = original.c;
	}

	public AssociatedTripleIndex copy() {
		return new AssociatedTripleIndex(this);
	}

	public int getA() {
		return a;
	}

	public void setA(int a) {
		this.a = a;
	}

	public int getB() {
		return b;
	}

	public void setB(int b) {
		this.b = b;
	}

	public int getC() {
		return c;
	}

	public void setC(int c) {
		this.c = c;
	}
}

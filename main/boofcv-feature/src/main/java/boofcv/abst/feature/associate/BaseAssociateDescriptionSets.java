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

package boofcv.abst.feature.associate;

/**
 * Base class for set aware feature association
 *
 * @author Peter Abeles
 */
public abstract class BaseAssociateDescriptionSets<Desc> extends BaseAssociateSets<Desc> {

	/**
	 * Specifies the type of descriptor
	 */
	protected BaseAssociateDescriptionSets( Associate<Desc> associator ) {
		super(associator);
	}

	/**
	 * Adds a new descriptor and its set to the list. The order that descriptors are added is important and saved.
	 */
	public abstract void addSource( Desc description, int set );

	/**
	 * Adds a new descriptor and its set to the list. The order that descriptors are added is important and saved.
	 */
	public abstract void addDestination( Desc description, int set );

	/**
	 * Specifies the number of sets and resets all internal data structures. This must be called before any other
	 * function.
	 */
	public abstract void initialize( int numberOfSets );
}

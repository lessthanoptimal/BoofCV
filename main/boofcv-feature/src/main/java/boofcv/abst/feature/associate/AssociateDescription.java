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

import org.ddogleg.struct.FastAccess;

/**
 * <p>
 * Generalized interface for associating features. Finds matches for each feature in the source
 * list to one in the destination list. There is only one match found for each member of source, but multiple
 * matches can be found for destination. If the best match has an error which is too high then a member of
 * source might not be matched.
 * </p>
 *
 * <p>
 * DESIGN NOTE: {@link FastAccess} is used instead of {@link java.util.List} because in the association
 * micro benchmark it produced results that were about 20% faster consistently. Which is surprising since
 * one would think descriptor comparisons would dominate.
 * </p>
 *
 * @param <Desc> Feature description type.
 * @author Peter Abeles
 */
public interface AssociateDescription<Desc> extends Associate<Desc> {

	/**
	 * Sets the list of source features.
	 *
	 * NOTE: A reference to the input list might be saved internally until the next call to this function.
	 *
	 * @param listSrc List of features
	 */
	void setSource( FastAccess<Desc> listSrc );

	/**
	 * Sets the list of destination features
	 *
	 * NOTE: A reference to the input list might be saved internally until the next call to this function.
	 *
	 * @param listDst List of features
	 */
	void setDestination( FastAccess<Desc> listDst );
}

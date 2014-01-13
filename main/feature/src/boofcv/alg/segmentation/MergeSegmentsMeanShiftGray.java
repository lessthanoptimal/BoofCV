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

package boofcv.alg.segmentation;

/**
 * In uniform regions mean-shift segmentation will produce lots of segments with identical colors since they
 * are all local maximums.  This will find all such neighbors and merge them into one group.  For each
 * pixel it checks its 4-connect neighbors to see if they are in the same segment or not.  If not in the same
 * segment it checks to see if their peaks have the same color to within tolerance.  If so a mark will be
 * made in an integer list of segments that one should be merged into another.  A check is made to see
 * if the segment it is merged into doesn't merge into another one.  If it does a link will be made directly to
 * the last one it gets merged into.
 *
 * @author Peter Abeles
 */
public class MergeSegmentsMeanShiftGray {
}

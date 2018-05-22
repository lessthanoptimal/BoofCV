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

package boofcv.abst.filter.binary;

import boofcv.alg.filter.binary.ContourPacked;
import boofcv.alg.filter.binary.LinearExternalContours;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.ConnectRule;
import boofcv.struct.PackedSetsPoint2D_I32;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;

import java.util.List;

/**
 * Wrapper around {@link boofcv.alg.filter.binary.LinearExternalContours}
 *
 * @author Peter Abeles
 */
public class BinaryContourFinderLinearExternal implements BinaryContourFinder, BinaryContourInterface.Padded
{
	LinearExternalContours alg;

	boolean copyForPadding = true;
	int adjustX,adjustY;

	GrayU8 work = new GrayU8(1,1);

	FastQueue<ContourPacked> contours = new FastQueue<>(ContourPacked.class,true);

	public BinaryContourFinderLinearExternal() {
		this.alg = new LinearExternalContours(ConnectRule.FOUR);
	}

	@Override
	public void process(GrayU8 binary) {

		if(copyForPadding) {
			work.reshape(binary.width + 2, binary.height + 2);
			ImageMiscOps.copy(0, 0, 1, 1, binary.width, binary.height, binary, work);
			alg.process(work,1,1);
		} else {
			alg.process(binary,adjustX,adjustY);
		}

		// create the contours list
		contours.reset();
		PackedSetsPoint2D_I32 points = alg.getExternalContours();

		for( int i = 0; i < points.size(); i++ ) {
			ContourPacked p = contours.grow();
			p.externalIndex = i;
			p.id = i;
		}
	}

	@Override
	public List<ContourPacked> getContours() {
		return contours.toList();
	}

	@Override
	public void loadContour(int contourID, FastQueue<Point2D_I32> storage) {
		alg.getExternalContours().getSet(contourID,storage);
	}

	@Override
	public void writeContour(int contourID, List<Point2D_I32> storage) {
		alg.getExternalContours().writeOverSet(contourID,storage);
	}

	@Override
	public void setSaveInnerContour(boolean enabled) {
	}

	@Override
	public boolean isSaveInternalContours() {
		return false;
	}

	@Override
	public void setMinContour(int length) {
		alg.setMinContourLength(length);
	}

	@Override
	public int getMinContour() {
		return alg.getMinContourLength();
	}

	@Override
	public void setMaxContour(int length) {
		alg.setMaxContourLength(length);
	}

	@Override
	public int getMaxContour() {
		return alg.getMaxContourLength();
	}

	@Override
	public void setConnectRule(ConnectRule rule) {
		alg.setConnectRule(rule);
	}

	@Override
	public ConnectRule getConnectRule() {
		return alg.getConnectRule();
	}

	@Override
	public void setCreatePaddedCopy(boolean padded) {
		this.copyForPadding = padded;
	}

	@Override
	public boolean isCreatePaddedCopy() {
		return copyForPadding;
	}

	@Override
	public void setCoordinateAdjustment(int x, int y) {
		this.adjustX = x;
		this.adjustY = y;
	}
}

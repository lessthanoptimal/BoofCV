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
import boofcv.alg.filter.binary.LinearContourLabelChang2004;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;

import java.util.List;

/**
 * Wrapper around {@link boofcv.alg.filter.binary.LinearContourLabelChang2004} for
 * {@link BinaryLabelContourFinder}
 *
 * @author Peter Abeles
 */
public class BinaryLabelContourFinderChang2004 implements BinaryLabelContourFinder {

	LinearContourLabelChang2004 finder = new LinearContourLabelChang2004(ConnectRule.FOUR);

	@Override
	public void process(GrayU8 binary, GrayS32 labeled) {
		finder.process(binary,labeled);
	}

	@Override
	public List<ContourPacked> getContours() {
		return finder.getContours().toList();
	}

	@Override
	public void loadContour(int contourID, FastQueue<Point2D_I32> storage) {
		finder.getPackedPoints().getSet(contourID,storage);
	}

	@Override
	public void writeContour(int contourID, List<Point2D_I32> list) {
		finder.getPackedPoints().writeOverSet(contourID,list);
	}

	@Override
	public void setSaveInnerContour(boolean enabled) {
		finder.setSaveInternalContours(enabled);
	}

	@Override
	public boolean isSaveInternalContours() {
		return finder.isSaveInternalContours();
	}

	@Override
	public void setMinContour(int length) {
		finder.setMinContourSize(length);
	}

	@Override
	public int getMinContour() {
		return finder.getMinContourSize();
	}

	@Override
	public void setMaxContour(int length) {
		finder.setMaxContourSize(length);
	}

	@Override
	public int getMaxContour() {
		return finder.getMaxContourSize();
	}

	@Override
	public void setConnectRule(ConnectRule rule) {
		finder.setConnectRule(rule);
	}

	@Override
	public ConnectRule getConnectRule() {
		return finder.getConnectRule();
	}
}

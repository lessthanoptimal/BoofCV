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

package boofcv.abst.disparity;

import boofcv.alg.segmentation.cc.ConnectedSpeckleFiller;
import boofcv.alg.segmentation.cc.ConnectedTwoRowSpeckleFiller_F32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Set;

/**
 * Wrapper around {@link ConnectedTwoRowSpeckleFiller_F32} for {@link DisparitySmoother}
 *
 * @author Peter Abeles
 */
public class DisparitySmootherSpeckleFilter<Image extends ImageBase<Image>, Disp extends ImageGray<Disp>>
		implements DisparitySmoother<Image,Disp> {

	@Getter ConnectedSpeckleFiller<Disp> filler;

	@Getter ConfigSpeckleFilter config;

	@Nullable PrintStream out;

	@SuppressWarnings("unchecked")
	public DisparitySmootherSpeckleFilter( ConnectedSpeckleFiller<Disp> filler, ConfigSpeckleFilter config ) {
		this.config = config;
		this.filler = filler;
	}

	@Override public void process( Image image, Disp disp, int disparityRange ) {
		int size = config.maximumArea.computeI(disp.totalPixels());

		// Do nothing if it can't filer anything since the maximum size is zero
		if (size==0)
			return;
		filler.process(disp,size,config.similarTol,disparityRange);

		if (out!=null)
			out.println("Speckle maxArea="+size+" filled="+filler.getTotalFilled());
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.out = out;
	}
}

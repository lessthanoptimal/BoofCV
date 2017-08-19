/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.blur;

import boofcv.override.BOverrideClass;
import boofcv.override.BOverrideManager;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.convolve.Kernel1D_F64;
import boofcv.struct.convolve.Kernel1D_S32;
import boofcv.struct.image.*;

/**
 * Override for blur image ops functions
 * 
 * @author Peter Abeles
 */
public class BOverrideBlurImageOps extends BOverrideClass {

	static {
		BOverrideManager.register(BOverrideBlurImageOps.class);
	}

	static MeanU8 mean_U8;
	static MedianU8 median_U8;
	static GuasianU8 gaussian_U8;

	static MeanF32 mean_F32;
	static MedianF32 median_F32;
	static GuasianF32 gaussian_F32;

	static MeanF64 mean_F64;
//	static MedianF64 median_F64;
	static GuasianF64 gaussian_F64;

//	static MeanIU8 mean_IU8;
//	static MedianIU8 median_IU8;
	static GuasianIU8 gaussian_IU8;

//	static MeanIF32 mean_IF32;
//	static MedianIF32 median_IF32;
	static GuasianIF32 gaussian_IF32;

//	static MeanIF64 mean_IF64;
//	static MedianIF64 median_IF64;
	static GuasianIF64 gaussian_IF64;
	
	public interface MeanU8 {
		void process(GrayU8 input, GrayU8 output, int radius, GrayU8 storage);
	}

	public interface MedianU8 {
		void process(GrayU8 input, GrayU8 output, int radius);
	}
	
	public interface GuasianU8 {
		void process(GrayU8 input, GrayU8 output, Kernel1D_S32 kernel, GrayU8 storage );
	}

	public interface MeanF32 {
		void process(GrayF32 input, GrayF32 output, int radius, GrayF32 storage);
	}

	public interface MedianF32 {
		void process(GrayF32 input, GrayF32 output, int radius);
	}

	public interface GuasianF32 {
		void process(GrayF32 input, GrayF32 output, Kernel1D_F32 kernel, GrayF32 storage );
	}

	public interface MeanF64 {
		void process(GrayF64 input, GrayF64 output, int radius, GrayF64 storage);
	}

	public interface MedianF64 {
		void process(GrayF64 input, GrayF64 output, int radius);
	}

	public interface GuasianF64 {
		void process(GrayF64 input, GrayF64 output, Kernel1D_F64 kernel, GrayF64 storage );
	}
	
	public interface MeanIU8 {
		void process(InterleavedU8 input, InterleavedU8 output, int radius, InterleavedU8 storage);
	}

	public interface MedianIU8 {
		void process(InterleavedU8 input, InterleavedU8 output, int radius);
	}

	public interface GuasianIU8 {
		void process(InterleavedU8 input, InterleavedU8 output, Kernel1D_S32 kernel, InterleavedU8 storage );
	}

	public interface MeanIF32 {
		void process(InterleavedF32 input, InterleavedF32 output, int radius, InterleavedF32 storage);
	}

	public interface MedianIF32 {
		void process(InterleavedF32 input, InterleavedF32 output, int radius);
	}

	public interface GuasianIF32 {
		void process(InterleavedF32 input, InterleavedF32 output, Kernel1D_F32 kernel, InterleavedF32 storage );
	}

	public interface MeanIF64 {
		void process(InterleavedF64 input, InterleavedF64 output, int radius, InterleavedF64 storage);
	}

	public interface MedianIF64 {
		void process(InterleavedF64 input, InterleavedF64 output, int radius);
	}

	public interface GuasianIF64 {
		void process(InterleavedF64 input, InterleavedF64 output, Kernel1D_F64 kernel, InterleavedF64 storage );
	}
}

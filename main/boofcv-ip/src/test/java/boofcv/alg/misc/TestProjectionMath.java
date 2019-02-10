 /*
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

package boofcv.alg.misc;

import boofcv.core.image.FactoryGImageGray;
import boofcv.core.image.GImageGray;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.Planar;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Nico
 */
public class TestProjectionMath {
   int width = 10;
	int height = 15;
	int numBands = 11;
   int firstBand = 0;
   int lastBand = numBands -1;

	Random rand = new Random(234);

	float pixelA[] = new float[numBands];
	float pixelB[] = new float[numBands];
   
   
   @Test
	public void checkAll() {
		int numExpected = 234;
		Method methods[] = ProjectionMath.class.getMethods();

		// sanity check to make sure the functions are being found
		int numFound = 0;
		for (Method m : methods) {
			if( !isTestMethod(m)) {
				continue;
         }
		   try {
            if( m.getName().compareTo("averageBand") == 0 ) {
					TestAverageBand(m);
            } else if ( m.getName().compareTo("minimumBand") == 0 ) {
               TestMinimumBand(m);
				} else if ( m.getName().compareTo("maximumBand") == 0 ) {
               TestMaximumBand(m);
				} else if ( m.getName().compareTo("medianBand") == 0 ) {
               TestMedianBand(m);
				} else if ( m.getName().compareTo("stdDevBand") == 0 ) {
               TestStdDevBand(m);
				} else {
					// throw new RuntimeException("Unknown function: "+m.getName());
				}
            numFound++;
			} catch (InvocationTargetException | IllegalAccessException e) {
				throw new RuntimeException(e);
         }	
      }
      System.out.println("NumFound: " + numFound);
   }
   
            
   private boolean isTestMethod(Method m ) {
		Class param[] = m.getParameterTypes();

		if( param.length < 1 ) {
			return false;
      }

		for (int i = 0; i < param.length; i++) {
			if( ImageBase.class.isAssignableFrom(param[i]) ) {
				return true;
         }
		}
		return false;
	}
   
   private void TestAverageBand(Method m) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		Planar input = new Planar(paramTypes[1], width, height,numBands);
		ImageGray output = GeneralizedImageOps.createSingleBand(paramTypes[1], width, height);

		if( output.getDataType().isSigned() ) {
			GImageMiscOps.fillUniform(input, rand, -20,20);
		} else {
			GImageMiscOps.fillUniform(input, rand, 0,20);
		}
      GImageGray[] testImages = new GImageGray[lastBand - firstBand + 1];
      for (int i = firstBand; i <= lastBand; i++) {
         testImages[i] = FactoryGImageGray.wrap(input.getBand(i));
      }

      // check that a single band gives the expected output
		m.invoke(null,input,output, firstBand, firstBand);
      GImageGray r = FactoryGImageGray.wrap(output);
		boolean isInteger = output.getDataType().isInteger();
      
      for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
            double expected = testImages[firstBand].get(j,i).doubleValue();
            double found = r.get(j,i).doubleValue();

				if( isInteger ) {
					expected = (int)expected;
            }
				assertEquals(expected,found,1e-4);
         }
      }
      
      // now check all bands
      m.invoke(null, input, output, firstBand, lastBand);
      r = FactoryGImageGray.wrap(output);

		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				double expected = 0;
            for (int b = firstBand; b <= lastBand; b++) {
               expected += testImages[b].get(j,i).doubleValue();
            }
				expected /= (lastBand - firstBand + 1);

				double found = r.get(j,i).doubleValue();

				if( isInteger ) {
					expected = (int)expected;
            }
				assertEquals(expected,found,1e-4);
			}
		}
	}
   
   private void TestMinimumBand(Method m) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		Planar input = new Planar(paramTypes[1], width, height,numBands);
		ImageGray output = GeneralizedImageOps.createSingleBand(paramTypes[1], width, height);

		if( output.getDataType().isSigned() ) {
			GImageMiscOps.fillUniform(input, rand, -20,20);
		} else {
			GImageMiscOps.fillUniform(input, rand, 0,20);
		}
      GImageGray[] testImages = new GImageGray[lastBand - firstBand + 1];
      for (int i = firstBand; i <= lastBand; i++) {
         testImages[i] = FactoryGImageGray.wrap(input.getBand(i));
      }

      // check that a single band gives the expected output
		m.invoke(null,input,output, firstBand, firstBand);
      GImageGray r = FactoryGImageGray.wrap(output);
		boolean isInteger = output.getDataType().isInteger();
      
      for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
            double expected = testImages[firstBand].get(j,i).doubleValue();
            double found = r.get(j,i).doubleValue();

				if( isInteger ) {
					expected = (int)expected;
            }
				assertEquals(expected,found,1e-4);
         }
      }
      
      // now check all bands
      m.invoke(null, input, output, firstBand, lastBand);
      r = FactoryGImageGray.wrap(output);

		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				double expected = Double.MAX_VALUE;
            for (int b = firstBand; b <= lastBand; b++) {
               if (testImages[b].get(j,i).doubleValue() < expected) {
                  expected = testImages[b].get(j,i).doubleValue();
               }
            }

				double found = r.get(j,i).doubleValue();

				if( isInteger ) {
					expected = (int)expected;
            }
				assertEquals(expected,found,1e-4);
			}
		}
	}
   
   private void TestMaximumBand(Method m) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		Planar input = new Planar(paramTypes[1], width, height,numBands);
		ImageGray output = GeneralizedImageOps.createSingleBand(paramTypes[1], width, height);

		if( output.getDataType().isSigned() ) {
			GImageMiscOps.fillUniform(input, rand, -20,20);
		} else {
			GImageMiscOps.fillUniform(input, rand, 0,20);
		}
      GImageGray[] testImages = new GImageGray[lastBand - firstBand + 1];
      for (int i = firstBand; i <= lastBand; i++) {
         testImages[i] = FactoryGImageGray.wrap(input.getBand(i));
      }

      // check that a single band gives the expected output
		m.invoke(null, input, output, firstBand, firstBand);
      GImageGray r = FactoryGImageGray.wrap(output);
		boolean isInteger = output.getDataType().isInteger();
      
      for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
            double expected = testImages[firstBand].get(j,i).doubleValue();
            double found = r.get(j,i).doubleValue();

				if( isInteger ) {
					expected = (int)expected;
            }
				assertEquals(expected,found,1e-4);
         }
      }
      
      // now check all bands
      m.invoke(null, input, output, firstBand, lastBand);
      r = FactoryGImageGray.wrap(output);

		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				double expected = -Double.MAX_VALUE;
            for (int b = firstBand; b <= lastBand; b++) {
               if (testImages[b].get(j,i).doubleValue() > expected) {
                  expected = testImages[b].get(j,i).doubleValue();
               }
            }

				double found = r.get(j,i).doubleValue();

				if( isInteger ) {
					expected = (int)expected;
            }
            if (expected != found) {
               System.out.println("Expected: " + expected + ", Found: " + found);
            }
				assertEquals(expected,found,1e-4);
			}
		}
	}
   
   private void TestMedianBand(Method m) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		Planar input = new Planar(paramTypes[1], width, height,numBands);
		ImageGray output = GeneralizedImageOps.createSingleBand(paramTypes[1], width, height);

		if( output.getDataType().isSigned() ) {
			GImageMiscOps.fillUniform(input, rand, -20,20);
		} else {
			GImageMiscOps.fillUniform(input, rand, 0,20);
		}
      GImageGray[] testImages = new GImageGray[lastBand - firstBand + 1];
      for (int i = firstBand; i <= lastBand; i++) {
         testImages[i] = FactoryGImageGray.wrap(input.getBand(i));
      }

      // check that a single band gives the expected output
		m.invoke(null,input,output, firstBand, firstBand);
      GImageGray r = FactoryGImageGray.wrap(output);
		boolean isInteger = output.getDataType().isInteger();
      
      for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
            double expected = testImages[firstBand].get(j,i).doubleValue();
            double found = r.get(j,i).doubleValue();

				if( isInteger ) {
					expected = (int)expected;
            }
				assertEquals(expected,found,1e-4);
         }
      }
      
      // now check all bands
      m.invoke(null, input, output, firstBand, lastBand);
      r = FactoryGImageGray.wrap(output);

      double[] values = new double[lastBand - firstBand + 1];
		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
            for (int b = firstBand; b <= lastBand; b++) {
               values[b - firstBand] = testImages[b].get(j,i).doubleValue();
            }
            Arrays.sort(values);
            double expected;
            if (values.length % 2 == 0) {
               expected = (values[values.length/2] + values[values.length/2 - 1])/2;
            } else {
               expected = values[values.length/2];
            }

				double found = r.get(j,i).doubleValue();

				if( isInteger ) {
					expected = (int)expected;
            }
				assertEquals(expected,found,1e-4);
			}
		}
	}
   
   private void TestStdDevBand(Method m) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		Planar input = new Planar(paramTypes[1], width, height,numBands);
		ImageGray output = GeneralizedImageOps.createSingleBand(paramTypes[1], width, height);
      ImageGray av = GeneralizedImageOps.createSingleBand(paramTypes[1], width, height);
      GProjectionMath.averageBand(input, av, firstBand, lastBand);

		if( output.getDataType().isSigned() ) {
			GImageMiscOps.fillUniform(input, rand, -20,20);
		} else {
			GImageMiscOps.fillUniform(input, rand, 0,40);
		}
      GImageGray[] testImages = new GImageGray[lastBand - firstBand + 1];
      for (int i = firstBand; i <= lastBand; i++) {
         testImages[i] = FactoryGImageGray.wrap(input.getBand(i));
      }

      // check that a single band gives the expected output
		m.invoke(null, input, output, av, firstBand, firstBand);
      GImageGray r = FactoryGImageGray.wrap(output);
      GImageGray avg = FactoryGImageGray.wrap(av);
		boolean isInteger = output.getDataType().isInteger();
      
      for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
            double expected = testImages[firstBand].get(j,i).doubleValue();
            double found = r.get(j,i).doubleValue();

				if( isInteger ) {
					expected = (int)expected;
            }
				assertEquals(expected,found,1e-4);
         }
      }
      
      // now check all bands
      m.invoke(null, input, output, avg, firstBand, lastBand);
      r = FactoryGImageGray.wrap(output);

      double diff;
      double expected;
		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				double sum = 0;
            for (int b = firstBand; b <= lastBand; b++) {
               diff = testImages[b].get(j,i).doubleValue()- avg.get(j, i).doubleValue();
               sum += diff * diff;
            }
				expected = Math.sqrt(sum / (numBands - 1));

				double found = r.get(j,i).doubleValue();

				if( isInteger ) {
					expected = (int)expected;
            }
				assertEquals(expected,found,1e-4);
			}
		}
	}
   
}

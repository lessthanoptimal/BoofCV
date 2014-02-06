package org.boofcv.example.android;

import android.graphics.Bitmap;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.android.VisualizeImageData;
import boofcv.android.gui.VideoImageProcessing;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.ImageUInt8;

/**
 * Processes the video data and displays it in a Bitmap image.  The bitmap is automatically scaled and translated
 * into the display's center by {@link VideoActivity}.  Locking of data structures is all handled by the parent.
 *
 * @author Peter Abeles
 */
public class ShowGradient extends VideoImageProcessing<ImageUInt8>
{
   // Storage for the gradient
   private ImageSInt16 derivX = new ImageSInt16(1,1);
   private ImageSInt16 derivY = new ImageSInt16(1,1);

   // computes the image gradient
   private ImageGradient<ImageUInt8,ImageSInt16> gradient = FactoryDerivative.three(ImageUInt8.class, ImageSInt16.class);

   protected ShowGradient()
   {
      super(ImageType.single(ImageUInt8.class));
   }

   @Override
   protected void declareImages( int width , int height ) {
      // You must call the super or else it will crash horribly
      super.declareImages(width,height);

      derivX.reshape(width,height);
      derivY.reshape(width,height);
   }

   @Override
   protected void process(ImageUInt8 gray, Bitmap output, byte[] storage)
   {
      gradient.process(gray,derivX,derivY);
      VisualizeImageData.colorizeGradient(derivX, derivY, -1, output, storage);
   }
}

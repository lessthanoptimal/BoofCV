/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package boofcv.io.gstwebcamcapture;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferInt;
import java.awt.image.WritableRaster;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.freedesktop.gstreamer.Buffer;
import org.freedesktop.gstreamer.Element;
import org.freedesktop.gstreamer.Pad;
import org.freedesktop.gstreamer.PadProbeInfo;
import org.freedesktop.gstreamer.PadProbeReturn;

/**
 *
 * @author techgarage
 */
public class GStreamerImagePipeline implements Pad.PROBE, ImageProducer {

    private final Lock bufferLock = new ReentrantLock();
    private final BufferedImage image;
    private final int[] data;
    private Element identity;

    public GStreamerImagePipeline(int width, int height, Element identity) {
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_BGR);
        data = ((DataBufferInt) (image.getRaster().getDataBuffer())).getData();
        this.identity = identity;
    }

    @Override
    public PadProbeReturn probeCallback(Pad pad, PadProbeInfo info) {

        if (false) {
            return PadProbeReturn.OK;
        }

        if (!bufferLock.tryLock()) {
            System.out.println("Busy");
            return PadProbeReturn.OK;  //https://gstreamer.freedesktop.org/documentation/gstreamer/gstpad.html?gi-language=c
        }

        try {

            Buffer buffer = info.getBuffer();
            if (buffer.isWritable()) {
                IntBuffer ib = buffer.map(true).asIntBuffer();
                ib.get(data);
                process();
                ib.rewind();
                ib.put(data);
                buffer.unmap();
            }

        } finally {
            bufferLock.unlock();
        }

        return PadProbeReturn.OK;
    }

    private void process() {
        for (ImageListener imageListener : imageListeners) {
            ColorModel cm = image.getColorModel();
            boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
            WritableRaster raster = image.copyData(image.getRaster().createCompatibleWritableRaster());
            BufferedImage copy = new BufferedImage(cm, raster, isAlphaPremultiplied, null);
            imageListener.newImage(copy);
        }

    }

    ArrayList<ImageListener> imageListeners = new ArrayList<ImageListener>();

    @Override
    public ImageProducer addImageListener(ImageListener imageListener) {
        imageListeners.add(imageListener);
        return this;
    }
}

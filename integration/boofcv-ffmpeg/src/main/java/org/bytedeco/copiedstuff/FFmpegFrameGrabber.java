/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

/*
 * Copyright (C) 2009-2017 Samuel Audet
 *
 * Licensed either under the Apache License, Version 2.0, or (at your option)
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation (subject to the "Classpath" exception),
 * either version 2, or any later version (collectively, the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     http://www.gnu.org/licenses/
 *     http://www.gnu.org/software/classpath/license.html
 *
 * or as provided in the LICENSE.txt file that accompanied this code.
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * Based on the avcodec_sample.0.5.0.c file available at
 * http://web.me.com/dhoerl/Home/Tech_Blog/Entries/2009/1/22_Revised_avcodec_sample.c_files/avcodec_sample.0.5.0.c
 * by Martin Böhme, Stephen Dranger, and David Hoerl
 * as well as on the decoding_encoding.c file included in FFmpeg 0.11.1,
 * which is covered by the following copyright notice:
 *
 * Copyright (c) 2001 Fabrice Bellard
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.bytedeco.copiedstuff;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVInputFormat;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVDictionaryEntry;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.ffmpeg.swscale.SwsContext;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.PointerPointer;

import java.io.File;
import java.nio.Buffer;
import java.util.Map.Entry;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avformat.*;
import static org.bytedeco.ffmpeg.global.avutil.*;
import static org.bytedeco.ffmpeg.global.swscale.*;

/**
 * Decodes video frames from a file using FFmpeg.
 *
 * <p>Trimmed from JavaCV for BoofCV. Audio decoding, reading from an {@link java.io.InputStream},
 * capture-device support, {@code trigger()}, {@code grabPacket()} and {@code ImageMode.RAW} were all
 * removed since BoofCV decodes video out of files only. The decode call was migrated from the
 * {@code avcodec_decode_video2()} API, removed in FFmpeg 5.0, to
 * {@code avcodec_send_packet()}/{@code avcodec_receive_frame()}. See the readme in this package.</p>
 *
 * @author Samuel Audet
 */
@SuppressWarnings({"UnsafeFinalization", "MissingOverride", "BadImport"})
public class FFmpegFrameGrabber extends FrameGrabber {

    private static Exception loadingException = null;

    public static void tryLoad() throws Exception {
        if (loadingException != null) {
            throw loadingException;
        }
        try {
            Loader.load(org.bytedeco.ffmpeg.global.avutil.class);
            Loader.load(org.bytedeco.ffmpeg.global.avcodec.class);
            Loader.load(org.bytedeco.ffmpeg.global.avformat.class);
            Loader.load(org.bytedeco.ffmpeg.global.swscale.class);

            // av_register_all() and avcodec_register_all() were no-ops from FFmpeg 4.0 and were
            // removed in 5.0. Codecs and formats register themselves now.
            avformat_network_init();
        } catch (Throwable t) {
            if (t instanceof Exception) {
                throw loadingException = (Exception)t;
            } else {
                throw loadingException = new Exception("Failed to load " + FFmpegFrameGrabber.class, t);
            }
        }
    }

    static {
        try {
            tryLoad();
        } catch (Exception ex) {}
    }

    public FFmpegFrameGrabber(File file) {
        this(file.getAbsolutePath());
    }

    public FFmpegFrameGrabber(String filename) {
        this.filename = filename;
    }

    private String filename;
    private AVFormatContext oc;
    private AVStream video_st;
    private AVCodecContext video_c;
    private AVFrame picture, picture_rgb;
    private BytePointer[] image_ptr;
    private Buffer[] image_buf;
    private AVPacket pkt;
    private SwsContext img_convert_ctx;
    private boolean frameGrabbed;
    /** Set once av_read_frame() hits the end of the file and the decoder is being flushed. */
    private boolean draining;
    private Frame frame;

    public void release() throws Exception {
        synchronized (org.bytedeco.ffmpeg.global.avcodec.class) {
            releaseUnsafe();
        }
    }

    void releaseUnsafe() throws Exception {
        // Allocated by av_packet_alloc() in startUnsafe(), so it has to be freed and not just unref'd
        if (pkt != null) {
            av_packet_free(pkt);
            pkt = null;
        }

        // Free the RGB image
        if (image_ptr != null) {
            for (int i = 0; i < image_ptr.length; i++) {
                av_free(image_ptr[i]);
            }
            image_ptr = null;
        }
        if (picture_rgb != null) {
            av_frame_free(picture_rgb);
            picture_rgb = null;
        }

        // Free the native format picture frame
        if (picture != null) {
            av_frame_free(picture);
            picture = null;
        }

        // Close the video codec
        if (video_c != null) {
            avcodec_free_context(video_c);
            video_c = null;
        }

        // Close the video file
        if (oc != null && !oc.isNull()) {
            avformat_close_input(oc);
            oc = null;
        }

        if (img_convert_ctx != null) {
            sws_freeContext(img_convert_ctx);
            img_convert_ctx = null;
        }

        frameGrabbed = false;
        draining = false;
        frame = null;
        timestamp = 0;
        frameNumber = 0;
    }

    @Override public double getGamma() {
        // default to a gamma of 2.2 for cheap Webcams, DV cameras, etc.
        if (gamma == 0.0) {
            return 2.2;
        } else {
            return gamma;
        }
    }

    @Override public String getFormat() {
        if (oc == null) {
            return super.getFormat();
        } else {
            return oc.iformat().name().getString();
        }
    }

    @Override public int getImageWidth() {
        return imageWidth > 0 || video_c == null ? super.getImageWidth() : video_c.width();
    }

    @Override public int getImageHeight() {
        return imageHeight > 0 || video_c == null ? super.getImageHeight() : video_c.height();
    }

    @Override public int getPixelFormat() {
        if (pixelFormat == AV_PIX_FMT_NONE) {
            return imageMode == ImageMode.COLOR ? AV_PIX_FMT_BGR24 : AV_PIX_FMT_GRAY8;
        } else {
            return pixelFormat;
        }
    }

    @Override public int getVideoCodec() {
        return video_c == null ? super.getVideoCodec() : video_c.codec_id();
    }

    @Override public int getVideoBitrate() {
        return video_c == null ? super.getVideoBitrate() : (int)video_c.bit_rate();
    }

    @Override public double getAspectRatio() {
        if (video_st == null) {
            return super.getAspectRatio();
        } else {
            AVRational r = av_guess_sample_aspect_ratio(oc, video_st, picture);
            double a = (double)r.num()/r.den();
            return a == 0.0 ? 1.0 : a;
        }
    }

    @Override public double getFrameRate() {
        if (video_st == null) {
            return super.getFrameRate();
        } else {
            AVRational r = video_st.avg_frame_rate();
            if (r.num() == 0 && r.den() == 0) {
                r = video_st.r_frame_rate();
            }
            return (double)r.num()/r.den();
        }
    }

    @Override public String getMetadata(String key) {
        if (oc == null) {
            return super.getMetadata(key);
        }
        AVDictionaryEntry entry = av_dict_get(oc.metadata(), key, null, 0);
        return entry == null || entry.value() == null ? null : entry.value().getString();
    }

    @Override public String getVideoMetadata(String key) {
        if (video_st == null) {
            return super.getVideoMetadata(key);
        }
        AVDictionaryEntry entry = av_dict_get(video_st.metadata(), key, null, 0);
        return entry == null || entry.value() == null ? null : entry.value().getString();
    }

    @Override public void setFrameNumber(int frameNumber) throws Exception {
        // best guess, AVSEEK_FLAG_FRAME has not been implemented in FFmpeg...
        setTimestamp(Math.round(1000000L*frameNumber/getFrameRate()));
    }

    @Override public void setTimestamp(long timestamp) throws Exception {
        int ret;
        if (oc == null) {
            super.setTimestamp(timestamp);
        } else {
            timestamp = timestamp*AV_TIME_BASE/1000000L;
            /* add the stream start time */
            if (oc.start_time() != AV_NOPTS_VALUE) {
                timestamp += oc.start_time();
            }
            if ((ret = avformat_seek_file(oc, -1, Long.MIN_VALUE, timestamp, Long.MAX_VALUE, AVSEEK_FLAG_BACKWARD)) < 0) {
                throw new Exception("avformat_seek_file() error " + ret + ": Could not seek file to timestamp " + timestamp + ".");
            }
            if (video_c != null) {
                avcodec_flush_buffers(video_c);
            }
            draining = false;
            /* comparing to timestamp +/- 1 avoids rounding issues for framerates
               which are no proper divisors of 1000000, e.g. where
               AVFrame.best_effort_timestamp in grabFrame sets this.timestamp
               to ...666 and the given timestamp has been rounded to ...667
               (or vice versa)
            */
            while (this.timestamp > timestamp + 1 && grabFrame(false) != null) {
                // flush frames if seeking backwards
            }
            while (this.timestamp < timestamp - 1 && grabFrame(false) != null) {
                // decode up to the desired frame
            }
            if (video_c != null) {
                frameGrabbed = true;
            }
        }
    }

    @Override public int getLengthInFrames() {
        // best guess...
        return (int)(getLengthInTime()*getFrameRate()/1000000L);
    }

    @Override public long getLengthInTime() {
        return oc.duration()*1000000L/AV_TIME_BASE;
    }

    public void start() throws Exception {
        synchronized (org.bytedeco.ffmpeg.global.avcodec.class) {
            startUnsafe();
        }
    }

    void startUnsafe() throws Exception {
        int ret;
        img_convert_ctx = null;
        oc = new AVFormatContext(null);
        video_c = null;
        pkt = av_packet_alloc();
        if (pkt == null) {
            throw new Exception("av_packet_alloc() error: Could not allocate packet.");
        }
        frameGrabbed = false;
        draining = false;
        frame = new Frame();
        timestamp = 0;
        frameNumber = 0;

        // Open video file
        AVInputFormat f = null;
        if (format != null && format.length() > 0) {
            if ((f = av_find_input_format(format)) == null) {
                throw new Exception("av_find_input_format() error: Could not find input format \"" + format + "\".");
            }
        }
        AVDictionary options = new AVDictionary(null);
        if (frameRate > 0) {
            AVRational r = av_d2q(frameRate, 1001000);
            av_dict_set(options, "framerate", r.num() + "/" + r.den(), 0);
        }
        if (pixelFormat >= 0) {
            av_dict_set(options, "pixel_format", av_get_pix_fmt_name(pixelFormat).getString(), 0);
        } else {
            av_dict_set(options, "pixel_format", imageMode == ImageMode.COLOR ? "bgr24" : "gray8", 0);
        }
        if (imageWidth > 0 && imageHeight > 0) {
            av_dict_set(options, "video_size", imageWidth + "x" + imageHeight, 0);
        }
        for (Entry<String, String> e : this.options.entrySet()) {
            av_dict_set(options, e.getKey(), e.getValue(), 0);
        }
        if ((ret = avformat_open_input(oc, filename, f, options)) < 0) {
            av_dict_set(options, "pixel_format", null, 0);
            if ((ret = avformat_open_input(oc, filename, f, options)) < 0) {
                throw new Exception("avformat_open_input() error " + ret + ": Could not open input \"" + filename + "\". (Has setFormat() been called?)");
            }
        }
        av_dict_free(options);

        // Retrieve stream information
        if ((ret = avformat_find_stream_info(oc, (PointerPointer)null)) < 0) {
            throw new Exception("avformat_find_stream_info() error " + ret + ": Could not find stream information.");
        }

        // Dump information about file onto standard error
        av_dump_format(oc, 0, filename, 0);

        // Find the first video stream, unless the user specified otherwise
        video_st = null;
        AVCodecParameters video_par = null;
        int nb_streams = oc.nb_streams();
        for (int i = 0; i < nb_streams; i++) {
            AVStream st = oc.streams(i);
            AVCodecParameters par = st.codecpar();
            if (video_st == null && par.codec_type() == AVMEDIA_TYPE_VIDEO && (videoStream < 0 || videoStream == i)) {
                video_st = st;
                video_par = par;
            }
        }
        if (video_st == null) {
            throw new Exception("Did not find a video stream inside \"" + filename
                    + "\" for videoStream == " + videoStream + ".");
        }

        // Find the decoder for the video stream
        AVCodec codec = avcodec_find_decoder(video_par.codec_id());
        if (codec == null) {
            throw new Exception("avcodec_find_decoder() error: Unsupported video format or codec not found: " + video_par.codec_id() + ".");
        }

        /* Allocate a codec context for the decoder */
        if ((video_c = avcodec_alloc_context3(codec)) == null) {
            throw new Exception("avcodec_alloc_context3() error: Could not allocate video decoding context.");
        }

        /* copy the stream parameters from the muxer */
        if ((ret = avcodec_parameters_to_context(video_c, video_st.codecpar())) < 0) {
            release();
            throw new Exception("avcodec_parameters_to_context() error: Could not copy the video stream parameters.");
        }

        options = new AVDictionary(null);
        for (Entry<String, String> e : videoOptions.entrySet()) {
            av_dict_set(options, e.getKey(), e.getValue(), 0);
        }
        // Open video codec
        if ((ret = avcodec_open2(video_c, codec, options)) < 0) {
            throw new Exception("avcodec_open2() error " + ret + ": Could not open video codec.");
        }
        av_dict_free(options);

        // Hack to correct wrong frame rates that seem to be generated by some codecs
        if (video_c.time_base().num() > 1000 && video_c.time_base().den() == 1) {
            video_c.time_base().den(1000);
        }

        // Allocate video frame and an AVFrame structure for the RGB image
        if ((picture = av_frame_alloc()) == null) {
            throw new Exception("av_frame_alloc() error: Could not allocate raw picture frame.");
        }
        if ((picture_rgb = av_frame_alloc()) == null) {
            throw new Exception("av_frame_alloc() error: Could not allocate RGB picture frame.");
        }

        int width = imageWidth > 0 ? imageWidth : video_c.width();
        int height = imageHeight > 0 ? imageHeight : video_c.height();
        int fmt = getPixelFormat();

        // Determine required buffer size and allocate buffer
        int size = av_image_get_buffer_size(fmt, width, height, 1);
        image_ptr = new BytePointer[]{new BytePointer(av_malloc(size)).capacity(size)};
        image_buf = new Buffer[]{image_ptr[0].asBuffer()};

        // Assign appropriate parts of buffer to image planes in picture_rgb
        // Note that picture_rgb is an AVFrame, but AVFrame is a superset of AVPicture
        av_image_fill_arrays(new PointerPointer(picture_rgb), picture_rgb.linesize(), image_ptr[0], fmt, width, height, 1);
        picture_rgb.format(fmt);
        picture_rgb.width(width);
        picture_rgb.height(height);
    }

    public void stop() throws Exception {
        release();
    }

    private void processImage() throws Exception {
        frame.imageWidth = imageWidth > 0 ? imageWidth : video_c.width();
        frame.imageHeight = imageHeight > 0 ? imageHeight : video_c.height();
        frame.imageDepth = Frame.DEPTH_UBYTE;

        // Convert the image into BGR or GRAY format
        img_convert_ctx = sws_getCachedContext(img_convert_ctx,
                video_c.width(), video_c.height(), video_c.pix_fmt(),
                frame.imageWidth, frame.imageHeight, getPixelFormat(), SWS_BILINEAR,
                null, null, (DoublePointer)null);
        if (img_convert_ctx == null) {
            throw new Exception("sws_getCachedContext() error: Cannot initialize the conversion context.");
        }

        // Convert the image from its native format to RGB or GRAY
        sws_scale(img_convert_ctx, new PointerPointer(picture), picture.linesize(), 0,
                video_c.height(), new PointerPointer(picture_rgb), picture_rgb.linesize());
        frame.imageStride = picture_rgb.linesize(0);
        frame.image = image_buf;

        frame.image[0].limit(frame.imageHeight*frame.imageStride);
        frame.imageChannels = frame.imageStride/frame.imageWidth;
    }

    @Override
    public Frame grab() throws Exception {
        return grabFrame(true);
    }

    /**
     * Decodes the next video frame.
     *
     * @param processImage If false the frame is decoded but not converted into {@link Frame#image}.
     * Used while seeking, where the intermediate frames are thrown away.
     * @return The next frame, or null once the end of the stream has been reached.
     */
    public Frame grabFrame(boolean processImage) throws Exception {
        if (oc == null || oc.isNull()) {
            throw new Exception("Could not grab: No AVFormatContext. (Has start() been called?)");
        }
        if (video_st == null) {
            return null;
        }
        frame.imageWidth = 0;
        frame.imageHeight = 0;
        frame.imageDepth = 0;
        frame.imageChannels = 0;
        frame.imageStride = 0;
        frame.image = null;
        frame.opaque = null;

        // setTimestamp() leaves a decoded frame sitting in picture, hand that one back first
        if (frameGrabbed) {
            frameGrabbed = false;
            if (processImage) {
                processImage();
            }
            frame.image = image_buf;
            frame.opaque = picture;
            return frame;
        }

        while (true) {
            // Pull whatever the decoder has ready before feeding it more. A single packet can
            // produce several frames, and at the end of the file the decoder still holds a few.
            int ret = avcodec_receive_frame(video_c, picture);
            if (ret == 0) {
                long pts = picture.best_effort_timestamp();
                AVRational time_base = video_st.time_base();
                timestamp = 1000000L*pts*time_base.num()/time_base.den();
                // best guess, AVCodecContext.frame_number = number of decoded frames...
                frameNumber = (int)(timestamp*getFrameRate()/1000000L);
                if (processImage) {
                    processImage();
                }
                frame.image = image_buf;
                frame.opaque = picture;
                return frame;
            }
            if (ret == AVERROR_EOF) {
                return null;
            }
            if (ret != avutil.AVERROR_EAGAIN()) {
                throw new Exception("avcodec_receive_frame() error " + ret + ": Could not decode a video frame.");
            }

            // The decoder wants more input
            if (draining) {
                // Already sent the flush packet and it still wants more, nothing left to give
                return null;
            }
            if (av_read_frame(oc, pkt) < 0) {
                // End of the file. A null packet puts the decoder into draining mode so that any
                // frames it is still holding come out of avcodec_receive_frame() above.
                draining = true;
                if ((ret = avcodec_send_packet(video_c, (AVPacket)null)) < 0) {
                    throw new Exception("avcodec_send_packet() error " + ret + ": Could not flush the video decoder.");
                }
                continue;
            }
            try {
                if (pkt.stream_index() == video_st.index()) {
                    if ((ret = avcodec_send_packet(video_c, pkt)) < 0) {
                        throw new Exception("avcodec_send_packet() error " + ret + ": Could not send a video packet.");
                    }
                }
            } finally {
                av_packet_unref(pkt);
            }
        }
    }
}

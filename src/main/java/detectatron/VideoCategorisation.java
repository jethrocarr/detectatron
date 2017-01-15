package detectatron;


import com.sun.tools.internal.ws.wsdl.framework.ValidationException;
import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacv.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Takes a video and breaks it into stills, which are then processed by the ImageCategorisation library.
 *
 * We make some lazy assumptions in this class. Since we're processing surveillance video we're not worried about
 * sub-second imagery, generally any event we care about will take more than 1 second - so we just grab a frame for
 * each second of footage and run those through the image categorisation class.
 *
 * TODO: some areas for improvement:
 *  - This performs like shit. The ImageCategorisation task is very slowly and it results in an extremely long
 *    processing time per video whilst we wait for the video to be categorised. Need to move to categorising each
 *    video in a parallel thread.
 *  - Add logic that compares before/after of frames and determine if there's actually enough change to justify
 *    categorising each independently or not. This could save significant compute/cost.
 *  - Add logic that can detect movement of categorised objects. Eg if a car has been detected, is that car moving
 *    in the frames (eg driving up a driveway).
 *
 */
public class VideoCategorisation {
    private static final Logger logger = Logger.getLogger("VideoCategorisation");

    /**
     * This function takes the full video binary (as byte array), extracts the frames and processes each one
     * for categorisation.
     *
     * @param videoBinary
     * @return
     */
    public static String process (
            byte[] videoBinary
    ) {
        logger.log(Level.INFO, "Extracting frames from the supplied video file...");

        // Silence the output of ffmpeg. You may wish to comment this out if debugging some nasty issue, but it
        // makes the logs very noisy otherwise.
        avutil.av_log_set_level(avutil.AV_LOG_QUIET);

        // Note that normally it would be better to use streams rather than loading the entire video into memory at
        // once to reduce G/C and avoid running out of heap, but in our use case we expect lots of smaller files and
        // the need to analyse frames in relation to other frames, requiring most of it to be in memory buffers rather
        // than streaming from the upload/HTTP POST.

        InputStream videoStream = new ByteArrayInputStream(videoBinary);
        FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(videoStream);

        try {
            frameGrabber.start();

            // Useful debugging stats
            int videoLengthSeconds = (int) frameGrabber.getLengthInTime() / 1000000;
            int videoLengthFrames  = frameGrabber.getLengthInFrames();

            logger.log(Level.INFO, "Video in format: " + frameGrabber.getFormat());
            logger.log(Level.INFO, "Length (frame count): " + videoLengthFrames);
            logger.log(Level.INFO, "Length (seconds): " + videoLengthSeconds);


            // Get the frame rate, we need this in order to pick off one frame per second. We round down to the nearest
            // integer to ensure we can iterate through it properly.
            int videoFrameRate = (int) Math.floor( frameGrabber.getFrameRate() );
            logger.log(Level.INFO, "Frame rate: " + videoFrameRate + " frames/second");


            // Process one frame per second of video.
            for (int i=0; i < (videoLengthFrames / videoFrameRate); i++) {

                // We need to jump to specific frames based on the framerate to grab one for each second of the video.
                int frameNumber = (i * videoFrameRate);
                frameGrabber.setFrameNumber(frameNumber);

                // Extract the frame
                BufferedImage currentFrameBuff = new Java2DFrameConverter().convert(frameGrabber.grab());
                ByteArrayOutputStream currentFramePng = new ByteArrayOutputStream();

                // Encode the frame as PNG format (Reckognition supports only PNG or JPG) and we generally
                // wouldn't want to pass around full size binary anyway.
                ImageIO.write(currentFrameBuff, "png", currentFramePng);
                byte[] currentFrameBytes = currentFramePng.toByteArray();

                // Debug example
                //ImageIO.write(currentFrameBuff, "png", new File("/tmp/debug-" + frameNumber + ".png"));

                logger.log(Level.INFO, "Frame number " + frameNumber + " size is: "+ currentFrameBytes.length + " bytes.");

                // Pass the frame to the image categorisation
                String results = ImageCategorisation.process(currentFrameBytes);
                logger.log(Level.INFO, "Results from image categorisation: " + results);
            }

        } catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
            e.printStackTrace();
            throw new ValidationException("An unexpected fault occurred when extracting frames from the video.");
        } catch (java.io.IOException e) {
            e.printStackTrace();
            throw new ValidationException("An unexpected fault occurred when transcoding frame to image.");
        }

        return "placeholder";
    }
}

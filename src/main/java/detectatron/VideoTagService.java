package detectatron;


import com.amazonaws.services.rekognition.model.Label;
import javax.validation.ValidationException;
import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacv.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Takes a video and breaks it into stills, which are then processed by the ImageTagService library.
 *
 * We make some lazy assumptions in this class. Since we're processing surveillance video we're not worried about
 * sub-second imagery, generally any event we care about will take more than 1 second - so we just grab a frame for
 * each second of footage and run those through the image categorisation class.
 *
 * TODO: some areas for improvement:
 *  - Add logic that compares before/after of frames and determine if there's actually enough change to justify
 *    categorising each independently or not. This could save significant compute/cost.
 *  - Add logic that can detect movement of categorised objects. Eg if a car has been detected, is that car moving
 *    in the frames (eg driving up a driveway).
 *
 */

@Service

public class VideoTagService {
    private static final Logger logger = Logger.getLogger("VideoTagService");

    @Autowired
    private ImageTagService myImageTagService;


    /**
     * Take the full video binary (as byte array), extracts the frames and processes each one
     * for categorisation.
     *
     * @param videoBinary
     * @return
     */
    public TagModel process (
            byte[] videoBinary
    ) {
        logger.log(Level.INFO, "Extracting frames from the supplied video file...");

        // We need to collect all the tags
        TagModel videoTags = new TagModel();

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

            // We process up to 30 seconds of footage at the rate of 1 frame per second. If we get a video that is
            // longer than this, we should only pull a max of 30 frames, but expand how frequently we obtain them.
            if (videoLengthSeconds >= 30) {
                logger.log(Level.INFO, "Video submitted is longer than 30 seconds, skipping frames.");

                // Basically we increase the frame rate to assume the video was 30 seconds long, which means we take
                // samples across the entire duration of the video, but just not as frequently. This new framerate is
                // then used when we loop through the video.
                videoFrameRate = (videoLengthFrames / 30);
            }

            // Create an array of futures to allow for background processing.
            List<Future<TagModel>> frameCategorisations = new ArrayList<Future<TagModel>>();

            // Process one frame per second of video.
            for (int i=1; i < (videoLengthFrames / videoFrameRate); i++) {

                // We need to jump to specific frames based on the framerate to grab one for each second of the video.
                int frameNumber = (i * videoFrameRate);
                frameGrabber.setFrameNumber(frameNumber);

                // Extract the frame
                BufferedImage currentFrameBuff = new Java2DFrameConverter().convert(frameGrabber.grab());
                ByteArrayOutputStream currentFrameJpg = new ByteArrayOutputStream();

                // Encode the frame as JPG format (Reckognition supports only PNG or JPG) and we generally
                // wouldn't want to pass around full size binary anyway. Note - we use JPG specifically since the image
                // sizes generated by the PNG writer from ImageIO are bloody awful... about 10x the size of JPGs.
                ImageIO.write(currentFrameBuff, "jpg", currentFrameJpg);
                byte[] currentFrameBytes = currentFrameJpg.toByteArray();

                // Debug example
                //ImageIO.write(currentFrameBuff, "png", new File("/tmp/debug-" + frameNumber + ".png"));

                logger.log(Level.INFO, "Frame number " + frameNumber + " size is: "+ currentFrameBytes.length + " bytes.");

                // Pass the frame to the image categorisation async background pool
                logger.log(Level.INFO, "Submitted frame for background processing...");
                frameCategorisations.add(myImageTagService.processAsync(currentFrameBytes));

                //String results = ImageTagService.process(currentFrameBytes);
                //logger.log(Level.INFO, "Results from image categorisation: " + results);
            }



            // Now we wait for all background workers to complete.
            logger.log(Level.INFO, "All frames submitted, waiting for data to be returned....");
            for (Future<TagModel> frameResult : frameCategorisations) {

                // Wait until this particular frame's processing has been completed.
                while (!frameResult.isDone()) {
                    Thread.sleep(1000);
                }

                // Results!
                TagModel frameTags = frameResult.get();
                videoTags.importLabels(frameTags.rawLabels);
            }

        } catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
            e.printStackTrace();
            throw new ValidationException("An unexpected fault occurred when extracting frames from the video.");
        } catch (java.io.IOException e) {
            e.printStackTrace();
            throw new ValidationException("An unexpected fault occurred when transcoding frame to image.");
        } catch (java.lang.InterruptedException e) {
            e.printStackTrace();
            throw new ValidationException("Process terminated before background workers completed.");
        } catch (java.util.concurrent.ExecutionException e) {
            e.printStackTrace();
            throw new ValidationException("Something went really wrong with the background workers.");
        }

        return videoTags;
    }
}

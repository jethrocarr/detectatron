package detectatron;

import com.amazonaws.services.rekognition.model.Label;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jethro on 21/01/17.
 */
public class TagModel {

    public List<Label> rawLabels = new ArrayList<Label>();
    public List<Label> keyLabels = new ArrayList<Label>();
    public List<String> allTags = new ArrayList<String>();
    public List<String> keyTags = new ArrayList<String>();
    public byte[] keyFrameData;

    /**
     * Instantiation with no data is acceptable.
     */
    public TagModel() {
    }

    /**
     * Create new object using labels provided from Rekognition.
     * @param rekognitionLabels
     */
    public TagModel(List<Label> rekognitionLabels) {
        importLabels(rekognitionLabels);
    }

    /**
     * Import additional labels from AWS rekognition.
     * @param rekognitionLabels
     */
    public void importLabels(List<Label> rekognitionLabels) {

        // Import only the highest confidence version of any given label - we are only interested in what items we
        // are sure was in the video.
        //
        // TOOD: is there a better way of doing this? This is a pretty evil for loop where we use the index to avoid
        // throwing a ConcurrentModificationException
        loopImport:
        for (Label importLabel: rekognitionLabels) {

            loopRaw:
            for (int i =0; i < rawLabels.size(); i++) {
                Label currentLabel = rawLabels.get(i);

                if (currentLabel.getName().equals(importLabel.getName())) {
                    if (importLabel.getConfidence() > currentLabel.getConfidence()) {
                        // Remove the existing label to make room for the new one
                        rawLabels.remove(i);
                        break loopRaw;
                    }
                    else
                    {
                        // New label is lower confidence, ignore and jump to the next one to process.
                        continue loopImport;
                    }
                }
            }

            rawLabels.add(importLabel);
        }

        // Extract useful data
        extractKeyLabels();
        extractTagsOnly();
    }

    /**
     * Extract key labels we care about.
     */
    private void extractKeyLabels() {

        // Reset current key labels
        keyLabels = new ArrayList<Label>();

        // Add all interesting labels from the raw data.
        for (Label label: rawLabels) {
            String labelName = label.getName().toLowerCase();

            switch (labelName) {

                case "people":
                case "person":
                case "cat":
                case "pet":
                    keyLabels.add(label);
                    break;

                default:
                    // Nothing todo if we don't match
                    break;
            }
        }
    }

    /**
     * Create list of strings of the main tags.
     */
    private void extractTagsOnly() {

        for (Label label: rawLabels) {
            if (!allTags.contains(label.getName())) {
                allTags.add(label.getName());
            }
        }

        for (Label label: keyLabels) {
            if (!keyTags.contains(label.getName())) {
                keyTags.add(label.getName());
            }
        }
    }


}

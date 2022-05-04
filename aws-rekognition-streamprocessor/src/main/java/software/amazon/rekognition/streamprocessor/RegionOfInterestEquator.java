package software.amazon.rekognition.streamprocessor;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Equator;
import software.amazon.awssdk.services.rekognition.model.RegionOfInterest;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;

/**
 * Equator implementation for Region of Interest comparisons
 */
public class RegionOfInterestEquator implements Equator<RegionOfInterest> {

    @Override
    public boolean equate(RegionOfInterest roi1, RegionOfInterest roi2) {
        if(roi1 == null && roi2 == null) {
            return true;
        }
        if(roi1 == null || roi2 == null) {
            return false;
        }
        if((roi1.boundingBox() != null && roi1.hasPolygon()) || (roi2.boundingBox() != null && roi2.hasPolygon()) ) {
            throw new CfnServiceInternalErrorException("Unexpected state. Invalid ROI object found. Both BoundingBox and Polygon cannot be present in the same ROI object");
        }
        if(roi1.hasPolygon() && roi2.hasPolygon()) {
            // Checks ordering as well and not just cardinality
            return roi1.polygon().equals(roi2.polygon());
        }
        if(roi1.boundingBox() != null && roi2.boundingBox() != null) {
            return roi1.boundingBox().equals(roi2.boundingBox());
        }
        return false;
    }

    @Override
    public int hash(RegionOfInterest regionOfInterest) {
        return regionOfInterest == null ? 0 : regionOfInterest.getClass().hashCode();
    }
}
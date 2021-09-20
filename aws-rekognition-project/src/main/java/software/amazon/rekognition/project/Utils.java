package software.amazon.rekognition.project;

import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.services.rekognition.model.ProjectDescription;
import software.amazon.awssdk.services.rekognition.model.DescribeProjectsResponse;

import java.util.Optional;

public class Utils {

    static String getProjectNameFromArn(final String projectArn) {
        return Arn.fromString(projectArn).resourceAsString().split("/", 3)[1];
    }

    static Optional<ProjectDescription> findProjectByNameInResponse(DescribeProjectsResponse describeProjectsResponse,
                                                                    final String projectName) 
    {
        return describeProjectsResponse.projectDescriptions().stream()
                .filter(projectDescription -> Utils.getProjectNameFromArn(projectDescription.projectArn()).equals(projectName))
                .findFirst();
    }
}
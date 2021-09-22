package software.amazon.rekognition.project;

import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.services.rekognition.model.ProjectDescription;
import software.amazon.awssdk.services.rekognition.model.DescribeProjectsResponse;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

public class UtilsTest {

    @Test
    void GetProjectNameFromArn_ShouldSucceed() {
        // arrange
        final String expectedProjectName = "projectName";
        final String projectArn = "arn:aws:rekognition:us-east-1:000000000000:project/" + expectedProjectName + "/1111111111111";

        // act
        String projectName = Utils.getProjectNameFromArn(projectArn);

        // assert
        assertEquals(projectName, expectedProjectName);
    }

    @Test
    void findProjectByNameInResponse_ShouldSucceed_WhenProjectExists() {
        // arrange
        List<ProjectDescription> projects = new ArrayList<>();
        final String projectName = "Project1";

        projects.add(ProjectDescription.builder()
            .projectArn("arn:aws:rekognition:us-east-1:000000000000:project/" + projectName + "/1111111111111")
            .build());
        DescribeProjectsResponse describeProjectsResponse = DescribeProjectsResponse.builder()
            .projectDescriptions(projects)
            .build();

        // act
        Optional<ProjectDescription> projectDescription = Utils.findProjectByNameInResponse(describeProjectsResponse, projectName);

        // assert
        assertEquals(true, projectDescription.isPresent());
        assertEquals(projectName, Utils.getProjectNameFromArn(projectDescription.get().projectArn()));
    }

    @Test
    void findProjectByNameInResponse_ShouldSucceed_WhenProjectDoesNotExistAndListEmpty() {
        // arrange
        List<ProjectDescription> projects = new ArrayList<>();
        final String projectName = "Project1";

        DescribeProjectsResponse describeProjectsResponse = DescribeProjectsResponse.builder()
            .projectDescriptions(projects)
            .build();

        // act
        Optional<ProjectDescription> projectDescription = Utils.findProjectByNameInResponse(describeProjectsResponse, projectName);

        // assert
        assertEquals(false, projectDescription.isPresent());
    }

    @Test
    void findProjectByNameInResponse_ShouldFail_WhenProjectDoesNotExistAndListNotEmpty() {
        // arrange
        List<ProjectDescription> projects = new ArrayList<>();
        final String projectName1 = "Project1";
        final String projectName2 = "Project2";

        projects.add(ProjectDescription.builder()
            .projectArn("arn:aws:rekognition:us-east-1:000000000000:project/" + projectName1 + "/1111111111111")
            .build());
        DescribeProjectsResponse describeProjectsResponse = DescribeProjectsResponse.builder()
            .projectDescriptions(projects)
            .build();

        // act
        Optional<ProjectDescription> projectDescription = Utils.findProjectByNameInResponse(describeProjectsResponse, projectName2);

        // assert
        assertEquals(false, projectDescription.isPresent());
    }
}

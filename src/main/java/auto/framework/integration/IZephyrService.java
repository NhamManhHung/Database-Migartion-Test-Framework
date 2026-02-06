package auto.framework.integration;

import auto.framework.models.connection.DefectRequest;

import java.util.List;

public interface IZephyrService {

    boolean isTestRunExist(String runKey);

    String reportResult(
            String runKey,
            String testCaseKey,
            String status,
            String defectKey,
            DefectRequest req
    );

    void attachFiles(
            String runKey,
            String testCaseKey,
            String testResultId,
            List<String> filePaths,
            String fieldName
    );
}


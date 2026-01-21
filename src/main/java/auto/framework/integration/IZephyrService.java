package auto.framework.integration;

import auto.framework.reporting.DefectRequest;

public interface IZephyrService {

    boolean isTestRunExist(String runKey);

    void reportResult(
            String runKey,
            String testCaseKey,
            String status,
            String defectKey,
            DefectRequest req
    );
}


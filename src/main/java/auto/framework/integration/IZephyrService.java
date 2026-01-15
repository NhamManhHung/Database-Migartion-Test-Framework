package auto.framework.integration;

public interface IZephyrService {

    boolean isTestRunExist(String runKey);

    void reportResult(
            String runKey,
            String testCaseKey,
            String status,
            String defectKey
    );
}


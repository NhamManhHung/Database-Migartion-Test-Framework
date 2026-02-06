package auto.framework.integration;

import auto.framework.models.connection.DefectRequest;

import java.util.List;

public interface IJiraService {
    String createDefect(DefectRequest request);
    void attachFiles(String defectKey, List<String> filePaths, String fieldName);
}

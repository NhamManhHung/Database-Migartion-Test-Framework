package auto.framework.integration;

import auto.framework.reporting.DefectRequest;

import java.io.File;

public interface IJiraService {
    String createDefect(DefectRequest request);
    void attachFile (String defectKey, File file);
}

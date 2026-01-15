package auto.framework.integration;

import auto.framework.reporting.DefectRequest;

public interface IJiraService {
    String createDefect(DefectRequest request);
}

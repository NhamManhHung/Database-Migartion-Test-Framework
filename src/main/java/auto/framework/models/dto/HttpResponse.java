package auto.framework.models.dto;

import lombok.Getter;

@Getter
public class HttpResponse {
    private final int status;
    private final String body;

    public HttpResponse(int status, String body) {
        this.status = status;
        this.body = body;
    }

}

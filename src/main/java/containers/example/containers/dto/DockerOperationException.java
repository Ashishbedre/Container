package containers.example.containers.dto;

public class DockerOperationException extends RuntimeException {
    private final int statusCode;

    public DockerOperationException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}

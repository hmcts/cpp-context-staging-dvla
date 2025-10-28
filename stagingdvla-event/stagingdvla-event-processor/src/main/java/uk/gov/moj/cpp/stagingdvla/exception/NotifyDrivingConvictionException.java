package uk.gov.moj.cpp.stagingdvla.exception;

public class NotifyDrivingConvictionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NotifyDrivingConvictionException() {
    }

    public NotifyDrivingConvictionException(final String message) {
        super(message);
    }
}


package org.sharedhealth.mci.web.controller;

import org.sharedhealth.mci.web.exception.PatientNotFoundException;
import org.sharedhealth.mci.web.model.MCIResponse;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static spark.Spark.exception;

public class GlobalExceptionHandler {
    public GlobalExceptionHandler() {
        handlePatientNotFoundException();
    }

    private void handlePatientNotFoundException() {
        exception(PatientNotFoundException.class, (exception, request, response) -> {
            response.status(SC_NOT_FOUND);
            MCIResponse mciResponse = new MCIResponse(SC_NOT_FOUND);
            mciResponse.setMessage(exception.getMessage());
            response.body(mciResponse.toString());
        });
    }
}
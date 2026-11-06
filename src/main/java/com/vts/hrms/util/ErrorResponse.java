package com.vts.hrms.util;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ErrorResponse {
    private int status;
    private String errorCode;
    private String message;
    private boolean hideLoginButton; // used for the MDM offline action

    public ErrorResponse(int status, String errorCode, String message) {
        this(status, errorCode, message, false);
    }

    public ErrorResponse(int status, String errorCode, String message, boolean hideLoginButton) {
        this.status = status;
        this.errorCode = errorCode;
        this.message = message;
        this.hideLoginButton = hideLoginButton;
    }
}
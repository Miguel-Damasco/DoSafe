package com.miguel_damasco.DoSafe.common.apiResponse;

public final class ApiResponses {

    public static <T> ApiResponse<T> success(T data, int pStatusCode, String pMessage) {

        return new ApiResponse.Success<T>(data, 
                                            Meta.of(true, 
                                                    pStatusCode, 
                                                    pMessage));
    }

    public static <T> ApiResponse<Void> error(String pDetails, String pCode, int pStatusCode, String pMessage) {

        ErrorInfo errorInfo = new ErrorInfo(pDetails, pCode);

        return new ApiResponse.Error(errorInfo, Meta.of(false, 
                                                            pStatusCode, 
                                                            pMessage));
    }

    private ApiResponses() {}
}

package com.mahendra.bizcart_backend.admin.dto;

public record ApiResponse<T>(String message, T data) {
}

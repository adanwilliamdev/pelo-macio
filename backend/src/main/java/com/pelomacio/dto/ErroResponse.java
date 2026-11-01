package com.pelomacio.dto;

import java.util.Map;

public record ErroResponse(int status, String message, Map<String, String> campos) {}

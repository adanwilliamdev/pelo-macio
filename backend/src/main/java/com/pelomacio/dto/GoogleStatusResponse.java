package com.pelomacio.dto;

import java.util.List;

/** {@code redirectUri} é a URI que precisa estar cadastrada no Google Cloud Console. */
public record GoogleStatusResponse(boolean configurado, String redirectUri, List<GoogleConexaoResponse> profissionais) {}

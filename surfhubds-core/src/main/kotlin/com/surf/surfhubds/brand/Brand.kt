package com.surf.surfhubds.brand

enum class Brand(val raw: String) {
    MATIZCONECTA("matizconecta"),
    UBER("uber"),
    IFOOD("ifood"),
    BANDSPORTS("bandsports"),
    FLACHIP("flachip"),
    CONECTA("conecta"),
    MEGA("mega"),
    FLUXO("fluxo"),
    PAFER("pafer"),
    PAGUEMENOS("paguemenos"),
    CARREFOURCHIP("carrefourchip"),
    CORREIOSCELULAR("correioscelular"),
    DEFAULT("default");

    companion object {
        fun from(raw: String?): Brand {
            val lower = raw?.lowercase() ?: return DEFAULT
            return values().firstOrNull { it.raw == lower } ?: DEFAULT
        }
    }
}

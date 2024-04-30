package com.khairo.printer.models

class ImpresoraObject (
    val ip: String,
    val nombre: String
) {
    override fun toString(): String {
        return "Impresora [ip: ${this.ip}, nombre: ${this.nombre}]"
    }
}
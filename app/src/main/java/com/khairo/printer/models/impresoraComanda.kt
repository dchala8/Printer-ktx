package com.khairo.printer.models

class impresoraComanda (
    val id_adr: String,
    val adr: String,
    val nombreimpresora: String,
    val ip: String,
    val port: String,
) {
    override fun toString(): String {
        return "Impresora [id_adr: ${this.id_adr}, adr: ${this.adr}, nombreimpresora: ${this.nombreimpresora}, ip: ${this.ip}, port: ${this.port}]"
    }
}
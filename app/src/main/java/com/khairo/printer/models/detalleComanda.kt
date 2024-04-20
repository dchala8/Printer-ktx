package com.khairo.printer.models

class detalleComanda(
    val product_store_name: String,
    val notas: String,
    val estado: String,
    val adr: String
) {
    override fun toString(): String {
        return "Detalle [product_store_name: ${this.product_store_name}, notas: ${this.notas}, estado: ${this.estado}, adr: ${this.adr}]"
    }
}
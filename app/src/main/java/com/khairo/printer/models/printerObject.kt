package com.khairo.printer.models

class printerObject (
    val textToPrint: String,
    val ip: String,
    val port: String,
    val idbill: String,
    val adr: String
) {
    override fun toString(): String {
        return "Impresora [textToPrint: ${this.textToPrint}, ip: ${this.ip}, port: ${this.port}, idbill: ${this.idbill},adr: ${this.adr}]"
    }
}
package com.khairo.printer.models

class encabezadoComanda(
    val num_documento: String,
    val totalprice: Float,
    val purchase_date: String,
    val id_bill: Long
) {
    override fun toString(): String {
        return "Encabezado [num_documento: ${this.num_documento}, totalprice: ${this.totalprice}, purchase_date: ${this.purchase_date}, id_bill: ${this.id_bill}]"
    }
}
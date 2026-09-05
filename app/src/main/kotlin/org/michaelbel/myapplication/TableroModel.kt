package org.michaelbel.myapplication

data class TableroModel(
    val id: Int,
    val fecha: String,
    val codigo: String,
    val serie: String,
    val tieneImg: Boolean,
    val tieneVid: Boolean,
    val tieneDiag: Boolean,
    val tieneGar: Boolean,
    val rutaPdfGarantia: String? = null,
    val seleccionado: Boolean = false
)

package org.michaelbel.myapplication

import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.share.DiskShare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

class SmbScannerService(
    private val host: String,
    private val shareName: String,
    private val domain: String = "",
    private val user: String,
    private val pass: String
) {
    suspend fun escanearTableros(): List<TableroModel> = withContext(Dispatchers.IO) {
        val lista = mutableListOf<TableroModel>()
        val client = SMBClient()

        try {
            client.connect(host).use { connection ->
                val auth = AuthenticationContext(user, pass.toCharArray(), domain)
                val session = connection.authenticate(auth)
                (session.connectShare(shareName) as DiskShare).use { share ->
                    
                    val anioActual = Calendar.getInstance().get(Calendar.YEAR).toString()
                    val rutaBase = "SEGUIMIENTO COTIZACION\\$anioActual"

                    if (!share.folderExists(rutaBase)) return@withContext emptyList()

                    val meses = share.list(rutaBase)
                        .filter { it.fileName !in listOf(".", "..") }
                        .sortedByDescending { it.changeTime.toEpochMillis() }

                    var contador = 1

                    for (mes in meses) {
                        val rutaAprobado = "$rutaBase\\${mes.fileName}\\APROBADO"
                        if (!share.folderExists(rutaAprobado)) continue

                        val tableros = share.list(rutaAprobado)
                            .filter { it.fileName !in listOf(".", "..") }
                            .sortedByDescending { it.changeTime.toEpochMillis() }

                        for (tablero in tableros) {
                            if (contador > 20) break
                            val codigoTablero = tablero.fileName
                            val rutaTablero = "$rutaAprobado\\$codigoTablero"

                            val sucursales = share.list(rutaTablero).filter { it.fileName !in listOf(".", "..") }
                            for (suc in sucursales) {
                                val rutaCliente = "$rutaTablero\\${suc.fileName}\\INFORMACION DE CLIENTE"
                                if (!share.folderExists(rutaCliente)) continue

                                val series = extraerSeries(share, rutaCliente)

                                for (serie in series) {
                                    if (contador > 20) break
                                    val numSolo = serie.replace("Serie", "").trim()

                                    val tieneImg = validarExistencia(share, "$rutaCliente\\img", numSolo)
                                    val tieneVid = validarExistencia(share, "$rutaCliente\\Pruebas", numSolo, extensiones = listOf("mp4", "avi", "mov"))
                                    val tieneDiag = validarExistencia(share, "$rutaCliente\\Diagrama", numSolo)
                                    
                                    val pdfGarantia = buscarPdfGarantia(share, "$rutaCliente\\Garantía", numSolo)

                                    lista.add(
                                        TableroModel(
                                            id = contador++,
                                            fecha = mes.fileName,
                                            codigo = codigoTablero,
                                            serie = serie,
                                            tieneImg = tieneImg,
                                            tieneVid = tieneVid,
                                            tieneDiag = tieneDiag,
                                            tieneGar = pdfGarantia != null,
                                            rutaPdfGarantia = pdfGarantia
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            client.close()
        }
        return@withContext lista
    }

    private fun extraerSeries(share: DiskShare, rutaCliente: String): List<String> {
        val seriesSet = mutableSetOf<String>()
        val carpetas = listOf("img", "Pruebas", "Diagrama", "Garantía")
        for (c in carpetas) {
            val sub = "$rutaCliente\\$c"
            if (share.folderExists(sub)) {
                share.list(sub).forEach { f ->
                    if (f.fileName.contains("Serie", ignoreCase = true)) {
                        val num = f.fileName.substringAfter("Serie").trim().split(" ").firstOrNull() ?: ""
                        if (num.isNotEmpty()) seriesSet.add("Serie $num")
                    }
                }
            }
        }
        return if (seriesSet.isEmpty()) listOf("(Sin Serie)") else seriesSet.toList()
    }

    private fun validarExistencia(share: DiskShare, ruta: String, serieNum: String, extensiones: List<String>? = null): Boolean {
        if (!share.folderExists(ruta)) return false
        val items = share.list(ruta)
        return items.any { item ->
            val match = item.fileName.contains(serieNum, ignoreCase = true)
            if (extensiones != null) {
                match && extensiones.any { ext -> item.fileName.endsWith(".$ext", ignoreCase = true) }
            } else match
        }
    }

    private fun buscarPdfGarantia(share: DiskShare, rutaGarantia: String, serieNum: String): String? {
        if (!share.folderExists(rutaGarantia)) return null
        
        val subcarpetas = share.list(rutaGarantia).filter { it.fileName.contains(serieNum, ignoreCase = true) }
        for (sub in subcarpetas) {
            val subRuta = "$rutaGarantia\\${sub.fileName}"
            if (share.folderExists(subRuta)) {
                val pdf = share.list(subRuta).firstOrNull { it.fileName.endsWith(".pdf", ignoreCase = true) }
                if (pdf != null) return "$subRuta\\${pdf.fileName}"
            }
        }
        return null
    }

    suspend fun descargarPdfTemporal(rutaRemota: String, archivoDestino: File) = withContext(Dispatchers.IO) {
        val client = SMBClient()
        client.connect(host).use { conn ->
            val auth = AuthenticationContext(user, pass.toCharArray(), domain)
            val session = conn.authenticate(auth)
            (session.connectShare(shareName) as DiskShare).use { share ->
                share.openFile(
                    rutaRemota,
                    setOf(com.hierynomus.msdtyp.AccessMask.GENERIC_READ),
                    null,
                    com.hierynomus.mssmb2.SMB2ShareAccess.ALL,
                    com.hierynomus.mssmb2.SMB2CreateDisposition.FILE_OPEN,
                    null
                ).use { remoteFile ->
                    remoteFile.inputStream.use { input ->
                        FileOutputStream(archivoDestino).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
        client.close()
    }
}

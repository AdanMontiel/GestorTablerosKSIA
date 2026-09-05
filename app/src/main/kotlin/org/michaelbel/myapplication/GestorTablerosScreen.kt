package org.michaelbel.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestorTablerosScreen(smbService: SmbScannerService) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var tableros by remember { mutableStateOf<List<TableroModel>>(emptyList()) }
    var cargando by remember { mutableStateOf(false) }

    fun cargarDatos() {
        coroutineScope.launch {
            cargando = true
            tableros = smbService.escanearTableros()
            cargando = false
        }
    }

    LaunchedEffect(Unit) { cargarDatos() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestor Tableros KSI", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { cargarDatos() }) {
                        Text("🔄", fontSize = 18.sp)
                    }
                }
            )
        },
        bottomBar = {
            val seleccionados = tableros.filter { it.seleccionado && it.tieneGar }
            if (seleccionados.isNotEmpty()) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            seleccionados.forEach { item ->
                                item.rutaPdfGarantia?.let { ruta ->
                                    val tempFile = File(context.cacheDir, "${item.serie}.pdf")
                                    smbService.descargarPdfTemporal(ruta, tempFile)
                                    despacharImpresion(context, tempFile)
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Imprimir Seleccionados (${seleccionados.size})")
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (cargando) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                    items(tableros) { tablero ->
                        TableroCard(
                            item = tablero,
                            onCheckedChange = { checked ->
                                tableros = tableros.map {
                                    if (it.id == tablero.id) it.copy(seleccionado = checked) else it
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TableroCard(item: TableroModel, onCheckedChange: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.seleccionado,
                onCheckedChange = onCheckedChange,
                enabled = item.tieneGar
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.codigo, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(text = "${item.serie} • ${item.fecha}", color = Color.Gray, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusChip("IMG", item.tieneImg)
                    StatusChip("VID", item.tieneVid)
                    StatusChip("DIAG", item.tieneDiag)
                    StatusChip("GAR", item.tieneGar)
                }
            }
        }
    }
}

@Composable
fun StatusChip(label: String, activo: Boolean) {
    Box(
        modifier = Modifier
            .background(
                color = if (activo) Color(0xFF2E7D32) else Color(0xFFB0B0B0),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

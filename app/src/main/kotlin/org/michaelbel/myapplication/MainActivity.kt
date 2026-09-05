package org.michaelbel.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

       val smbService = SmbScannerService(
    host = "192.168.99.201",
    shareName = "Tableros",
    domain = "",              // Si el usuario en cmdkey no muestra prefijo (ej. DOMINIO\juan.ochoa), déjalo vacío ""
    user = "juan.ochoa",      // Usuario exacto registrado en la red
    pass = "CONTRASEÑA_DE_JUAN_OCHOA" // Contraseña de red asignada a esa cuenta
)
        )

        setContent {
            MaterialTheme {
                GestorTablerosScreen(smbService = smbService)
            }
        }
    }
}

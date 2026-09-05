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
    user = "Tuvanosa",          // Tu usuario de Windows
    pass = "TU_CONTRASEÑA"      // La contraseña con la que inicias sesión en la laptop/red
)
        )

        setContent {
            MaterialTheme {
                GestorTablerosScreen(smbService = smbService)
            }
        }
    }
}

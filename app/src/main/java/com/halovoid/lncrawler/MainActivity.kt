package com.halovoid.lncrawler

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.halovoid.lncrawler.api.loader.DexLoader
import com.halovoid.lncrawler.ui.screens.MainScreen
import com.halovoid.lncrawler.ui.theme.LNCrawlerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LNCrawlerTheme {
                val loader = DexLoader(this)

                // Define the Picker Launcher
                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri ->
                    uri?.let {
                        // Open the stream from the picked file
                        contentResolver.openInputStream(it)?.let { stream ->
                            val dexFile = loader.copyDexFromInputStream(stream)

                            // Load and Execute
                            val clazz = loader.load(dexFile, "com.halovoid.lncrawlersources.TestCrawler2")
                            val instance = clazz.getDeclaredConstructor().newInstance()
                            val result = clazz.getMethod("hello").invoke(instance)
                            Log.i("DEX_TEST", "Result: $result")
                        }
                    }
                }

                // 3. Add a button to trigger the picker for testing
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//                    MainScreen() // Your existing UI

                    Button(onClick = { launcher.launch("*/*") }) {
                        Text("Select & Load DEX File")
                    }
                }
            }
        }
    }
}

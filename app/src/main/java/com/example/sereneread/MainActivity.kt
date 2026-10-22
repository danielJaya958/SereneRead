package com.example.sereneread

import android.app.Application
import android.os.Bundle
import android.widget.Toast
import android.graphics.Color
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.sereneread.data.remote.network.TokenManager
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.launch
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Aplikasi selalu memakai latar terang, jadi ikon status bar harus gelap
        // walaupun HP sedang dalam mode gelap
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        setContentView(R.layout.activity_main)

        lifecycleScope.launch {
            tokenManager.sessionExpiredEvent.collect {
                // Cari NavController utama aplikasi
                val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                val navController = navHostFragment.navController

                // Cek agar tidak me-refresh jika sudah berada di halaman Login
                if (navController.currentDestination?.id != R.id.loginFragment) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Sesi habis! Silakan login ulang.", Toast.LENGTH_LONG).show()

                        // Bersihkan seluruh riwayat halaman (agar tidak bisa di-back)
                        val navOptions = NavOptions.Builder()
                            .setPopUpTo(R.id.nav_graph, true)
                               .build()

                        navController.navigate(R.id.loginFragment, null, navOptions)
                    }
                }
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
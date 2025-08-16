package org.utl.pruebaproyecto

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.utl.pruebaproyecto.databinding.ActivityMainBinding
import org.utl.pruebaproyecto.ui.notificaciones.NotificacionService
import org.utl.pruebaproyecto.ui.inicioSesion.Login
import kotlin.jvm.java


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificar sesión activa
        auth = FirebaseAuth.getInstance()

        // Usar AuthStateListener para manejar mejor la persistencia de sesión
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user == null) {
                // Solo ir al login si la activity no está siendo destruida
                if (!isFinishing) {
                    val intent = Intent(this, Login::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }

        val user = auth.currentUser
        if (user == null) {
            // Verificación inmediata adicional
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
            return
        }

        checkNotificationPermission()

        startService(Intent(this, NotificacionService::class.java))
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        handleNotificationRedirect(intent)
        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        navView.setNavigationItemSelectedListener(this)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_historial, R.id.nav_notificaciones,
                R.id.nav_monitoreo_condiciones, R.id.nav_control_asistencia, R.id.nav_configuracion
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        val headerView = navView.getHeaderView(0)
        val txtLetraPerfil = headerView.findViewById<TextView>(R.id.txtLetraPerfil)
        val textNombreUsuario = headerView.findViewById<TextView>(R.id.textNombreUsuario)
        val textEmail = headerView.findViewById<TextView>(R.id.textEmail)
        val firestore = FirebaseFirestore.getInstance()
        textEmail.text = user.email ?: "Correo no disponible"
        firestore.collection("usuarios").document(user.uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val nombre = document.getString("nombre") ?: "Usuario"
                    textNombreUsuario.text = nombre

                    val letra = nombre.firstOrNull()?.uppercaseChar() ?: '?'
                    txtLetraPerfil.text = letra.toString()

                    val fondo = GradientDrawable()
                    fondo.shape = GradientDrawable.OVAL
                    fondo.setColor(Color.parseColor("#FF6200EE"))
                    txtLetraPerfil.background = fondo

                } else {
                    textNombreUsuario.text = "Usuario"
                    txtLetraPerfil.text = "?"
                }
            }
            .addOnFailureListener {
                textNombreUsuario.text = "Usuario"
                txtLetraPerfil.text = "?"
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Limpiar el listener
        if (::auth.isInitialized) {
            auth.removeAuthStateListener { }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                Toast.makeText(this, "Inicio", Toast.LENGTH_SHORT).show()
                findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.nav_home)
            }
            R.id.nav_historial -> {
                Toast.makeText(this, "Historial", Toast.LENGTH_SHORT).show()
                findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.nav_historial)
            }
            R.id.nav_notificaciones -> {
                Toast.makeText(this, "Notificaciones", Toast.LENGTH_SHORT).show()
                findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.nav_notificaciones)
            }
            R.id.nav_monitoreo_condiciones -> {
                Toast.makeText(this, "Monitoreo de condiciones", Toast.LENGTH_SHORT).show()
                findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.nav_monitoreo_condiciones)
            }
            R.id.nav_control_asistencia -> {
                Toast.makeText(this, "Control y asistencia", Toast.LENGTH_SHORT).show()
                findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.nav_control_asistencia)
            }
            R.id.nav_configuracion -> {
                Toast.makeText(this, "Configuración", Toast.LENGTH_SHORT).show()
                findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.nav_configuracion)
            }

        }

        // Cerrar el drawer
        binding.drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START)
        return true
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationRedirect(intent)
    }

    private fun handleNotificationRedirect(intent: Intent?) {
        if (intent?.hasExtra("redirect") == true) {
            when (intent.getStringExtra("redirect")) {
                "control_asistencia" -> {
                    findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.nav_control_asistencia)
                }
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }
}
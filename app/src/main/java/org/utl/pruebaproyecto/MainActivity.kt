package org.utl.pruebaproyecto

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
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
import org.utl.pruebaproyecto.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
                // Navegar al fragment de historial o mostrar Toast temporal
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
}
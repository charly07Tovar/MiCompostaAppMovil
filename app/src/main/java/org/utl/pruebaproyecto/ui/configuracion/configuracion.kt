package org.utl.pruebaproyecto.ui.configuracion

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.utl.pruebaproyecto.R
import org.utl.pruebaproyecto.ui.inicioSesion.Login

class ConfiguracionFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_configuracion, container, false)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val txtNombre = view.findViewById<TextView>(R.id.txtNombreUsuario)
        val txtCorreo = view.findViewById<TextView>(R.id.txtCorreoUsuario)
        val txtTelefono = view.findViewById<TextView>(R.id.txtTelefono)
        val letraPerfil = view.findViewById<TextView>(R.id.txtLetraPerfil)
        val btnCerrarSesion = view.findViewById<Button>(R.id.btnCerrarSesion)

        val user = auth.currentUser
        if (user != null) {
            val uid = user.uid
            val correo = user.email ?: ""

            txtCorreo.text = correo

            firestore.collection("usuarios").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val nombre = document.getString("nombre") ?: ""
                        val telefono = document.getString("telefono") ?: ""
                        val letra = nombre.firstOrNull()?.uppercase() ?: "?"

                        txtNombre.text = nombre
                        txtTelefono.text = telefono
                        letraPerfil.text = letra

                        val fondo = GradientDrawable()
                        fondo.shape = GradientDrawable.OVAL
                        fondo.setColor(Color.parseColor("#FF6200EE"))
                        letraPerfil.background = fondo
                    } else {
                        Toast.makeText(context, "Datos no encontrados", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Error al cargar datos", Toast.LENGTH_SHORT).show()
                }
        }

        btnCerrarSesion.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        return view
    }
}

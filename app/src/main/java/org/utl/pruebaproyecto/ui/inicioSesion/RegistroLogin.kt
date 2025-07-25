package org.utl.pruebaproyecto.ui.inicioSesion

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import org.utl.pruebaproyecto.R

class RegistroLogin : AppCompatActivity() {

    private lateinit var etNombreCompleto: EditText
    private lateinit var etTelefono: EditText
    private lateinit var etRegisterEmail: EditText
    private lateinit var etRegisterPassword: EditText
    private lateinit var btnContinue: Button

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro_login)

        auth = FirebaseAuth.getInstance()
        initViews()
        setupRegister()
    }

    private fun initViews() {
        etNombreCompleto = findViewById(R.id.etNombreCompleto)
        etTelefono = findViewById(R.id.etTelefono)
        etRegisterEmail = findViewById(R.id.etRegisterEmail)
        etRegisterPassword = findViewById(R.id.etRegisterPassword)
        btnContinue = findViewById(R.id.btnContinue)
    }

    private fun setupRegister() {
        btnContinue.setOnClickListener {
            val nombre = etNombreCompleto.text.toString().trim()
            val telefono = etTelefono.text.toString().trim()
            val email = etRegisterEmail.text.toString().trim()
            val password = etRegisterPassword.text.toString().trim()

            if (nombre.isEmpty() || telefono.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        val userId = user?.uid ?: return@addOnCompleteListener

                        val userMap = hashMapOf(
                            "uid" to userId,
                            "nombre" to nombre,
                            "telefono" to telefono,
                            "email" to email
                        )

                        db.collection("usuarios").document(userId).set(userMap)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Cuenta registrada correctamente", Toast.LENGTH_SHORT).show()

                                etNombreCompleto.text.clear()
                                etTelefono.text.clear()
                                etRegisterEmail.text.clear()
                                etRegisterPassword.text.clear()

                                val intent = Intent(this, Login::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error al guardar datos: ${e.message}", Toast.LENGTH_LONG).show()
                            }

                    } else {
                        if (task.exception is FirebaseAuthUserCollisionException) {
                            Toast.makeText(this, "Este correo ya está en uso", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "Error: ${task.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        startActivity(Intent(this, Login::class.java))
        finish()
    }
}

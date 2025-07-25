package org.utl.pruebaproyecto.ui.inicioSesion

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import org.utl.pruebaproyecto.MainActivity
import org.utl.pruebaproyecto.R

class Login : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var ivShowPassword: ImageView
    private lateinit var btnSignIn: Button
    private lateinit var tvCreateAccount: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var tvForgotPassword: TextView

    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        initViews()
        setupClickListeners()
        val receivedEmail = intent.getStringExtra("email")
        receivedEmail?.let {
            etEmail.setText(it)
        }
    }

    private fun initViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        ivShowPassword = findViewById(R.id.ivShowPassword)
        btnSignIn = findViewById(R.id.btnSignIn)
        tvCreateAccount = findViewById(R.id.tvCreateAccount)
        tvForgotPassword = findViewById(R.id.forgotPassword)
    }

    private fun setupClickListeners() {
        ivShowPassword.setOnClickListener {
            togglePasswordVisibility()
        }

        tvForgotPassword.setOnClickListener {
            showPasswordResetDialog()
        }

        btnSignIn.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            if (validateInputs(email, password)) {
                loginUser(email, password)
            }
        }

        tvCreateAccount.setOnClickListener {
            val intent = Intent(this, RegistroLogin::class.java)
            startActivity(intent)
        }
    }

    private fun togglePasswordVisibility() {
        if (isPasswordVisible) {
            etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            ivShowPassword.setImageResource(R.drawable.ic_ojo_cerrado)
        } else {
            etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            ivShowPassword.setImageResource(R.drawable.ic_ojo_abierto)
        }
        etPassword.setSelection(etPassword.text.length)
        isPasswordVisible = !isPasswordVisible
    }

    private fun validateInputs(email: String, password: String): Boolean {
        return when {
            email.isEmpty() -> {
                etEmail.error = "El email es requerido"
                etEmail.requestFocus()
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                etEmail.error = "Ingresa un email válido"
                etEmail.requestFocus()
                false
            }
            password.isEmpty() -> {
                etPassword.error = "La contraseña es requerida"
                etPassword.requestFocus()
                false
            }
            password.length < 6 -> {
                etPassword.error = "La contraseña debe tener al menos 6 caracteres"
                etPassword.requestFocus()
                false
            }
            else -> true
        }
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()

                } else {
                    Toast.makeText(this, "Error al iniciar sesión: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }


    private fun showPasswordResetDialog() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Recuperar contraseña")
        builder.setMessage("Introduce tu correo electrónico para enviar un enlace de recuperación.")

        val input = EditText(this)
        input.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        input.hint = "correo@ejemplo.com"
        builder.setView(input)

        builder.setPositiveButton("Enviar") { dialog, _ ->
            val email = input.text.toString().trim()
            if (email.isNotEmpty()) {
                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Correo de recuperación enviado.", Toast.LENGTH_LONG).show()
                        } else {
                            val error = task.exception?.message ?: "Ocurrió un error"
                            Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Por favor ingresa un correo válido.", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

}

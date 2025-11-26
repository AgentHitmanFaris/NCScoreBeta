package com.noobcompany.nc_scorebeta

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var isLoginMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etName = findViewById<EditText>(R.id.etName)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvRegister = findViewById<TextView>(R.id.tvRegister)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)

        tvRegister.setOnClickListener {
            isLoginMode = !isLoginMode
            if (isLoginMode) {
                tvWelcome.text = "Welcome Back"
                btnLogin.text = "Log In"
                tvRegister.text = "Don't have an account? Register"
                etName.visibility = View.GONE
                etConfirmPassword.visibility = View.GONE
            } else {
                tvWelcome.text = "Create Account"
                btnLogin.text = "Register"
                tvRegister.text = "Already have an account? Log In"
                etName.visibility = View.VISIBLE
                etConfirmPassword.visibility = View.VISIBLE
            }
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val name = etName.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isLoginMode) {
                if (name.isEmpty()) {
                    Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (password != confirmPassword) {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            progressBar.visibility = View.VISIBLE

            if (isLoginMode) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        checkFirestoreUser(it.user?.uid)
                    }
                    .addOnFailureListener {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this, "Login Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener { result ->
                        createFirestoreUser(result.user?.uid, email, name)
                    }
                    .addOnFailureListener {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this, "Registration Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun createFirestoreUser(uid: String?, email: String, name: String) {
        if (uid == null) return

        val userMap = hashMapOf(
            "name" to name,
            "email" to email,
            "createdAt" to com.google.firebase.Timestamp.now(),
            "isPremiumUser" to false // Default to false
        )

        db.collection("users").document(uid).set(userMap)
            .addOnSuccessListener {
                finishLogin()
            }
            .addOnFailureListener {
                findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
                Toast.makeText(this, "Error creating profile. Please try again.", Toast.LENGTH_LONG).show()
                // Optionally sign out if profile creation failed
                auth.signOut()
            }
    }

    private fun checkFirestoreUser(uid: String?) {
        if (uid == null) return
        // Just verify access exists
        db.collection("users").document(uid).get()
            .addOnSuccessListener {
                finishLogin()
            }
            .addOnFailureListener {
                findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
                Toast.makeText(this, "Error verifying profile. Please contact support.", Toast.LENGTH_LONG).show()
                auth.signOut()
            }
    }

    private fun finishLogin() {
        findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
        Toast.makeText(this, "Success!", Toast.LENGTH_SHORT).show()
        setResult(RESULT_OK)
        finish()
    }
}
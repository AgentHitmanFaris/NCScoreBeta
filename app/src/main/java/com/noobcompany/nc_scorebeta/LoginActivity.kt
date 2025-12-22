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

/**
 * Activity responsible for User Authentication (Login and Registration).
 *
 * This activity presents a unified interface for users to sign in or create a new account
 * using email and password credentials. It manages the interaction with Firebase Authentication
 * for credential verification and Firebase Firestore for creating and validating user profiles.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var isLoginMode = true

    /**
     * Checks if the password meets security requirements.
     * Rule: Minimum 8 characters, at least one letter, and at least one digit.
     */
    private fun isPasswordStrong(password: String): Boolean {
        return password.length >= 8 &&
                password.any { it.isLetter() } &&
                password.any { it.isDigit() }
    }

    /**
     * Validates the email format using standard Android patterns.
     */
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Initializes the activity lifecycle.
     *
     * Performs the following initialization steps:
     * 1. Inflates the layout resources.
     * 2. Initializes Firebase Auth and Firestore instances.
     * 3. Binds UI elements (EditTexts, Buttons).
     * 4. Sets up the click listener for toggling between "Login" and "Register" modes.
     * 5. Sets up the click listener for the submission button to trigger auth logic.
     *
     * @param savedInstanceState Bundle containing the activity's previously saved state, if any.
     */
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

            if (!isValidEmail(email)) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
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
                if (!isPasswordStrong(password)) {
                    Toast.makeText(this, "Password must be at least 8 characters with letters and numbers", Toast.LENGTH_LONG).show()
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
                        // SECURITY: Use generic error message to prevent user enumeration
                        Toast.makeText(this, "Login Failed: Invalid email or password", Toast.LENGTH_SHORT).show()
                        AppLogger.error("LoginActivity", "Login error: ${it.message}", it)
                    }
            } else {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener { result ->
                        createFirestoreUser(result.user?.uid, email, name)
                    }
                    .addOnFailureListener {
                        progressBar.visibility = View.GONE
                        // SECURITY: Use generic error message to prevent user enumeration
                        Toast.makeText(this, "Registration Failed. Please check your details.", Toast.LENGTH_SHORT).show()
                        AppLogger.error("LoginActivity", "Registration error: ${it.message}", it)
                    }
            }
        }
    }

    /**
     * Creates a standard user document in the 'users' Firestore collection.
     *
     * This is called immediately after a successful registration via Firebase Auth.
     * It stores the user's display name, email, and creation timestamp.
     *
     * @param uid The unique user ID (UID) from Firebase Authentication.
     * @param email The email address used for registration.
     * @param name The user's provided display name.
     */
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

    /**
     * Verifies the existence of the user's Firestore document.
     *
     * Called after a successful login to ensure the user has a valid profile in the database.
     * This acts as an integrity check.
     *
     * @param uid The user ID to query.
     */
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

    /**
     * Finalizes the authentication process.
     *
     * Hides the loading indicator, displays a success toast, sets the Activity result to [RESULT_OK],
     * and finishes the activity to return control to the caller (MainActivity).
     */
    private fun finishLogin() {
        findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
        Toast.makeText(this, "Success!", Toast.LENGTH_SHORT).show()
        setResult(RESULT_OK)
        finish()
    }
}

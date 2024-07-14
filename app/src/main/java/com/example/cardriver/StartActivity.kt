package com.example.cardriver

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import classes.User
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.*

class StartActivity : AppCompatActivity() {
    private lateinit var textEmail: EditText
    private lateinit var textPassword: EditText
    private lateinit var login: Button
    private lateinit var register: TextView
    private lateinit var forgotPass: TextView
    private var reference: DatabaseReference? = null
    private var ID: String? = null
    private lateinit var auth: FirebaseAuth

    private var backPressed = false

    private lateinit var rememberMe: CheckBox

    private var gsc: GoogleSignInClient? = null
    private lateinit var googleButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        textEmail = findViewById(R.id.email)
        textPassword = findViewById(R.id.password)
        login = findViewById(R.id.signin)
        rememberMe = findViewById(R.id.rememberMe)

        auth = FirebaseAuth.getInstance()

        // Google Sign in: .........................................
        googleButton = findViewById(R.id.googleb)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(com.example.cardriver.R.string.default_web_client_id))
            .requestEmail()
            .build()
        gsc = GoogleSignIn.getClient(this, gso)

        login.setOnClickListener {
            val email = textEmail.text.toString()
            val password = textPassword.text.toString()
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(this@StartActivity, "Please enter email.", Toast.LENGTH_SHORT).show()
                textEmail.requestFocus()
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this@StartActivity, "Please enter valid email.", Toast.LENGTH_SHORT)
                    .show()
                textEmail.requestFocus()
            } else if (TextUtils.isEmpty(password)) {
                Toast.makeText(this@StartActivity, "Please enter password.", Toast.LENGTH_SHORT)
                    .show()
                textPassword.requestFocus()
            } else if (password.length < 6) {
                Toast.makeText(
                    this@StartActivity,
                    "Please enter at least 6 characters.",
                    Toast.LENGTH_SHORT
                ).show()
                textPassword.requestFocus()
            } else {
                loginUser(email, password)
            }
        }

        register = findViewById(R.id.register)
        register.setOnClickListener {
            val intent = Intent(applicationContext, RegisterActivity::class.java)
            startActivity(intent)
        }

//        forgotPass = findViewById(R.id.forgotPass)
//        forgotPass.setOnClickListener {
//            val intent = Intent(applicationContext, ForgotPassActivity::class.java)
//            startActivity(intent)
//        }

        // Remember me: ................................................................
        val preferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE)
        val rememberBox = preferences.getString("remember", "")
        if (rememberBox == "true") {
            val user = FirebaseAuth.getInstance().currentUser
            reference = FirebaseDatabase.getInstance().getReference("Users")
            ID = user!!.uid
            reference!!.child(ID!!).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(error: DatabaseError) {}

                override fun onDataChange(snapshot: DataSnapshot) {
                    startActivity(
                        Intent(this@StartActivity, MainActivity::class.java)
                    )
                    Toast.makeText(this@StartActivity, "Login successful!", Toast.LENGTH_SHORT)
                        .show()
                    finish()
                }

            })
        }

        // Set remember me checkbox
        rememberMe.setOnCheckedChangeListener { buttonView, isChecked ->
            val sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString("remember", if (isChecked) "true" else "false")
            editor.apply()
        }

        // Google sign-in button
        googleButton.setOnClickListener { signIn() }
    }

    private fun signIn() {
        val signInIntent = gsc!!.signInIntent
        startActivityForResult(signInIntent, 1234)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1234) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)

            try {
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                FirebaseAuth.getInstance().signInWithCredential(credential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val intent = Intent(applicationContext, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(
                                this@StartActivity,
                                task.exception!!.message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            } catch (e: ApiException) {
                println("Exception: $e")
                Toast.makeText(applicationContext, "Something went wrong", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    override fun onBackPressed() {
        if (backPressed) {
            super.onBackPressed()
            return
        }
        Toast.makeText(this, "Press 'back' again to exit", Toast.LENGTH_SHORT).show()
        backPressed = true

        Handler().postDelayed({ backPressed = false }, 2000)
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = FirebaseAuth.getInstance().currentUser

                if (user!!.isEmailVerified) {
                    reference = FirebaseDatabase.getInstance().getReference("Users")
                    ID = user.uid
                    reference!!.child(ID!!).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onCancelled(error: DatabaseError) {}

                        override fun onDataChange(snapshot: DataSnapshot) {
                            startActivity(
                                Intent(this@StartActivity, MainActivity::class.java)
                            )
                            Toast.makeText(this@StartActivity, "Login successful!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    })
                } else {
                    user.sendEmailVerification()
                    Toast.makeText(this@StartActivity, "Check your email to verify.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this@StartActivity, "Wrong email or password!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val SHARED_PREFS = "sharedPrefs"
    }
}

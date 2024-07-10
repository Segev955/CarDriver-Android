package com.example.cardriver

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import classes.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val fullNameEditText: EditText = findViewById(R.id.fullName)
        val emailEditText: EditText = findViewById(R.id.email)
        val passwordEditText: EditText = findViewById(R.id.password)
        val dateEditText: EditText = findViewById(R.id.date)
        val carTypeEditText: EditText = findViewById(R.id.carType)
        val genderRadioGroup: RadioGroup = findViewById(R.id.gender)
        val signUpButton: Button = findViewById(R.id.signup)

        signUpButton.setOnClickListener {
            val fullName = fullNameEditText.text.toString()
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            val birthDate = dateEditText.text.toString()
            val carType = carTypeEditText.text.toString()
            val selectedGenderId = genderRadioGroup.checkedRadioButtonId
            val selectedGender: RadioButton? = findViewById(selectedGenderId)
            val gender = selectedGender?.text.toString()

            // Perform checks
            val fullNameCheck = User.check_fullName(fullName)
            if (fullNameCheck != "accept") {
                Toast.makeText(this, fullNameCheck, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val emailCheck = User.check_email(email)
            if (emailCheck != "accept") {
                Toast.makeText(this, emailCheck, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val passwordCheck = User.check_pass(password)
            if (passwordCheck != "accept") {
                Toast.makeText(this, passwordCheck, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dateCheck = User.check_date(birthDate)
            if (dateCheck != "accept") {
                Toast.makeText(this, dateCheck, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // All checks passed, create user in Firebase Authentication
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        val firebaseUser = auth.currentUser
                        val userId = firebaseUser?.uid

                        // Create user object
                        val user = User(fullName, email, birthDate, gender, carType)

                        // Save user object in Firebase Realtime Database
                        userId?.let {
                            FirebaseDatabase.getInstance().reference.child("Users").child(it).setValue(user)
                                .addOnCompleteListener { databaseTask ->
                                    if (databaseTask.isSuccessful) {
                                        Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                                        // Optionally, proceed to the next activity
                                        val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                                        startActivity(intent)
                                        finish()
                                    } else {
                                        Toast.makeText(this, "Failed to save user data: ${databaseTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        }
                    } else {
                        // If sign in fails, display a message to the user.
                        Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}

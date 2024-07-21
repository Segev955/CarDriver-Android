package com.example.cardriver

import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import classes.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Calendar

class EditProfileActivity : AppCompatActivity() {
    private var text_fullName: EditText? = null
    private var text_date: EditText? = null
    private var radio_gender: RadioGroup? = null
    private var male_r: RadioButton? = null
    private var female_r: RadioButton? = null
    private var gender_r: RadioButton? = null
    private var submit: Button? = null
    private var user: FirebaseUser? = null
    private var reference: DatabaseReference? = null
    private var ID: String? = null
    private var profile: User? = null

    //    private ProgressBar progressBar;
    private var auth: FirebaseAuth? = null
    private var mDateSetListener: OnDateSetListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.example.cardriver.R.layout.activity_edit_profile)
        text_fullName = findViewById<EditText>(com.example.cardriver.R.id.fullName)
        text_date = findViewById<EditText>(com.example.cardriver.R.id.date)
        submit = findViewById<Button>(com.example.cardriver.R.id.submit)
        radio_gender = findViewById<RadioGroup>(com.example.cardriver.R.id.gender)
        male_r = findViewById<RadioButton>(com.example.cardriver.R.id.male)
        female_r = findViewById<RadioButton>(com.example.cardriver.R.id.female)
        user = FirebaseAuth.getInstance().currentUser
        reference = FirebaseDatabase.getInstance().getReference("Users")
        ID = user!!.uid
        reference!!.child(ID!!).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                profile = snapshot.getValue(User::class.java)
                if (profile != null) {
                    text_fullName?.setText(profile!!.getFullName())
                    text_date?.setText(profile!!.getDate())
                    if (profile!!.getGender() == "Male") male_r?.isChecked = true
                    else female_r?.isChecked = true
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })

        auth = FirebaseAuth.getInstance()

        // Date Button:
        text_date?.setOnClickListener {
            val cal = Calendar.getInstance()
            val day = cal[Calendar.DAY_OF_MONTH]
            val month = cal[Calendar.MONTH]
            val year = cal[Calendar.YEAR]
            val dateDialog = DatePickerDialog(
                this@EditProfileActivity,
                android.R.style.Theme_Holo_Dialog_MinWidth, mDateSetListener, year, month, day
            )
            dateDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dateDialog.show()
        }

        mDateSetListener = OnDateSetListener { _, year, month, dayOfMonth ->
            val date = "$dayOfMonth/${month + 1}/$year"
            text_date?.setText(date)
        }

        submit?.setOnClickListener {
            val fullName = text_fullName?.text.toString()
            val date = text_date?.text.toString()
            val select_gender = radio_gender?.checkedRadioButtonId

            var validans: String? = ""
            if (User.check_fullName(fullName).also { validans = it } != "accept") {
                Toast.makeText(this@EditProfileActivity, validans, Toast.LENGTH_SHORT).show()
                text_fullName?.requestFocus()
            } else if (User.check_date(date).also { validans = it } != "accept") {
                Toast.makeText(this@EditProfileActivity, validans, Toast.LENGTH_SHORT).show()
                text_date?.requestFocus()
            } else if (select_gender == -1) {
                Toast.makeText(
                    this@EditProfileActivity,
                    "Please choose gender.",
                    Toast.LENGTH_SHORT
                ).show()
                radio_gender?.requestFocus()
            } else {
                gender_r = findViewById<View>(select_gender!!) as RadioButton
                val gender = gender_r!!.text.toString()
                updateUser(fullName, date, gender)
            }
        }
    }

    private fun updateUser(fullName: String, date: String, gender: String) {
        profile?.setFullName(fullName)
        profile?.setGender(gender)
        profile?.setDate(date)
        FirebaseDatabase.getInstance().getReference("Users")
            .child(FirebaseAuth.getInstance().currentUser!!.uid).setValue(profile)
            .addOnCompleteListener {
                Toast.makeText(
                    this@EditProfileActivity, "$fullName Edit profile successfully!", Toast.LENGTH_SHORT).show()
                val intent = Intent(applicationContext, MainActivity::class.java)
                startActivity(intent)
            }
    }
}

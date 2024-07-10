package classes

import android.os.Build
import android.text.TextUtils
import android.util.Patterns
import java.time.LocalDate

/**
 * class User
 */
class User {
    // ---- PRIVATE PROPERTIES ---- //
    private var fullName: String? = null
    private var email: String? = null
    private var date: String? = null
    private var gender: String? = null
    private var carType: String? = null

    constructor()

    /**
     * constructor
     */
    constructor(fullName: String?, email: String?, date: String?, gender: String?, carType: String?) {
        this.fullName = fullName
        this.email = email
        this.date = date
        this.gender = gender
        this.carType = carType
    }

    // ---- GETTERS ---- //
    fun getFullName(): String? {
        return fullName
    }

    fun getEmail(): String? {
        return email
    }

    fun getDate(): String? {
        return date
    }

    fun getGender(): String? {
        return gender
    }

    fun getCarType(): String? {
        return carType
    }

    companion object {
        // ---- END OF GET(ERS) & SET(ERS) ---- //
        /**
         * check full Name of User (need be correct)
         *
         * @param  fullName    full name of user
         * @return         (String) accept/not
         */
        fun check_fullName(fullName: String): String {
            if (TextUtils.isEmpty(fullName)) return "Please enter full name."
            for (i in 0 until fullName.length) {
                if (fullName[i] == '\n') return "Full name not valid!"
            }
            return "accept"
        }

        /**
         * check if email of User is valid (need be correct)
         *
         * @param  email    email of user
         * @return         (String) accept/not
         */
        fun check_email(email: String?): String {
            if (TextUtils.isEmpty(email)) return "Please enter email."
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) return "Please enter valid email."
            return "accept"
        }

        /**
         * check if password of User is valid (need be correct)
         *
         * @param  password    password of user
         * @return         (String) accept/not
         */
        fun check_pass(password: String): String {
            if (TextUtils.isEmpty(password)) return "Please enter password."
            if (password.length < 6) return "Please enter at least 6 characters."
            return "accept"
        }

        /**
         * check if user's date of birth is valid (need be correct)
         *
         * @param  date    date of user
         * @return         (String) accept/not
         */
        fun check_date(date: String): String {
            val minage = 16
            if (TextUtils.isEmpty(date)) return "Please enter date (dd/mm/yyyy)."
            val splited = date.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (splited.size != 3) return "Please enter valid date (dd/mm/yyyy)"
            val day: Int
            val month: Int
            val year: Int
            try {
                day = splited[0].toInt()
                month = splited[1].toInt()
                year = splited[2].toInt()
            } catch (nfe: NumberFormatException) {
                return "Date must be numbers (dd/mm/yyyy)"
            }
            var nowy = 2023
            var nowm = 1
            var nowd = 1
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nowy = LocalDate.now().year
                nowm = LocalDate.now().month.value
                nowd = LocalDate.now().dayOfMonth
            }
            if (nowy - 120 > year || year > nowy) return "Year not valid"
            if (month < 1 || month > 12) return "Month not valid"
            if (day < 1 || day > 31 || (day == 31 && (month == 4 || month == 6 || month == 9 || month == 11)) || (month == 2 && (day > 29 || (day == 29 && (year % 4 != 0 || (year % 100 == 0 && year % 400 != 0)))))) return "Day not valid"
            if (year > nowy - minage || (year == nowy - minage && (month < nowm || (month == nowm && day < nowd)))) return "Minimum age for using this app is $minage."
            return "accept"
        }
    }

    override fun toString(): String {
        return "User(fullName=$fullName, email=$email, date=$date, gender=$gender, carType=$carType)"
    }
}

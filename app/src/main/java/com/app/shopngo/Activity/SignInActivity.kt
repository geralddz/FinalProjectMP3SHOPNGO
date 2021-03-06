package com.app.shopngo.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.app.shopngo.Object.CustomDialog
import com.app.shopngo.R
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.*

class SignInActivity : AppCompatActivity() {
    private lateinit var btnSignIn: Button
    private lateinit var btnGoogleSignIn: ImageView
    private lateinit var btnFBSignIn: ImageView
    private lateinit var etEmailIn: EditText
    private lateinit var etPassIn: EditText
    private lateinit var tvForgotPass: TextView
    private lateinit var tvSignUp: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var callbackManager: CallbackManager

    companion object {
        const val RC_SIGN_IN = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        btnSignIn = findViewById(R.id.btnSignIn)
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)
        btnFBSignIn = findViewById(R.id.btnFacebookSignIn)
        etEmailIn = findViewById(R.id.etEmailSignIn)
        etPassIn = findViewById(R.id.etPasswordSignIn)
        tvSignUp = findViewById(R.id.tvSignUp)
        tvForgotPass = findViewById(R.id.tvForgotPass)

        initFirebase()

        btnSignIn.setOnClickListener {
            val email = etEmailIn.text.toString().trim()
            val pass = etPassIn.text.toString().trim()

            if (CheckValidation(email, pass)) {
                LoginToServer(email, pass)
            }
        }
        tvSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        tvForgotPass.setOnClickListener {
            startActivity(Intent(this, ResetPassActivity::class.java))
        }

        btnGoogleSignIn.setOnClickListener {
            val signIntent = mGoogleSignInClient.signInIntent
            startActivityForResult(signIntent, RC_SIGN_IN)
        }

        btnFBSignIn.setOnClickListener {
            loginFacebook()
        }
    }

    private fun initFirebase() {
        auth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
        callbackManager = CallbackManager.Factory.create()
    }

    private fun CheckValidation(email: String, pass: String): Boolean {
        if (email.isEmpty()) {
            etEmailIn.error = "Masukkan Email Anda"
            etEmailIn.requestFocus()
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmailIn.error = "Masukkan Email dengan Benar"
            etEmailIn.requestFocus()
        } else if (pass.isEmpty()) {
            etPassIn.error = "Masukkan Password Ada"
            etPassIn.requestFocus()
        } else {
            return true
        }
        CustomDialog.hideLoading()
        return false
    }

    private fun LoginToServer(email: String, pass: String) {
        val credential = EmailAuthProvider.getCredential(email, pass)
        firebaseAuth(credential)
    }

    private fun firebaseAuth(credential: AuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                CustomDialog.hideLoading()
                if (task.isSuccessful) {
                    startActivity(Intent(this, MainActivity::class.java))
                    Toast.makeText(this, "Sign In Berhasil", Toast.LENGTH_SHORT).show()
                    finishAffinity()
                } else {
                    Toast.makeText(this, "Sign In Gagal", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                CustomDialog.hideLoading()
                Toast.makeText(this, "Pastikan Email dan Password Sudah Benar", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun loginFacebook() {
        LoginManager.getInstance().logInWithReadPermissions(this, listOf("email"))
        LoginManager.getInstance().registerCallback(callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onCancel() {
                    TODO("Not yet implemented")
                }

                override fun onError(error: FacebookException) {
                    Toast.makeText(this@SignInActivity, error.message, Toast.LENGTH_SHORT).show()
                }

                override fun onSuccess(result: LoginResult) {
                    CustomDialog.showLoading(this@SignInActivity)
                    val credential = FacebookAuthProvider.getCredential(result.accessToken.token)
                    firebaseAuth(credential)
                }
            })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        callbackManager.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            CustomDialog.showLoading(this)
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
                firebaseAuth(credential)
            } catch (e: ApiException) {
                CustomDialog.hideLoading()
                Toast.makeText(this, "Sign In Cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    }


}
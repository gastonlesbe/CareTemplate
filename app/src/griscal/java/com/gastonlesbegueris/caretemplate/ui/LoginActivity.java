package com.gastonlesbegueris.caretemplate.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

import com.gastonlesbegueris.caretemplate.R;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_GOOGLE_SIGN_IN = 9001;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    private TextInputEditText etEmail, etPassword;
    private Button btnSignIn, btnRegister, btnGoogle;
    private TextView tvError;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        // Already signed in → go straight to main
        if (mAuth.getCurrentUser() != null) {
            goToMain();
            return;
        }

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnSignIn = findViewById(R.id.btn_sign_in);
        btnRegister = findViewById(R.id.btn_register);
        btnGoogle = findViewById(R.id.btn_google);
        tvError = findViewById(R.id.tv_error);
        progressBar = findViewById(R.id.progress_bar);

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        btnSignIn.setOnClickListener(v -> handleEmailSignIn());
        btnRegister.setOnClickListener(v -> handleRegister());
        btnGoogle.setOnClickListener(v -> handleGoogleSignIn());
    }

    private void handleEmailSignIn() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";
        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter email and password.");
            return;
        }
        setLoading(true);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        goToMain();
                    } else {
                        showError(friendlyError(task.getException()));
                    }
                });
    }

    private void handleRegister() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";
        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter email and password.");
            return;
        }
        setLoading(true);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        goToMain();
                    } else {
                        showError(friendlyError(task.getException()));
                    }
                });
    }

    private void handleGoogleSignIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                showError("Google sign-in failed: " + e.getMessage());
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        setLoading(true);
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        goToMain();
                    } else {
                        showError(friendlyError(task.getException()));
                    }
                });
    }

    private void goToMain() {
        startActivity(new Intent(this, GriscalDashboardActivity.class));
        finish();
    }

    private void setLoading(boolean loading) {
        btnSignIn.setEnabled(!loading);
        btnRegister.setEnabled(!loading);
        btnGoogle.setEnabled(!loading);
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }

    private String friendlyError(Exception e) {
        if (e == null) return "Sign-in failed.";
        String msg = e.getMessage();
        if (msg == null) return "Sign-in failed.";
        if (msg.contains("no user record")) return "No account found with that email.";
        if (msg.contains("password is invalid") || msg.contains("INVALID_LOGIN_CREDENTIALS"))
            return "Incorrect email or password.";
        if (msg.contains("badly formatted")) return "Invalid email address.";
        if (msg.contains("too many")) return "Too many attempts. Try again later.";
        if (msg.contains("email address is already")) return "Email already in use. Try signing in.";
        return msg;
    }
}

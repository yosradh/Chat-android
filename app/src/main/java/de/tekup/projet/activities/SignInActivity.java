package de.tekup.projet.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import de.tekup.projet.databinding.ActivitySignInBinding;
import de.tekup.projet.utilities.Constants;
import de.tekup.projet.utilities.PreferenceManager;

public class SignInActivity extends AppCompatActivity {

    private ActivitySignInBinding binding;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceManager = new PreferenceManager(getApplicationContext());
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();
    }

    private void setListeners(){
        binding.textCreatNexAccount.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(),SingUpActivity.class)));
        // lorsque vous cliquer sur le button les fonctions qu'il va travailler
        binding.buttonSingIn.setOnClickListener(v ->{
            if(isValidSignInDetails()){
                SignIn();
        }}
        );
    }

    private void SignIn(){
        // appel fonction loading
        loading(true);

        //instance d'une base de données
        FirebaseFirestore database = FirebaseFirestore.getInstance();

        //lire la table de users pour lire les informations
        database.collection(Constants.Key_Collection_Users)
                //comparer les informations saisir par l'utilisateur et la base
                .whereEqualTo(Constants.key_Email, binding.inputEmail.getText().toString())
                .whereEqualTo(Constants.key_password , binding.inputPassword.getText().toString())
                .get()
                //pour prendre tte les informations a propos de vous
                .addOnCompleteListener(task ->{
                    if(task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
                        DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                        preferenceManager.putBoolean(Constants.Key_is_Sign_In,true);
                        preferenceManager.putString(Constants.Key_user_id, documentSnapshot.getId());
                        preferenceManager.putString(Constants.Key_name,documentSnapshot.getString(Constants.Key_name));
                        preferenceManager.putString(Constants.key_Email,documentSnapshot.getString(Constants.key_Email));
                        preferenceManager.putString(Constants.key_password,documentSnapshot.getString(Constants.key_password));
                        preferenceManager.putString(Constants.Key_image, documentSnapshot.getString(Constants.Key_image));

                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();


                    }else{
                        // on cas ou l email et le password n'existe pas dans database ou incorrecte
                        loading(false);
                        showToat(" Verifier vos coordonnées sinon cliquer sur Create new account !! ");
                    }
                });
    }


    //à propos le button de sign In
    private void loading(Boolean isLoading){
        if(isLoading){
            binding.buttonSingIn.setVisibility(View.INVISIBLE);
            binding.progressBarIn.setVisibility(View.VISIBLE);
        }else{
            binding.buttonSingIn.setVisibility(View.VISIBLE);
            binding.progressBarIn.setVisibility(View.INVISIBLE);
        }
    }


    private void showToat(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    //fonction pour verifier tte les informations en cas vide
    private  boolean isValidSignInDetails(){
        if(binding.inputEmail.getText().toString().trim().isEmpty()){
            showToat("Tapper votre adresse email");
            return false;
        }else if (binding.inputPassword.getText().toString().trim().isEmpty()){
            showToat("Tapper votre mot de passe ");
            return false;
        }else {
            return true;
        }
    }

}
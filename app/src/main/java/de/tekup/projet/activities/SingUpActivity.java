package de.tekup.projet.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;

import de.tekup.projet.databinding.ActivitySingUpBinding;
import de.tekup.projet.utilities.Constants;
import de.tekup.projet.utilities.PreferenceManager;

public class SingUpActivity extends AppCompatActivity {

    private ActivitySingUpBinding binding;
    private String encodedImage;
    private PreferenceManager preferenceManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding= ActivitySingUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        setListeners();
    }

    private void setListeners(){
        binding.textSignIn.setOnClickListener(v -> onBackPressed());
        binding.buttonSingUp.setOnClickListener(v -> {
            if(IsValidSignUp()){
                signUp();
            }
        });

        binding.LayoutImage.setOnClickListener(v->{
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImage.launch(intent);
        });

    }

    private void shownToast (String message){
        Toast.makeText(getApplicationContext(),message,Toast.LENGTH_LONG).show();
    }

    private void signUp(){
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        HashMap<String, Object> user = new HashMap<>();
        user.put(Constants.Key_name, binding.inputName.getText().toString());
        user.put(Constants.key_Email , binding.inputEmail.getText().toString());
        user.put(Constants.key_password , binding.inputPassword.getText().toString());
        user.put(Constants.Key_image , encodedImage);

        database.collection(Constants.Key_Collection_Users)
                .add(user)
                .addOnSuccessListener(documentReference -> {
                    loading(false);
                    preferenceManager.putBoolean(Constants.Key_is_sign_up,true);
                    preferenceManager.putString(Constants.Key_user_id, documentReference.getId());
                    preferenceManager.putString(Constants.Key_name,binding.inputName.getText().toString());
                    preferenceManager.putString(Constants.key_Email, binding.inputEmail.getText().toString());
                    preferenceManager.putString(Constants.key_password , binding.inputPassword.getText().toString());
                    preferenceManager.putString(Constants.Key_image , encodedImage);

                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);
                })
                .addOnFailureListener(exception ->{
                    loading(false);
                    shownToast(exception.getMessage());
                });


    }

    private String encodedImage(Bitmap image){

        int width = 150;
        int length = image.getHeight() * width / image.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(image , width , length , false);
        ByteArrayOutputStream byteArrayOutputStream= new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50 , byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes,Base64.DEFAULT);
    }


    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(result.getResultCode() == RESULT_OK){
                    if(result.getData()!=null){
                        Uri imageURi = result.getData().getData();
                        try{
                            InputStream inputStream = getContentResolver().openInputStream(imageURi);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            binding.imageProfil.setImageBitmap(bitmap);
                            binding.textAddImage.setVisibility(View.GONE);
                            encodedImage = encodedImage(bitmap);
                             }catch(FileNotFoundException e){
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

    private Boolean IsValidSignUp(){

         if(encodedImage == null){
             shownToast("select un photo de profil");
              return false;
            }
         else if (binding.inputName.getText().toString().trim().isEmpty()){
             shownToast(" tapper votre nom ");
             return false;
         }
         else if(binding.inputEmail.getText().toString().trim().isEmpty()) {
             shownToast(" tapper votre email ");
             return false;
         }
         else if (!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.getText().toString()).matches()){
             shownToast(" tapper email correct ");
             return false;

         }
         else if(binding.inputPassword.getText().toString().trim().isEmpty()){
             shownToast(" tapper votre mot de passe ");
             return false;
         }
         else if (!binding.inputPassword.getText().toString().equals(binding.inputConfirmPassword.getText().toString())){
             shownToast(" tapper la meme mot de passe svp !");
             return  false;
         }
         else{
             return true;
          }}

    private void loading(Boolean isLoading){

        if(isLoading){
            binding.buttonSingUp.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        }else{
            binding.buttonSingUp.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }


}
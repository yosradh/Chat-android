package de.tekup.projet.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import de.tekup.projet.listeners.UserListener;
import de.tekup.projet.Models.User;
import de.tekup.projet.adapters.UsersAdapter;
import de.tekup.projet.databinding.ActivityUsersBinding;
import de.tekup.projet.utilities.Constants;
import de.tekup.projet.utilities.PreferenceManager;

public class usersActivity extends AppCompatActivity implements UserListener {

    private ActivityUsersBinding binding;
    private PreferenceManager preferenceManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUsersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        setListenners();
        getUsers();
    }


    private void showErrorMessage(){
        binding.textErrorMsg.setText(String.format("%s","no user available"));
        binding.textErrorMsg.setVisibility(View.VISIBLE);
    }

    private void setListenners(){
        binding.imageBack.setOnClickListener(v -> onBackPressed());
    }

    //afficher la liste des users dans recycle view depuis firebase
    private void getUsers() {
       loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.Key_Collection_Users)
                .get()
                .addOnCompleteListener(task -> {
                    loading(false);
                    String currentUserId = preferenceManager.getString(Constants.Key_user_id);
                    if(task.isSuccessful() && task.getResult() != null){
                        List<User> users = new ArrayList<>();
                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()){
                            if(currentUserId.equals(queryDocumentSnapshot.getId())){
                                continue;
                            }

                            User user = new User();
                            user.name = queryDocumentSnapshot.getString(Constants.Key_name);
                            user.email = queryDocumentSnapshot.getString(Constants.key_Email);
                            user.image = queryDocumentSnapshot.getString(Constants.Key_image);
                            user.token = queryDocumentSnapshot.getString(Constants.Key_FCM_Token);
                            user.id = queryDocumentSnapshot.getId();
                            users.add(user);
                        }

                        if(users.size() > 0){
                            UsersAdapter usersAdapter = new UsersAdapter(users, this);
                            binding.userRecycleView.setAdapter(usersAdapter);
                            binding.userRecycleView.setVisibility(View.VISIBLE);
                        }else{
                            showErrorMessage();
                        }
                    }else{
                        showErrorMessage();}
                });
    }

    private void loading(Boolean isLoading){
        if (isLoading){
            binding.progressBar.setVisibility(View.VISIBLE);
        }else{
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onUserClicked(User user) {
        Intent intent = new Intent(getApplicationContext(),ChatActivity.class);
        intent.putExtra(Constants.Key_User, user);
        startActivity(intent);
        finish();
    }
}
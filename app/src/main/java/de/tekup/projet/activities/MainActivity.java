package de.tekup.projet.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.*;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import de.tekup.projet.Models.ChatMessage;
import de.tekup.projet.Models.User;
import de.tekup.projet.adapters.RecentConversationsAdapter;
import de.tekup.projet.databinding.ActivityMainBinding;
import de.tekup.projet.listeners.ConversionListener;
import de.tekup.projet.utilities.Constants;
import de.tekup.projet.utilities.PreferenceManager;

public class MainActivity extends AppCompatActivity implements ConversionListener {

    private ActivityMainBinding binding;
    private PreferenceManager preferenceManager;
    private List<ChatMessage> conversations;
    private RecentConversationsAdapter conversationsAdapter;
    private FirebaseFirestore database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        init();
        loadUserDetails();
        getToken();
        setListeners();
        listenerConvertions();

    }

    //initier ttes les outils utilisées(array list, adaptater , recyclerView , base de donnees)
    private void init(){
        conversations = new ArrayList<>();
        conversationsAdapter = new RecentConversationsAdapter(conversations, this);
        binding.ConversationRecyclerView.setAdapter(conversationsAdapter);
        database = FirebaseFirestore.getInstance();
    }


    private void setListeners(){
        //pour deconnecter
        binding.imageSignOut.setOnClickListener(v -> signOut());
        //pour ajouter une nouvelle discussion
        binding.newChat.setOnClickListener(v ->
                startActivity(new Intent (getApplicationContext(), usersActivity.class)));
    }

    //pour charger les informations user :nom et photo de profil
    private void loadUserDetails(){
        binding.textName.setText(preferenceManager.getString(Constants.Key_name));
        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.Key_image), Base64.DEFAULT);
        Bitmap image = BitmapFactory.decodeByteArray(bytes,0 ,bytes.length);
        binding.imageProfile.setImageBitmap(image);
    }

    //pour afficer les messages d'erreur et de succees
    private void showToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        }

     private void listenerConvertions(){
        database.collection(Constants.Key_Collection_Conversations)
                .whereEqualTo(Constants.Key_sender_id, preferenceManager.getString(Constants.Key_user_id))
                .addSnapshotListener(eventListener);

        database.collection(Constants.Key_Collection_Conversations)
                .whereEqualTo(Constants.Key_receive_id, preferenceManager.getString(Constants.Key_user_id))
                .addSnapshotListener(eventListener);
     }



    private final EventListener<QuerySnapshot> eventListener = (value , error) ->{
        if (error != null){
            return;
    }
         if(value != null){
            for (DocumentChange documentChange : value.getDocumentChanges()){

                if(documentChange.getType() == DocumentChange.Type.ADDED){
                    String senderId = documentChange.getDocument().getString(Constants.Key_sender_id);
                    String receiverId = documentChange.getDocument().getString(Constants.Key_receive_id);
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.sendId = senderId;
                     chatMessage.receiveId = receiverId;

                        if(preferenceManager.getString(Constants.Key_user_id).equals(senderId)){
                            chatMessage.conversionImage = documentChange.getDocument().getString(Constants.Key_Receiver_Image);
                            chatMessage.conversionName = documentChange.getDocument().getString(Constants.Key_Receiver_Name);
                            chatMessage.conversionId = documentChange.getDocument().getString(Constants.Key_receive_id);
                        }
                        else{
                            chatMessage.conversionImage = documentChange.getDocument().getString(Constants.Key_Sender_Image);
                            chatMessage.conversionName = documentChange.getDocument().getString(Constants.Key_Sender_Name);
                            chatMessage.conversionId = documentChange.getDocument().getString(Constants.Key_sender_id);
                        }

                        chatMessage.message = documentChange.getDocument().getString(Constants.Key_Last_Message);
                        chatMessage.dateMessage =documentChange.getDocument().getDate(Constants.Key_DateEnvoi);
                        conversations.add(chatMessage);
                    }
                    else if (documentChange.getType() == DocumentChange.Type.MODIFIED){
                        for(int i = 0; i< conversations.size(); i++){
                            String senderId = documentChange.getDocument().getString(Constants.Key_sender_id);
                            String receiverId = documentChange.getDocument().getString(Constants.Key_receive_id);

                            if(conversations.get(i).sendId.equals(senderId) && conversations.get(i).receiveId.equals(receiverId)){
                                conversations.get(i).message = documentChange.getDocument().getString(Constants.Key_Last_Message);
                                conversations.get(i).dateMessage = documentChange.getDocument().getDate(Constants.Key_DateEnvoi);
                                break;
                            }
                        }
                    }
                }

                Collections.sort(conversations , (obj1, obj2)-> obj2.dateMessage.compareTo(obj1.dateMessage));
                conversationsAdapter.notifyDataSetChanged();
                binding.ConversationRecyclerView.smoothScrollToPosition(0);
                binding.ConversationRecyclerView.setVisibility(View.VISIBLE);
                binding.progressBar.setVisibility(View.GONE);
            }
        };

    //c'est comme prendre le parole
    private void getToken(){
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken);
    }


    private void updateToken(String token){
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                database.collection(Constants.Key_Collection_Users).document(
                        preferenceManager.getString(Constants.Key_user_id)
                );

        documentReference.update(Constants.Key_FCM_Token,token)

                .addOnFailureListener(e -> showToast(" oops il ya un erreur "));
    }


    //pour deconnecter
    private void signOut(){
        showToast(" Vous etes déconnecté ");
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                database.collection(Constants.Key_Collection_Users).document(
                        preferenceManager.getString(Constants.Key_user_id)
                );

        HashMap<String , Object> updates = new HashMap<>();
        //pour supprimer le token lorsque il est déconnect
        updates.put(Constants.Key_FCM_Token, FieldValue.delete());
        documentReference.update(updates)
                .addOnSuccessListener(unused -> {
                    startActivity(new Intent(getApplicationContext(),SignInActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> showToast(" il ya un erreur "));
    }


    @Override
    public void onConversionClicked(User user) {
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constants.Key_User, user);
        startActivity(intent);
    }}
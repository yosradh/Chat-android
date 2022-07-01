package de.tekup.projet.activities;

import static de.tekup.projet.utilities.Constants.Key_Collection_Chat;
import static de.tekup.projet.utilities.Constants.Key_user_id;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import de.tekup.projet.Models.ChatMessage;
import de.tekup.projet.Models.User;
import de.tekup.projet.utilities.Constants;
import de.tekup.projet.adapters.ChatAdapter;
import de.tekup.projet.databinding.ActivityChatBinding;
import de.tekup.projet.utilities.PreferenceManager;

public class ChatActivity extends AppCompatActivity {

    private PreferenceManager preferenceManager;
    private ActivityChatBinding binding;
    private User receiverUser;
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private FirebaseFirestore database;
    private String conversationId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        SetListeners();
        loadReceiverDetails();
        init();
        listesMessages();
    }

    private void init(){
        preferenceManager = new PreferenceManager(getApplicationContext());
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(
                chatMessages,
                getBitmapFormEncodedString(receiverUser.image),
                preferenceManager.getString(Constants.Key_user_id)
        );
        binding.chatRecycleView.setAdapter(chatAdapter);
        database = FirebaseFirestore.getInstance();
    }

    //sauvgarder les messages envoyer dans la base
    private void SendMessage(){

        //envoyer les info d message vers le base
        HashMap<String, Object> message = new HashMap<>();
        message.put(Constants.Key_sender_id, preferenceManager.getString(Key_user_id));
        message.put(Constants.Key_receive_id, receiverUser.id);
        message.put(Constants.Key_Message, binding.inputMessage.getText().toString());
        message.put(Constants.Key_DateEnvoi,new Date());

        database.collection(Constants.Key_Collection_Chat).add(message);

        if(conversationId != null){
            //appel d'une methode
            updateConversation(binding.inputMessage.getText().toString());
        }else{
            //envoyer les info d message vers le base
            HashMap<String, Object> conversations = new HashMap<>();
            conversations.put(Constants.Key_sender_id , preferenceManager.getString(Constants.Key_user_id));
            conversations.put(Constants.Key_Sender_Name , preferenceManager.getString(Constants.Key_name));
            conversations.put(Constants.Key_Sender_Image , preferenceManager.getString(Constants.Key_image));
            conversations.put(Constants.Key_receive_id , receiverUser.id);
            conversations.put(Constants.Key_Receiver_Name, receiverUser.name);
            conversations.put(Constants.Key_Receiver_Image, receiverUser.image);
            conversations.put(Constants.Key_Last_Message, binding.inputMessage.getText().toString());
            conversations.put(Constants.Key_DateEnvoi, new Date());
            addConversion(conversations);
        }
        binding.inputMessage.setText(null);

    }


    //extraire les messages envoyer et recu du base
    public void listesMessages(){
        database.collection(Constants.Key_Collection_Chat)
                .whereEqualTo(Constants.Key_sender_id, preferenceManager.getString(Constants.Key_user_id))
                .whereEqualTo(Constants.Key_receive_id, receiverUser.id)
                .addSnapshotListener(eventListener);

        database.collection(Key_Collection_Chat)
                .whereEqualTo(Constants.Key_sender_id , receiverUser.id)
                .whereEqualTo(Constants.Key_receive_id, preferenceManager.getString(Constants.Key_user_id))
                .addSnapshotListener(eventListener);
    }


    //decoder l image
    private Bitmap getBitmapFormEncodedString(String  encodedImage){
        byte[] bytes = Base64.decode(encodedImage,Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes,0,bytes.length);
    }


    //prendre les informations qui envoyer par l activité précedent
    private void loadReceiverDetails() {
        receiverUser = (User) getIntent().getSerializableExtra(Constants.Key_User);
        binding.textName.setText(receiverUser.name);
    }



    //pour le bouton retour
    private void SetListeners(){
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        binding.layoutSend.setOnClickListener(v -> SendMessage());
    }

    //ajouter une converssation
    private void addConversion(HashMap<String, Object> conversion){
        database.collection(Constants.Key_Collection_Conversations)
                .add(conversion)
                .addOnSuccessListener(documentReference -> conversationId = documentReference.getId());
    }

    //modifier une conversation
    private void updateConversation(String message){
        DocumentReference documentReference =
                database.collection(Constants.Key_Collection_Conversations).document(conversationId);
        documentReference.update(
                Constants.Key_Last_Message , message,
                Constants.Key_DateEnvoi ,new Date()
        );
    }

    private final EventListener<QuerySnapshot> eventListener = (value , error) ->{
        if (error != null){
            return ;
        }
        if (value != null){
            int count = chatMessages.size();
            for (DocumentChange documentChange : value.getDocumentChanges()){

                if(documentChange.getType() == DocumentChange.Type.ADDED){
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.sendId = documentChange.getDocument().getString(Constants.Key_sender_id);
                    chatMessage.receiveId = documentChange.getDocument().getString(Constants.Key_receive_id);
                    chatMessage.message = documentChange.getDocument().getString(Constants.Key_Message);
                    chatMessage.dateTime = getFormatDate(documentChange.getDocument().getDate(Constants.Key_DateEnvoi));
                    chatMessage.dateMessage = documentChange.getDocument().getDate(Constants.Key_DateEnvoi);
                    chatMessages.add(chatMessage);
                }
            }
            Collections.sort(chatMessages , (obj1, obj2)-> obj1.dateMessage.compareTo(obj2.dateMessage));
            if (count == 0) {
                chatAdapter.notifyDataSetChanged();

            } else {
                chatAdapter.notifyItemRangeInserted(chatMessages.size() , chatMessages.size());
                binding.chatRecycleView.smoothScrollToPosition(chatMessages.size() - 1);
                 }

            binding.chatRecycleView.setVisibility(View.VISIBLE);
        }
        binding.progressBar.setVisibility(View.GONE);
        if( conversationId == null){
            //fonction appeler
            checkForConversions();
        }
    };
    

    // metter le format du date
    private String getFormatDate(Date date){
        return new SimpleDateFormat("dd-MM-yyyy , hh:mm a", Locale.getDefault()).format(date);
    }

    //verification pour la conversation s il est vide ou nn
    private void checkForConversions(){
        if(chatMessages.size() != 0){

            //fonction appeler
            checkForConversionRemotly(
                    preferenceManager.getString(Constants.Key_user_id),
                    receiverUser.id
            );
            //fonction appeler
            checkForConversionRemotly(
                    receiverUser.id,
                    preferenceManager.getString(Constants.Key_user_id)
            );
        }
    }


    //pour extraire les messages qui a un id données
    private void checkForConversionRemotly(String senderId, String receiverId){
        database.collection(Constants.Key_Collection_Conversations)
                .whereEqualTo(Constants.Key_sender_id , senderId)
                .whereEqualTo(Constants.Key_receive_id, receiverId)
                .get()
                .addOnCompleteListener(conversionsOnCompleteListener);
    }


    private final OnCompleteListener<QuerySnapshot> conversionsOnCompleteListener = task -> {
        if(task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0){
            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
            conversationId = documentSnapshot.getId();
        }
    };

}
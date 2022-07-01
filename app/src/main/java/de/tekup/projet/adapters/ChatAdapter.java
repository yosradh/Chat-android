package de.tekup.projet.adapters;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import de.tekup.projet.Models.ChatMessage;
import de.tekup.projet.databinding.ItemContainerReceivedMessageBinding;
import de.tekup.projet.databinding.ItemContainerSentMessageBinding;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<ChatMessage> chatMessages;
    private final Bitmap receiverProfilPhoto;
    private final String senderId;
    public static final int View_Type_Send = 1;
    public static final int View_Type_Recu = 2 ;

    //Constructeur
    public ChatAdapter(List<ChatMessage> chatMessages, Bitmap receiverProfilPhoto, String senderId) {
        this.chatMessages = chatMessages;
        this.receiverProfilPhoto = receiverProfilPhoto;
        this.senderId = senderId;
    }

    //les methodes override
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        if(viewType == View_Type_Send){
            return new SentMessageViewHolder(
                    ItemContainerSentMessageBinding.inflate(
                            LayoutInflater.from(parent.getContext()),
                            parent,
                            false
                    )
            );
        }else{
            return new ReceiveMessageViewHolder(
                    ItemContainerReceivedMessageBinding.inflate(
                            LayoutInflater.from(parent.getContext()),
                            parent,
                            false)
            );
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        if(getItemViewType(position) == View_Type_Send){
            ((SentMessageViewHolder) holder).setData(chatMessages.get(position));
        }else{
            ((ReceiveMessageViewHolder) holder).setData(chatMessages.get(position),receiverProfilPhoto);
        }
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    @Override
    public int getItemViewType(int position) {
        if(chatMessages.get(position).sendId.equals(senderId)){
            return View_Type_Send;
        }else {
            return View_Type_Recu;
        } }

    //class envoyer des messages
    static class SentMessageViewHolder extends RecyclerView.ViewHolder{
        private final ItemContainerSentMessageBinding binding;

        SentMessageViewHolder(ItemContainerSentMessageBinding itemContainerSentMessageBinding){
            super(itemContainerSentMessageBinding.getRoot());
            binding = itemContainerSentMessageBinding;
        }

        void setData(ChatMessage chatMessage){
            binding.textMessage.setText(chatMessage.message);
            binding.textDateTime.setText(chatMessage.dateTime);
        }
    }



    //class recevoir les messages
    static class ReceiveMessageViewHolder extends RecyclerView.ViewHolder{

        private final ItemContainerReceivedMessageBinding binding;

        ReceiveMessageViewHolder (ItemContainerReceivedMessageBinding itemContainerReceivedMessageBinding){
            super(itemContainerReceivedMessageBinding.getRoot());
            binding = itemContainerReceivedMessageBinding;
        }

        void setData(ChatMessage chatMessage,Bitmap imageProfilRecu){
            binding.textMessage.setText(chatMessage.message);
            binding.textDateTime.setText(chatMessage.dateTime);
            binding.imageProfile.setImageBitmap(imageProfilRecu);
        }
    }
}

package com.yuyang.autoscrambleredpacket;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by yuyang on 2018/1/4.
 */

public class KeyWordAdapter extends RecyclerView.Adapter<KeyWordAdapter.KeyWordHolder> {
    private List<String> keyWords;
    private DeleteListener deleteListener;

    public KeyWordAdapter(List<String> keyWords, DeleteListener listener){
        this.keyWords = keyWords;
        this.deleteListener = listener;
    }

    @Override
    public KeyWordHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new KeyWordHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_key_word, parent, false));
    }

    @Override
    public void onBindViewHolder(KeyWordHolder holder, final int position) {
        holder.mKeyWord.setText(keyWords.get(position));
        holder.mDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (deleteListener != null){
                    deleteListener.delete(keyWords.get(position));
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return keyWords.size();
    }

    class KeyWordHolder extends RecyclerView.ViewHolder{
        private TextView mKeyWord;
        private ImageView mDelete;
        public KeyWordHolder(View itemView) {
            super(itemView);
            mKeyWord = (TextView) itemView.findViewById(R.id.key_word);
            mDelete = (ImageView) itemView.findViewById(R.id.delete);
        }
    }

    public interface DeleteListener{
        void delete(String keyWord);
    }
}

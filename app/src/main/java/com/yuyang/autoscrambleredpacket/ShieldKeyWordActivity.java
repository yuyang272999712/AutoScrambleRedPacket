package com.yuyang.autoscrambleredpacket;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 屏蔽关键字(比如：**专属红包，其他人勿抢)
 */

public class ShieldKeyWordActivity extends AppCompatActivity implements KeyWordAdapter.DeleteListener {
    public static final String KEY_WORDS = "key_words";
    private RecyclerView mRecyclerView;
    private EditText mEditText;
    private Button mButton;

    private SharedPreferences.Editor editor;
    private Gson gson = new Gson();
    private List<String> keyWords = new ArrayList<>();
    private KeyWordAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_key_word);
        initView();
        SharedPreferences sharedPreferences = getSharedPreferences(KEY_WORDS, MODE_PRIVATE);
        editor = sharedPreferences.edit();
        String keyWordsJson = sharedPreferences.getString(KEY_WORDS, null);
        if (keyWordsJson != null) {
            Type type = new TypeToken<List<String>>() {}.getType();
            keyWords.addAll((Collection<? extends String>) gson.fromJson(keyWordsJson, type));
        }

        mAdapter = new KeyWordAdapter(keyWords, this);
        mRecyclerView.setAdapter(mAdapter);

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String keyWord = mEditText.getText().toString();
                if (keyWord != null && keyWord.trim().length() > 0){
                    if (keyWords.contains(keyWord.trim())){
                        Toast.makeText(ShieldKeyWordActivity.this, "已包含此关键字", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    keyWords.add(keyWord.trim());
                    mAdapter.notifyDataSetChanged();
                    editor.putString(KEY_WORDS, gson.toJson(keyWords));
                    editor.commit();
                    Toast.makeText(ShieldKeyWordActivity.this, "添加成功", Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(ShieldKeyWordActivity.this, "关键字不能为空", Toast.LENGTH_SHORT).show();
                }
                mEditText.setText("");
            }
        });
    }

    private void initView() {
        getSupportActionBar().setTitle("屏蔽关键字");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mRecyclerView = (RecyclerView) findViewById(R.id.key_words);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mEditText = (EditText) findViewById(R.id.key_word);
        mButton = (Button) findViewById(R.id.add);
    }

    @Override
    public void delete(String keyWord) {
        keyWords.remove(keyWord);
        mAdapter.notifyDataSetChanged();
        editor.putString(KEY_WORDS, gson.toJson(keyWords));
        editor.commit();
        Toast.makeText(ShieldKeyWordActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
    }
}

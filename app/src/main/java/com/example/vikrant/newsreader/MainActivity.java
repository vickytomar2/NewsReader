package com.example.vikrant.newsreader;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    ArrayList<String> headlines;
    ArrayList<String> articleC = new ArrayList<>();
    ArrayAdapter arrayAdapter;
    SQLiteDatabase articleDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listView = findViewById(R.id.listView);

        headlines = new ArrayList<>();
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, headlines);
        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(getApplicationContext(),ArticleActivity.class);
                intent.putExtra("content", articleC.get(i));
                startActivity(intent);
            }
        });

        articleDB = this.openOrCreateDatabase("Articles", MODE_PRIVATE, null);
        articleDB.execSQL("Create Table if not exists articles(id integer primary key, articleId integer,title varchar, content varchar)");

        updateList();

        DownloadTask task = new DownloadTask();
        try {
            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void updateList() {
        Cursor c = articleDB.rawQuery("Select * from articles", null);

        int contentIndex = c.getColumnIndex("content");
        int titleIndex = c.getColumnIndex("title");

        if (c.moveToFirst()) {
            headlines.clear();
            articleC.clear();

            do {
                headlines.add(c.getString(titleIndex));
                articleC.add(c.getString(contentIndex));

            } while (c.moveToNext());
        }

        arrayAdapter.notifyDataSetChanged();

    }

    public class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            String result = "";
            URL url;
            HttpURLConnection urlConnection = null;

            try {
                url = new URL(strings[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream is = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(is);
                int data = reader.read();
                while (data != -1) {
                    char ch = (char) data;
                    result += ch;
                    data = reader.read();
                }

                JSONArray jsonArray = new JSONArray(result);
                int n = 20;
                if (jsonArray.length() < 20) {
                    n = jsonArray.length();
                }

                articleDB.execSQL("delete from articles");
                for (int i = 0; i < n; i++) {
                    String id = jsonArray.getString(i);
                    url = new URL("https://hacker-news.firebaseio.com/v0/item/" + id + ".json?print=pretty");
                    urlConnection = (HttpURLConnection) url.openConnection();

                    is = urlConnection.getInputStream();
                    reader = new InputStreamReader(is);

                    data = reader.read();
                    String articleInfo = "";

                    while (data != -1) {
                        char ch = (char) data;
                        articleInfo += ch;
                        data = reader.read();
                    }

                    JSONObject jsonObject = new JSONObject(articleInfo);

                    if (!jsonObject.isNull("title") && !jsonObject.isNull("url")) {
                        String title = jsonObject.getString("title");
                        String articleUrl = jsonObject.getString("url");
                        url = new URL(articleUrl);
//                        urlConnection = (HttpURLConnection) url.openConnection();
//
//                        is = urlConnection.getInputStream();
//                        reader = new InputStreamReader(is);
//
//                        data = reader.read();
//                        String articleContent = "";
//
//                        while (data != -1) {
//                            char ch = (char) data;
//                            articleContent += ch;
//                            data = reader.read();
//                        }
//                        Log.i("c", "doing artv=cle" + id);

                        String sql = "Insert into articles(articleId, title, content) values (?,?,?)";

                        SQLiteStatement statement = articleDB.compileStatement(sql);
                        statement.bindString(1, id);
                        statement.bindString(2, title);
                        statement.bindString(3, url.toString());

                        statement.execute();
                    }
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            updateList();
        }
    }


}

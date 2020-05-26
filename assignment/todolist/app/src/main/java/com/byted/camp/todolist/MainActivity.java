package com.byted.camp.todolist;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.byted.camp.todolist.beans.Note;
import com.byted.camp.todolist.beans.State;
import com.byted.camp.todolist.db.TodoContract;
import com.byted.camp.todolist.db.TodoDbHelper;
import com.byted.camp.todolist.operation.activity.DatabaseActivity;
import com.byted.camp.todolist.operation.activity.DebugActivity;
import com.byted.camp.todolist.operation.activity.SettingActivity;
import com.byted.camp.todolist.ui.NoteListAdapter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private static final int REQUEST_CODE_ADD = 1002;

    private RecyclerView recyclerView;
    private NoteListAdapter notesAdapter;

    private TodoDbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(
                        new Intent(MainActivity.this, NoteActivity.class),
                        REQUEST_CODE_ADD);
            }
        });

        recyclerView = findViewById(R.id.list_todo);
        recyclerView.setLayoutManager(new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false));
        recyclerView.addItemDecoration(
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        notesAdapter = new NoteListAdapter(new NoteOperator() {
            @Override
            public void deleteNote(Note note) {
                MainActivity.this.deleteNote(note);
            }

            @Override
            public void updateNote(Note note) {
                MainActivity.this.updateNode(note);
            }
        });
        recyclerView.setAdapter(notesAdapter);

        notesAdapter.refresh(loadNotesFromDatabase());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingActivity.class));
                return true;
            case R.id.action_debug:
                startActivity(new Intent(this, DebugActivity.class));
                return true;
            case R.id.action_database:
                startActivity(new Intent(this, DatabaseActivity.class));
                return true;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ADD
                && resultCode == Activity.RESULT_OK) {
            notesAdapter.refresh(loadNotesFromDatabase());
        }
    }

    private List<Note> loadNotesFromDatabase() {
        // TODO 从数据库中查询数据，并转换成 JavaBeans
        List<Note> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] projection = {
                BaseColumns._ID,
                TodoContract.TodoEntry.COLUMN_DATE,
                TodoContract.TodoEntry.COLUMN_STATE,
                TodoContract.TodoEntry.COLUMN_PRIORITY,
                TodoContract.TodoEntry.COLUMN_CONTENT
        };

        String sortOrder = TodoContract.TodoEntry.COLUMN_PRIORITY + " ASC";
        Cursor cursor = db.query(
                TodoContract.TodoEntry.TABLE_NAME,  //表名
                projection,                         //返回元组
                null,                     //where语句列
                null,                 //where语句值
                null,                     //groupby语句
                null,                       //having语句
                sortOrder                           //排序
        );
        Log.i(TAG,"execute sql");

        while(cursor.moveToNext()){
            long itemId = cursor.getLong(cursor.getColumnIndexOrThrow(TodoContract.TodoEntry._ID));
            String content = cursor.getString(cursor.getColumnIndex(TodoContract.TodoEntry.COLUMN_DATE));
            String date = cursor.getString(cursor.getColumnIndex(TodoContract.TodoEntry.COLUMN_DATE));
            int  state = cursor.getInt(cursor.getColumnIndex(TodoContract.TodoEntry.COLUMN_STATE));
            int priority = cursor.getInt(cursor.getColumnIndex(TodoContract.TodoEntry.COLUMN_PRIORITY));
            Note note = new Note(itemId);
            note.setContent(content);
            try{
                String format = "E, dd MMM yyyy HH:mm:ss";
                SimpleDateFormat dateFormat = new SimpleDateFormat(format, Locale.ENGLISH);
                Date date2 = dateFormat.parse(date);
                note.setDate(date2);
            }catch(ParseException e){
                e.printStackTrace();
            }
            note.setState(State.from(state));
            note.setPriority(priority);
            list.add(note);
        }

        return null;
    }

    private void deleteNote(Note note) {
        // TODO 删除数据
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String selection = TodoContract.TodoEntry._ID + " = ?";
        String[] selectionArgs = {String.valueOf(note.id)};
        int deletedRows = db.delete(TodoContract.TodoEntry.TABLE_NAME,selection,selectionArgs);
        Log.i(TAG,"delete: "+ deletedRows);
    }

    private void updateNode(Note note) {
        // 更新数据
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String selection = TodoContract.TodoEntry._ID + " = ?";
        String[] selectionArgs = {String.valueOf(note.id)};

        State state = note.getState();
        ContentValues values = new ContentValues();

        if (state == State.TODO) {
            values.put(TodoContract.TodoEntry.COLUMN_STATE, 0);
            Log.i(TAG, "state: 0");
        } else {
            values.put(TodoContract.TodoEntry.COLUMN_STATE, 1);
            Log.i(TAG, "state: 1");
        }
        int count = db.update(
                TodoContract.TodoEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs);
        Log.i(TAG, "execute sql. result:" + count);
    }

}

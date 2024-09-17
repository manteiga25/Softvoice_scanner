package com.example.softvoicescanner;

import static java.lang.Long.parseLong;

import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowInsetsCompat;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.Toast;

import com.example.softvoicescanner.databinding.ActivityDatabaseWindowBinding;
import com.example.softvoicescanner.Database;

import java.util.ArrayList;

public class Database_window extends AppCompatActivity {

    private ActivityDatabaseWindowBinding binding;
    private Database DB;
    private ArrayList<String> data;  // Supõe-se que data já foi inicializado com dados
    private TableLayout tabela;
    private TextView[] productId, quantity;
   // private TableRow[] newRow;

    protected ArrayList<String> fetch_data() {
        Cursor cursor = DB.fetchAllDataCursor();
        ArrayList<String> list = new ArrayList<String>();
        if (cursor == null) {
            System.out.println("cursor is null");
            return list;
        }
        if (cursor.moveToFirst()) {
            int idx = 0;
            do { list.add(cursor.getString(0));
                list.add(cursor.getString(1));
                System.out.println("prod " + list.get(idx) + "\n" + "quant " + list.get(idx+1) + "\n");
                idx += 2; }
            while (cursor.moveToNext());
             /*   list.add(cursor.getString(0));
                list.add(cursor.getString(1));
                System.out.println("prod " + list.get(idx) + "\n" + "quant " + list.get(idx+1) + "\n");
                idx += 2;
            } */
        }
        cursor.close();
        return list;
    }

    private void showEditDialog(final int widget, final int index) {
        // Infla o layout do dialog
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.edit_item, null);

        final EditText editProductId = dialogView.findViewById(R.id.edit_product_id);
        final EditText editQuantity = dialogView.findViewById(R.id.edit_quantity);

        final String tmp_product_id = data.get(index);
        final String tmp_quantity = data.get(index+1);

        // Preenche os campos com dados existentes
        //   if (index < data.size()) {
        editProductId.setText(tmp_product_id);
        //     if (index + 1 < data.size()) {
        editQuantity.setText(tmp_quantity);
        //   }
        // }
        new AlertDialog.Builder(this)
                .setTitle("Edit Item")
                .setView(dialogView)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final String new_tmp_product_id = editProductId.getText().toString();
                        final long new_tmp_quantity = new Long(editQuantity.getText().toString());
                        if (new_tmp_product_id.isEmpty() || new_tmp_quantity == 0) {
                            androidx.appcompat.app.AlertDialog.Builder error_win = new androidx.appcompat.app.AlertDialog.Builder(Database_window.this);
                            error_win.setTitle("Invalid value");
                            error_win.setMessage("You need to set a value.");
                            error_win.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Apenas fecha o diálogo de erro
                                    dialog.dismiss();
                                    showEditDialog(widget, index);
                                }
                            }).show();
                        }
                            else if (tmp_product_id.equals(new_tmp_product_id) && new Long(tmp_quantity) == new_tmp_quantity) {
                                Toast.makeText(Database_window.this, "Without changes", Toast.LENGTH_SHORT).show();
                            } else {
                            // Atualiza os dados com os novos valores
                            //   data.set(index, editProductId.getText().toString());
                            //      if (index + 1 < data.size()) {
                            //   data.set(index + 1, editQuantity.getText().toString());
                            //    }

                            new AsyncTask<Void, Void, Boolean>() {
                                @Override
                                protected Boolean doInBackground(Void... voids) {
                                    // Atualizar o banco de dados em segundo plano
                                    return DB.updateData(tmp_product_id, editProductId.getText().toString(), new Long(editQuantity.getText().toString())) != -1;
                                }

                  /*      if (DB.updateData(data.get(index), editProductId.getText().toString(), new Long(editQuantity.getText().toString())) == -1) {
                            Toast.makeText(Database_window.this, "Error to update database", Toast.LENGTH_LONG).show();
                        }
                        else {
                            Toast.makeText(Database_window.this, "Sucess to update database", Toast.LENGTH_LONG).show();
                        } */

                                @Override
                                protected void onPostExecute(Boolean success) {
                                    // Executa na UI thread após a operação de background
                                    if (success) {
                                        data.set(index, editProductId.getText().toString());
                                        //      if (index + 1 < data.size()) {
                                        data.set(index + 1, editQuantity.getText().toString());
                                        Toast.makeText(Database_window.this, "Success to update database", Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(Database_window.this, "Error to update database", Toast.LENGTH_LONG).show();
                                        return;
                                    }

                                    // Atualiza a visualização, se necessário
                                    updateTable(widget, editProductId.getText().toString(), new Long(editQuantity.getText().toString()));
                                }
                            }.execute();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateTable(int index, String newId, long nreQuantity) {
        productId[index].setText(newId);
        quantity[index].setText(String.valueOf(nreQuantity));

        }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_database_window);

        Intent intent = getIntent();

        // Obtém os dados extras do Intent
       // data = intent.getStringArrayListExtra("extra_data_key");

      /*  Button btnDeleteDatabase = findViewById(R.id.btn_delete_database);
        btnDeleteDatabase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DB.deleteAllData();
                data.clear();
                tabela.removeAllViews();
                Toast.makeText(Database_window.this, "Database deletada com sucesso!", Toast.LENGTH_SHORT).show();
            }
        } */

        try {
            DB = new Database(Database_window.this);
            DB.defaultDB = intent.getStringExtra("databaseName");
            DB.open();
        }
        catch (Exception e) {
            System.out.println("null database");
            Toast.makeText(Database_window.this, "Error to open database", Toast.LENGTH_LONG).show();
            System.out.println("error to open database");
            return;
        }

        TextView textBar = findViewById(R.id.DatabaseLabel);
        textBar.setText("Database " + DB.defaultDB);

        System.out.println("created");

        data = fetch_data();

//        binding = ActivityDatabaseWindowBinding.inflate(getLayoutInflater());
  //      setContentView(binding.getRoot());

    //    Toolbar toolbar = binding.toolbar;
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Softvoice scanner");
   //     CollapsingToolbarLayout toolBarLayout = binding.toolbarLayout;
     //   toolBarLayout.setTitle(getTitle());

 /*       ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        }); */

        tabela = findViewById(R.id.data_grid);

       // TableRow newRow = new TableRow(this);

        if (data == null) {
            try {
                System.out.println("error " + data);
            }
            catch (Exception e) {
                System.out.println("Fatal error");
            }
            return;
        }

        int data_size = data.size() / 2;

        productId = new TextView[data_size];
        quantity = new TextView[data_size];

        for (int index = 0, item = 0; item < data_size; index += 2, item += 1) {
            System.out.println(index);


            TableRow newRow = new TableRow(this);

          //  TextView productId = new TextView(this);
            productId[item] = new TextView(this);
            productId[item].setText(data.get(index));
            productId[item].setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 4f));
            productId[item].setGravity(Gravity.CENTER_HORIZONTAL);
            productId[item].setPadding(10, 10, 10, 10);
            productId[item].setTextColor(getResources().getColor(android.R.color.white));
            productId[item].setTextSize(14);

          //  TextView quantity = new TextView(this);
            quantity[item] = new TextView(this);
            quantity[item].setText(data.get(index+1));
            quantity[item].setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 4f));
            quantity[item].setGravity(Gravity.CENTER_HORIZONTAL);
            quantity[item].setPadding(10, 10, 10, 10);
            quantity[item].setTextColor(getResources().getColor(android.R.color.white));
            quantity[item].setTextSize(14);

            newRow.addView(productId[item]);
            newRow.addView(quantity[item]);

            final int indexWidget = item;
            final int constantIndex = index;

            newRow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showEditDialog(indexWidget, constantIndex);
                    System.out.println("true");
                }
            });



            View separator = new View(this);
            separator.setLayoutParams(new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
            separator.setBackgroundColor(getResources().getColor(android.R.color.white));

            tabela.addView(newRow);
            tabela.addView(separator);
        }


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();  // Volta para a atividade anterior
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
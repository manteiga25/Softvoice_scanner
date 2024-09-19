package com.example.softvoicescanner;

import static com.google.zxing.integration.android.IntentIntegrator.REQUEST_CODE;
import static java.lang.Long.parseLong;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowInsetsCompat;

import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
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
import android.net.Uri;

import android.Manifest;

import com.example.softvoicescanner.databinding.ActivityDatabaseWindowBinding;
import com.example.softvoicescanner.Database;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.softvoicescanner.ExcelDB;

public class Database_window extends AppCompatActivity {

    private ActivityDatabaseWindowBinding binding;
    private Database DB;
    private ArrayList<String> data;  // Supõe-se que data já foi inicializado com dados
    private TableLayout tabela;
    private TextView[] productId, quantity;
    private ExecutorService executorService;
    private Handler mainHandler;
   // private TableRow[] newRow;

    protected ArrayList<String> fetch_data() {
        Cursor cursor = DB.fetchAllDataCursor();
        ArrayList<String> list = new ArrayList<String>();
        if (cursor == null) {
            System.out.println("cursor is null");
            return list;
        }
        if (cursor.moveToFirst()) {
            do { list.add(cursor.getString(0));
                list.add(cursor.getString(1));
            }
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

        private androidx.appcompat.app.AlertDialog init_win() {
            androidx.appcompat.app.AlertDialog.Builder wait_win = new androidx.appcompat.app.AlertDialog.Builder(Database_window.this);
            wait_win.setTitle("Connecting to database");
            wait_win.setMessage("Fetching data.");
           // wait_win.setCancelable(false);
          //  wait_win.show();

            View customLayout = LayoutInflater.from(this).inflate(R.layout.progresslayout, null);
            wait_win.setView(customLayout);

// Criar e mostrar o AlertDialog
            androidx.appcompat.app.AlertDialog progressDialog = wait_win.create();
            progressDialog.show();
            return progressDialog;
        }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_database_window);

        Intent intent = getIntent();

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

        String[] d1 = new String[1000];
        long[] d2 = new long[1000];


        for (long i = 0, quantity = 0; i < 1000; i++, quantity += 10) {
            d1[(int) i] = Long.valueOf(i).toString();
            d2[(int) i] = quantity;
        }
        DB.insertAllData(d1, d2);

        executorService = Executors.newSingleThreadExecutor();

        // Inicializa o handler para a thread principal
        mainHandler = new Handler(Looper.getMainLooper());

        TextView textBar = findViewById(R.id.DatabaseLabel);
        textBar.setText("Database " + DB.defaultDB);

        System.out.println("created");
        System.out.println(DB.defaultDB);

        startFetchingData();

     /*   for (long i = 0; i < 1000; i++) {
            DB.addData(Long.valueOf(i).toString(), i);
        }*/

    }

    private void startFetchingData() {
        // Mostra a janela de carregamento antes de iniciar a busca
        androidx.appcompat.app.AlertDialog loadingDialog = init_win();

        // Executa a busca no banco de dados em uma thread separada
        executorService.execute(() -> {
            // Realize a busca no banco de dados
            data = fetch_data();

            // Retorna para a thread principal para atualizar a UI
            mainHandler.post(() -> {
                // Atualiza a tabela com os dados buscados
                create_table(loadingDialog);

                // Fecha a janela de carregamento
                if (loadingDialog != null && loadingDialog.isShowing()) {
                    System.out.println("yupi");
                }
            });
        });
    }

      /*  wait_win.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Apenas fecha o diálogo de erro
                dialog.dismiss();
                showEditDialog(widget, index);
            }
        }).show();
    } */





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

    void create_table(androidx.appcompat.app.AlertDialog loadingDialog) {

       // data = fetch_data();

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
        List<TableRow> selectedRows = new ArrayList<>();
        final boolean[] isSelectionModeActive = {false};
        final Button buttonExlude = findViewById(R.id.delete_button);

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

        System.out.println(data_size);

        productId = new TextView[data_size];
        quantity = new TextView[data_size];

        for (int index = 0, item = 0; item < data_size; index += 2, item += 1) {

      //      View bar = findViewById(R.id.bar);

            TableRow newRow = new TableRow(this);

          //  TextView productId = new TextView(this);
            productId[item] = new TextView(this);
            productId[item].setText(data.get(index));
            productId[item].setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 4f));
            productId[item].setGravity(Gravity.CENTER_HORIZONTAL);
            productId[item].setPadding(10, 10, 10, 10);
            productId[item].setTextColor(Color.WHITE);
            productId[item].setTextSize(14);

    //        newRow.addView(bar);

          //  TextView quantity = new TextView(this);
            quantity[item] = new TextView(this);
            quantity[item].setText(data.get(index+1));
            quantity[item].setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 4f));
            quantity[item].setGravity(Gravity.CENTER_HORIZONTAL);
            quantity[item].setPadding(10, 10, 10, 10);
            quantity[item].setTextColor(Color.WHITE);
            quantity[item].setTextSize(14);

            newRow.addView(productId[item]);
            newRow.addView(quantity[item]);

            final int indexWidget = item;
            final int constantIndex = index;

            newRow.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    TableRow clickedRow = (TableRow) v;
                    if (!isSelectionModeActive[0]) {
                        isSelectionModeActive[0] = true; // Ativar modo de seleção
                    //    buttonExlude.setVisibility(View.VISIBLE);
                        clickedRow.setBackgroundColor(Color.LTGRAY); // Indicar seleção visualmente
                        selectedRows.add(clickedRow); // Adicionar à lista de exclusão
                        buttonExlude.setVisibility(View.VISIBLE); // Mostrar o botão de exclusão
                    }
                    return true; // Indicar que o evento foi consumido
                }
            });

            newRow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (isSelectionModeActive[0]) {
                        TableRow clickedRow = (TableRow) v;

                        // Alternar a seleção
                        if (selectedRows.contains(clickedRow)) {
                            clickedRow.setBackgroundColor(Color.TRANSPARENT); // Desmarcar visualmente
                            selectedRows.remove(clickedRow); // Remover da lista de exclusão
                        } else {
                            clickedRow.setBackgroundColor(Color.LTGRAY); // Marcar visualmente
                            selectedRows.add(clickedRow); // Adicionar à lista de exclusão
                        }

                        // Se não houver mais linhas selecionadas, sair do modo de seleção
                        if (selectedRows.size() == 0) {
                            isSelectionModeActive[0] = false;
                            buttonExlude.setVisibility(View.INVISIBLE); // Esconder o botão de exclusão
                        }
                    } else {
                        showEditDialog(indexWidget, constantIndex);
                        System.out.println("true");
                    }
                }
            });



            View separator = new View(this);
            separator.setLayoutParams(new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
            separator.setBackgroundColor(getResources().getColor(android.R.color.white));

            tabela.addView(newRow);
            tabela.addView(separator);
        }

        buttonExlude.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean error = false;
                Iterator<TableRow> iterator = selectedRows.iterator();
                while (iterator.hasNext()) {
                    TableRow id = iterator.next();
                    TextView tmp_prod = (TextView) id.getChildAt(1);
                    final String tmp_prod_str = tmp_prod.getText().toString();
                    if (DB.deleteData(tmp_prod_str) <= 0) {
                        error = true;
                        Toast.makeText(Database_window.this, "Error to delete " + tmp_prod_str, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(Database_window.this, "Success to delete " + tmp_prod_str, Toast.LENGTH_SHORT).show();
                        tabela.removeView(id);
                        iterator.remove();
                    }
                }
                if (!error) {
                    isSelectionModeActive[0] = false;
                    buttonExlude.setVisibility(View.INVISIBLE);
                }
            }
        });

        Button buttonExcel = findViewById(R.id.Excel);

        buttonExcel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(Database_window.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(Database_window.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
                    return;
                }

                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                startActivityForResult(intent, 2);
            }
            });

        loadingDialog.dismiss();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent dataf) {
        super.onActivityResult(requestCode, resultCode, dataf);

        if (requestCode == 2 && resultCode == RESULT_OK) {
            Uri uri = dataf.getData();
            if (uri != null) {
                // Use DocumentsContract APIs to create a file in the selected directory
                String documentId = DocumentsContract.getTreeDocumentId(uri);
                // You would then use this ID to create a new file in the selected directory
                Toast.makeText(this, "Directory selected: " + documentId, Toast.LENGTH_SHORT).show();
                ExcelDB excelDB = new ExcelDB(Database_window.this);
                excelDB.GenerateExcel(data, uri);
            }
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
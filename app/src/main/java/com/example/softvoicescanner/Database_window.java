package com.example.softvoicescanner;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.Toolbar;

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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.Toast;
import android.net.Uri;

import android.Manifest;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.softvoicescanner.databinding.ActivityDatabaseWindowBinding;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Database_window extends AppCompatActivity {

    private ActivityDatabaseWindowBinding binding;
    private Database DB;
    private ArrayList<String> data, rawDataExcel;  // Supõe-se que data já foi inicializado com dados
    private TableLayout tabela;
    private TextView[] productId, quantity;
    private ExecutorService executorService, executorServiceExcel;
    private Handler mainHandler, excelHandler;
    private String FinalFileName;
    private String currentFilter = "", LastFilter = "";
    private EditText search;
    private Button buttonExlude;

    public interface NameCallback {
        void onNameEntered(String fileName);
    }

    protected ArrayList<String> fetch_data() {
        if (data != null) {
            data.clear();
        }
        Cursor cursor;
        if (currentFilter.isEmpty()) {
            System.out.println("fdhgiutrh");
            cursor = DB.fetchAllDataCursor();
        }
        else {
            cursor = DB.fetchFilterData(currentFilter);
        }
        ArrayList<String> list = new ArrayList<String>();
        if (cursor == null || cursor.getCount() == 0) {
            System.out.println("cursor is null");
            return null;
        }
        if (cursor.moveToFirst()) {
            do { list.add(cursor.getString(0));
                list.add(cursor.getString(1));
            }
            while (cursor.moveToNext());
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
        editProductId.setText(tmp_product_id);
        editQuantity.setText(tmp_quantity);
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

                            new AsyncTask<Void, Void, Boolean>() {
                                @Override
                                protected Boolean doInBackground(Void... voids) {
                                    // Atualizar o banco de dados em segundo plano
                                    return DB.updateData(tmp_product_id, editProductId.getText().toString(), new Long(editQuantity.getText().toString())) != -1;
                                }

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

        private androidx.appcompat.app.AlertDialog init_win(final String title, final String message) {
            androidx.appcompat.app.AlertDialog.Builder wait_win = new androidx.appcompat.app.AlertDialog.Builder(Database_window.this);
            wait_win.setTitle(title);
            wait_win.setCancelable(false);
            wait_win.setMessage(message);

            View customLayout = LayoutInflater.from(this).inflate(R.layout.progresslayout, null);
            wait_win.setView(customLayout);

            ImageView gif = customLayout.findViewById(R.id.imageViewGif);

            Glide.with(this)
                    .asGif()
                    .load(R.drawable.loadb)
               //     .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                 //   .override(200, 200)  // Define um tamanho específico
                    .into(gif);

            androidx.appcompat.app.AlertDialog progressDialog = wait_win.create();

            progressDialog.show();
            return progressDialog;
        }

        private void searchItem() {
         /*       for (int itens = 2; itens < tabela.getChildCount(); itens++) {
                    tabela.removeViewAt(itens);
                    //tabela.removeViewAt(itens+1);
                } */
            tabela.removeAllViews();

            TableRow newRow = new TableRow(this);

            TextView constProductId = new TextView(this);
            constProductId.setText("Product id");
            constProductId.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 4f));
            constProductId.setGravity(Gravity.CENTER_HORIZONTAL);
            constProductId.setPadding(10, 10, 10, 10);
            constProductId.setTextColor(Color.WHITE);
            constProductId.setTextSize(14);

            TextView constQuant = new TextView(this);
            constQuant.setText("Quantity");
            constQuant.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 4f));
            constQuant.setGravity(Gravity.CENTER_HORIZONTAL);
            constQuant.setPadding(10, 10, 10, 10);
            constQuant.setTextColor(Color.WHITE);
            constQuant.setTextSize(14);

            newRow.addView(constProductId);
            newRow.addView(constQuant);

            View separator = new View(this);
            separator.setLayoutParams(new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
            separator.setBackgroundColor(getResources().getColor(android.R.color.white));

            tabela.addView(newRow);
            tabela.addView(separator);


                startFetchingData();
        }

        @Override
        protected void onDestroy() {
            super.onDestroy();
            getDelegate().onDestroy();
            DB.close();
            System.out.println("destroing");
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
            Toast.makeText(Database_window.this, "Error to open database", Toast.LENGTH_LONG).show();
            System.out.println("error to open database");
            return;
        }

        buttonExlude = findViewById(R.id.delete_button);
        search = findViewById(R.id.Filter);
        ImageButton searchButton = findViewById(R.id.SearchButton);

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentFilter = search.getText().toString();
                if (currentFilter.equals(LastFilter)) {
                    return;
                }
                LastFilter = currentFilter;

                searchItem();
            }
            });


                executorService = Executors.newSingleThreadExecutor();

        // Inicializa o handler para a thread principal
        mainHandler = new Handler(Looper.getMainLooper());

        TextView textBar = findViewById(R.id.DatabaseLabel);
        textBar.setText("Database " + DB.defaultDB);

        startFetchingData();

    }

    private void startFetchingData() {
        // Mostra a janela de carregamento antes de iniciar a busca
        final androidx.appcompat.app.AlertDialog loadingDialog = init_win("Connecting to database", "Fetching data.");

        // Executa a busca no banco de dados em uma thread separada
        executorService.execute(() -> {
            try {
                // Realiza a busca no banco de dados
                data = fetch_data();
                System.out.println("fetched");

                // Retorna para a thread principal para atualizar a UI
                mainHandler.post(() -> {
                    try {
                        // Atualiza a tabela com os dados buscados
                        System.out.println("creating");
                        create_table(loadingDialog);
                        System.out.println("created");
                    } catch (Exception e) {
                        System.out.println("Error while creating table: " + e.getMessage());
                        e.printStackTrace();
                    } finally {
                        // Garante que o fechamento do dialog seja feito na UI Thread
                        runOnUiThread(() -> {
                         //   create_table(loadingDialog);
                            if (loadingDialog.isShowing()) {
                                System.out.println("Dismissing loading dialog");
                                loadingDialog.dismiss();
                            }
                        });
                    }
                });
            } catch (Exception e) {
                System.out.println("Error fetching data: " + e.getMessage());
                e.printStackTrace();
                // Mostra o Toast na thread principal e fecha o diálogo
                runOnUiThread(() -> {
                    if (loadingDialog.isShowing()) {
                        loadingDialog.dismiss();
                    }
                    Toast.makeText(Database_window.this, "Error fetching data", Toast.LENGTH_SHORT).show();
                });
            } finally {
                // Garante o fechamento do diálogo caso o cursor seja null ou outra exceção aconteça
                runOnUiThread(() -> {
                    if (loadingDialog.isShowing()) {
                        loadingDialog.dismiss();
                    }
                });
            }
        });
    }

    void create_table(androidx.appcompat.app.AlertDialog loadingDialog) {

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Softvoice scanner");

        tabela = findViewById(R.id.data_grid);
        List<TableRow> selectedRows = new ArrayList<>();
        final boolean[] isSelectionModeActive = {false};

        if (data == null) {
        return;
        }

        int data_size = data.size() / 2;

        productId = new TextView[data_size];
        quantity = new TextView[data_size];

        for (int index = 0, item = 0; item < data_size; index += 2, item += 1) {

            TableRow newRow = new TableRow(this);

            productId[item] = new TextView(this);
            productId[item].setText(data.get(index));
            productId[item].setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 4f));
            productId[item].setGravity(Gravity.CENTER_HORIZONTAL);
            productId[item].setPadding(10, 10, 10, 10);
            productId[item].setTextColor(Color.WHITE);
            productId[item].setTextSize(14);

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
            separator.setLayoutParams(new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
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
                    TextView tmp_prod = (TextView) id.getChildAt(0);
                    final String tmp_prod_str = tmp_prod.getText().toString();
                    System.out.println(tmp_prod_str);
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
                checkData();

            }
            });
        System.out.println("fechado");
        loadingDialog.dismiss();

    }

    private void ExcelInterface() {
        if (ContextCompat.checkSelfPermission(Database_window.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(Database_window.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
        }
        else {
        NameOfFile(new NameCallback() {
            @Override
            public void onNameEntered(String fileName) {
                // Aqui você pode usar o nome do arquivo retornado
                if (!fileName.isEmpty()) {
                    // Prossiga com a lógica, como salvar o arquivo Excel
                    FinalFileName = fileName;
                    // Sua lógica para criar o arquivo Excel aqui...
                    executorServiceExcel = Executors.newSingleThreadExecutor();

                    // Inicializa o handler para a thread principal
                    excelHandler = new Handler(Looper.getMainLooper());

                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                    startActivityForResult(intent, 2);
                }
            }
        });
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 2) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissão concedida, criar o arquivo
                NameOfFile(new NameCallback() {
                    @Override
                    public void onNameEntered(String fileName) {
                        if (!fileName.isEmpty()) {
                            FinalFileName = fileName;
                            executorServiceExcel = Executors.newSingleThreadExecutor();

                            excelHandler = new Handler(Looper.getMainLooper());

                            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                            startActivityForResult(intent, 2);
                        }
                    }
                });
            } else {
                // Permissão negada, exibir uma mensagem para o usuário
                Toast.makeText(this, "Permissão negada para acessar o armazenamento externo.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void checkData() {
        if (!currentFilter.isEmpty()) { // empty é o filtro que mostra todos os dados
            System.out.println("data filter detected");
            LayoutInflater inflater = getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.filterlayout, null);
            if (dialogView == null) {
                System.out.println("Null viewer");
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle("Data filter detected")
                    .setMessage("Do you want to apply the filter?\nDo not accept if you need all data to Excel")
                    .setView(dialogView)
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final String tmp_filter = currentFilter;
                            currentFilter = "";
                            rawDataExcel = fetch_data();
                            currentFilter = tmp_filter;
                            ExcelInterface();
                        }
                    })
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            rawDataExcel = data;
                            ExcelInterface();
                        }
                    }).show();
        }
        else {
            rawDataExcel = data;
            ExcelInterface();
        }


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
                startExcel(uri);
            }
        }
    }

    private void NameOfFile(NameCallback callback) {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.new_database_layout, null);
        final EditText file_name = dialogView.findViewById(R.id.NewDatabaseInput);

        file_name.setHint("File name....");

        // Obter a data e hora formatadas
        LocalDateTime myDateObj = LocalDateTime.now();
        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("dd-MM-yyyy-HH:mm:ss");
        final String formattedDate = myDateObj.format(myFormatObj);

        // Define o nome padrão do arquivo
        final String defaultName = DB.defaultDB + "-" + formattedDate;
        file_name.setText(defaultName);

        // Cria e mostra o diálogo de entrada de nome
        new android.app.AlertDialog.Builder(this)
                .setTitle("Name of Excel file")
                .setView(dialogView)
                .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String FileName = file_name.getText().toString();

                        // Verifica se o nome está vazio
                        if (FileName.isEmpty()) {
                            Toast.makeText(Database_window.this, "Invalid name for Excel file", Toast.LENGTH_LONG).show();
                            // Reabre o diálogo se o nome estiver vazio
                            NameOfFile(callback);
                        } else {
                            // Retorna o nome através do callback se for válido
                            callback.onNameEntered(FileName);
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss(); // Fechar o diálogo sem nenhuma ação adicional
                    }
                })
                .show();
    }

    private void startExcel(Uri uri) {
        // Mostra a janela de carregamento antes de iniciar a busca
        androidx.appcompat.app.AlertDialog loadingExcel = init_win("Creating Excel", "Do not stop program.\nIt will create Excel file.");

        // Executa a busca no banco de dados em uma thread separada
        executorServiceExcel.execute(() -> {
            // Realize a busca no banco de dados
            ExcelDB excelDB = new ExcelDB(Database_window.this);
            excelDB.FileName = FinalFileName;
            excelDB.GenerateExcel(rawDataExcel, uri);

            // Retorna para a thread principal para atualizar a UI
            excelHandler.post(() -> {
                // Atualiza a tabela com os dados buscados
                excelDB.checkExcel();


                // Fecha a janela de carregamento
                if (loadingExcel != null && loadingExcel.isShowing()) {
                    System.out.println("yupi");
                    loadingExcel.dismiss();
                }
            });
        });
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
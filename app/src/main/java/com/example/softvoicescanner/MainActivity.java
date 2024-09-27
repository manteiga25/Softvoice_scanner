package com.example.softvoicescanner;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.textfield.TextInputEditText;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import android.widget.Toast;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    Database DB;

    private TextView prod_id, quantity;
    private String FinalFileName;
    private ExecutorService executorService;
    private Handler mainHandler;
    private ArrayList<String> selectedTables;

    private boolean light = false;

    String defaultDB;

    private void flash(ImageButton button) {
        light = !light;
        if (light) {
            button.setImageResource(R.drawable.baseline_flash_on_24);
        }
        else {
            button.setImageResource(R.drawable.baseline_flash_off_24);
        }
    }

    public interface NameCallback {
        void onNameEntered(String fileName);
    }

    private androidx.appcompat.app.AlertDialog init_win(final String title, final String message) {
        androidx.appcompat.app.AlertDialog.Builder wait_win = new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this);
        wait_win.setTitle(title);
        wait_win.setCancelable(false);
        wait_win.setMessage(message);

        View customLayout = LayoutInflater.from(this).inflate(R.layout.progresslayout, null);
        wait_win.setView(customLayout);

        ImageView gif = customLayout.findViewById(R.id.imageViewGif);

        Glide.with(this)
                .asGif()
                .load(R.drawable.loadb)
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                //   .override(200, 200)  // Define um tamanho específico
                .into(gif);

        androidx.appcompat.app.AlertDialog progressDialog = wait_win.create();

        progressDialog.show();
        return progressDialog;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Infla o menu; isto adiciona itens à barra de menu
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Lidar com cliques nos itens de menu
        int id = item.getItemId();

        if (id == R.id.database) {
            if (DB.defaultDB == null || DB.defaultDB.isEmpty()) {
                noDatabase("Database not selected");
                return super.onOptionsItemSelected(item);
            }

            //    DB.close();
            Intent intent = new Intent(MainActivity.this, Database_window.class);
            //    intent.putExtra("extra_data_key", DB);
            intent.putExtra("databaseName", DB.defaultDB);
           // intent.putExtra("widget", wait_win);
            startActivity(intent);
            // Código para lidar com a ação do botão de configurações
            return true;
        } else if (id == R.id.NewDatabase) {
            LayoutInflater inflater = getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.new_database_layout, null);
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Create Database")
                    .setView(dialogView)
                    .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final EditText database_name = dialogView.findViewById(R.id.NewDatabaseInput);
                            System.out.println("clicked");
                            final String data_name = database_name.getText().toString();
                            if (data_name.isEmpty()) {
                                Toast.makeText(MainActivity.this, "Invalid database name", Toast.LENGTH_LONG).show();
                                onOptionsItemSelected(item);
                            } else {
                                final short status = DB.createDatabase(data_name);
                                if (status == 1) {

                                    Toast.makeText(MainActivity.this, "Success to create database " + data_name, Toast.LENGTH_SHORT).show();
                                    System.out.println(DB.defaultDB);
                                    DB.writeDefaultDB(data_name);
                                    System.out.println(DB.defaultDB);
                                    getSupportActionBar().setTitle("Softvoice scanner " + DB.defaultDB);

                                    System.out.println("current db " + DB.defaultDB);

                                } else if (status == 2) {
                                    Toast.makeText(MainActivity.this, "Database with name " + data_name + " already exists", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(MainActivity.this, "Error to create database " + data_name, Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else if (id == R.id.DelDatabase) {
            LayoutInflater inflater = getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.scrollayout, null);

            LinearLayout checkboxesContainer = dialogView.findViewById(R.id.checkboxes_container);

            // Lista de tabelas do banco de dados
            ArrayList<String> options = DB.listAllTables();

            // Lista para armazenar as CheckBoxes criadas
            ArrayList<CheckBox> checkBoxList = new ArrayList<>();

            // Criar dinamicamente CheckBoxes
            for (String option : options) {
                System.out.println(option);
                CheckBox checkBox = new CheckBox(this);
                checkBox.setText(option);
                checkboxesContainer.addView(checkBox);
                checkBoxList.add(checkBox);
            }

            Button submitButton = new Button(this);
            submitButton.setText("Delete");
            checkboxesContainer.addView(submitButton);

            // Criar e mostrar o AlertDialog
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Delete Databases")
                    .setView(dialogView)
                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            deleteDatabase(checkBoxList, checkboxesContainer);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();

            // Evento de clique no botão "Submit"
            submitButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteDatabase(checkBoxList, checkboxesContainer);
                }
            });
        } else if (id == R.id.RenameDatabase) {
            ArrayList<String> databases = DB.listAllTables();
            if (databases.isEmpty()) {
                noDatabase("Rename Database");
                return super.onOptionsItemSelected(item);
            }

            LayoutInflater inflater = getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.renamelayout, null);
            Spinner spinner = dialogView.findViewById(R.id.spinnerrename);

            final TextInputEditText renameBox = dialogView.findViewById(R.id.RenameBox);

            final boolean dbEmpty = DB.defaultDB.isEmpty();
            String cacheDBName = "(None)";

            ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_dropdown_item, databases);

            spinner.setAdapter(adapter);

            int index = 0;
            if (!dbEmpty) {
                for (final String name : databases) {
                    if (name.equals(DB.defaultDB)) {
                        cacheDBName = DB.defaultDB;
                        break;
                    }
                    index += 1;
                }
            }
            spinner.setSelection(index, true);

            new android.app.AlertDialog.Builder(this)
                    .setTitle("Rename Database")
                    .setMessage("current database " + cacheDBName)
                    .setView(dialogView)
                    .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final String NewName = renameBox.getText().toString();
                            if (NewName.isEmpty()) {
                                Toast.makeText(MainActivity.this, "Invalid name", Toast.LENGTH_LONG).show();
                                onOptionsItemSelected(item);
                                return;
                            }
                            final String Database_selected = spinner.getSelectedItem().toString();
                            // Definir o Adapter no Spinner
                            if (!DB.changeDbName(Database_selected, NewName)) {
                                Toast.makeText(MainActivity.this, "Error to rename database from " + Database_selected + " to " + NewName, Toast.LENGTH_LONG).show();
                                return;
                            }

                            DB.writeDefaultDB(NewName);

                            Toast.makeText(MainActivity.this, "Success to rename database from " + Database_selected + " to " + NewName, Toast.LENGTH_SHORT).show();

                            if (DB.defaultDB.equals(Database_selected)) {
                                DB.defaultDB = NewName;
                            }

                            defaultDB = Database_selected;

                            System.out.println(spinner.getSelectedItem().toString() + " " + DB.defaultDB);
                            getSupportActionBar().setTitle("Softvoice scanner " + DB.defaultDB);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else if (id == R.id.SwiDatabase) {
            ArrayList<String> databases = DB.listAllTables();
            if (databases.isEmpty()) {
                noDatabase("Switch Database");
                return super.onOptionsItemSelected(item);
            }
            LayoutInflater inflater = getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.spinbox, null);
            Spinner spinner = dialogView.findViewById(R.id.spinner);

            final boolean dbEmpty = DB.defaultDB.isEmpty();
            String cacheDBName = "(None)";

            ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_dropdown_item, databases);

            spinner.setAdapter(adapter);

            int index = 0;
            if (!dbEmpty) {
                for (final String name : databases) {
                    if (name.equals(DB.defaultDB)) {
                        cacheDBName = DB.defaultDB;
                        break;
                    }
                    index += 1;
                }
            }
            spinner.setSelection(index, true);

            new android.app.AlertDialog.Builder(this)
                    .setTitle("Switch Database")
                    .setMessage("current database " + cacheDBName)
                    .setView(dialogView)
                    .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final String Database_selected = spinner.getSelectedItem().toString();
                            // Definir o Adapter no Spinner
                            DB.writeDefaultDB(Database_selected);
                            defaultDB = Database_selected;

                            System.out.println(spinner.getSelectedItem().toString() + " " + DB.defaultDB);
                            getSupportActionBar().setTitle("Softvoice scanner " + DB.defaultDB);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
        else if (id == R.id.ImportDatabase) {
            System.out.println("clicked");
        }
        else if (id == R.id.ExportDatabase) {
            LayoutInflater inflater = getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.scrollayout, null);

            LinearLayout checkboxesContainer = dialogView.findViewById(R.id.checkboxes_container);

            // Lista de tabelas do banco de dados
            ArrayList<String> options = DB.listAllTables();

            // Lista para armazenar as CheckBoxes criadas
            ArrayList<CheckBox> checkBoxList = new ArrayList<>();

            // Criar dinamicamente CheckBoxes
            for (String option : options) {
                System.out.println(option);
                CheckBox checkBox = new CheckBox(this);
                checkBox.setText(option);
                checkboxesContainer.addView(checkBox);
                checkBoxList.add(checkBox);
            }

            // Criar e mostrar o AlertDialog
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Export Databases")
                    .setView(dialogView)
                    .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            selectedTables = new ArrayList<>();
                            for (final CheckBox box : checkBoxList) {
                                if (box.isChecked()) {
                                    selectedTables.add(box.getText().toString());
                                    System.out.println(box.getText().toString());
                                }
                            }
                            if (selectedTables.isEmpty()) {
                                Toast.makeText(MainActivity.this, "No database selected", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            DbInterface();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
        else if (id == R.id.Unifydatabase) {
            ArrayList<String> databases = DB.listAllTables();
            if (databases.isEmpty()) {
                noDatabase("Unify Database");
                return super.onOptionsItemSelected(item);
            }

            LayoutInflater inflater = getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.fundirdblayout, null);
            Spinner spinner = dialogView.findViewById(R.id.spinnerDb);

            final LinearLayout frameBox = dialogView.findViewById(R.id.checkbox_container);

            final boolean dbEmpty = DB.defaultDB.isEmpty();
            String cacheDBName = "(None)";

            ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_dropdown_item, databases);

            spinner.setAdapter(adapter);

            final int[] index = {0};

            ArrayList<CheckBox> checkBoxList = new ArrayList<>();

            // Criar dinamicamente CheckBoxes
            for (final String option : databases) {
                System.out.println(option);
                CheckBox checkBox = new CheckBox(this);
                checkBox.setText(option);
                if (DB.defaultDB.equals(option)) {
                    checkBox.setEnabled(false);
                }
                frameBox.addView(checkBox);
                checkBoxList.add(checkBox);
            }

            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {

                    final CheckBox OldCheckBox = checkBoxList.get(index[0]);

                    OldCheckBox.setEnabled(true);

                    index[0] = position;

                    System.out.println(index[0]);

                    final CheckBox tmpCheckBox = checkBoxList.get(position);

                    tmpCheckBox.setChecked(false);

                    tmpCheckBox.setEnabled(false);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                    return;
                }
            });

            if (!dbEmpty) {
                for (final String name : databases) {
                    if (name.equals(DB.defaultDB)) {
                        cacheDBName = DB.defaultDB;
                        break;
                    }
                    index[0] += 1;
                }
            }
            spinner.setSelection(index[0], true);

            new android.app.AlertDialog.Builder(this)
                    .setTitle("Unify Database")
                    .setMessage("current database " + cacheDBName)
                    .setView(dialogView)
                    .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final ArrayList<String> TablesSelected = new ArrayList<>();
                            for (final CheckBox box : checkBoxList) {
                                if (box.isChecked()) {
                                    TablesSelected.add(box.getText().toString());
                                }
                            }
                            if (TablesSelected.isEmpty()) {
                                Toast.makeText(MainActivity.this, "No database selected", Toast.LENGTH_LONG).show();
                          //      onOptionsItemSelected(item);
                                return;
                            }

                            executorService = Executors.newSingleThreadExecutor();

                            mainHandler = new Handler(Looper.getMainLooper());

                            AlertDialog loadingWin = init_win("Connecting to database", "Unifying databases\nDo not close the App");
                            executorService.execute(() -> {
                                final String Database_selected = spinner.getSelectedItem().toString();

                                final boolean status = DB.unifyDB(TablesSelected, Database_selected);

                                mainHandler.post(() -> {
                                    loadingWin.dismiss();
                                    if (status) {
                                        Toast.makeText(MainActivity.this, "Success to unify database", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(MainActivity.this, "Error to unify database", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            });
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();

        }


        return super.onOptionsItemSelected(item);
    }

    private void DbInterface() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
            if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                return;
            }
        }
            NameOfFile(new NameCallback() {
                @Override
                public void onNameEntered(String fileName) {
                    // Aqui você pode usar o nome do arquivo retornado
                    if (!fileName.isEmpty()) {
                        // Prossiga com a lógica, como salvar o arquivo Excel
                        FinalFileName = fileName;
                        // execute service
                        executorService = Executors.newSingleThreadExecutor();

                        // Inicializa o handler para a thread principal
                        mainHandler = new Handler(Looper.getMainLooper());

                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                        startActivityForResult(intent, 2);
                    }
                }
            });
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
        final String defaultName = "Backup-" + formattedDate;
        file_name.setText(defaultName);

        // Cria e mostra o diálogo de entrada de nome
        new android.app.AlertDialog.Builder(this)
                .setTitle("Name of Database file")
                .setView(dialogView)
                .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String FileName = file_name.getText().toString();

                        // Verifica se o nome está vazio
                        if (FileName.isEmpty()) {
                            Toast.makeText(MainActivity.this, "Invalid name for Database file", Toast.LENGTH_LONG).show();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 2 && resultCode == RESULT_OK) {
            final androidx.appcompat.app.AlertDialog loadingDialog = init_win("Connecting to database", "Coping data.");
            executorService.execute(() -> {
                Uri uri = data.getData();
                if (uri != null) {
                    // Use DocumentsContract APIs to create a file in the selected directory
                    //  String documentId = DocumentsContract.getTreeDocumentId(uri);
                    File externalDbPath = new File(getExternalFilesDir(null), "BackupDatabase");

                    if (!externalDbPath.exists()) {
                        externalDbPath.mkdirs();
                    }

                    String dbPath = externalDbPath.getAbsolutePath();

                    final boolean status = DB.createExternalDb(selectedTables, dbPath, FinalFileName);

                    mainHandler.post(() -> {
                        // You would then use this ID to create a new file in the selected directory
                        // Toast.makeText(this, "Directory selected: " + documentId, Toast.LENGTH_SHORT).show();
                        if (status) {
                            Toast.makeText(MainActivity.this, "Success to create Backup in android/data", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(MainActivity.this, "Error to create Backup in android/data", Toast.LENGTH_LONG).show();
                        }
                        selectedTables.clear();
                        loadingDialog.dismiss();
                    });
                }
            });
        }
    }

    private void deleteDatabase(ArrayList<CheckBox> checkBoxList, LinearLayout checkboxesContainer) {
        Iterator<CheckBox> iterator = checkBoxList.iterator();
        boolean selected = false;
        while (iterator.hasNext()) {
            CheckBox checkBox = iterator.next();
            if (checkBox.isChecked()) {
                final String databaseToDelete = checkBox.getText().toString();
                final boolean success = DB.deleteAllData(databaseToDelete);
                selected = true;
                if (success) {
                    if (databaseToDelete.equals(DB.defaultDB)) {
                        DB.writeDefaultDB("");
                    }
                    Toast.makeText(MainActivity.this, "Database " + databaseToDelete + " deleted", Toast.LENGTH_SHORT).show();
                    checkboxesContainer.removeView(checkBox);
                    iterator.remove();
                    System.out.println("rgrg " + DB.defaultDB);
                    System.out.println("rgrg2 " + databaseToDelete);
                } else {
                    Toast.makeText(MainActivity.this, "Failed to delete " + databaseToDelete, Toast.LENGTH_SHORT).show();
                }
            }
        }
        if (!selected) {
            Toast.makeText(MainActivity.this, "No databases selected", Toast.LENGTH_SHORT).show();
        }
        else {
            getSupportActionBar().setTitle("Softvoice scanner " + DB.defaultDB);
        }
    }

    private void noDatabase(final String title) {
        LinearLayout layout = new LinearLayout(MainActivity.this);
        layout.setOrientation(LinearLayout.VERTICAL);
        TextView texto = new TextView(MainActivity.this);
        texto.setText("No databases found\nCreate new database");
        texto.setTextColor(Color.BLACK);
        texto.setGravity(Gravity.CENTER);
        texto.setTextSize(20f);
        layout.addView(texto);
        View dialogView = layout;

        new android.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setView(dialogView)
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);

        ImageView imagem_logo = findViewById(R.id.imageView);
        //  imagem_logo.setLayoutParams(new ConstraintLayout.LayoutParams((int) (screenWidth * 0.50), (int) (screenHeight * 0.50)));
        ConstraintLayout.LayoutParams params_image = (ConstraintLayout.LayoutParams) imagem_logo.getLayoutParams();

        imagem_logo.setLayoutParams(params_image);

        // Configurar a Toolbar como a ActionBar da Activity
        setSupportActionBar(toolbar);

        ImageButton flashButton = findViewById(R.id.imageButton);

        flashButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flash(flashButton); // Alternar o estado da lanterna
            }
        });

      //  getSupportActionBar().setIcon(R.drawable.ic_toolbar_icon);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Button scan_button = findViewById(R.id.btn_camera);
        scan_button.setBackgroundColor(Color.WHITE);
        scan_button.setOnClickListener(v->scanCode());

        prod_id = findViewById(R.id.Product_code);
        quantity = findViewById(R.id.Number_products);

        Button button_add_prod = findViewById(R.id.relase_data);
        button_add_prod.setOnClickListener(v->check_values());

        DB = new Database(this);
        DB.open();
        DB.readtable();

    //    defaultDB = DB.readtable();
        System.out.println(DB.defaultDB);

        getSupportActionBar().setTitle("Softvoice scanner " + DB.defaultDB);

    }
    void scanCode() {
        ScanOptions options = new ScanOptions();
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setCaptureActivity(CaptureAct.class);
        options.setBarcodeImageEnabled(true);
        options.setTorchEnabled(light);

        barLaunch.launch(options);
    }

    ActivityResultLauncher<ScanOptions> barLaunch = registerForActivityResult(new ScanContract(), result->{
        if (result.getContents() != null) {

            String code_value = result.getContents();

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Result");
            builder.setMessage(code_value);

            LinearLayout layout = new LinearLayout(MainActivity.this);
            layout.setOrientation(LinearLayout.VERTICAL);

            final EditText input = new EditText(MainActivity.this);
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            input.setText("1");

            final TextView out_label = new TextView(MainActivity.this);
            out_label.setText("Insira a quantidade.");

            layout.addView(out_label);
            layout.addView(input);

            builder.setView(layout);

            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (DB.defaultDB == null || DB.defaultDB.isEmpty()) {
                        noDatabase("Database not selected");
                        return;
                    }
                    String value = input.getText().toString();
                    if (value.isEmpty()) {
                        System.out.println("vazio");
                        AlertDialog.Builder error_win = new AlertDialog.Builder(MainActivity.this);
                        error_win.setTitle("Invalid value");
                        error_win.setMessage("You need to set a value.");
                        error_win.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Apenas fecha o diálogo de erro
                                dialog.dismiss();
                            }
                        }).show();
                    }
                    else {
                          long num = Long.parseLong(input.getText().toString());
                          System.out.println(num);
                        dialogInterface.dismiss();
                        final int existe = DB.check_id(code_value, num, MainActivity.this);
                        if (existe == 0) {
                            if (DB.insertData(code_value, num) > 0) {
                                Toast.makeText(MainActivity.this, "Sucess to insert data to database", Toast.LENGTH_SHORT).show();
                            }
                                else {
                                Toast.makeText(MainActivity.this, "Error to insert data to database", Toast.LENGTH_LONG).show();
                                }
                        }
                    }
                }
            }).show();
        }
    });

    private void check_values() {
        System.out.println("clicado");
        final String id = prod_id.getText().toString();
        final String quantidade = quantity.getText().toString();
        if (id.isEmpty() || quantidade.isEmpty()) {
            Toast.makeText(this, "É necessario preencher todos os campos", Toast.LENGTH_LONG).show();
            return;
        }
        else if (DB.defaultDB == null || DB.defaultDB.isEmpty()) {
            noDatabase("Database not selected");
            return;
        }
        add_to_db(id, Long.parseLong(quantidade));
    }

    private void add_to_db(String id, long quantidade) {
        System.out.println(quantidade);
        int existe = DB.check_id(id, quantidade, MainActivity.this);
        if (existe == 0) {
            if (DB.insertData(id, quantidade) == 0) {
            Toast.makeText(this, "Error to insert data to database", Toast.LENGTH_LONG).show();
            }
            else {
                Toast.makeText(this, "Sucess to insert data to database", Toast.LENGTH_SHORT).show();
            }
        }
    }

}


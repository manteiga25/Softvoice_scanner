package com.example.softvoicescanner;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import android.widget.Toast;

import java.util.ArrayList;
import java.util.Iterator;

import java.io.Serializable;

public class MainActivity extends AppCompatActivity {

    Database DB;

    private DrawerLayout drawer;
    private TextView prod_id, quantity;
    NavigationView navebar;
    ActionBarDrawerToggle drawer_togle;

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
            if (DB.defaultDB.isEmpty()) {
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

            // Criar um botão para processar a seleção

           // Button b = dialogView.findViewById(R.id.DeleteButton);
        //    checkboxesContainer.removeView(b);
            //checkboxesContainer.addView(b);

          //  Button submitButton = new Button(this);
          //  submitButton.setText("Delete");
           // checkboxesContainer.addView(submitButton);

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


        return super.onOptionsItemSelected(item);
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
                        int existe = DB.check_id(code_value, num, MainActivity.this);
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


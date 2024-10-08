package com.example.softvoicescanner;

import static android.content.Context.MODE_PRIVATE;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class Database extends SQLiteOpenHelper {

    private SQLiteDatabase database;

    public String defaultDB;

    private Context context;

    private final String[] message_update_toast = {"Error to update database.", "Database update with successful"};
    private final int[] mode_toaste = {1, 0};

    public Database(Context activity_context) {
        super(activity_context, "soft_database.db", null, 1);
        context = activity_context;
    }

    public boolean createExternalDb(final ArrayList<String> tables, final String DbPath, String DbName) {
        try {

            long offset = 0;

            System.out.println("name " + DbName);

            DbName = DbPath + "/" + DbName;

            File dbFile = context.getDatabasePath("soft_database.db");

            System.out.println("Path " + DbName);

            System.out.println("0");

            if (!dbFile.exists()) {
                System.out.println(dbFile.getPath() + " not found");
                return false;
            }
            SQLiteDatabase existingDb = SQLiteDatabase.openDatabase(dbFile.getPath(), null, SQLiteDatabase.OPEN_READONLY);

            System.out.println("1");

            SQLiteDatabase newDb = SQLiteDatabase.openOrCreateDatabase(DbName, null);

            System.out.println("2");

            Cursor cursor;

            for (final String table : tables) {
                newDb.execSQL("CREATE TABLE IF NOT EXISTS " + table + " (product_id TEXT PRIMARY KEY NOT NULL, quantidade BIGINT NOT NULL);");
                System.out.println(table);
                boolean HasData = true;
                while (HasData) {
                    cursor = existingDb.rawQuery("SELECT * FROM " + table + " LIMIT 1000 OFFSET ?", new String[]{String.valueOf(offset)});
                    if (cursor == null) {
                        System.out.println("null");
                        return false;
                    }
                    if (cursor.moveToFirst()) {
                        do {
                            ContentValues contentValues = new ContentValues();
                            contentValues.put("product_id", cursor.getString(0));
                            contentValues.put("quantidade", cursor.getString(1));

                            newDb.insert(table, null, contentValues);
                        }
                        while (cursor.moveToNext());
                    }
                    if (cursor.getCount() < 1000) {
                        HasData = false;
                    }
                    offset += 1000;
                    cursor.close();
                }
            }

            newDb.close();
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean unifyDB(final ArrayList<String> tables, final String tableDst) {
        try {
            database.beginTransaction();
            for (final String table : tables) {
                database.execSQL("UPDATE " + tableDst + " SET quantidade = quantidade + " +
                        "(SELECT " + table + ".quantidade FROM " + table +
                        " WHERE " + table + ".product_id = " + tableDst + ".product_id)" +
                        " WHERE EXISTS (SELECT 1 FROM " + table +
                        " WHERE " + table + ".product_id = " + tableDst + ".product_id);");

                database.execSQL("INSERT INTO " + tableDst + " (product_id, quantidade) " +
                        "SELECT t2.product_id, t2.quantidade " +
                        "FROM " + table + " t2 " +
                        "LEFT JOIN " + tableDst + " t1 ON t1.product_id = t2.product_id " +
                        "WHERE t1.product_id IS NULL;");
            }
            database.setTransactionSuccessful();
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
    } finally {
        database.endTransaction(); // Finaliza a transação
    }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Cria o banco de dados pela primeira vez
        db.execSQL("CREATE TABLE IF NOT EXISTS products (product_id TEXT PRIMARY KEY NOT NULL, quantidade BIGINT NOT NULL);");
        System.out.println("criado");
        writeDefaultDB("products");
    //    file = new File("default_table.txt");
    }

  /*  public void readtable() {
        try (BufferedReader br = new BufferedReader(new FileReader("default_table.txt"))) {
            String linha;
            while ((linha = br.readLine()) != null) {
                System.out.println(linha);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    } */

    public String readtable() {
        try {
            FileInputStream fis = context.openFileInput("default_table.txt");

            // Usando um StringBuilder para armazenar o conteúdo lido
            StringBuilder stringBuilder = new StringBuilder();

            // Buffer para leitura
            byte[] buffer = new byte[1024];
            int length;

            // Lendo o arquivo em bytes e convertendo para String
            while ((length = fis.read(buffer)) != -1) {
                stringBuilder.append(new String(buffer, 0, length));
            }

            // Fechando o FileInputStream
            fis.close();

            // Exibindo o conteúdo do arquivo
            defaultDB = stringBuilder.toString();
            return defaultDB;

        } catch (IOException e) {
            System.out.println("Ocorreu um erro ao ler o arquivo: " + e.getMessage());
            return "";
        }
    }

    public void writeDefaultDB(final String databaseName) {
        try {
            // Usando o contexto para acessar openFileOutput()
            FileOutputStream fos = context.openFileOutput("default_table.txt", MODE_PRIVATE);
            fos.write(databaseName.getBytes());
            fos.close();
            defaultDB = databaseName;
            System.out.println("d " + databaseName + " == " + defaultDB);
        } catch (IOException e) {
            System.out.println("Ocorreu um erro ao criar o arquivo: " + e.getMessage());
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Atualiza o banco de dados se a versão mudar
        db.execSQL("DROP TABLE IF EXISTS " + defaultDB);
        onCreate(db);
    }

    public void open() {
        database = this.getWritableDatabase();
    }

    public void close() {
        database.close();
    }

    public boolean tabelaExiste(final String nomeTabela) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT count(*) FROM sqlite_master WHERE type='table' AND name=?", new String[]{nomeTabela});
        if (cursor.moveToFirst()) {
            int count = cursor.getInt(0);
            cursor.close();
            return count > 0;
        } else {
            cursor.close();
            return false;
        }
    }

    public ArrayList<String> listAllTables() {
        // Abre o banco de dados em modo leitura

        // Query para listar todas as tabelas no banco de dados
        Cursor cursor = database.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name != 'android_metadata'", null);
        ArrayList<String> tables = new ArrayList<>();

        // Itera sobre os resultados e exibe os nomes das tabelas
        if (cursor.moveToFirst()) {
            do {
            //    String tableName = cursor.getString(0); // 0 é o índice da coluna 'name'
                tables.add(cursor.getString(0));
              //  System.out.println("Tabela: " + tableName);
            } while (cursor.moveToNext());
        }

        // Fecha o cursor e o banco de dados
        cursor.close();
        return tables;
    }

    // return 0 error
    // return 1 sucess
    // return 2 exists
    public short createDatabase(final String name) {
        try {
            final boolean exists = tabelaExiste(name);
            if (exists) { return 2; }
            database.execSQL("CREATE TABLE IF NOT EXISTS " + name + " (product_id TEXT PRIMARY KEY NOT NULL, quantidade BIGINT NOT NULL);");
        }
        catch (Exception e) {
            return 0;
        }
        return 1;
    }

    public long insertData(String prod_id, long quantidade) {
        ContentValues values = new ContentValues();
        values.put("product_id", prod_id);
        values.put("quantidade", quantidade);

        return database.insert(defaultDB, null, values);
    }

    public Cursor fetchFilterData(final String id, final String offset, final int limit) {
        return database.rawQuery("Select * from " + defaultDB + " WHERE product_id LIKE ? LIMIT ? OFFSET ?", new String[]{"%" + id + "%", String.valueOf(limit), offset});
    }

   /* public double numberOfRows() {
        long offset = 0;
        double numberOfItens = 0.0;
        boolean HasData = true;
        while (HasData) {
            Cursor cursor = database.rawQuery("Select * from " + defaultDB + " LIMIT 1000 OFFSET ?", new String[]{String.valueOf(offset)});
            //   final int tmp_value = cursor.getCount();
            if (cursor == null) {
                System.err.println("error");
            }
            final int DataLen = cursor.getCount();
            if (DataLen > 0) {
             //   final int tmp_value = cursor.getInt(0);
                numberOfItens += DataLen;
                offset += 1000;
            }
            else {
                HasData = false;
            }
            System.out.println(DataLen);
            cursor.close();
        }
        return numberOfItens / 100.0;
    } */

    public double numberOfRows() {
        double numberOfItens = 0.0;
            Cursor cursor = database.rawQuery("Select count(*) from " + defaultDB, null);
            if (cursor == null) {
                System.err.println("error");
                return -1;
            }
        if (cursor.moveToFirst()) {
            final long tmp_value = cursor.getLong(0);
            numberOfItens = tmp_value / 100.0;
        }
            cursor.close();
        return numberOfItens;
    }

    public double numberOfRowsFilter(final String filter) {
        double numberOfItens = 0.0;
        Cursor cursor = database.rawQuery("Select count(*) from " + defaultDB + " WHERE product_id LIKE ?", new String[]{"%" + filter + "%"});
        //   final int tmp_value = cursor.getCount();
        if (cursor.moveToFirst()) {
            final int tmp_value = cursor.getInt(0);
            System.out.println(tmp_value);
            numberOfItens = tmp_value / 100.0;
            System.out.println(numberOfItens);
        }
        cursor.close();
        return numberOfItens;
    }

    public boolean insertAllData(String[] prod_id, long[] quantidade) {
        database.beginTransaction();
        try {
            for (int i = 0; i < prod_id.length; i++) {
                ContentValues values = new ContentValues();
                values.put("product_id", prod_id[i]);
                values.put("quantidade", quantidade[i]);

                long result = database.insert(defaultDB, null, values);

                // Verifica se a inserção falhou
                if (result == -1) {
                    throw new Exception("Falha ao inserir dados na linha " + i);
                }
                else {
                    System.out.println("success to insert all data");
                }
            }
            database.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            database.endTransaction();
        }
        return true;
    }

        public int check_id(String prod_id, long quantidade, Context context) {
        String[] columns = {"product_id", "quantidade"};
        String[] param = {prod_id};
        Cursor cursor = database.query(defaultDB, columns, "product_id = ?", param, null, null, null);
        if (cursor == null) { cursor.close(); return -1; }

        System.out.println("product " + prod_id);

        if (cursor.getCount() > 0) {
            System.out.println("Cursor count " + cursor.getCount());
            int ret = 1;
            System.out.println("igual");
            cursor.close();
            ContentValues values = new ContentValues();
            values.put("quantidade", "quantidade + " + quantidade); // Aqui, estamos construindo a expressão aritmética

            String selection = "product_id = ?";
            String[] selectionArgs = { String.valueOf(prod_id) };

            String sql = "UPDATE " + defaultDB + " SET quantidade = quantidade + ? WHERE product_id = ?";

            try {
                database.execSQL(sql, new Object[]{quantidade, prod_id});
            }
            catch (Exception e) {
                System.out.println("error " + e);
                ret = 0;
            }

            Toast.makeText(context, message_update_toast[ret], mode_toaste[ret]).show();
            return ret;
        }
        return 0;

    }

    public long addData(String prod_id, long quantidade) {
        ContentValues valores = new ContentValues();
        valores.put("product_id", prod_id);
        valores.put("quantidade", quantidade);
        long resulado = database.insert(defaultDB, null, valores);
        return resulado;
    }

    // public long UpdateDataQuant(String prod_id, long quantidade) {

    //}

    public Cursor fetchAllDataCursor() {
        return database.rawQuery("Select * from " + defaultDB, null);
    }

    public Cursor fetchDataCursor(final String currentCursor, final int limit) {
        return database.rawQuery("Select * from " + defaultDB + " LIMIT ? OFFSET ?", new String[]{String.valueOf(limit), currentCursor});
    }

    public int updateData(String oldProduct_id, String newProductId, long newQuantidade) {
        ContentValues Valores = new ContentValues();
        Valores.put("product_id", oldProduct_id);
        Cursor cursor = database.rawQuery("Select * from " + defaultDB + " where product_id = ?", new String[] {oldProduct_id});
        if (cursor.getCount() > 0) {
            Valores.put("product_id", String.valueOf(newProductId));
            Valores.put("quantidade", newQuantidade);

            try {
                // Atualiza o banco de dados
                System.out.println("Valores = " + Valores);
                int ret = database.update(defaultDB, Valores, "product_id=?", new String[]{oldProduct_id});
                cursor.close();
                return ret;
            }
            catch (Exception e) {
                System.out.println("error " + e);
                cursor.close();
                return -1;
            }
        }
        cursor.close();
        return  -1;
    }

    public boolean deleteAllData(final String databaseName) {
        try {
            database.execSQL("DROP TABLE IF EXISTS " + databaseName);  // Deleta a tabela
        }
        catch (Exception e) {
            return false;
        }
        return true;
    }

    public int deleteData(String product_id) {
        return database.delete(defaultDB, "product_id = ?", new String[]{String.valueOf(product_id)});
    }

    public boolean changeDbName(final String Table, final String newTable) {
        try {
            database.execSQL("ALTER TABLE " + Table + " RENAME TO " + newTable);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}

package com.example.softvoicescanner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;

import android.content.pm.PackageManager;
import android.Manifest;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelDB {

    private final Context context;

    public ExcelDB(Context context) {
        this.context = context;
    }

    public void GenerateExcel(ArrayList<String> data, android.net.Uri uri) {

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Dados do Banco");

        // Obtenha os dados do SQLite

        // Criar a linha de cabeçalhos
        Row headerRow = sheet.createRow(0);
        Cell headerCell1 = headerRow.createCell(0);
        headerCell1.setCellValue("Product ID");
        Cell headerCell2 = headerRow.createCell(1);
        headerCell2.setCellValue("Quantity");

        // Preencher o Excel com os dados
        int rowCount = 1; // Começar a partir da segunda linha, pois a primeira é o cabeçalho
        for (int index = 0; index < data.size(); index += 2) {
            Row row = sheet.createRow(rowCount++);
            Cell cell1 = row.createCell(0);
            cell1.setCellValue(data.get(index));  // Coloque o valor do produto
            Cell cell2 = row.createCell(1);
            cell2.setCellValue(data.get(index+1));   // Coloque o valor da quantidade
        }

        try {
        // Criar o arquivo Excel no armazenamento externo
        DocumentFile pickedDir = DocumentFile.fromTreeUri(context, uri);
        DocumentFile file = pickedDir.createFile("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "DadosBanco.xlsx");

            OutputStream outputStream = context.getContentResolver().openOutputStream(file.getUri());
            workbook.write(outputStream);
            workbook.close();
            Toast.makeText(context, "Arquivo Excel criado com sucesso!", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Erro ao criar o arquivo Excel!", Toast.LENGTH_LONG).show();
        }
    }

}

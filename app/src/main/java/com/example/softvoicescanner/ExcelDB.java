package com.example.softvoicescanner;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import android.content.Context;
import android.widget.Toast;
import androidx.documentfile.provider.DocumentFile;


import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelDB {

    private final Context context;

    private short statusExcel;

    public String FileName;

    public ExcelDB(Context context) {
        this.context = context;
    }

    public void GenerateExcel(ArrayList<String> data, android.net.Uri uri) {

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Banco de dados");

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

            System.out.println("path " + uri);

        DocumentFile pickedDir = DocumentFile.fromTreeUri(context, uri);
        DocumentFile file = pickedDir.createFile("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", FileName + ".xlsx");

            OutputStream outputStream = context.getContentResolver().openOutputStream(file.getUri());
            workbook.write(outputStream);
            workbook.close();
            statusExcel = 1;
        } catch (IOException e) {
            e.printStackTrace();
            statusExcel = 0;
        }
    }

    public void checkExcel() {
        String[] message = {"Erro ao criar o arquivo Excel!", "Arquivo Excel criado com sucesso!"};
        Toast.makeText(context, message[statusExcel], Toast.LENGTH_LONG).show();
    }

}

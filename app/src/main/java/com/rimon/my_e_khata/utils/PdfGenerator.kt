package com.rimon.my_e_khata.utils

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import com.rimon.my_e_khata.data.model.Customer
import com.rimon.my_e_khata.data.model.Supplier
import com.rimon.my_e_khata.data.model.Transaction
import java.io.File
import java.io.FileOutputStream

object PdfGenerator {

    private val titleFont = Font(Font.FontFamily.HELVETICA, 18f, Font.BOLD, BaseColor(98, 0, 238))
    private val headerFont = Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD, BaseColor.WHITE)
    private val bodyFont = Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL, BaseColor(33, 33, 33))
    private val amountGreenFont = Font(Font.FontFamily.HELVETICA, 10f, Font.BOLD, BaseColor(0, 150, 100))
    private val amountRedFont = Font(Font.FontFamily.HELVETICA, 10f, Font.BOLD, BaseColor(211, 47, 47))
    private val headerBg = BaseColor(98, 0, 238)

    fun generateCustomerReport(
        context: Context,
        customer: Customer,
        transactions: List<Transaction>
    ): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir
        val file = File(dir, "report_${customer.name}_${System.currentTimeMillis()}.pdf")

        val document = Document(PageSize.A4, 40f, 40f, 40f, 40f)
        PdfWriter.getInstance(document, FileOutputStream(file))
        document.open()

        val prefs = AppPreferences.getInstance(context)
        val businessName = prefs.businessName

        // Title
        document.add(Paragraph(businessName, titleFont).apply { alignment = Element.ALIGN_CENTER })
        document.add(Paragraph("Customer Account Statement", bodyFont).apply { alignment = Element.ALIGN_CENTER })
        document.add(Paragraph("Customer: ${customer.name}", bodyFont))
        if (customer.mobile.isNotBlank()) document.add(Paragraph("Mobile: ${customer.mobile}", bodyFont))
        document.add(Paragraph("Generated: ${FormatUtils.formatDateShort(System.currentTimeMillis())}", bodyFont))
        document.add(Chunk.NEWLINE)

        // Balance summary
        val balanceColor = if (customer.balance >= 0) BaseColor(0, 150, 100) else BaseColor(211, 47, 47)
        val balanceText = if (customer.balance >= 0) "You will get: ৳ ${customer.balance}" else "You will give: ৳ ${Math.abs(customer.balance)}"
        document.add(Paragraph(balanceText, Font(Font.FontFamily.HELVETICA, 14f, Font.BOLD, balanceColor)))
        document.add(Chunk.NEWLINE)

        // Transaction table
        val table = PdfPTable(4)
        table.widthPercentage = 100f
        table.setWidths(floatArrayOf(3f, 2f, 2f, 2f))

        // Header
        listOf("Date", "You Gave", "You Got", "Balance").forEach { header ->
            val cell = PdfPCell(Phrase(header, headerFont))
            cell.backgroundColor = headerBg
            cell.horizontalAlignment = Element.ALIGN_CENTER
            cell.paddingTop = 6f
            cell.paddingBottom = 6f
            table.addCell(cell)
        }

        // Rows
        transactions.forEach { tx ->
            table.addCell(PdfPCell(Phrase(FormatUtils.formatDate(tx.createdAt), bodyFont)).apply {
                paddingTop = 4f; paddingBottom = 4f
            })
            val gaveText = if (tx.type == "gave") "৳ ${tx.amount}" else ""
            val gotText = if (tx.type == "got") "৳ ${tx.amount}" else ""
            table.addCell(PdfPCell(Phrase(gaveText, amountRedFont)).apply { horizontalAlignment = Element.ALIGN_RIGHT })
            table.addCell(PdfPCell(Phrase(gotText, amountGreenFont)).apply { horizontalAlignment = Element.ALIGN_RIGHT })
            table.addCell(PdfPCell(Phrase("৳ ${tx.balance}", bodyFont)).apply { horizontalAlignment = Element.ALIGN_RIGHT })
        }

        document.add(table)
        document.close()
        return file
    }

    fun generateSupplierReport(
        context: Context,
        supplier: Supplier,
        transactions: List<Transaction>
    ): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir
        val file = File(dir, "report_${supplier.name}_${System.currentTimeMillis()}.pdf")

        val document = Document(PageSize.A4, 40f, 40f, 40f, 40f)
        PdfWriter.getInstance(document, FileOutputStream(file))
        document.open()

        val prefs = AppPreferences.getInstance(context)
        val businessName = prefs.businessName

        document.add(Paragraph(businessName, titleFont).apply { alignment = Element.ALIGN_CENTER })
        document.add(Paragraph("Supplier Account Statement", bodyFont).apply { alignment = Element.ALIGN_CENTER })
        document.add(Paragraph("Supplier: ${supplier.name}", bodyFont))
        if (supplier.mobile.isNotBlank()) document.add(Paragraph("Mobile: ${supplier.mobile}", bodyFont))
        document.add(Paragraph("Generated: ${FormatUtils.formatDateShort(System.currentTimeMillis())}", bodyFont))
        document.add(Chunk.NEWLINE)

        val balanceText = if (supplier.balance >= 0) "You will give: ৳ ${supplier.balance}" else "You will get: ৳ ${Math.abs(supplier.balance)}"
        document.add(Paragraph(balanceText, Font(Font.FontFamily.HELVETICA, 14f, Font.BOLD, BaseColor(211, 47, 47))))
        document.add(Chunk.NEWLINE)

        val table = PdfPTable(4)
        table.widthPercentage = 100f
        table.setWidths(floatArrayOf(3f, 2f, 2f, 2f))

        listOf("Date", "You Gave", "You Got", "Balance").forEach { header ->
            val cell = PdfPCell(Phrase(header, headerFont))
            cell.backgroundColor = headerBg
            cell.horizontalAlignment = Element.ALIGN_CENTER
            cell.paddingTop = 6f; cell.paddingBottom = 6f
            table.addCell(cell)
        }

        transactions.forEach { tx ->
            table.addCell(PdfPCell(Phrase(FormatUtils.formatDate(tx.createdAt), bodyFont)).apply {
                paddingTop = 4f; paddingBottom = 4f
            })
            val gaveText = if (tx.type == "gave") "৳ ${tx.amount}" else ""
            val gotText = if (tx.type == "got") "৳ ${tx.amount}" else ""
            table.addCell(PdfPCell(Phrase(gaveText, amountRedFont)).apply { horizontalAlignment = Element.ALIGN_RIGHT })
            table.addCell(PdfPCell(Phrase(gotText, amountGreenFont)).apply { horizontalAlignment = Element.ALIGN_RIGHT })
            table.addCell(PdfPCell(Phrase("৳ ${tx.balance}", bodyFont)).apply { horizontalAlignment = Element.ALIGN_RIGHT })
        }

        document.add(table)
        document.close()
        return file
    }

    fun shareFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Report"))
    }
}

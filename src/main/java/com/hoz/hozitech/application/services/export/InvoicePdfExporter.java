package com.hoz.hozitech.application.services.export;

import com.hoz.hozitech.application.repositories.OrderRepository;
import com.hoz.hozitech.domain.entities.Order;
import com.hoz.hozitech.domain.entities.OrderItem;
import com.hoz.hozitech.domain.enums.PaymentStatus;
import com.hoz.hozitech.web.exceptions.ExportException;
import com.hoz.hozitech.web.exceptions.ResourceNotFoundException;
import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.UUID;

import static com.hoz.hozitech.application.services.export.ExportHelpers.*;

/**
 * Generates PDF invoices for individual orders.
 */
@Component
@RequiredArgsConstructor
public class InvoicePdfExporter {

    private final OrderRepository orderRepository;

    public byte[] exportOrderInvoicePdf(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(document, out);
            document.open();

            BaseFont baseFont = loadVietnameseFont();
            Font titleFont = new Font(baseFont, 22, Font.BOLD, PRIMARY_COLOR);
            Font headerFont = new Font(baseFont, 11, Font.BOLD, Color.WHITE);
            Font normalFont = new Font(baseFont, 10, Font.NORMAL, Color.BLACK);
            Font boldFont = new Font(baseFont, 10, Font.BOLD, Color.BLACK);
            Font smallFont = new Font(baseFont, 9, Font.NORMAL, Color.GRAY);
            Font bigBoldFont = new Font(baseFont, 13, Font.BOLD, PRIMARY_COLOR);

            // ═══ HEADER: Shop info + Invoice title ═══
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{55, 45});

            PdfPCell shopCell = new PdfPCell();
            shopCell.setBorder(Rectangle.NO_BORDER);
            shopCell.setPaddingBottom(15);
            Paragraph shopName = new Paragraph("HoziTech", titleFont);
            shopCell.addElement(shopName);
            shopCell.addElement(new Paragraph("123 Đường Công Nghệ, Quận IT, TP.HCM", smallFont));
            shopCell.addElement(new Paragraph("SĐT: 0123.456.789 | Email: contact@hozitech.com", smallFont));
            headerTable.addCell(shopCell);

            PdfPCell invoiceCell = new PdfPCell();
            invoiceCell.setBorder(Rectangle.NO_BORDER);
            invoiceCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            invoiceCell.setPaddingBottom(15);
            Paragraph invoiceLabel = new Paragraph("HÓA ĐƠN", new Font(baseFont, 20, Font.BOLD, PRIMARY_COLOR));
            invoiceLabel.setAlignment(Element.ALIGN_RIGHT);
            invoiceCell.addElement(invoiceLabel);
            Paragraph orderNum = new Paragraph("#" + order.getOrderNumber(), new Font(baseFont, 14, Font.BOLD, Color.BLACK));
            orderNum.setAlignment(Element.ALIGN_RIGHT);
            invoiceCell.addElement(orderNum);
            String dateStr = order.getCreatedAt() != null ? order.getCreatedAt().format(DATE_FMT) : "";
            Paragraph dateP = new Paragraph("Ngày: " + dateStr, smallFont);
            dateP.setAlignment(Element.ALIGN_RIGHT);
            invoiceCell.addElement(dateP);
            headerTable.addCell(invoiceCell);

            document.add(headerTable);

            // ═══ SEPARATOR LINE ═══
            PdfPTable line = new PdfPTable(1);
            line.setWidthPercentage(100);
            PdfPCell lineCell = new PdfPCell();
            lineCell.setBorder(Rectangle.BOTTOM);
            lineCell.setBorderColor(new Color(200, 200, 200));
            lineCell.setBorderWidth(1f);
            lineCell.setFixedHeight(5);
            line.addCell(lineCell);
            document.add(line);
            document.add(new Paragraph(" "));

            // ═══ CUSTOMER + PAYMENT INFO ═══
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[]{50, 50});

            PdfPCell custCell = new PdfPCell();
            custCell.setBorder(Rectangle.NO_BORDER);
            custCell.setPaddingBottom(10);
            custCell.addElement(new Paragraph("KHÁCH HÀNG", new Font(baseFont, 9, Font.BOLD, Color.GRAY)));
            String custName = order.getUser() != null ? order.getUser().getFullName() : "N/A";
            custCell.addElement(new Paragraph(custName, boldFont));
            String address = parseAddress(order.getShippingAddressJson());
            custCell.addElement(new Paragraph(address, normalFont));
            infoTable.addCell(custCell);

            PdfPCell payCell = new PdfPCell();
            payCell.setBorder(Rectangle.NO_BORDER);
            payCell.setPaddingBottom(10);
            payCell.addElement(new Paragraph("THANH TOÁN", new Font(baseFont, 9, Font.BOLD, Color.GRAY)));
            payCell.addElement(new Paragraph(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : "N/A", boldFont));
            String payStatus = order.getPaymentStatus() == PaymentStatus.COMPLETED ? "Đã thanh toán ✓" : "Chưa thanh toán";
            Font payStatusFont = new Font(baseFont, 10, Font.BOLD,
                    order.getPaymentStatus() == PaymentStatus.COMPLETED ? new Color(5, 150, 105) : new Color(220, 38, 38));
            payCell.addElement(new Paragraph(payStatus, payStatusFont));
            infoTable.addCell(payCell);

            document.add(infoTable);
            document.add(new Paragraph(" "));

            // ═══ ORDER ITEMS TABLE ═══
            PdfPTable itemTable = new PdfPTable(5);
            itemTable.setWidthPercentage(100);
            itemTable.setWidths(new float[]{8, 37, 15, 15, 25});

            String[] itemHeaders = {"#", "Sản phẩm", "Đơn giá", "SL", "Thành tiền"};
            for (String h : itemHeaders) {
                PdfPCell hCell = new PdfPCell(new Phrase(h, headerFont));
                hCell.setBackgroundColor(PRIMARY_COLOR);
                hCell.setPadding(8);
                hCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                itemTable.addCell(hCell);
            }

            Color altRowColor = new Color(248, 250, 252);
            int idx = 1;
            for (OrderItem item : order.getOrderItems()) {
                Color bgColor = idx % 2 == 0 ? altRowColor : Color.WHITE;

                PdfPCell c1 = new PdfPCell(new Phrase(String.valueOf(idx), normalFont));
                c1.setPadding(6); c1.setBackgroundColor(bgColor); c1.setHorizontalAlignment(Element.ALIGN_CENTER);
                itemTable.addCell(c1);

                String name = item.getProductName();
                if (item.getVariantName() != null && !item.getVariantName().isEmpty()) {
                    name += "\n(" + item.getVariantName() + ")";
                }
                PdfPCell c2 = new PdfPCell(new Phrase(name, normalFont));
                c2.setPadding(6); c2.setBackgroundColor(bgColor);
                itemTable.addCell(c2);

                PdfPCell c3 = new PdfPCell(new Phrase(formatMoney(item.getUnitPrice()), normalFont));
                c3.setPadding(6); c3.setBackgroundColor(bgColor); c3.setHorizontalAlignment(Element.ALIGN_RIGHT);
                itemTable.addCell(c3);

                PdfPCell c4 = new PdfPCell(new Phrase(String.valueOf(item.getQuantity()), normalFont));
                c4.setPadding(6); c4.setBackgroundColor(bgColor); c4.setHorizontalAlignment(Element.ALIGN_CENTER);
                itemTable.addCell(c4);

                PdfPCell c5 = new PdfPCell(new Phrase(formatMoney(item.getSubtotal()), boldFont));
                c5.setPadding(6); c5.setBackgroundColor(bgColor); c5.setHorizontalAlignment(Element.ALIGN_RIGHT);
                itemTable.addCell(c5);

                idx++;
            }

            document.add(itemTable);

            // ═══ TOTALS ═══
            document.add(new Paragraph(" "));
            PdfPTable totalsTable = new PdfPTable(2);
            totalsTable.setWidthPercentage(50);
            totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

            addTotalRow(totalsTable, "Tạm tính:", formatMoney(order.getSubtotal()), normalFont, normalFont);
            if (order.getDiscountAmount() != null && order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                Font greenFont = new Font(baseFont, 10, Font.BOLD, new Color(5, 150, 105));
                addTotalRow(totalsTable, "Giảm giá:", "-" + formatMoney(order.getDiscountAmount()), normalFont, greenFont);
            }
            addTotalRow(totalsTable, "Phí ship:", formatMoney(order.getShippingFee()), normalFont, normalFont);

            PdfPCell sepCell = new PdfPCell();
            sepCell.setBorder(Rectangle.BOTTOM);
            sepCell.setBorderColor(Color.LIGHT_GRAY);
            sepCell.setFixedHeight(2);
            sepCell.setColspan(2);
            totalsTable.addCell(sepCell);

            addTotalRow(totalsTable, "TỔNG CỘNG:", formatMoney(order.getTotalAmount()), bigBoldFont, bigBoldFont);

            document.add(totalsTable);

            // ═══ SIGNATURES ═══
            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));

            PdfPTable sigTable = new PdfPTable(2);
            sigTable.setWidthPercentage(80);
            sigTable.setHorizontalAlignment(Element.ALIGN_CENTER);

            PdfPCell sigLeft = new PdfPCell();
            sigLeft.setBorder(Rectangle.NO_BORDER);
            sigLeft.setHorizontalAlignment(Element.ALIGN_CENTER);
            sigLeft.addElement(createCenteredParagraph("NGƯỜI MUA HÀNG", boldFont));
            sigLeft.addElement(createCenteredParagraph("(Ký, ghi rõ họ tên)", smallFont));
            sigTable.addCell(sigLeft);

            PdfPCell sigRight = new PdfPCell();
            sigRight.setBorder(Rectangle.NO_BORDER);
            sigRight.setHorizontalAlignment(Element.ALIGN_CENTER);
            sigRight.addElement(createCenteredParagraph("NGƯỜI BÁN HÀNG", boldFont));
            sigRight.addElement(createCenteredParagraph("(Ký, đóng dấu)", smallFont));
            sigTable.addCell(sigRight);

            document.add(sigTable);

            // ═══ FOOTER ═══
            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));
            Paragraph thankYou = new Paragraph("Cảm ơn quý khách đã mua hàng tại HoziTech!", smallFont);
            thankYou.setAlignment(Element.ALIGN_CENTER);
            document.add(thankYou);
            Paragraph contact = new Paragraph("Hotline: 0123.456.789 | Website: www.hozitech.com", smallFont);
            contact.setAlignment(Element.ALIGN_CENTER);
            document.add(contact);

            document.close();
            return out.toByteArray();

        } catch (ExportException e) {
            throw e;
        } catch (Exception e) {
            throw new ExportException("Failed to generate invoice PDF", e);
        }
    }
}

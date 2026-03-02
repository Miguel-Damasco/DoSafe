package com.miguel_damasco.DoSafe.document.infraestructure.conversion;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;

@Component
public class PdfDocumentConverter implements DocumentConverter {
    
    @Override
    public ConvertedDocument convertToPdf(InputStream input, String originalFilename, String contentType) {
        
        try {
            if ("application/pdf".equalsIgnoreCase(contentType)) {
                byte[] bytes = input.readAllBytes();
                return new ConvertedDocument(
                        new ByteArrayInputStream(bytes),
                        bytes.length,
                        "application/pdf"
                );
            }

            if ("multipart/form-data".equalsIgnoreCase(contentType)) {
                return imageToPdf(input);
            }

            throw new IllegalArgumentException(
                    "Unsupported content type: " + contentType
            );

        } catch (IOException e) {
            throw new RuntimeException("Error converting document to PDF", e);
        }
    }

    private ConvertedDocument imageToPdf(InputStream input) throws IOException {

        BufferedImage image = ImageIO.read(input);

        PDDocument pdf = new PDDocument();
        PDPage page = new PDPage(
                new PDRectangle(image.getWidth(), image.getHeight())
        );

        pdf.addPage(page);

        PDImageXObject pdImage =
                LosslessFactory.createFromImage(pdf, image);

        PDPageContentStream contentStream =
                new PDPageContentStream(pdf, page);

        contentStream.drawImage(
                pdImage,
                0,
                0,
                image.getWidth(),
                image.getHeight()
        );

        contentStream.close();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pdf.save(out);
        pdf.close();

        byte[] bytes = out.toByteArray();

        return new ConvertedDocument(
                new ByteArrayInputStream(bytes),
                bytes.length,
                "application/pdf"
        );
    }
}

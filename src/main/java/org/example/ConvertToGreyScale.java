package org.example;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdfwriter.ContentStreamWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.graphics.color.PDColorSpace;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceGray;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class ConvertToGreyScale {
    public static final String BASE_DIR = "/Users/subratag/Documents/git/be/TestProject1/src/main/resources/input";

    public void convertPDFToGreyScaleUsingPDFBox() {
        // Load PDF from Base Directory using PDFBox
        try {
            File file = new File(BASE_DIR + "/BSS-232261.pdf");
            PDDocument document = Loader.loadPDF(file);
            System.out.println("PDF loaded");

            int pageCount = document.getPages().getCount();
            for (int i = 0; i < pageCount; i++) {
                PDPage currentPage = document.getPage(i);
                PDFStreamParser parser = new PDFStreamParser(currentPage);
                // loop through all the tokens in the stream
                List<Object> pageTokens = parser.parse();
                System.out.println("pageTokens size: " + pageTokens.size());
                List<Object> editedPageTokens = new ArrayList<>();
                for (int counter = 0; counter < pageTokens.size(); counter++) {
                    Object token = pageTokens.get(counter);
                    if (token instanceof Operator) {
                        //TODO Need to check cs token to be removed or not

//                        if (((Operator) token).getName().equals("cs") || ((Operator) token).getName().equals("CS")) {
//                            System.out.println("Color Space: " + ((Operator) token).getName());
//                            System.out.println("Previous Token value of  Color Space " + (counter -1) + " " + pageTokens.get(counter-1));
//                            //System.out.println("Token value counter " + (counter -1) + " " + pageTokens.get(counter-1));
//                            editedPageTokens.remove(editedPageTokens.size() - 1);
//                            editedPageTokens.add(COSName.getPDFName("DeviceGray"));
//                        }
                        if (((Operator) token).getName().equals("k")) {
                            System.out.println("Color Operator: " + ((Operator) token).getName() + " for token counter: " + counter);
                            convertCMYKToRGBToGrey(pageTokens, editedPageTokens, counter);
                            editedPageTokens.add(Operator.getOperator("g"));
                        } else if (((Operator) token).getName().equals("K")) {
                            System.out.println("Color Operator: " + ((Operator) token).getName() + " for token counter: " + counter);
                            convertCMYKToRGBToGrey(pageTokens, editedPageTokens, counter);
                            editedPageTokens.add(Operator.getOperator("G"));
                        } else {
                            editedPageTokens.add(token);
                        }

                    } else {
                        editedPageTokens.add(token);
                    }

                }

//TODO need to check for images

                for (COSName consName : currentPage.getResources().getXObjectNames()) {
                    System.out.println(consName.getName());
                    if( currentPage.getResources().getXObject(consName) instanceof PDImageXObject) {
                            PDImageXObject pdImageXObject = (PDImageXObject) currentPage.getResources().getXObject(consName);
                        System.out.println("Image color space: "  + pdImageXObject.getColorSpace() );

                        int width =  pdImageXObject.getWidth();
                        int height =  pdImageXObject.getHeight();
                        int xMin = pdImageXObject.getRawRaster().getMinX();
                        int yMin = pdImageXObject.getRawRaster().getMinY();
                        int[] pixels = pdImageXObject.getRawRaster().getPixels(xMin,yMin,width,height, (int[]) null);
                        // convert to grayscale
                        for (int j = 0; j < pixels.length; j++) {

                            // Here i denotes the index of array of pixels
                            // for modifying the pixel value.
                            int p = pixels[j];

                            int a = (p >> 24) & 0xff;
                            int r = (p >> 16) & 0xff;
                            int g = (p >> 8) & 0xff;
                            int b = p & 0xff;

                            // calculate average
                            int avg = (r + g + b) / 3;

                            // replace RGB value with avg
                            p = (a << 24) | (avg << 16) | (avg << 8) | avg;

                            pixels[j] = p;
                        }
                        pdImageXObject.getRawRaster().setPixels(xMin,yMin,width,height,pixels);
                        System.out.println("Image greyscale done");
//
//                        if(!pdImageXObject.getColorSpace().getName().equals("DeviceGray") ){
//
//                            PDColorSpace pdColorSpace = PDDeviceGray.INSTANCE;
//                            pdImageXObject.setColorSpace(pdColorSpace);
//                        }

                    }
                }

                PDStream updatedPageContents = new PDStream(document);

                OutputStream outputStream = updatedPageContents.createOutputStream(COSName.FLATE_DECODE);
                ContentStreamWriter contentWriter = new ContentStreamWriter(outputStream);
                contentWriter.writeTokens(editedPageTokens);
                currentPage.setContents(updatedPageContents);
                outputStream.close();
            }
            document.save(BASE_DIR + "/Updated_BSS-232261.pdf");
            // Remember to close the PDF document
            document.close();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void convertCMYKToRGBToGrey(List<Object> pageTokens, List<Object> editedPageTokens, int counter) {
        Object o1 = pageTokens.get(counter - 4);
        Object o2 = pageTokens.get(counter - 3);
        Object o3 = pageTokens.get(counter - 2);
        Object o4 = pageTokens.get(counter - 1);
        System.out.println("CMYK values: " + o1 + " " + o2 + " " + o3 + " " + o4);
        float cyan;
        float magenta;
        float yellow;
        float black;

        if (o1 instanceof COSFloat) {
            cyan = ((COSFloat) o1).floatValue();
        } else {
            cyan = ((COSInteger) o1).floatValue();
        }

        if (o2 instanceof COSFloat) {
            magenta = ((COSFloat) o2).floatValue();
        } else {
            magenta = ((COSInteger) o2).floatValue();
        }

        if (o3 instanceof COSFloat) {
            yellow = ((COSFloat) o3).floatValue();
        } else {
            yellow = ((COSInteger) o3).floatValue();
        }

        if (o4 instanceof COSFloat) {
            black = ((COSFloat) o4).floatValue();
        } else {
            black = ((COSInteger) o4).floatValue();
        }

        int red = Math.round(255 * (1 - cyan) * (1 - black));
        int green = Math.round(255 * (1 - magenta) * (1 - black));
        int blue = Math.round(255 * (1 - yellow) * (1 - black));
        System.out.println("RGB values: " + red + " " + green + " " + blue);

        // convert RGB to greyscale
        int grey = Math.round(0.299f * red + 0.587f * green + 0.114f * blue);
        System.out.println("Grey value: " + grey);
        // Add greyscale, grey
        float grey1 = (float) grey / 255;
        BigDecimal bd = new BigDecimal(Float.toString(grey1));
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        grey1 = bd.floatValue();
        System.out.println("Grey1 value: " + grey1);

        // remove CMYK
        editedPageTokens.remove(editedPageTokens.size() - 1);
        editedPageTokens.remove(editedPageTokens.size() - 1);
        editedPageTokens.remove(editedPageTokens.size() - 1);
        editedPageTokens.remove(editedPageTokens.size() - 1);

        // Add greyscale, grey
        editedPageTokens.add(new COSFloat(grey1));
    }
}
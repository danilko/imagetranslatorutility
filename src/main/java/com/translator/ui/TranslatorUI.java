package com.translator.ui;

import com.translator.core.ImageScanner;
import com.translator.core.ImageScannerImpl;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class TranslatorUI {

    public static void main(String[] args) throws AWTException, IOException {

        // Need to figure out a way to fit screenshot into proper image
       // Dimension screenDim = Toolkit.getDefaultToolkit().getScreenSize();
       // Robot robot = new Robot();

       // BufferedImage image = robot.createScreenCapture(new Rectangle(0, 0, (int) screenDim.getWidth(),
       //         (int) screenDim.getHeight()));

       // ImageIO.write(image, "png", new File("capture.png"));

        // Right now need to manually create capture.png
        ImageScanner imageScanner = new ImageScannerImpl();

        System.out.println("Retrieve Text:" + imageScanner.getTextFromImage("capture.png"));

    }

}

package com.translator.core;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.lept;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.tesseract.TessBaseAPI;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;

import java.awt.*;
import java.io.File;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import static org.bytedeco.javacpp.lept.pixDestroy;
import static org.bytedeco.javacpp.lept.pixRead;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgcodecs.cvLoadImage;
import static org.bytedeco.javacpp.opencv_imgcodecs.cvSaveImage;
import static org.bytedeco.javacpp.opencv_imgproc.*;

public class ImageScannerImpl implements ImageScanner {

    // Reference from https://gmartinezgil.wordpress.com/2016/04/16/create-a-receipt-scanner-app-in-java-using-javacv-opencv-and-tesseract-ocr/

    public String getTextFromImage(String imageFilePath) {
        final File imageFile = new File(imageFilePath);
        final String ImageFilePath = imageFile.getAbsolutePath();

        System.out.println(ImageFilePath);

        //  try {


          // IplImage targetImage = cvLoadImage(ImageFilePath);

            //IplImage cannyEdgeImage = applyCannySquireEdgeDetectionOnImage(targetImage, 30);
            //CvSeq largestSqure = findLargestSquareONCannyDetectedImage(cannyEdgeImage);
            //targetImage = applyPerspectveTransformThresholdOnOriginalIMage(targetImage, largestSqure, 30);

            //targetImage = scaleDownImage(targetImage, 30);
            //targetImage = cleanImageSmoothingForOCR(targetImage);

          //  final File cleanImage = new File("cleaned_image.png");

            // Save clean image version
          //  final String cleanedImageFilePath = cleanImage.getAbsolutePath();

          //  cvSaveImage(cleanedImageFilePath, targetImage);

           // System.out.println(cleanedImageFilePath);

            //cvRelease(cannyEdgeImage);
            //cannyEdgeImage = null;
            //cvRelease(targetImage);
           // targetImage = null;

           //return getStringFromImage(cleanedImageFilePath);


      //  }
       // catch (Exception exception)
       // {
       //     exception.printStackTrace();
       //     return null;
       // }

        return getStringFromImage(imageFilePath);
    }

    public String getStringFromImage(final String imageFilePath)
    {
        try {
            final URL tesseractDataResource = getClass().getResource("/");
            final File tesseractFolder = new File(tesseractDataResource.toURI());
            final String tesseractAbsoluteFilePath = tesseractFolder.getAbsolutePath();

            System.out.println(tesseractAbsoluteFilePath);

            BytePointer outText;

            TessBaseAPI api = new TessBaseAPI();
           // api.SetVariable("tessedit_char_whitelist", "0123456789,/ABCDEFGHJKLMNPQRSTUVWXY");

            if (api.Init(tesseractAbsoluteFilePath, "jpn") != 0) {
                System.err.println("Cloud not initialze tesseract");
            }

            // Open input image with loptonica library
            lept.PIX image = pixRead(imageFilePath);
            api.SetImage(image);
            // Get OCR Result
            outText = api.GetUTF8Text();
            String string = outText.getString();
            //Destroy used object and release memory
            api.End();
            outText.deallocate();
            pixDestroy(image);


            return string;

        }
        catch (Exception exception)
        {
            exception.printStackTrace();
            return null;
        }
    }

    /**
     * Resize image
     * @param sourceImage
     * @param percent
     * @return
     */
    private IplImage scaleDownImage(IplImage sourceImage, int percent)
    {
        IplImage destinationImage = cvCreateImage(
                cvSize((sourceImage.width() * percent) / 100,
                        (sourceImage.height() * percent) / 100),
                sourceImage.depth(),
                sourceImage.nChannels());

        cvResize(sourceImage, destinationImage);

        return destinationImage;
    }

    /**
     *
     * @param sourceImage
     * @param percent
     * @return
     */
    private IplImage applyCannySquireEdgeDetectionOnImage(IplImage sourceImage, int percent)
    {
        IplImage destinationImage = scaleDownImage(sourceImage, percent);

        IplImage grayImage = cvCreateImage(cvGetSize(destinationImage), IPL_DEPTH_8U, 1);

        // Convert to gray
        cvCvtColor(destinationImage, grayImage, CV_BGR2GRAY);

        OpenCVFrameConverter.ToMat convertToMat = new OpenCVFrameConverter.ToMat();

        Frame grayImageFrame = convertToMat.convert(grayImage);

        Mat grayImageMat = convertToMat.convert(grayImageFrame);

        GaussianBlur(grayImageMat, grayImageMat, new Size(5, 5), 0.0, 0.0, BORDER_DEFAULT);

        destinationImage = convertToMat.convertToIplImage(grayImageFrame);

        // Clean for better detection
        cvErode(destinationImage, destinationImage);
        cvDilate(destinationImage, destinationImage);

        // apply the canny edge detection method
        cvCanny(destinationImage, destinationImage, 75.0, 200.0);

        File file = new File("canny-detect.png");

        // debug purpose
        cvSaveImage(file.getAbsolutePath(), destinationImage);

        return destinationImage;
    }


    /**
     * Once applied canny edge to the image, we can find the largest square
     * using the find contours (square) method and asking for the largest one that will be the receipt on the image hopefully
     * @param cannyEdgeDtectedImage
     * @return
     */
    private CvSeq findLargestSquareONCannyDetectedImage(IplImage cannyEdgeDtectedImage)
    {
        IplImage foundedContoursImage = cvCloneImage(cannyEdgeDtectedImage);

        CvMemStorage memory = CvMemStorage.create();
        CvSeq contours = new CvSeq();
        cvFindContours(foundedContoursImage,
                memory,
                contours,
                Loader.sizeof(CvContour.class),
                CV_RETR_LIST,
                CV_CHAIN_APPROX_SIMPLE,
                cvPoint(0,0));

        int maxWidth = 0;
        int maxHeight = 0;

        CvRect contour = null;
        CvSeq seqFounded = null;
        CvSeq nextSeq = new CvSeq();

        for(nextSeq = contours; nextSeq != null; nextSeq = nextSeq.h_next()) {
            contour = cvBoundingRect(nextSeq, 0);
            if((contour.width() >= maxWidth)
            && (contour.height() >= maxHeight))
            {
                maxWidth = contour.width();
                maxHeight = contour.height();
                seqFounded = nextSeq;
            }
        }
        CvSeq result = cvApproxPoly(seqFounded, Loader.sizeof(CvContour.class), memory, CV_POLY_APPROX_DP, cvContourPerimeter(seqFounded) * 0.02, 0);



        for(int index = 0; index < result.total(); index ++) {
            CvPoint v = new CvPoint(cvGetSeqElem(result, index));
            cvDrawCircle(foundedContoursImage, v, 5, CvScalar.BLUE, 20, 8, 0);

            System.out.println("Found Point (" + v.x() + ", " + contour.y() + ")");
        }

        File file = new File("find-contours.png");

        cvSaveImage(file.getAbsolutePath(), foundedContoursImage);

        return result;
    }

    private IplImage applyPerspectveTransformThresholdOnOriginalIMage(
            IplImage sourceImage, CvSeq contour, int percent) {
        IplImage wrapImage = cvCloneImage(sourceImage);

        for(int index = 0; index < contour.total(); index++) {
            CvPoint point = new CvPoint(cvGetSeqElem(contour, index));
            point.x((int) (point.x() * 100) / percent);
            point.y((int) (point.y() * 100) / percent);
        }

        CvPoint topRightPoint = new CvPoint(cvGetSeqElem(contour, 0));
        CvPoint topLeftPoint = new CvPoint(cvGetSeqElem(contour, 1));
        CvPoint bottomLeftPoint = new CvPoint(cvGetSeqElem(contour, 2));
        CvPoint bottomRightPoint = new CvPoint(cvGetSeqElem(contour, 3));

        int resultWidth = (int) (bottomLeftPoint.x() - topLeftPoint.x());
        int bottomWidth = (int) (bottomRightPoint.x() - bottomLeftPoint.x());
        if(bottomWidth > resultWidth)
        {
            resultWidth = bottomWidth;
        }

        int resultHeight = (int) (bottomLeftPoint.y() - topLeftPoint.y());
        int bottomHeight = (int) (bottomRightPoint.y() - topRightPoint.y());

        if(bottomHeight > resultHeight)
        {
            resultHeight = bottomHeight;
        }

        float[] sourcePoints = {topLeftPoint.x(), topLeftPoint.y(), topRightPoint.x(), topRightPoint.y(), bottomLeftPoint.x(), bottomLeftPoint.y(), bottomRightPoint.x(), bottomRightPoint.y()};

        float[] destinationPoints = {0,0, resultWidth, 0,0, resultHeight, resultWidth, resultHeight};
        CvMat homography = cvCreateMat(3, 3, CV_32FC1);
        cvGetPerspectiveTransform(sourcePoints, destinationPoints, homography);

        IplImage destImage = cvCloneImage(wrapImage);
        cvWarpPerspective(wrapImage, destImage, homography, CV_INTER_LINEAR, CvScalar.ZERO);

        return cropImage(destImage, 0, 0, resultWidth, resultHeight);

    }

    /**
     * Corps an squire from an image to the new width and height from the 0,0 position
     * @param sourceImage
     * @param fromX
     * @param fromY
     * @param toWidth
     * @param toHeight
     * @return
     */
    private IplImage cropImage(IplImage sourceImage, int fromX, int fromY, int toWidth, int toHeight)
    {
        cvSetImageROI(sourceImage, cvRect(fromX, fromY, toWidth, toHeight));
        IplImage destinationImage = cvCloneImage(sourceImage);
        cvCopy(sourceImage, destinationImage);
        return destinationImage;
    }

    /**
     * Clean an image of noise converting to grey, smoothing and applying Otsu throld to the image and leaving the image with whitebackground balck foreground (letters).
     * @param sourceImage
     * @return
     */
    private IplImage cleanImageSmoothingForOCR(IplImage sourceImage) {
        IplImage destinationImage = cvCreateImage(cvGetSize(sourceImage), IPL_DEPTH_8U, 1);
        cvCvtColor(sourceImage, destinationImage, CV_BGR2GRAY);
        cvSmooth(destinationImage,destinationImage, CV_MEDIAN, 3, 0, 0,0);
        cvThreshold(destinationImage, destinationImage, 0, 255, CV_THRESH_OTSU);

        return destinationImage;
    }

}

package com.yaoling.coinclassifier;


import android.os.Environment;
import android.util.Log;
import android.widget.ImageView;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static android.content.ContentValues.TAG;
import static org.opencv.imgproc.Imgproc.initUndistortRectifyMap;
import static org.opencv.imgproc.Imgproc.rectangle;

public class CircleActivity {

    public void findCircles(Mat input) {
        Mat circles = new Mat();

        // decrease noise to avoid detect the wrong circle
        Imgproc.blur(input, input, new Size(7, 7), new Point(2, 2));
        // use HoughCircle to find circle
        Imgproc.HoughCircles(input, circles, Imgproc.CV_HOUGH_GRADIENT, 2, 50, 100, 90, 20, 200);
        Log.i(TAG, String.valueOf("size: " + circles.cols()) + ", " + String.valueOf(circles.rows()));

        // draw the circle which be detected
        if (circles.cols() > 0) {

            for (int x = 0; x < Math.min(circles.cols(), 5); x++ ) {
                double circleVec[] = circles.get(0, x);

                if (circleVec == null) {
                    break;
                }

                Point center = new Point((int) circleVec[0], (int) circleVec[1]);
                int radius = (int) circleVec[2];

                // draw the center of circle
                Imgproc.circle(input, center, 3, new Scalar(0, 0, 50), 5);
                //draw the contour of circle
                Imgproc.circle(input, center, radius, new Scalar(255, 255, 255), 5);
            }
        }

//        circles.release();
//        input.release();
    }



    public void findEllipses(Mat input) {
        List<MatOfPoint> contours = new ArrayList<>();

        // Pre-processing
        Mat image = new Mat();
        Imgproc.blur(input, image, new Size(5,5 ));
        Imgproc.Canny(image, image, 100, 280);

        Imgproc.findContours(image, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        for( int i = 0; i < contours.size(); i++){
//           Imgproc.drawContours(input,contours, i, new Scalar(0, 100, 255), 2);
        }
        // Find rotated rectangles
        List<RotatedRect> rects = new ArrayList<>();
        for (int i = 0; i < contours.size(); i++) {
            Point[] array = contours.get(i).toArray();
            // Fit ellipse in contours
            if (array.length > 5) {
                RotatedRect rect = Imgproc.fitEllipse(new MatOfPoint2f(array));
                if(checkEllipse(array, rect)){
                    rects.add(rect);
                    Log.i(TAG, "rect x " + rect.center.x);
                    Log.i(TAG, "rect y" + rect.center.y);
                    Log.i(TAG, "rec area: " +rect.size.area());
                }
            }
        }
        Log.i(TAG, "Contour count " + contours.size());
        Log.i(TAG, "Ellipse count " + rects.size());

        int i = 0;
        List<Rect> ROIs = new ArrayList<>();

        for (RotatedRect rect: rects) {
            i++;
            Point[] pts = new Point[4];
            rect.points(pts);
            Imgproc.rectangle(input, pts[1], pts[3], new Scalar(255, 0, 0, 0), 2);
            Imgproc.ellipse(input, rect, new Scalar(255, 255, 255), 2);

            int startx = (int) (rect.center.x - 0.5 * rect.size.width);
            int starty = (int) (rect.center.y - 0.5 * rect.size.height);
            int width = (int) rect.size.width;
            int height = (int) rect.size.height;
            Rect area=new Rect(startx, starty, width , height);
//            Mat ROI = new Mat(input, area);
            ROIs.add(area);
//            Imgcodecs.imwrite(inPath +"/ROI/" + i + ".jpg", ROI);
        }
        ROIs.size();

        Iterator<Rect> ROIiterator = ROIs.iterator();
        while (ROIiterator.hasNext()){
            Rect rectFilt = ROIiterator.next();
            for(Rect s : ROIs){
                if(Math.abs(s.x - rectFilt.x ) < 50 && Math.abs(s.x - rectFilt.x ) > 0 &&
                        Math.abs(s.y - rectFilt.y) < 50 && Math.abs(s.y - rectFilt.y) > 0 ){
                    ROIiterator.remove();
                }
            }
        }
        ROIs.size();
        String inPath = getInnerSDCardPath();
        for (int k = 0; k <ROIs.size(); k++){
            Imgcodecs.imwrite(inPath + "/ROI/" + k +".jpg",new Mat(input, ROIs.get(k)));
        }
    }

    private boolean checkEllipse(Point[] array, RotatedRect rect){

        double threshold_error = 20.0;
        double threshould_ratio_min = 0.85;
        double threshould_ratio_max = 1.15;
        double threshold_area_min = 70;
        double threshold_area_max = 297;


        return (
                calculateError(array, rect) < threshold_error ) &&
                calculateRatioofAxis(rect) > threshould_ratio_min &&
                calculateRatioofAxis(rect) < threshould_ratio_max &&
                calculateArea(rect) > threshold_area_min &&
                (calculateArea(rect) < threshold_area_max)
                ;
    }


    private  double calculateArea(RotatedRect rect){
        double ellipseArea;
        ellipseArea = rect.size.area();
        return  ellipseArea / 1000 / 1.27;
    }

    private double calculateRatioofAxis( RotatedRect rect){
        double ratio = 0;
        ratio = Math.abs((rect.size.width / 2 ) / (rect.size.height / 2));
        return ratio;
    }

    private double calculateError(Point[] array, RotatedRect rect){
        double err = 0;
        for (Point point: array) {
            double f = (Math.pow(point.x - rect.center.x, 2) / Math.pow(rect.size.width / 2, 2)) +
                    (Math.pow(point.y - rect.center.y, 2) / Math.pow(rect.size.height / 2, 2));
            err = Math.pow(Math.abs(1.0 - f)  * 10, 2);
        }

        err = err / array.length;
        return err;
    }

    public String getInnerSDCardPath() {
        return Environment.getExternalStorageDirectory().getPath();
    }

}

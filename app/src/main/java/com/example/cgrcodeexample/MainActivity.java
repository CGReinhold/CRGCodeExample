package com.example.cgrcodeexample;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.opencv.android.JavaCameraView;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements CvCameraViewListener2 {

    // Used for logging success or failure messages
    private static final String TAG = "OCVSample::Activity";

    // Loads camera view of OpenCV for us to use. This lets us see using OpenCV
    private CameraBridgeViewBase mOpenCvCameraView;

    // These variables are used (at the moment) to fix camera orientation from 270degree to 0degree
    Mat mRgba;
    Mat mGray;
    Mat mOuterShape;
    Mat mInnerShape;
    Mat mClean;
    String textoDecode = "";

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                Log.i(TAG, "OpenCV loaded successfully");
                mOpenCvCameraView.enableView();
            } else {
                super.onManagerConnected(status);
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.show_camera);

        mOpenCvCameraView = (JavaCameraView) findViewById(R.id.show_camera_activity_java_surface_view);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);

        mOpenCvCameraView.setMaxFrameSize(1000, 800);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC4);
    }

    public void onCameraViewStopped() {
        mGray.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        Rect boundBox = Imgproc.boundingRect(mGray);
        int centerImageX = (boundBox.width + boundBox.x) / 2;
        int centerImageY = (boundBox.height + boundBox.y) / 2;

        int quarter = boundBox.width / 4;

//        Imgproc.GaussianBlur(mGray, mGray, new Size(5, 5), 0);
        Imgproc.threshold(mGray, mGray, 0, 255, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);

        Mat mask = new Mat(mGray.rows(), mGray.cols(), mGray.type(), Scalar.all(0));
        Imgproc.rectangle(mask, new Rect(centerImageX - quarter, centerImageY - quarter, quarter * 2, quarter * 2), new Scalar(255, 255, 255), -1);

        Mat cropped = new Mat();
        mGray.copyTo(cropped, mask);
        mGray = cropped;
        Core.bitwise_not(mGray, mGray);

        //Drawing the part that the code will validate
        Imgproc.rectangle(mRgba, new Point(centerImageX - 220, centerImageY - 220), new Point(centerImageX + 220, centerImageY + 220), new Scalar(0, 0, 0), 3);

        ArrayList<MatOfPoint> allContours = new ArrayList<>();
        Mat allHierarchy = new Mat();

//        Imgproc.GaussianBlur(mGray, mGray, new Size(5, 5), 0);
        Imgproc.threshold(mGray, mGray, 0, 255, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);

//        return mGray;
        Imgproc.findContours(mGray, allContours, allHierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        Imgproc.dilate(mGray, mGray, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(11, 11)), new Point(-1, -1), 5);
        Imgproc.morphologyEx(mGray, mGray, Imgproc.MORPH_CLOSE, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(25, 25)));

        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(mGray, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        //Drawing the outer contour
        mOuterShape = Mat.zeros(mGray.size(), 0);
        mInnerShape = Mat.zeros(mGray.size(), 0);
        mClean = Mat.zeros(mGray.size(), 0);

        Imgproc.drawContours(mOuterShape, contours, -1, new Scalar(255, 255, 255), -1);


        Imgproc.drawContours(mInnerShape, contours, -1, new Scalar(255, 255, 255), -1);
        Imgproc.erode(mInnerShape, mInnerShape, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(21, 21)), new Point(-1, -1), 5);

//        Imgproc.drawContours(mInnerShape, allContours, -1, new Scalar(0, 0, 0), -1);
//        return mInnerShape;

        //Drawing the inner contours
        if (allContours.size() > 0) {
            ArrayList<MatOfPoint> newAllContours = new ArrayList<>();
            ArrayList<Integer> newAllContoursPoints = new ArrayList<>();
            ArrayList<Integer> paisVazados = new ArrayList<>();
            ArrayList<Integer> filhosVazados = new ArrayList<>();
            int pontoInicial = -1;
            int i = 0;
            //Get list of contours to filter only those that are inside the outerShape and outside the innerShape
            for (MatOfPoint contour : allContours) {
                Point center = getCenterContour(contour);

                if (mOuterShape != null && mInnerShape != null) {
                    double[] pixelOuter = mOuterShape.get((int) center.y, (int) center.x);
                    double[] pixelInner = mInnerShape.get((int) center.y, (int) center.x);
                    if (pixelOuter != null && pixelInner != null) {
                        if (pixelOuter.length > 0 && pixelOuter[0] == 255 && pixelInner.length > 0 && pixelInner[0] == 0) {
                            if (allHierarchy.get(0, i)[3] != -1) {
                                int pai = (int)allHierarchy.get(0, i)[3];
                                if (filhosVazados.contains(pai)) {
                                    pontoInicial = (int)allHierarchy.get(0, pai)[3];
                                } else {
                                    paisVazados.add(pai);
                                    filhosVazados.add(i);
                                }
                            } else {
                                newAllContours.add(contour);
                                newAllContoursPoints.add(i);
                            }
                        }
                    }
                }
                i++;
            }

            if (newAllContours.size() > 0) {
                Imgproc.drawContours(mRgba, newAllContours, -1, new Scalar(0, 255, 0), -1);
                Imgproc.drawContours(mClean, newAllContours, -1, new Scalar(255, 255, 255), -1);
            }

            if (pontoInicial != -1 && newAllContours.size() > 28) {
                ArrayList<PointValue> pointsAndValues = new ArrayList<>();

                for (int contourIdx = 0; contourIdx < allContours.size(); contourIdx++) {
                    if (newAllContoursPoints.contains(contourIdx)) {
                        // Minimum size allowed for consideration
                        MatOfPoint2f approxCurve = new MatOfPoint2f();
                        MatOfPoint2f contour2f = new MatOfPoint2f(allContours.get(contourIdx).toArray());
                        //Processing on mMOP2f1 which is in type MatOfPoint2f
                        double approxDistance = Imgproc.arcLength(contour2f, true) * 0.02;
                        Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);

                        //Convert back to MatOfPoint
                        MatOfPoint points = new MatOfPoint(approxCurve.toArray());

                        // Get bounding rect of contour
                        Rect rect = Imgproc.boundingRect(points);

                        if (contourIdx == pontoInicial) {
                            Imgproc.putText(mRgba, "4", new Point(rect.x, rect.y), 3, 1, new Scalar(0, 0, 0, 255), 2);
                            pointsAndValues.add(new PointValue(new Point(rect.x + rect.width / 2, rect.y + rect.height / 2), "4"));
//                        Imgproc.rectangle(mRgba, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255, 0, 255), 3);
                        } else if (rect.width > rect.height * 1.5 || rect.height > rect.width * 1.5) {
//                        Imgproc.putText(mRgba, "0", new Point(rect.x, rect.y), 3, 1, new Scalar(0, 0, 0, 255), 2);
                            pointsAndValues.add(new PointValue(new Point(rect.x + rect.width / 2, rect.y + rect.height / 2), "0"));
//                        Imgproc.rectangle(mRgba, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 0, 255, 255), 3);
                        } else if (paisVazados.contains(contourIdx)) {
//                        Imgproc.putText(mRgba, "1", new Point(rect.x, rect.y), 3, 1, new Scalar(0, 0, 0, 255), 2);
                            pointsAndValues.add(new PointValue(new Point(rect.x + rect.width / 2, rect.y + rect.height / 2), "1"));
//                        Imgproc.rectangle(mRgba, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255, 255, 255), 3);
                        } else if (!paisVazados.contains(contourIdx)) {
//                        Imgproc.putText(mRgba, "2", new Point(rect.x, rect.y), 3, 1, new Scalar(0, 0, 0, 255), 2);
                            pointsAndValues.add(new PointValue(new Point(rect.x + rect.width / 2, rect.y + rect.height / 2), "2"));
//                        Imgproc.rectangle(mRgba, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(255, 0, 0, 255), 3);
                        }
                    }
                }

                if (pointsAndValues.size() > 0) {
                    String text = decodeText(pointsAndValues);
                    String novoTexto = convertFromBase3(text);
                    if (novoTexto.matches("[a-zA-Z0-9]+")) {
                        textoDecode = novoTexto;
                    }
                }
            }

            Imgproc.putText(mRgba, textoDecode, new Point(20, 100), 3, 3, new Scalar(0, 0, 255, 255), 2);
        }

        return mRgba;
    }

    private Point getCenterContour(MatOfPoint contour)
    {
        Rect bound = Imgproc.boundingRect(contour);
        return new Point(bound.x + bound.width / 2f, bound.y + bound.height / 2f);
    }

    private String convertFromBase3(String text) {
        String textoFinal = "";
        while (text.length() > 0) {
            if (text.length() > 5) {
                String texto = text.substring(0, 6);
                Log.println(Log.INFO, "", "texto: " + texto);
                text = text.substring(6);
                if (!texto.contains("4")) {
                    textoFinal += Character.toString((char) Integer.parseInt(texto, 3));
                }
            } else {
                break;
            }
        }

        return removeLeadingChar(textoFinal, '0');
    }

    private static String removeLeadingChar(String s, char c) {
        int i;
        for(i = 0; i < s.length() && s.charAt(i) == c; ++i);
        return s.substring(i);
    }

    private String decodeText(ArrayList<PointValue> points) {
        try {
            Point pontoAtual = null;
            for (int i = 0; i < points.size(); i++) {
                if (points.get(i).value == "4") {
                    pontoAtual = points.get(i).point;
                    points.remove(i);
                    break;
                }
            }

            String retorno = "";

            while (points.size() > 0) {
                int indexClosest = getIndexClosestPoint(pontoAtual, points);
                PointValue closestPoint = points.get(indexClosest);
                pontoAtual = closestPoint.point;
                retorno += closestPoint.value;
                points.remove(indexClosest);
            }

            return retorno;
        } catch (Exception e) {
            return "";
        }
    }

    private int getIndexClosestPoint(Point point, ArrayList<PointValue> listPoints) {
        double distanceClosestPoint = 9999999;
        int indexClosestPoint = -1;

        for (int i = 0; i < listPoints.size(); i++) {
            if (listPoints.get(i) != null && listPoints.get(i).point != null) {
                double distance = Math.sqrt(Math.pow(point.x - listPoints.get(i).point.x, 2) + Math.pow(point.y - listPoints.get(i).point.y, 2));
                if (distance < distanceClosestPoint) {
                    distanceClosestPoint = distance;
                    indexClosestPoint = i;
                }
            }
        }

        return indexClosestPoint;
    }

    private class PointValue
    {
        private Point point;
        private String value;

        public PointValue(Point point, String value)
        {
            this.point = point;
            this.value = value;
        }

        public Point getPoint() {
            return point;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "Point: " + point.toString() + " - Value: " + value;
        }
    }
}

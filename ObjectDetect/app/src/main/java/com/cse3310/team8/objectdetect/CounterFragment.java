package com.cse3310.team8.objectdetect;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorMOG;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * A simple {@link android.support.v4.app.Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link com.cse3310.team8.objectdetect.CounterFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link com.cse3310.team8.objectdetect.CounterFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CounterFragment extends Fragment implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String     TAG = "Counter::Fragment";

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.i(TAG, "opencv initialization failed");
        }
        else {
            Log.i(TAG, "opencv initialization successful");
        }
    }

    private OnFragmentInteractionListener mListener;

    private CameraBridgeViewBase    mOpenCvCameraView;
    private Mat                     mRgba;
    private Mat mGray;
    private BackgroundSubtractorMOG sub;
    private Mat                     mFGMask;
    private Mat                     mPyrDownMat;
    private Mat                     mHsvMat;
    private Mat                     mDilatedMask;
    private Mat                     mHierarchy;
    private int                     counter = 0;
    private List<MatOfPoint>        contours;
    private List<MatOfPoint>        mContours;
    private static double           mMinContourArea = 0.01;
    private int                     flag;



    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(super.getActivity()) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "CallBack loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public static CounterFragment newInstance() {
        CounterFragment fragment = new CounterFragment();
        return fragment;
    }

    public CounterFragment() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //mOpenCvCameraView = (CameraBridgeViewBase) super.getActivity().findViewById(R.id.surface_view_color_detect);
        //mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        //mOpenCvCameraView.setCvCameraViewListener(this);

        //**TESTING**
        /*
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        */
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getActivity().setRequestedOrientation(
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        View view = inflater.inflate(R.layout.fragment_counter, container, false);

        mOpenCvCameraView = (CameraBridgeViewBase) view.findViewById(R.id.surface_view_counter);
        mOpenCvCameraView.setCvCameraViewListener(this);
        // Inflate the layout for this fragment
        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    //**TESTING**
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        /*
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
        */
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
        //**TESTING**
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);

        //Async initialization with OPENCV Manager
        //OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, super.getActivity(), mLoaderCallback);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }



    //**TESTING**

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat();
        mFGMask = new Mat();

        //Cache
        mPyrDownMat = new Mat();
        mHsvMat = new Mat();
        mDilatedMask = new Mat();
        mHierarchy = new Mat();


        mContours = new ArrayList<MatOfPoint>();

        sub = new BackgroundSubtractorMOG(2, 2, 0.3, 0.5);



        Log.i(TAG, "height " + mRgba.height() + "width " + mRgba.width());

    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
        mFGMask.release();
        mGray.release();
        mPyrDownMat.release();
        mDilatedMask.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        Point point1 = new Point(1, mRgba.height() / 2);
        Point point2 = new Point(mRgba.width(), mRgba.height() / 2);

        //**TESTING**
        //mGray = inputFrame.gray();
        /*
        Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_RGBA2GRAY);

        mFGMask = inputFrame.rgba();

        Imgproc.cvtColor(mFGMask, mDilatedMask, Imgproc.COLOR_RGBA2GRAY);

        Core.absdiff(mGray, mDilatedMask, mPyrDownMat);



        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

        Imgproc.findContours(mPyrDownMat, contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        Log.i(TAG, "contours: " + contours.size());
        //Imgproc.threshold(mPyrDownMat, mHierarchy, 20, 255, Imgproc.THRESH_BINARY);
        */

        //**TESTING**
        Imgproc.pyrDown(mRgba, mPyrDownMat);
        Imgproc.pyrDown(mPyrDownMat, mPyrDownMat);

        //Imgproc.pyrDown(mRgba, mPyrDownMat);
        //Imgproc.pyrDown(mPyrDownMat, mPyrDownMat);
        //Imgproc.pyrDown(mPyrDownMat, mPyrDownMat);

        //Imgproc.cvtColor(mRgba, mHsvMat, Imgproc.COLOR_RGB2HSV_FULL);
        Imgproc.cvtColor(mPyrDownMat, mGray, Imgproc.COLOR_RGB2GRAY);

        //Imgproc.cvtColor(mPyrDownMat, mHsvMat, Imgproc.COLOR_RGB2HSV_FULL);
        //Imgproc.cvtColor(mPyrDownMat, mHsvMat, Imgproc.COLOR_RGB2HSV);
        //Imgproc.cvtColor(mPyrDownMat, mGray, Imgproc.COLOR_RGB2GRAY);



        sub.apply(mGray, mFGMask, 0);

        //**TESTING**
        //Imgproc.erode(mFGMask, mFGMask, new Mat());
        //Imgproc.dilate(mFGMask, mDilatedMask, new Mat());

        Imgproc.blur(mFGMask, mFGMask, new Size(10, 10));
        Imgproc.threshold(mFGMask, mDilatedMask, 20, 255, Imgproc.THRESH_BINARY);

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

        Imgproc.findContours(mDilatedMask, contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        double maxArea = 0;
        Iterator<MatOfPoint> each = contours.iterator();
        while (each.hasNext()) {
            MatOfPoint wrapper = each.next();
            double area = Imgproc.contourArea(wrapper);
            if (area > maxArea)
                maxArea = area;
        }
        Log.i(TAG, "Max area: " + maxArea);
        // Filter contours by area and resize to fit the original image size
        mContours.clear();
        each = contours.iterator();
        while (each.hasNext()) {
            MatOfPoint contour = each.next();
            Log.i(TAG, "Contours area: " + Imgproc.contourArea(contour));
            if (Imgproc.contourArea(contour) > mMinContourArea*maxArea) {
                Log.i(TAG, "Inside countour area checker");
                Log.i(TAG, "new bounds: " + mMinContourArea*maxArea);
                Core.multiply(contour, new Scalar(4,4), contour);
                mContours.add(contour);
            }
        }
        Log.i(TAG, "Contours count: " + contours.size());
        Log.i(TAG, "mContours count: " + mContours.size());

        MatOfPoint2f approxCurve = new MatOfPoint2f();

        //**TESTING**
        Core.putText(mRgba, "Count: " + counter, new Point(point1.x, point1.y-10), Core.FONT_HERSHEY_PLAIN, 3, new Scalar(255, 255, 0, 255), 3);
        Core.line(mRgba, point1, point2, new Scalar(255, 255, 0, 255), 5);

        for (int i=0; i<mContours.size(); i++) {
            Log.i(TAG, "Inside contour drawer loop");
            //Convert contours(i) from MatOfPoint to MatOfPoint2f
            MatOfPoint2f contour2f = new MatOfPoint2f( mContours.get(i).toArray() );
            flag = 0;

            //Processing on mMOP2f1 which is in type MatOfPoint2f
            double approxDistance = Imgproc.arcLength(contour2f, true)*0.02;
            Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);

            //Convert back to MatOfPoint
            MatOfPoint points = new MatOfPoint( approxCurve.toArray() );

            //Get bounding rect of contour
            Rect rect = Imgproc.boundingRect(points);
            Point center = new Point(rect.tl().x + rect.width/2, rect.tl().y + rect.height/2);

            //**TESTING**
            Core.circle(mRgba, center, 4, new Scalar(255, 0, 0, 255), 5);
            //Core.putText(mRgba, "center", center, Core.FONT_HERSHEY_PLAIN, 2, new Scalar(255, 0, 0, 255), 3);
            //draw enclosing rectangle (all same color, but you could use variable i to make them unique)
            //Point((rect.x+rect.width, rect.y+rect.height) , new Scalar(255, 0, 0, 255), 3);
            Core.rectangle(mRgba, rect.tl(), rect.br(), new Scalar(0, 0, 255, 255), 3, 8, 0);
            if (flag == 0) {
                if ( (center.y < point1.y+25) && (center.y > point1.y-25) ) {
                    counter+=1;
                    flag = 1;
                    Core.putText(mRgba, "Count: " + counter, new Point(point1.x, point1.y-10), Core.FONT_HERSHEY_PLAIN, 3, new Scalar(255, 0, 0, 255), 3);
                    Core.line(mRgba, point1, point2, new Scalar(255, 0, 0, 255), 5);

                }
            }




            //Core.rectangle(mRgba, rect.tl(), rect.br(), CONTOUR_COLOR, -1);

        }



        Log.i(TAG, "point1(x,y): (" + point1.x + ", " + point1.y +
                ") point2(x,y): (" + point2.x + ", " + point2.y + ")");



        //Imgproc.drawContours(mRgba, mContours, 3, new Scalar(255, 0, 0, 255), 3);
        //sub.apply(mGray, mFGMask, 0.5);
        return mRgba;
        /*
        sub.apply(mHsvMat, mFGMask, 0.9);
        //Core.inRange(mHsvMat, new Scalar(0,0, 0, 0), new Scalar(255, 255, 255, 255), mFGMask);

        Imgproc.erode(mFGMask, mFGMask, new Mat());
        Imgproc.dilate(mFGMask, mDilatedMask, new Mat());

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

        Imgproc.findContours(mDilatedMask, contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);




        // Find max contour area
        double maxArea = 0;
        Iterator<MatOfPoint> each = contours.iterator();
        while (each.hasNext()) {
            MatOfPoint wrapper = each.next();
            double area = Imgproc.contourArea(wrapper);
            if (area > maxArea)
                maxArea = area;
        }

        // Filter contours by area and resize to fit the original image size
        mContours.clear();
        each = contours.iterator();
        while (each.hasNext()) {
            MatOfPoint contour = each.next();
            if (Imgproc.contourArea(contour) > mMinContourArea*maxArea) {
                Core.multiply(contour, new Scalar(10,10), contour);
                mContours.add(contour);
            }
        }


        //Counter Line
        Point point1 = new Point(1, mRgba.height() / 2);
        Point point2 = new Point(mRgba.width(), mRgba.height() / 2);

        Log.i(TAG, "point1(x,y): (" + point1.x + ", " + point1.y +
                ") point2(x,y): (" + point2.x + ", " + point2.y + ")");


        Core.putText(mRgba, "Count: " + counter, new Point(point1.x, point1.y-10), Core.FONT_HERSHEY_PLAIN, 2, new Scalar(255, 255, 0, 255));
        Core.line(mRgba, point1, point2, new Scalar(255, 255, 0, 255), 5);

        //TESTING

        MatOfPoint2f approxCurve = new MatOfPoint2f();

        for (int i=0; i<contours.size(); i++)
        {
            //Convert contours(i) from MatOfPoint to MatOfPoint2f
            MatOfPoint2f contour2f = new MatOfPoint2f( contours.get(i).toArray() );
            //Processing on mMOP2f1 which is in type MatOfPoint2f
            double approxDistance = Imgproc.arcLength(contour2f, true)*0.02;
            Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);

            //Convert back to MatOfPoint
            MatOfPoint points = new MatOfPoint( approxCurve.toArray() );

            // Get bounding rect of contour
            Rect rect = Imgproc.boundingRect(points);

            // draw enclosing rectangle (all same color, but you could use variable i to make them unique)
            //Point((rect.x+rect.width, rect.y+rect.height) , new Scalar(255, 0, 0, 255), 3);
            Core.rectangle(mRgba, rect.tl(), rect.br(), new Scalar(255, 0, 0, 255), 3, 8, 0);


            //Core.rectangle(mRgba, rect.tl(), rect.br(), CONTOUR_COLOR, -1);
        }

        //contours = mContours;
        //mColorDetector.process(mRgba);
        //List<MatOfPoint> contours = mColorDetector.getContours();
        //contours.clear();
        //mGray = inputFrame.gray();

        //MatOfPoint2f approxCurve = new MatOfPoint2f();

        //Imgproc.cvtColor(mGray, mGray, Imgproc.COLOR_GRAY2RGB);





        //sub.apply(mGray, mFGMask, 0.8);

        //Imgproc.erode(mFGMask, mFGMask, new Mat());
        //Imgproc.dilate(mFGMask, mFGMask, new Mat());

        //Imgproc.findContours(mFGMask, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);

        Log.i(TAG, "Contours count: " + mContours.size());

        return mRgba;*/
    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }

}

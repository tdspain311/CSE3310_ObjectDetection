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
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorMOG;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link android.app.Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link EdgeDetectionFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link EdgeDetectionFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class EdgeDetectionFragment extends Fragment implements CameraBridgeViewBase.CvCameraViewListener2{

    private static final String     TAG = "MotionTrack::Fragment";

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
    private Mat                     mIntermediate;
    //private BackgroundSubtractorMOG sub;
    private Mat                     mGray;
    //private Mat                     mRgb;
    private Mat                     mFGMask;
    private List<MatOfPoint>        contours;
    //private double                  lRate = 0.5;

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

    public static EdgeDetectionFragment newInstance() {
        EdgeDetectionFragment fragment = new EdgeDetectionFragment();

        return fragment;
    }

    public EdgeDetectionFragment() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //**TESTING**
    }

    //@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getActivity().setRequestedOrientation(
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        View view = inflater.inflate(R.layout.fragment_edge_detection, container, false);

        mOpenCvCameraView = (CameraBridgeViewBase) view.findViewById(R.id.surface_view_edge_detection);
        mOpenCvCameraView.setCvCameraViewListener(this);

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
        mIntermediate = new Mat(height, width, CvType.CV_8UC4);

        //**TESTING**
        //creates a new BackgroundSubtractorMOG class with the arguments
        //sub = new BackgroundSubtractorMOG(3, 4, 0.8, 0.5);

        //creates matrices to hold the different frames
        //mRgba = new Mat();
        //mFGMask = new Mat();
        mGray = new Mat();

        //arraylist to hold individual contours
        //contours = new ArrayList<MatOfPoint>();

        Log.i(TAG, "height " + mRgba.height() + "width " + mRgba.width());

    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        Imgproc.Canny(mGray, mIntermediate, 80, 100);
        Imgproc.cvtColor(mIntermediate, mRgba, Imgproc.COLOR_GRAY2RGBA, 4);

        return mRgba;
        //**TESTING**
        //contours.clear();
        //gray frame because it requires less resource to process
        //mGray = inputFrame.gray();

        //this function converts the gray frame into the correct RGB format for the BackgroundSubtractorMOG apply function
        //Imgproc.cvtColor(mGray, mGray, Imgproc.COLOR_GRAY2RGB);

        //apply detects objects moving and produces a foreground mask
        //sub.apply(mGray, mFGMask, 0.8);


        //erode and dilate are used  to remove noise from the foreground mask
        //Imgproc.erode(mFGMask, mFGMask, new Mat());
        //Imgproc.dilate(mFGMask, mFGMask, new Mat());

        //drawing contours around the objects by first called findContours and then calling drawContours
        //RETR_EXTERNAL retrieves only external contours
        //CHAIN_APPROX_NONE detects all pixels for each contour

        //draws all the contours in red with thickness of 2


        //mRgba.convertTo(mRgba, CvType.CV_8UC4);


        //return mRgba;
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

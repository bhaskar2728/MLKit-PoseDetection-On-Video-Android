# MLKit-PoseDetection-On-Video-Android
Implementing the Google ML Kit Pose Detection on stored video file in Android Studio

Hello everyone, Here is one of the way through which you can implement Google MLKit Pose Detection on a stored video file in your phone.

So I basically used  the play movie on texture view feature of https://github.com/google/grafika repository,where in the decoder decodes the video and plays it on the texture view.And then I implemented SurfaceTextureListener on TextureView.

The onSurfaceTextureUpdated function calls everytime we get a new frame or new screen from video, so we can get the image from the texture view everytime the function runs and then make an InputImage from that and sending it to the Pose Detection Model.

In the onSuccessListener of pose model, we can draw overlay.

I hope this helps.

Hope to hear from you as your feedback is important for this project to improve

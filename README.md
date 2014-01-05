InstaChatX
==========

![ScreenShot](https://raw.github.com/erikswed/InstaChatX/master/InstaChatX/Device1.png) ..  ![ScreenShot](https://raw.github.com/erikswed/InstaChatX/master/InstaChatX/Device2.png)


Originally the Appsrox.com IM using GCM but now with AppCompat and more

This is to start with a Clone of the appsrox.com Instant Messaging app using Google Cloud Messaging (GCM). (clone of source code that was included in the apk). 

This is a basic GCM chat app using a GAE engine. The web server has one Table two column db for email and RegistrationId. The gui has chat bubbles and notifications. ContentProver and Cursor Loaders are handling the chat. It's a great skeleton to build on and you will learn basic core android functionality. 

My changes in first commit is adding:
- ActionBarActivity 
- android-support-v4.jar
- minSdkVersion 9
- extends CursorAdapter
- lazy loading of contact photos


To run this you need: 
- Demo ServerX located here
- android-support-v7-appcompat.jar (create library project)
- android-support-v4.jar (included)

Don't forget to change InstaChat Constants.java and Demo server Constants.java with your API KEY and project id

To smoothen the debugging I suggest

Create two emulators (genymotion). Have only one e-mail account on each emulator and then try it. monitor stuff like: Login to appengine.google.com and look at db row insertions. Pay attention to Consol output (eclipse). Create Log lines and debug since part of the code is swallowing errors. Also check the web server log at appengine.google.com. Eclipse when you debug the Web server locally right click "Terminate" for stopping it. Always do that before start another debug session. 

Web server ip address on your developer machine if you using Genymotion
http://bbowden.tumblr.com/post/58650831283/accessing-a-localhost-server-from-the-genymotion

source tutorial:
http://www.appsrox.com/android/tutorials/instachat/#

## Compose based chat app

This is my second ever modern Android application, based on Kotlin, View-Model-ViewModel and Jetpack Compose. For my first modern 
app, see [note-composer](https://github.com/Gtomika/note-composer).

Using Firebase as a backend for authentication and storage.

## Screenshots

<table>
  <tr>
    <td> <img src="screenshots/screenshot_login.png"  alt="1" width = 360px height = 640px ></td>

    <td><img src="screenshots/screenshot_register.png" alt="2" width = 360px height = 640px></td>
   </tr> 
   <tr>
      <td><img src="screenshots/screenshot_chats.png" alt="3" width = 360px height = 640px></td>

      <td><img src="screenshots/screenshot_chat.png" align="right" alt="4" width = 360px height = 640px></td>
  </tr>
   <tr>
      <td><img src="screenshots/screenshot_search.png" alt="5" width = 360px height = 640px></td>

      <td><img src="screenshots/screenshot_profile.png" align="right" alt="6" width = 360px height = 640px></td>
  </tr>
</table>

## Not intended for Google Play Store

This is a small scale app, intended for family-only use, and not to be deployed on Google Play Store. 
It only has hungarian language support.

## Build

To build this app, you must set up your own Firebase project, then:

 - Copy your *google-services.json* file to the *app* folder.
 - For cloud functions, copy your *admin service account JSON file* to functions folder and rename it *adminServiceAccountKey.json*

Furethermore, your Firebase project must have **Blaze** plan, since this project uses Cloud Functions.
const functions = require("firebase-functions");
const admin = require('firebase-admin');
const path = require('path');
const serviceAccount = require(path.resolve(__dirname, 'adminServiceAccountKey.json'))

admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();


// // Create and Deploy Your First Cloud Functions
// // https://firebase.google.com/docs/functions/write-firebase-functions
//
// exports.helloWorld = functions.https.onRequest((request, response) => {
//   functions.logger.info("Hello logs!", {structuredData: true});
//   response.send("Hello from Firebase!");
// });

/*
Listen when a Chat Room document gets a new message.
- Collection: chatRooms
- Document index: chat room UID (this is wildcarded)
- Sub-collection: chatRoomMessages
- Document index: message UID (this is wildcarded)
*/
exports.chatRoomNewMessage = functions
    .region('europe-west1')
    .firestore
    //specify wildcarded path to a document (a message)
    .document('chatRooms/{chatRoomUid}/chatRoomMessages/{messageUid}')
    //onCreate: called when message is inserted
    // - snap: The new document that was created (a message)
    // - context: used to access wildcard values, etc...
    .onCreate((snap, context) => {
        //this chat room received a new message
        const chatRoomUid = context.params.chatRoomUid
        console.log('New message cloud function: new message detected in ' + chatRoomUid)
        //the new message object
        const newMessage = snap.data()
        //get a document reference to the parent, a Chat Room
        const chatRoomRef = db.collection('chatRooms').doc(chatRoomUid)
        //get this document asynchronously
        chatRoomRef.get().then(chatRoomSnap => {
            //chat room object
            const chatRoom = chatRoomSnap.data()
            //get ref to each user and send them FCM message
            if(chatRoom.chatRoomUsers) {
                if(chatRoom.chatRoomUsers.length == 2) {
                    console.log('This message is in a One-to-One chat, sending FCM message to other user!')
                    //2 users (one-to-one), only send message to the other
                    const otherUserUid = getOtherUserUid(chatRoom.chatRoomUsers, newMessage.senderUid)

                    db.collection('users').doc(otherUserUid).get().then(userDoc => {
                        if(userDoc.exists) {
                            console.log('Other user queried, their display name is ' + userDoc.data().displayName + ', their token is ' + userDoc.data().messageToken)

                            const notification = buildNotificationForOneToOne(
                                userDoc.data().messageToken, //who to send to
                                chatRoomUid, //which chatroom got the message
                                newMessage.senderName,  //who was the sender
                                newMessage.messageText //what was the message text
                            )

                            admin.messaging().send(notification).then(response => { //response is a FCM message id, not used
                                console.log('Other user received the message!')
                            })
                            .catch(error => {
                                console.log('Failed to deliver FCM message: ' + error)
                            })
                        }
                    })
                } else {
                    console.log('This message is in a Group chat, sending message to all other group members!')
                    //more then 2 users (GROUP): get all users except the one who sent the message
                    const otherUserUids = getOtherUserUids(chatRoom.chatRoomUsers, newMessage.senderUid)
                    db.collection('users').where('uid', 'in', otherUserUids).get().then(usersSnapshot => {
                        if(!usersSnapshot.empty) {
                            //get tokens of these other users
                            const otherUserTokens = []
                            usersSnapshot.forEach( otherUserDoc => {
                                otherUserTokens.push(otherUserDoc.data().messageToken)
                            })
                            console.log('Collected ' + (otherUserTokens.length) + ' other users who will receive FCM message!')

                            //build and send notifications
                            const notification = buildNotificationForGroup(
                                otherUserTokens, //which users to send to
                                chatRoomUid, //which chat room got the message
                                chatRoom.chatRoomName, //name of the chat room
                                newMessage.messageText //message text
                            )

                            admin.messaging().sendMulticast(notification).then((response) => {
                                console.log(response.successCount + ' messages were sent successfully!');
                            });
                        }
                    })
                }
            }
        })
    });

    //users list assumed to be 2 long
    function getOtherUserUid(userUids, senderUid) {
        if(userUids[0] === senderUid) {
            return userUids[1]
        } else {
            return userUids[0]
        }
    }

    //users list is assumed to be more then 2 long
    function getOtherUserUids(userUids, senderUid) {
        otherUserUids = []
        for(userUid in userUids) {
            if(userUid !== senderUid) {
                otherUserUids.push(userUid)
            }
        }
    }

    //Returns the notification that is sent with FCM to a message token
    function buildNotificationForOneToOne(_token, _chatRoomUid, otherUserName, messageText) {
        const message = {
            notification: {
                title: otherUserName,
                body: messageText
            },
            data: {
                chatRoomUid: _chatRoomUid
            },
            token: _token
        }
        return message
    }

    //Returns the notification that is sent with FCM to a list of message tokens
    function buildNotificationForGroup(_tokens, _chatRoomUid, chatRoomName, messageText) {
        const message = {
            notification: {
                title: chatRoomName,
                body: messageText
            },
            data: {
                chatRoomUid: _chatRoomUid
            },
            tokens: _tokens
        }
        return message
    }

const functions = require("firebase-functions");
const admin = require('firebase-admin');
const path = require('path');
const serviceAccount = require(path.resolve(__dirname, 'adminServiceAccountKey.json'))

admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();
db.settings({
    ignoreUndefinedProperties: true
})

/*
Listen when a new group is created.
 - Collection: chatRooms
 - Document index: chat room UID (wildcarded)
*/
exports.newGroupCreated = functions
    .region('europe-west1')
    .firestore
    .document('chatRooms/{chatRoomUid}')
    .onCreate((chatRoomSnap, context) => {
        //UID of created chat room
        const chatRoomUid = context.params.chatRoomUid
        //actual object
        const chatRoom = chatRoomSnap.data()
        //only proceed if this is a group
        if(chatRoom.group && chatRoom.chatRoomUsers.length > 0) {
            //query all the users who are in this group
            return db.collection('users').where('uid', 'in', chatRoom.chatRoomUsers).get().then(usersSnapshot => {
                if(!usersSnapshot.empty) {
                    //collect tokens of other uses who are not blocked by the admin in this list
                    const tokens = []
                    //collect the name of the admin in this variable
                    let adminName = ''
                    usersSnapshot.forEach(userDoc => {
                        if(userDoc.data().uid === chatRoom.admin) {
                            //this is the admin, save his name
                            adminName = userDoc.data().displayName
                        } else {
                            //this is not the admin, if he did not block the admin, send notification
                            if(!isBlockedBy(userDoc.data(), chatRoom.admin)) {
                                tokens.push(userDoc.data().messageToken)
                            }
                        }
                    })
                    //create notification
                    const notification = buildNewGroupNotification(
                        chatRoomUid,
                        chatRoom.chatRoomName,
                        adminName,
                        tokens
                    )
                    //send notification
                    return admin.messaging().sendMulticast(notification).then((response) => {
                        //console.log(response.successCount + ' messages were sent successfully!');
                        return true
                    }).catch(error => {
                        console.log('Failed to deliver FCM messages to NEW group members: ' + error)
                        return false
                    });
                } else {
                    console.log('No members found for new group ' + chatRoom.chatRoomName + '! This is a database error!')
                    return false
                }
            })
        } else {
            return false
        }
    });

function buildNewGroupNotification(_chatRoomUid, chatRoomName, adminName, _tokens) {
    const message = {
        notification: {
            title: 'Új csoport: ' + chatRoomName,
            body: adminName + ' felvett ebbe a csoportba!'
        },
        data: {
            chatRoomUid: _chatRoomUid,
            messageType: "new_group"
        },
        tokens: _tokens
    }
    return message
}

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
        //console.log('New message cloud function: new message detected in ' + chatRoomUid)
        //the new message object
        const newMessage = snap.data()
        //get a document reference to the parent, a Chat Room
        const chatRoomRef = db.collection('chatRooms').doc(chatRoomUid)
        //get this document asynchronously
        return chatRoomRef.get().then(chatRoomSnap => {
            //chat room object
            const chatRoom = chatRoomSnap.data()
            //get ref to each user and send them FCM message
            if(chatRoom.chatRoomUsers) {
                if(!chatRoom.group) {
                    //console.log('This message is in a One-to-One chat, sending FCM message to other user!')
                    //(one-to-one), only send message to the other
                    const otherUserUid = getOtherUserUid(chatRoom.chatRoomUsers, newMessage.senderUid)

                    return db.collection('users').doc(otherUserUid).get().then(userDoc => {
                        if(userDoc.exists) {
                            //console.log('Other user queried, their display name is ' + userDoc.data().displayName + ', their token is ' + userDoc.data().messageToken)

                            //dont send notification if the target blocked the message sender
                            if(isBlockedBy(userDoc.data(), newMessage.senderUid)) {
                                console.log('Other user has blocked the message sender, NOT sending notification!')
                                return true
                            }

                            const notification = buildNotificationForOneToOne(
                                userDoc.data().messageToken, //who to send to
                                chatRoomUid, //which chatroom got the message
                                newMessage.senderName,  //who was the sender
                                newMessage.messageText //what was the message text
                            )

                            return admin.messaging().send(notification).then(response => { //response is a FCM message id, not used
                                //console.log('Other user received the message!')
                                return true
                            })
                            .catch(error => {
                                console.log('Failed to deliver FCM message: ' + error)
                                return false
                            })
                        } else {
                            console.log('Failed to find chat partner in user collection! This is a database error.')
                            return false
                        }
                    })
                } else {
                    //console.log('This message is in a Group chat, sending message to other ' + otherUserUids.length + ' users!')
                    return db.collection('users').where('uid', 'in', chatRoom.chatRoomUsers).get().then(usersSnapshot => {
                        if(!usersSnapshot.empty) {
                            //get tokens of these other users
                            const otherUserTokens = []
                            let senderName = ''
                            usersSnapshot.forEach( otherUserDoc => {
                                if(otherUserDoc.data().uid === newMessage.senderUid) {
                                    //find and save the name of the sender
                                    senderName = otherUserDoc.data().displayName
                                } else {
                                    //not the sender
                                    if(!isBlockedBy(otherUserDoc.data(), newMessage.senderUid)) {  //dont send notification if the target blocked the message sender
                                        if(otherUserDoc.data().messageToken !== '') {
                                            otherUserTokens.push(otherUserDoc.data().messageToken)
                                        }
                                    }
                                }
                            })
                            //console.log('Collected ' + (otherUserTokens.length) + ' other users who will receive FCM message (users who blocked the sender are not included)!')

                            //build and send notifications
                            const notification = buildNotificationForGroup(
                                otherUserTokens, //which users to send to
                                chatRoomUid, //which chat room got the message
                                chatRoom.chatRoomName, //name of the chat room
                                senderName + ': ' + newMessage.messageText //message text
                            )

                            return admin.messaging().sendMulticast(notification).then((response) => {
                                //console.log(response.successCount + ' messages were sent successfully!');
                                return true
                            }).catch(error => {
                                console.log('Failed to deliver FCM messages to group members: ' + error)
                                return false
                            });
                        } else {
                            console.log('Failed to find other group members in user collection! This is a database error.')
                            return false
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

//Excludes second parameter from the list (first parameter)
function getOtherUserUids(userUids, senderUid) {
    otherUserUids = []
    for(const userUid of userUids) {
        if(userUid === senderUid) {
            continue
        }
        otherUserUids.push(userUid)
    }
    return otherUserUids
}

//check if user has otherUserUid blocked
function isBlockedBy(user, otherUserUid) {
    for(const blockedUserUid of user.blockedUsers) {
        if(blockedUserUid === otherUserUid) {
            return true
        }
    }
    return false
}

//Returns the notification that is sent with FCM to a message token
function buildNotificationForOneToOne(_token, _chatRoomUid, otherUserName, messageText) {
    const message = {
        notification: {
            title: otherUserName,
            body: messageText
        },
        data: {
            chatRoomUid: _chatRoomUid,
            messageType: "new_message"
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
            chatRoomUid: _chatRoomUid,
            messageType: "new_message"
        },
        tokens: _tokens
    }
    return message
}

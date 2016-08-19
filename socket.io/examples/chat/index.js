// Number Guess Demo
// 有一个user是庄，输入一个数字，让其他user依次猜，猜中的user算输，
// 每次猜之前，后台返回正确数字的范围，其他user只能在这个范围内（全闭区间）猜。

// Setup basic express server
var express = require('express');
var app = express();
var server = require('http').createServer(app);
var io = require('../..')(server);
var port = process.env.PORT || 3000;

server.listen(port, function () {
  console.log('Server listening at port %d', port);
});

// Routing
app.use(express.static(__dirname + '/public'));

// Chatroom

var numUsers = 0;
// Fixme 第一个登陆的user为庄
var isKingSelected = false;
var TrueNumber = 0;
var LeftBoundNumber = 0;
var RightBoundNumber = 100;

io.on('connection', function (socket) {
  var addedUser = false;

  // when the client emits 'new message', this listens and executes
  socket.on('new message', function (data) {
    // we tell the client to execute 'new message'
    socket.broadcast.emit('new message', {
      isLog: false,
      username: socket.username,
      message: data
    });

  });

  // 增加一个庄家给数字的监听和玩家猜数字的监听
  socket.on('give number',function (data) {
    console.log('give number:'+data);
    TrueNumber = data;
    // FIXME 庄家给定数字后，需要通知玩家猜数字

  });

  //在这里拿到玩家猜的数字，然后判断是否和庄家给的数字相同，
  //若相同，则此局比赛结束，否则下发一个广播log，通知下一个猜数字的范围。
  socket.on('guess number',function (data) {
    console.log('guess number:'+data);
     
    var msg; 
    if(data===TrueNumber){
      msg = 'Lose! The Number is '+TrueNumber;
    }else if(data<TrueNumber){
      var leftNumber = data + 1;
      msg = '['+leftNumber +','+RightBoundNumber+']';
      LeftBoundNumber = leftNumber;
    }else{
      var rightNumber = data - 1;
      msg = '['+LeftBoundNumber+','+rightNumber+']';
      RightBoundNumber = rightNumber;
    } 
    console.log('number bound:'+ msg);

    //FIXME 需要把边界数字实时传给user，做为在前端的限定。

    // 发给其他人和自己
    socket.broadcast.emit('new message', {
      isLog: true,
      username: socket.username,
      message: msg
    });
    socket.emit('new message', {
      isLog: true,
      username: socket.username,
      message: msg
    });
  });

  // when the client emits 'add user', this listens and executes
  socket.on('add user', function (username) {
    console.log('add user: username='+username);

    if (addedUser) return;

    // we store the username in the socket session for this client
    socket.username = username;
    ++numUsers;
    addedUser = true;
    socket.emit('login', {
      numUsers: numUsers,
      isKing: !isKingSelected
    });
    isKingSelected = true;

    if(numUsers===1){
      // Fixme 需要延时给庄家发送
      socket.emit('new message', {
        isLog: true,
        message: '输入一个0-100内的数'
      });
    }

    // echo globally (all clients) that a person has connected
    socket.broadcast.emit('user joined', {
      username: socket.username,
      numUsers: numUsers
    });
  });


  // when the user disconnects.. perform this
  socket.on('disconnect', function () {
    if (addedUser) {
      --numUsers;

      // echo globally that this client has left
      socket.broadcast.emit('user left', {
        username: socket.username,
        numUsers: numUsers
      });
    }
  });
});

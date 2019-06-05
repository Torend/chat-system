var $messages = $("#messages"),
        $send = $("#send"),
        $message = $("#message"),
        connection = new WebSocket("@url");

//$send.prop("disabled", true);
$send.submit(function(){
alert("asdakd")
})
var send = function () {
    var text = $message.val();
    $message.val("");
    connection.send(text);
};

connection.onopen = function () {
    $send.prop("disabled", false);
    $messages.prepend($("<li class='bg-info' style='font-size: 1.5em'>Connected</li>"));
    $send.on('click', send);
    $message.keypress(function(event){
        var keycode = (event.keyCode ? event.keyCode : event.which);
        if(keycode == '13'){
            send();
        }
    });
};
connection.onerror = function (error) {
    console.log('WebSocket Error ', error);
};
connection.onmessage = function (event) {
    $messages.append($("<li style='font-size: 1.5em'>" + event.data + "</li>"))
}
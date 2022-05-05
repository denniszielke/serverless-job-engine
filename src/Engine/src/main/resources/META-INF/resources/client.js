(function() {
    var poll = function() {
        $.ajax({
          url: 'count',
          dataType: 'json',
          type: 'get',
          success: function(data) { // check if available
            $("#counter").text("Running: " + data.length);
            $("#hosts").empty();
            if (data){
                data.forEach(function(value){
                    $("#hosts").append('<li>' + value +'</li>');
                });
            }            
          },
          error: function() { // error logging
            console.log('Error!');
          }
        });
      },
      pollInterval = setInterval(function() { // run function every 2000 ms
        poll();
        }, 2000);
      poll(); // also run function on init
  })();

  $(document).ready(function(){
    $("button").click(function(){
      var text = $("#message").val();
      var uid = uuidv4();
      $.ajax({
        url: 'publish',
        contentType : 'application/json',
        dataType: 'json',
        data:  JSON.stringify({ "message": text, "guid": uid}),
        type: 'post',
        success: function(data) { // check if available
            $("#result").text("Result: " + data.message + " " + data.guid);  
        },
        error: function(e) { // error logging
            $("#result").text("Result: " + e.statusText);
        }
      });
    });
  });

function uuidv4() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
      var r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
      return v.toString(16);
  });
};

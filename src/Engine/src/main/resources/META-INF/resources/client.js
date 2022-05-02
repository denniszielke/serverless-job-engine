(function() {
    var poll = function() {
        $.ajax({
          url: 'count',
          dataType: 'json',
          type: 'get',
          success: function(data) { // check if available
            $("#counter").text("Running: " + data.Count);
            $("#hosts").empty();
            if (data.Hosts){
                data.Hosts.forEach(function(value){
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
      $.ajax({
        url: 'publish',
        contentType : 'application/json',
        dataType: 'json',
        data:  JSON.stringify({ "message": text}),
        type: 'post',
        success: function(data) { // check if available
            $("#result").text("Result: " + data);  
        },
        error: function(e) { // error logging
            $("#result").text("Result: " + e.statusText);
        }
      });
    });
  });
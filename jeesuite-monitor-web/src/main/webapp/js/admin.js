
$(function(){
	
	$('.admin-nav > li').on('click',function(){
		var $this = $(this);
		if($this.hasClass('active'))return;
		$this.addClass('active').siblings().removeClass('active');
	});
	
	$(".admin-nav a").on("click",function(){
		var loadUrl = $(this).attr('pagekey') + "?_t="+new Date().getTime();
		$('#maincontents').load(loadUrl, function() {
			registerEvent();
		});
	});
	
	//
	$('.J_sch_operator').on('click','#maincontents',function(){
		var $this = $(this),
		   group = $this.attr('data-group'),
		   job = $this.attr('data-job'),
		   event = $this.attr('data-event');
		$.ajax({
		     type: 'POST',
		     url: './scheduler/operator/'+event ,
		    data: {job:job,group:group} ,
		    success: function(json){
		    	alert(json.msg);
		    }

		});
	});
	
});

function registerEvent(){
	$(".collapse .panel-head").each(function() {
        var e = $(this);
        e.click(function() {
            e.closest(".collapse").find(".panel").removeClass("active");
            e.closest(".panel").addClass("active")
        })
    });
    $(".collapse-toggle .panel-head").each(function() {
        var e = $(this);
        e.click(function() {
            e.closest(".panel").toggleClass("active")
        })
    });
}